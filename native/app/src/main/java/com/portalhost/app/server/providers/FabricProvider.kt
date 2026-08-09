package com.portalhost.app.server.providers

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class FabricProvider(
    private val client: OkHttpClient,
    private val json: Json
) : ServerProvider {
    override val type = ServerType.FABRIC
    override val supportsBuilds = true
    private val TAG = "FabricProvider"
    private val metaUrl = "https://meta.fabricmc.net/v2"

    override suspend fun getVersions(): List<String> = withContext(Dispatchers.IO) {
        val url = "$metaUrl/versions/game"
        try {
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().bodyOrThrow(url)
            val versions = json.decodeFromString<List<FabricVersionEntry>>(body)
            versions
                .filter { it.stable }
                .map { it.version }
                .distinct()
                .sortedByDescending { parseSemver(it) }
        } catch (e: ServerProviderException) {
            Log.e(TAG, "getVersions: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "getVersions: ${e.message}")
            throw ServerProviderException("Failed to load Fabric versions from $url: ${e.message}", e)
        }
    }

    override suspend fun getBuildInfos(version: String): List<BuildInfo> = withContext(Dispatchers.IO) {
        val url = "$metaUrl/versions/loader/$version"
        try {
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().bodyOrThrow(url)
            val loaders = json.decodeFromString<List<FabricLoaderEntry>>(body)
            loaders.map { BuildInfo("Loader ${it.loader.version}", it.loader.version) }
        } catch (e: ServerProviderException) {
            Log.e(TAG, "getBuildInfos: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "getBuildInfos: ${e.message}")
            throw ServerProviderException("Failed to load Fabric loaders for $version from $url: ${e.message}", e)
        }
    }

    override suspend fun getDownloadInfo(version: String, buildId: String): DownloadInfo? {
        val loader = buildId.ifBlank {
            val builds = getBuildInfos(version)
            builds.firstOrNull()?.id ?: return null
        }
        val url = "$metaUrl/versions/loader/$version/$loader/server/jar"
        return DownloadInfo(url, null, "fabric-$version-loader-$loader.jar")
    }

    @Serializable
    private data class FabricVersionEntry(val version: String, val stable: Boolean)

    @Serializable
    private data class FabricLoaderEntry(val loader: FabricLoader)

    @Serializable
    private data class FabricLoader(val version: String)
}
