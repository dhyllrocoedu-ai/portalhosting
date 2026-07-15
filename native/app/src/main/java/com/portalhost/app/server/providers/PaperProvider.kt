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
    private val projectUrl = "https://api.papermc.io/v2/projects/paper"

    override suspend fun getVersions(): List<String> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(projectUrl).build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext emptyList()
            val response = json.decodeFromString<PaperProjectResponse>(body)
            response.versions
                .filter { it.matches(Regex("^\\d+(\\.\\d+)*$")) }
                .sortedByDescending { it }
        } catch (e: Exception) {
            Log.e(TAG, "getVersions: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getBuildInfos(version: String): List<BuildInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "$projectUrl/versions/$version"
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext emptyList()
            val response = json.decodeFromString<PaperVersionResponse>(body)
            response.builds
                .sortedByDescending { it }
                .map { BuildInfo("Build #$it", it.toString()) }
        } catch (e: Exception) {
            Log.e(TAG, "getBuildInfos: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getDownloadInfo(version: String, buildId: String): DownloadInfo? = withContext(Dispatchers.IO) {
        try {
            val buildNumber = buildId.toIntOrNull() ?: return@withContext null
            val url = "$projectUrl/versions/$version/builds/$buildNumber"
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().body?.string() ?: return@withContext null
            val response = json.decodeFromString<PaperBuildResponse>(body)
            val app = response.downloads?.application ?: return@withContext null
            val downloadUrl = "$projectUrl/versions/$version/builds/$buildNumber/downloads/${app.name}"
            DownloadInfo(downloadUrl, app.sha256, "paper-$version.jar")
        } catch (e: Exception) {
            Log.e(TAG, "getDownloadInfo: ${e.message}")
            null
        }
    }

    @Serializable
    private data class PaperProjectResponse(val versions: List<String>)

    @Serializable
    private data class PaperVersionResponse(val version: String, val builds: List<Int>)

    @Serializable
    private data class PaperBuildResponse(
        val build: Int,
        val downloads: PaperDownloads? = null
    )

    @Serializable
    private data class PaperDownloads(
        val application: PaperApplication? = null
    )

    @Serializable
    private data class PaperApplication(
        val name: String,
        val sha256: String
    )
}
