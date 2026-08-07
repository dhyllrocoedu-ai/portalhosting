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
    private val baseUrl = "https://api.papermc.io/v2/projects/paper"

    override suspend fun getVersions(): List<String> = withContext(Dispatchers.IO) {
        val url = "$baseUrl"
        try {
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().bodyOrThrow(url)
            val response = json.decodeFromString<PaperVersionsResponse>(body)
            response.versions
                .filter { it.matches(Regex("^\\d+(\\.\\d+)*$")) }
                .distinct()
                .sortedByDescending { it }
        } catch (e: ServerProviderException) {
            Log.e(TAG, "getVersions: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "getVersions: ${e.message}")
            throw ServerProviderException("Failed to load Paper versions from $url: ${e.message}", e)
        }
    }

    override suspend fun getBuildInfos(version: String): List<BuildInfo> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/versions/$version/builds"
        try {
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().bodyOrThrow(url)
            val response = json.decodeFromString<PaperBuildsResponse>(body)
            response.builds
                .sortedByDescending { it.build }
                .map { BuildInfo("#${it.build}", it.build.toString()) }
        } catch (e: ServerProviderException) {
            Log.e(TAG, "getBuildInfos: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "getBuildInfos: ${e.message}")
            throw ServerProviderException("Failed to load Paper builds for $version from $url: ${e.message}", e)
        }
    }

    override suspend fun getDownloadInfo(version: String, buildId: String): DownloadInfo? = withContext(Dispatchers.IO) {
        try {
            val url = if (buildId.isBlank()) {
                "$baseUrl/versions/$version/builds/latest"
            } else {
                "$baseUrl/versions/$version/builds/$buildId"
            }
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().bodyOrThrow(url)
            val response = json.decodeFromString<PaperBuildResponse>(body)

            val app = response.downloads?.get("application") ?: return@withContext null
            val jarName = app.name
            val sha256 = app.sha256
            val downloadUrl = "$baseUrl/versions/$version/builds/${response.build}/downloads/$jarName"
            DownloadInfo(downloadUrl, sha256, jarName)
        } catch (e: ServerProviderException) {
            Log.e(TAG, "getDownloadInfo: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "getDownloadInfo: ${e.message}")
            throw ServerProviderException("Failed to resolve Paper download for $version build $buildId: ${e.message}", e)
        }
    }

    @Serializable
    private data class PaperVersionsResponse(val versions: List<String>)
    @Serializable
    private data class PaperBuildsResponse(val builds: List<PaperBuildEntry>)
    @Serializable
    private data class PaperBuildEntry(val build: Int)
    @Serializable
    private data class PaperBuildResponse(
        val build: Int,
        val downloads: Map<String, PaperDownloadEntry>? = null
    )
    @Serializable
    private data class PaperDownloadEntry(
        val name: String,
        val sha256: String? = null
    )
}
