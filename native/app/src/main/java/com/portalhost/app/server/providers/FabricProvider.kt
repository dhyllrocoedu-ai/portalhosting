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

    override suspend fun getVersions(): List<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json"
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext emptyList()
            val manifest = json.decodeFromString<VanillaManifest>(body)
            manifest.versions
                .filter { it.type == "release" }
                .map { it.id }
                .reversed()
        } catch (e: Exception) {
            Log.e(TAG, "getVersions: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getBuildInfos(version: String): List<BuildInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "https://meta.fabricmc.net/v2/versions/loader/$version"
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext emptyList()
            val loaders = json.decodeFromString<List<FabricLoaderEntry>>(body)
            loaders.map { BuildInfo("Loader ${it.loader.version}", it.loader.version) }
        } catch (e: Exception) {
            Log.e(TAG, "getBuildInfos: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getDownloadInfo(version: String, buildId: String): DownloadInfo? {
        val loader = buildId.ifBlank {
            val builds = getBuildInfos(version)
            builds.firstOrNull()?.id ?: return null
        }
        val url = "https://meta.fabricmc.net/v2/versions/loader/$version/$loader/server/jar"
        return DownloadInfo(url, null, "fabric-$version-loader-$loader.jar")
    }

    @Serializable
    private data class FabricLoaderEntry(val loader: FabricLoader)

    @Serializable
    private data class FabricLoader(val version: String)

    @Serializable
    private data class VanillaManifest(val versions: List<VanillaVersionEntry>)

    @Serializable
    private data class VanillaVersionEntry(val id: String, val type: String, val url: String)
}
