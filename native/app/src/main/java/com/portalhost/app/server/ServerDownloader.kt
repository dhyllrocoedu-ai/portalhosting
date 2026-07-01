package com.portalhost.app.server

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

enum class ServerType(
    val displayName: String,
    val description: String
) {
    PAPER("Paper", "High-performance, feature-rich server software"),
    VANILLA("Vanilla", "Official Mojang server jar"),
    FABRIC("Fabric", "Lightweight mod loader for Minecraft")
}

data class PaperBuild(
    val version: String,
    val build: Int,
    val downloadUrl: String,
    val sha256: String?
)

class ServerDownloader {
    private val TAG = "ServerDownloader"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    /** Fetch available Paper versions. */
    suspend fun getPaperVersions(): List<String> = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("https://api.papermc.io/v2/projects/paper").build()
        val body = client.newCall(req).execute().body?.string() ?: return@withContext emptyList()
        val response = json.decodeFromString<Map<String, List<String>>>(body)
        response["versions"]?.reversed() ?: emptyList()
    }

    /** Get the latest stable Paper build for a version. */
    suspend fun getLatestPaperBuild(version: String): PaperBuild? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.papermc.io/v2/projects/paper/versions/$version/builds"
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext null
            val response = json.decodeFromString<PaperBuildsResponse>(body)
            val builds = response.builds.filter { it.channel == "default" || it.channel == "stable" }
            val latest = builds.lastOrNull() ?: return@withContext null

            val downloadUrl = "https://api.papermc.io/v2/projects/paper/versions/$version/builds/${latest.build}/downloads/paper-$version-${latest.build}.jar"
            val sha256 = latest.downloads?.application?.sha256
            PaperBuild(version, latest.build, downloadUrl, sha256)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Paper build: ${e.message}")
            null
        }
    }

    /** Download a server jar, optionally verifying SHA-256. */
    suspend fun download(
        url: String,
        destFile: File,
        sha256: String? = null,
        onProgress: ((Long, Long) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Downloading from: $url")
            destFile.parentFile?.mkdirs()

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: $url"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
            val contentLength = body.contentLength()

            body.byteStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    val buf = ByteArray(32768)
                    var totalRead = 0L
                    var read: Int
                    var lastProgressReport = 0L
                    while (input.read(buf).also { read = it } != -1) {
                        output.write(buf, 0, read)
                        totalRead += read
                        if (contentLength > 0 && totalRead - lastProgressReport > 65536) {
                            lastProgressReport = totalRead
                            onProgress?.invoke(totalRead, contentLength)
                        }
                    }
                    if (contentLength > 0) {
                        onProgress?.invoke(totalRead, contentLength)
                    }
                }
            }

            Log.i(TAG, "Downloaded ${destFile.length()} bytes to ${destFile.absolutePath}")

            // Verify SHA-256
            if (sha256 != null) {
                val actual = sha256sum(destFile)
                if (actual != sha256.lowercase()) {
                    destFile.delete()
                    return@withContext Result.failure(Exception("SHA-256 mismatch: expected $sha256, got $actual"))
                }
                Log.i(TAG, "SHA-256 verified")
            }

            destFile.setReadable(true)
            Result.success(destFile)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}", e)
            if (destFile.exists()) destFile.delete()
            Result.failure(e)
        }
    }

    /** Fetch available Vanilla Minecraft versions. */
    suspend fun getVanillaVersions(): List<String> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json").build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext emptyList()
            val manifest = json.decodeFromString<VanillaManifest>(body)
            manifest.versions
                .filter { it.type == "release" }
                .map { it.id }
                .reversed()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Vanilla versions: ${e.message}")
            emptyList()
        }
    }

    /** Get download URL for a specific Vanilla version. */
    suspend fun getVanillaDownloadUrl(version: String): String? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json").build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext null
            val manifest = json.decodeFromString<VanillaManifest>(body)
            val entry = manifest.versions.find { it.id == version } ?: return@withContext null

            val versionReq = Request.Builder().url(entry.url).build()
            val versionBody = client.newCall(versionReq).execute().body?.string() ?: return@withContext null
            val versionInfo = json.decodeFromString<VanillaVersionInfo>(versionBody)
            versionInfo.downloads?.server?.url
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Vanilla URL: ${e.message}")
            null
        }
    }

    /** Download Paper server jar: get latest build, download, verify. */
    suspend fun downloadPaper(
        version: String,
        destFile: File,
        onProgress: ((Long, Long) -> Unit)? = null
    ): Result<File> {
        val build = getLatestPaperBuild(version)
            ?: return Result.failure(Exception("No Paper build found for v$version"))
        return download(build.downloadUrl, destFile, build.sha256, onProgress)
    }

    /** Download Vanilla server jar. */
    suspend fun downloadVanilla(
        version: String,
        destFile: File,
        onProgress: ((Long, Long) -> Unit)? = null
    ): Result<File> {
        val url = getVanillaDownloadUrl(version)
            ?: return Result.failure(Exception("No Vanilla download URL for v$version"))
        return download(url, destFile, null, onProgress)
    }

    /** Download Fabric server jar (direct download from Fabric meta). */
    suspend fun downloadFabric(
        mcVersion: String,
        loaderVersion: String? = null,
        destFile: File,
        onProgress: ((Long, Long) -> Unit)? = null
    ): Result<File> {
        val loader = loaderVersion ?: getLatestFabricLoader(mcVersion)
            ?: return Result.failure(Exception("No Fabric loader found for MC $mcVersion"))

        val url = "https://meta.fabricmc.net/v2/versions/loader/$mcVersion/$loader/server/jar"
        return download(url, destFile, null, onProgress)
    }

    private suspend fun getLatestFabricLoader(mcVersion: String): String? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("https://meta.fabricmc.net/v2/versions/loader/$mcVersion").build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext null
            val loaders = json.decodeFromString<List<FabricLoaderEntry>>(body)
            loaders.firstOrNull()?.loader?.version
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Fabric loader: ${e.message}")
            null
        }
    }

    private fun sha256sum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(32768)
            var read: Int
            while (input.read(buf).also { read = it } != -1) {
                digest.update(buf, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

// API response data classes
@kotlinx.serialization.Serializable
private data class PaperBuildsResponse(
    val builds: List<PaperBuildEntry>
)

@kotlinx.serialization.Serializable
private data class PaperBuildEntry(
    val build: Int,
    val channel: String,
    val downloads: PaperDownloads?
)

@kotlinx.serialization.Serializable
private data class PaperDownloads(
    val application: PaperApplication?
)

@kotlinx.serialization.Serializable
private data class PaperApplication(
    val name: String,
    val sha256: String
)

@kotlinx.serialization.Serializable
private data class VanillaManifest(
    val versions: List<VanillaVersionEntry>
)

@kotlinx.serialization.Serializable
private data class VanillaVersionEntry(
    val id: String,
    val type: String,
    val url: String
)

@kotlinx.serialization.Serializable
private data class VanillaVersionInfo(
    val downloads: VanillaDownloads?
)

@kotlinx.serialization.Serializable
private data class VanillaDownloads(
    val server: VanillaServerDownload? = null
)

@kotlinx.serialization.Serializable
private data class VanillaServerDownload(
    val url: String
)

@kotlinx.serialization.Serializable
private data class FabricLoaderEntry(
    val loader: FabricLoader
)

@kotlinx.serialization.Serializable
private data class FabricLoader(
    val version: String
)
