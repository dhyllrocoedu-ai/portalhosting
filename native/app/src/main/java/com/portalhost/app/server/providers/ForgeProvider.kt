package com.portalhost.app.server.providers

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ForgeProvider(
    private val client: OkHttpClient,
    private val json: Json
) : ServerProvider {
    override val type = ServerType.FORGE
    override val supportsBuilds = true
    private val TAG = "ForgeProvider"

    override suspend fun getVersions(): List<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://files.minecraftforge.net/net/minecraftforge/forge/maven-metadata.json"
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext emptyList()
            @Suppress("UNCHECKED_CAST")
            val raw = json.decodeFromString<Map<String, List<String>>>(body)
            raw.keys.toList().sortedByDescending { it }
        } catch (e: Exception) {
            Log.e(TAG, "getVersions: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getBuildInfos(version: String): List<BuildInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "https://files.minecraftforge.net/net/minecraftforge/forge/maven-metadata.json"
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext emptyList()
            @Suppress("UNCHECKED_CAST")
            val raw = json.decodeFromString<Map<String, List<String>>>(body)
            val forgeVersions = raw[version] ?: return@withContext emptyList()
            forgeVersions.map { full ->
                val forge = full.removePrefix("$version-").ifBlank { full }
                BuildInfo(forge, forge)
            }.reversed()
        } catch (e: Exception) {
            Log.e(TAG, "getBuildInfos: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getDownloadInfo(version: String, buildId: String): DownloadInfo? {
        val forgeVer = buildId.ifBlank {
            val builds = getBuildInfos(version)
            builds.firstOrNull()?.id ?: return null
        }
        val fullVer = "$version-$forgeVer"
        val url = "https://maven.minecraftforge.net/net/minecraftforge/forge/$fullVer/forge-$fullVer-universal.jar"
        return DownloadInfo(url, null, "forge-$version-$forgeVer-server.jar")
    }
}
