package com.portalhost.app.server.providers

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class FoliaProvider(
    private val client: OkHttpClient,
    private val json: Json
) : ServerProvider {
    override val type = ServerType.FOLIA
    override val supportsBuilds = true
    private val TAG = "FoliaProvider"

    override suspend fun getVersions(): List<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.papermc.io/v3/projects/folia/versions"
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext emptyList()
            val response = json.decodeFromString<FoliaVersionsResponse>(body)
            response.versions
                .map { it.version.id }
                .filter { it.matches(Regex("^\\d+(\\.\\d+)*$")) }
                .distinct()
                .sortedByDescending { it }
        } catch (e: Exception) {
            Log.e(TAG, "getVersions: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getBuildInfos(version: String): List<BuildInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.papermc.io/v3/projects/folia/versions/$version/builds"
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext emptyList()
            val response = json.decodeFromString<FoliaBuildsResponse>(body)
            response.builds
                .sortedByDescending { it.id }
                .map { BuildInfo("#${it.id}", it.id.toString()) }
        } catch (e: Exception) {
            Log.e(TAG, "getBuildInfos: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getDownloadInfo(version: String, buildId: String): DownloadInfo? = withContext(Dispatchers.IO) {
        try {
            val url = if (buildId.isBlank()) {
                "https://api.papermc.io/v3/projects/folia/versions/$version/builds/latest"
            } else {
                "https://api.papermc.io/v3/projects/folia/versions/$version/builds/$buildId"
            }
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext null
            val response = json.decodeFromString<FoliaBuildResponse>(body)
            val serverDownload = response.downloads?.get("server:default") ?: return@withContext null
            DownloadInfo(serverDownload.url, null, "folia-$version.jar")
        } catch (e: Exception) {
            Log.e(TAG, "getDownloadInfo: ${e.message}")
            null
        }
    }

    @Serializable
    private data class FoliaVersionsResponse(val versions: List<FoliaVersionEntry>)
    @Serializable
    private data class FoliaVersionEntry(val version: FoliaVersionId)
    @Serializable
    private data class FoliaVersionId(val id: String)
    @Serializable
    private data class FoliaBuildsResponse(val builds: List<FoliaBuildEntry>)
    @Serializable
    private data class FoliaBuildEntry(val id: Int)
    @Serializable
    private data class FoliaBuildResponse(
        val id: Int,
        val downloads: Map<String, FoliaDownloadEntry>? = null
    )
    @Serializable
    private data class FoliaDownloadEntry(val url: String)
}
