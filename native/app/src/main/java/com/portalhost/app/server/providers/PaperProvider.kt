package com.portalhost.app.server.providers

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class PaperProvider(
    private val client: OkHttpClient,
    private val json: Json
) : ServerProvider {
    override val type = ServerType.PAPER
    override val supportsBuilds = true
    private val TAG = "PaperProvider"

    override suspend fun getVersions(): List<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://fill.papermc.io/v3/projects/paper/versions"
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext emptyList()
            val response = json.decodeFromString<PaperVersionsResponse>(body)
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
            val url = "https://fill.papermc.io/v3/projects/paper/versions/$version/builds"
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext emptyList()
            val response = json.decodeFromString<PaperBuildsResponse>(body)
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
                "https://fill.papermc.io/v3/projects/paper/versions/$version/builds/latest"
            } else {
                "https://fill.papermc.io/v3/projects/paper/versions/$version/builds/$buildId"
            }
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext null
            val response = json.decodeFromString<PaperBuildResponse>(body)

            val serverDownload = response.downloads?.get("server:default") ?: return@withContext null
            val sha256 = serverDownload.checksums?.get("sha256")
            DownloadInfo(serverDownload.url, sha256, "paper-$version.jar")
        } catch (e: Exception) {
            Log.e(TAG, "getDownloadInfo: ${e.message}")
            null
        }
    }

    @Serializable
    private data class PaperVersionsResponse(val versions: List<PaperVersionEntry>)
    @Serializable
    private data class PaperVersionEntry(val version: PaperVersionId)
    @Serializable
    private data class PaperVersionId(val id: String)
    @Serializable
    private data class PaperBuildsResponse(val builds: List<PaperBuildEntry>)
    @Serializable
    private data class PaperBuildEntry(val id: Int)
    @Serializable
    private data class PaperBuildResponse(
        val id: Int,
        val downloads: Map<String, PaperDownloadEntry>? = null
    )
    @Serializable
    private data class PaperDownloadEntry(
        val url: String,
        val checksums: Map<String, String>? = null
    )
}
