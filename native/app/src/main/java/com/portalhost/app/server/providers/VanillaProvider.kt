package com.portalhost.app.server.providers

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class VanillaProvider(
    private val client: OkHttpClient,
    private val json: Json
) : ServerProvider {
    override val type = ServerType.VANILLA
    override val supportsBuilds = false
    private val TAG = "VanillaProvider"

    override suspend fun getVersions(): List<String> = withContext(Dispatchers.IO) {
        val url = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json"
        try {
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().bodyOrThrow(url)
            val manifest = json.decodeFromString<VanillaManifest>(body)
            manifest.versions
                .filter { it.type == "release" }
                .map { it.id }
                .reversed()
        } catch (e: ServerProviderException) {
            Log.e(TAG, "getVersions: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "getVersions: ${e.message}")
            throw ServerProviderException("Failed to load Vanilla versions from $url: ${e.message}", e)
        }
    }

    override suspend fun getBuildInfos(version: String): List<BuildInfo> = emptyList()

    override suspend fun getDownloadInfo(version: String, buildId: String): DownloadInfo? = withContext(Dispatchers.IO) {
        val manifestUrl = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json"
        try {
            val req = Request.Builder().url(manifestUrl).build()
            val body = client.newCall(req).execute().bodyOrThrow(manifestUrl)
            val manifest = json.decodeFromString<VanillaManifest>(body)
            val entry = manifest.versions.find { it.id == version } ?: return@withContext null

            val versionReq = Request.Builder().url(entry.url).build()
            val versionBody = client.newCall(versionReq).execute().bodyOrThrow(entry.url)
            val versionInfo = json.decodeFromString<VanillaVersionInfo>(versionBody)
            val serverUrl = versionInfo.downloads?.server?.url ?: return@withContext null
            DownloadInfo(serverUrl, null, "vanilla-$version.jar")
        } catch (e: ServerProviderException) {
            Log.e(TAG, "getDownloadInfo: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "getDownloadInfo: ${e.message}")
            throw ServerProviderException("Failed to resolve Vanilla download for $version: ${e.message}", e)
        }
    }

    @Serializable
    private data class VanillaManifest(val versions: List<VanillaVersionEntry>)
    @Serializable
    private data class VanillaVersionEntry(val id: String, val type: String, val url: String)
    @Serializable
    private data class VanillaVersionInfo(val downloads: VanillaDownloads?)
    @Serializable
    private data class VanillaDownloads(val server: VanillaServerDownload? = null)
    @Serializable
    private data class VanillaServerDownload(val url: String)
}
