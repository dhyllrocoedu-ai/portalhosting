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
    private val baseUrl = "https://api.papermc.io/v2/projects/folia"

    override suspend fun getVersions(): List<String> = withContext(Dispatchers.IO) {
        val url = "$baseUrl"
        try {
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().bodyOrThrow(url)
            val response = json.decodeFromString<FoliaVersionsResponse>(body)
            response.versions
                .filter { it.matches(Regex("^\\d+(\\.\\d+)*$")) }
                .distinct()
                .sortedByDescending { it }
        } catch (e: ServerProviderException) {
            Log.e(TAG, "getVersions: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "getVersions: ${e.message}")
            throw ServerProviderException("Failed to load Folia versions from $url: ${e.message}", e)
        }
    }

    override suspend fun getBuildInfos(version: String): List<BuildInfo> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/versions/$version/builds"
        try {
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().bodyOrThrow(url)
            val response = json.decodeFromString<FoliaBuildsResponse>(body)
            response.builds
                .filter { it.channel == "default" }
                .sortedByDescending { it.build }
                .map { BuildInfo("#${it.build}", it.build.toString()) }
        } catch (e: ServerProviderException) {
            Log.e(TAG, "getBuildInfos: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "getBuildInfos: ${e.message}")
            throw ServerProviderException("Failed to load Folia builds for $version from $url: ${e.message}", e)
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
            val response = json.decodeFromString<FoliaBuildResponse>(body)

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
            throw ServerProviderException("Failed to resolve Folia download for $version build $buildId: ${e.message}", e)
        }
    }

    @Serializable
    private data class FoliaVersionsResponse(val versions: List<String>)
    @Serializable
    private data class FoliaBuildsResponse(val builds: List<FoliaBuildEntry>)
    @Serializable
    private data class FoliaBuildEntry(val build: Int, val channel: String)
    @Serializable
    private data class FoliaBuildResponse(
        val build: Int,
        val downloads: Map<String, FoliaDownloadEntry>? = null
    )
    @Serializable
    private data class FoliaDownloadEntry(
        val name: String,
        val sha256: String? = null
    )
}
