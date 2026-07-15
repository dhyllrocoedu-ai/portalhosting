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
import java.net.URL
import java.security.MessageDigest
import kotlin.Result
import kotlin.coroutines.coroutineContext

class PaperProvider : ServerProvider {
    override val id = "paper"
    override val name = "Paper"
    override val supportedTypes = setOf(ServerType.PAPER, ServerType.FOLIA)
    
    private val baseUrl = "https://api.papermc.io/v2/projects/paper"
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchVersions(): Result<List<ServerVersion>> = coroutineContext.runCatching {
        val url = URL("$baseUrl")
        val response = url.readText()
        val project = json.decodeFromString<PaperProject>(response)
        project.versions.filter { it.startsWith("1.") }
            .map { ServerVersion(version = it, stable = !it.contains("-"), releaseDate = null) }
            .reversed()
    }

    override suspend fun fetchBuilds(version: String): Result<List<ServerBuild>> = coroutineContext.runCatching {
        val url = URL("$baseUrl/versions/$version")
        val response = url.readText()
        val versionInfo = json.decodeFromString<PaperVersion>(response)
        versionInfo.builds.map { build ->
            ServerBuild(
                id = build.toString(),
                url = "$baseUrl/versions/$version/builds/$build/downloads/paper-$version-$build.jar",
                sha256 = null, // Paper doesn't provide SHA256 in API
                size = 0
            )
        }.reversed()
    }

    override suspend fun downloadBuild(build: ServerBuild, destination: File): Result<File> = coroutineContext.runCatching {
        destination.parentFile?.mkdirs()
        val url = URL(build.url)
        url.openStream().use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        }
        // Verify SHA256 if provided
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
    
    // Data classes for PaperMC API responses
    @Serializable
    private data class PaperProject(val project_name: String, val versions: List<String>)
    @Serializable
    private data class PaperVersion(val project_name: String, val version: String, val builds: List<Int>)
}
