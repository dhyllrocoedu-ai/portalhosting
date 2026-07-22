package com.portalhost.server.providers

import com.portalhost.model.ServerType
import com.portalhost.model.ServerVersion
import com.portalhost.model.ServerBuild
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.Result
import kotlin.coroutines.coroutineContext

class PaperProvider : ServerProvider {
    override val id = "paper"
    override val name = "Paper"
    override val supportedTypes = setOf(ServerType.PAPER)

    private val baseUrl = "https://fill.papermc.io/v3/projects/paper"
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val USER_AGENT = "PortalHost/5.0.40 (https://github.com/dhyllrocoedu-ai/portalhosting)"
    }

    override suspend fun fetchVersions(): Result<List<ServerVersion>> = coroutineContext.runCatching {
        val url = URL("$baseUrl")
        val response = url.readTextWithTimeout(headers = mapOf("User-Agent" to USER_AGENT))
        val project = json.decodeFromString<PaperProjectV3>(response)
        project.versions.values.flatten()
            .filter { !it.contains("-") }
            .map { ServerVersion(version = it, stable = true, releaseDate = null) }
            .sortedWith(compareByDescending { parseSemver(it.version) })
    }

    override suspend fun fetchBuilds(version: String): Result<List<ServerBuild>> = coroutineContext.runCatching {
        val url = URL("$baseUrl/versions/$version/builds")
        val response = url.readTextWithTimeout(headers = mapOf("User-Agent" to USER_AGENT))
        val buildsResponse = json.decodeFromString<List<PaperBuildEntryV3>>(response)
        buildsResponse
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
    }

    override suspend fun downloadBuild(build: ServerBuild, destination: File): Result<File> = coroutineContext.runCatching {
        destination.parentFile?.mkdirs()
        val url = URL(build.url)
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.connectTimeout = 30000
        conn.readTimeout = 300000
        conn.getInputStream().use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        }
        build.sha256?.let { expected ->
            val sha256 = calculateSha256(destination)
            if (sha256 != expected) {
                throw SecurityException("SHA256 mismatch: expected $expected, got $sha256")
            }
        }
        destination
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

    @Serializable
    private data class PaperProjectV3(
        val project: PaperProjectInfo,
        val versions: Map<String, List<String>>
    )

    @Serializable
    private data class PaperProjectInfo(val id: String, val name: String)

    @Serializable
    private data class PaperBuildEntryV3(
        val id: Int,
        val time: String,
        val channel: String,
        val downloads: Map<String, PaperDownloadV3>
    )

    @Serializable
    private data class PaperDownloadV3(
        val name: String,
        val checksums: Map<String, String>? = null,
        val size: Long? = null,
        val url: String
    )
}
