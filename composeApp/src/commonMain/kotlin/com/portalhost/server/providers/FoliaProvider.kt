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

class FoliaProvider : ServerProvider {
    override val id = "folia"
    override val name = "Folia"
    override val supportedTypes = setOf(ServerType.FOLIA)
    
    private val apiBase = "https://api.papermc.io/v2/projects/folia"
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class FoliaVersionsResponse(
        val project_name: String,
        val project_id: String,
        val version_groups: List<VersionGroup>,
        val versions: List<String>,
        val builds: Map<String, PaperBuild>,
    )
    
    @Serializable
    data class VersionGroup(
        val name: String,
        val versions: List<String>,
        val promoted: Boolean,
    )
    
    @Serializable
    data class PaperBuild(
        val build: Int,
        val download: BuildDownload,
        val time: String,
        val channel: String,
    )
    
    @Serializable
    data class BuildDownload(
        val application: BuildApplication,
        val sha256: String,
    )
    
    @Serializable
    data class BuildApplication(
        val name: String,
        val sha256: String,
    )

    override suspend fun fetchVersions(): Result<List<ServerVersion>> = withContext(Dispatchers.IO) {
        try {
            val url = URL(apiBase)
            val response = url.readText()
            val foliaResponse = json.decodeFromString<FoliaVersionsResponse>(response)
            
            Result.success(foliaResponse.versions
                .map { version ->
                    ServerVersion(
                        version = version,
                        stable = foliaResponse.version_groups
                            .firstOrNull { vg -> version in vg.versions }?.promoted ?: false,
                        releaseDate = null
                    )
                }
                .sortedByDescending { it.version }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchBuilds(version: String): Result<List<ServerBuild>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$apiBase/versions/$version/builds")
            val response = url.readText()
            val foliaResponse = json.decodeFromString<FoliaVersionsResponse>(response)
            
            Result.success(foliaResponse.builds.values
                .filter { it.channel == "default" }
                .map { build ->
                    ServerBuild(
                        id = build.build.toString(),
                        url = "$apiBase/versions/$version/builds/${build.build}/downloads/${build.download.application.name}",
                        sha256 = build.download.sha256,
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
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 300000
            
            if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("Download failed: ${connection.responseCode}"))
            }
            
            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            connection.disconnect()
            
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
