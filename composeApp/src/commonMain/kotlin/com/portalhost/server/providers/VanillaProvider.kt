package com.portalhost.server.providers

import com.portalhost.model.ServerType
import com.portalhost.model.ServerVersion
import com.portalhost.model.ServerBuild
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlin.Result

class VanillaProvider : ServerProvider {
    override val id = "vanilla"
    override val name = "Vanilla"
    override val supportedTypes = setOf(ServerType.VANILLA)
    
    private val manifestUrl = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json"
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class VersionManifest(
        val latest: Latest,
        val versions: List<VersionEntry>,
    )
    
    @Serializable
    data class Latest(
        val release: String,
        val snapshot: String,
    )
    
    @Serializable
    data class VersionEntry(
        val id: String,
        val type: String,
        val url: String,
        val time: String,
        val releaseTime: String,
    )
    
    @Serializable
    data class VersionDetails(
        val id: String,
        val downloads: Downloads,
    )
    
    @Serializable
    data class Downloads(
        val server: DownloadInfo?,
    )
    
    @Serializable
    data class DownloadInfo(
        val sha1: String,
        val size: Long,
        val url: String,
    )

    override suspend fun fetchVersions(): Result<List<ServerVersion>> = withContext(Dispatchers.IO) {
        try {
            val manifest = URL(manifestUrl).readTextWithTimeout()
            val versionManifest = json.decodeFromString<VersionManifest>(manifest)
            
            val versions = versionManifest.versions
                .filter { it.type == "release" }
                .map { entry ->
                    ServerVersion(
                        version = entry.id,
                        stable = true,
                        releaseDate = entry.releaseTime
                    )
                }
            
            Result.success(versions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchBuilds(version: String): Result<List<ServerBuild>> = withContext(Dispatchers.IO) {
        try {
            val manifest = URL(manifestUrl).readTextWithTimeout()
            val versionManifest = json.decodeFromString<VersionManifest>(manifest)
            
            val versionEntry = versionManifest.versions.firstOrNull { it.id == version }
                ?: return@withContext Result.failure(Exception("Version $version not found"))
            
            val detailsJson = URL(versionEntry.url).readTextWithTimeout()
            val details = json.decodeFromString<VersionDetails>(detailsJson)
            
            val serverDownload = details.downloads.server
                ?: return@withContext Result.failure(Exception("No server download for $version"))
            
            Result.success(listOf(ServerBuild(
                id = version,
                url = serverDownload.url,
                sha256 = null, // Mojang provides SHA1
                size = serverDownload.size
            )))
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
            Result.success(destination)
        } catch (e: Exception) {
            destination.delete()
            Result.failure(e)
        }
    }
}
