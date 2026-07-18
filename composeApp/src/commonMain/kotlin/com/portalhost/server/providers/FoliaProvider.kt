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
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.Result

class FoliaProvider : ServerProvider {
    override val id = "folia"
    override val name = "Folia"
    override val supportedTypes = setOf(ServerType.FOLIA)

    private val apiBase = "https://fill.papermc.io/v3/projects/folia"
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val USER_AGENT = "PortalHost/5.1.0 (https://github.com/portalhost/portalhost)"
    }

    @Serializable
    private data class FoliaProjectV3(
        val project: FoliaProjectInfo,
        val versions: Map<String, List<String>>
    )

    @Serializable
    private data class FoliaProjectInfo(val id: String, val name: String)

    @Serializable
    private data class FoliaBuildEntryV3(
        val id: Int,
        val time: String,
        val channel: String,
        val downloads: Map<String, FoliaDownloadV3>
    )

    @Serializable
    private data class FoliaDownloadV3(
        val name: String,
        val checksums: Map<String, String>? = null,
        val size: Long? = null,
        val url: String
    )

    override suspend fun fetchVersions(): Result<List<ServerVersion>> = withContext(Dispatchers.IO) {
        try {
            val url = URL(apiBase)
            val response = url.readTextWithTimeout(headers = mapOf("User-Agent" to USER_AGENT))
            val foliaResponse = json.decodeFromString<FoliaProjectV3>(response)

            Result.success(foliaResponse.versions.values.flatten()
                .filter { !it.contains("-") }
                .map { version ->
                    ServerVersion(
                        version = version,
                        stable = true,
                        releaseDate = null
                    )
                }
                .sortedWith(compareByDescending { parseSemver(it.version) })
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchBuilds(version: String): Result<List<ServerBuild>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$apiBase/versions/$version/builds")
            val response = url.readTextWithTimeout(headers = mapOf("User-Agent" to USER_AGENT))
            val buildsResponse = json.decodeFromString<List<FoliaBuildEntryV3>>(response)

            Result.success(buildsResponse
                .filter { it.channel == "STABLE" }
                .map { build ->
                    val download = build.downloads["server:default"]!!
                    ServerBuild(
                        id = build.id.toString(),
                        url = download.url,
                        sha256 = download.checksums?.get("sha256"),
                        size = download.size ?: 0
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
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", USER_AGENT)
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
