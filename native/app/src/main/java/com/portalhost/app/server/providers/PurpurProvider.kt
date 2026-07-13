package com.portalhost.app.server.providers

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class PurpurProvider(
    private val client: OkHttpClient,
    private val json: Json
) : ServerProvider {
    override val type = ServerType.PURPUR
    override val supportsBuilds = true
    private val TAG = "PurpurProvider"

    override suspend fun getVersions(): List<String> = withContext(Dispatchers.IO) {
        try {
            // PurpurMC API v2
            val url = "https://api.purpurmc.org/v2/purpur"
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext emptyList()
            val response = json.decodeFromString<PurpurApiResponse>(body)
            response.versions?.filter { it.matches(Regex("^\\d+\\.\\d+(\\.\\d+)?$")) }
                ?.sortedByDescending { it } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "getVersions: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getBuildInfos(version: String): List<BuildInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.purpurmc.org/v2/purpur/$version"
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext emptyList()
            val response = json.decodeFromString<PurpurBuildsResponse>(body)
            val builds = response.builds?.keys?.mapNotNull {
                it.toIntOrNull()
            }?.sortedByDescending { it } ?: emptyList()
            builds.map { BuildInfo("#$it", it.toString()) }
        } catch (e: Exception) {
            Log.e(TAG, "getBuildInfos: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getDownloadInfo(version: String, buildId: String): DownloadInfo? {
        val build = buildId.ifBlank {
            val builds = getBuildInfos(version)
            builds.firstOrNull()?.id ?: return null
        }
        val url = "https://api.purpurmc.org/v2/purpur/$version/$build/download"
        return DownloadInfo(url, null, "purpur-$version.jar")
    }

    @Serializable
    private data class PurpurApiResponse(val versions: List<String>? = null)
    @Serializable
    private data class PurpurBuildsResponse(val builds: Map<String, String>? = null)
}
