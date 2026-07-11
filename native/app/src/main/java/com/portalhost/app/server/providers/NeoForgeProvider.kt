package com.portalhost.app.server.providers

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class NeoForgeProvider(
    private val client: OkHttpClient,
    private val json: Json
) : ServerProvider {
    override val type = ServerType.NEOFORGE
    override val supportsBuilds = true
    private val TAG = "NeoForgeProvider"

    override suspend fun getVersions(): List<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge"
            val req = Request.Builder().url(url).header("Accept", "application/json").build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext emptyList()
            val response = json.decodeFromString<NeoForgeApiResponse>(body)
            response.versions
                .filter { it.matches(Regex("^\\d+\\.\\d+(\\.\\d+)?$")) }
                .sortedByDescending { it }
        } catch (e: Exception) {
            Log.e(TAG, "getVersions: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getBuildInfos(version: String): List<BuildInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge"
            val req = Request.Builder().url(url).header("Accept", "application/json").build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext emptyList()
            val response = json.decodeFromString<NeoForgeApiResponse>(body)
            val matched = response.versions.filter { it.startsWith(version) }
            matched.map { BuildInfo(it, it) }.sortedByDescending { it.label }
        } catch (e: Exception) {
            Log.e(TAG, "getBuildInfos: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getDownloadInfo(version: String, buildId: String): DownloadInfo? {
        val neoVer = buildId.ifBlank {
            val builds = getBuildInfos(version)
            builds.firstOrNull()?.id ?: return null
        }
        val baseMc = version
        val url = "https://maven.neoforged.net/releases/net/neoforged/neoforge/$neoVer/neoforge-$neoVer-server.jar"
        return DownloadInfo(url, null, "neoforge-$baseMc.jar")
    }

    @Serializable
    private data class NeoForgeApiResponse(val versions: List<String>)
}
