package com.portalhost.server.providers

import com.portalhost.model.ServerType
import com.portalhost.model.ServerVersion
import com.portalhost.model.ServerBuild
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest
import kotlin.Result

class PurpurProvider : ServerProvider {
    override val id = "purpur"
    override val name = "Purpur"
    override val supportedTypes = setOf(ServerType.PURPUR)
    
    private val apiBase = "https://api.purpurmc.org/v2/purpur"
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class PurpurVersionsResponse(
        val versions: List<String>,
    )
    
    @Serializable
    data class PurpurBuildsResponse(
        val builds: List<PurpurBuild>,
    )
    
    @Serializable
    data class PurpurBuild(
        val build: Int,
        val download: String,
        val sha256: String,
        val time: String,
    )

    override suspend fun fetchVersions(): Result<List<ServerVersion>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$apiBase/versions")
            val response = url.readTextWithTimeout()
            val versionsResponse = json.decodeFromString<PurpurVersionsResponse>(response)
            
            Result.success(versionsResponse.versions
                .map { ServerVersion(it, true, null) }
                .sortedByDescending { it.version }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchBuilds(version: String): Result<List<ServerBuild>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$apiBase/versions/$version/builds")
            val response = url.readTextWithTimeout()
            val buildsResponse = json.decodeFromString<PurpurBuildsResponse>(response)
            
            Result.success(buildsResponse.builds
                .map { build ->
                    ServerBuild(
                        id = build.build.toString(),
                        url = "$apiBase/versions/$version/builds/${build.build}/downloads/${build.download}",
                        sha256 = build.sha256,
                        size = 0
                    )
                }
                .sortedByDescending { it.id.toIntOrNull() ?: 0 }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadBuild(build: ServerBuild, destination: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            destination.parentFile?.mkdirs()
            val url = URL(build.url)
            val conn = url.openConnection()
            conn.connectTimeout = 30000
            conn.readTimeout = 300000
            conn.getInputStream().use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            // Verify SHA256 if provided
            build.sha256?.let { expectedSha ->
                val actualSha = calculateSha256(destination)
                if (actualSha != expectedSha) {
                    destination.delete()
                    return@withContext Result.failure(Exception("SHA256 mismatch: expected $expectedSha, got $actualSha"))
                }
            }
            Result.success(destination)
        } catch (e: Exception) {
            destination.delete()
            Result.failure(e)
        }
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        FileInputStream(file).use { input ->
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
