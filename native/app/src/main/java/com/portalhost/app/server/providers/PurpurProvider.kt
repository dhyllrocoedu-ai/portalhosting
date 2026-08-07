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
    private val baseUrl = "https://api.purpurmc.org/v2/purpur"

    override suspend fun getVersions(): List<String> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/versions"
        try {
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().bodyOrThrow(url)
            val response = json.decodeFromString<PurpurVersionsResponse>(body)
            response.versions.sortedByDescending { it }
        } catch (e: ServerProviderException) {
            Log.e(TAG, "getVersions: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "getVersions: ${e.message}")
            throw ServerProviderException("Failed to load Purpur versions from $url: ${e.message}", e)
        }
    }

    override suspend fun getBuildInfos(version: String): List<BuildInfo> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/versions/$version/builds"
        try {
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().bodyOrThrow(url)
            val response = json.decodeFromString<PurpurBuildsResponse>(body)
            response.builds
                .sortedByDescending { it.build }
                .map { BuildInfo("#${it.build}", it.build.toString()) }
        } catch (e: ServerProviderException) {
            Log.e(TAG, "getBuildInfos: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "getBuildInfos: ${e.message}")
            throw ServerProviderException("Failed to load Purpur builds for $version from $url: ${e.message}", e)
        }
    }

    override suspend fun getDownloadInfo(version: String, buildId: String): DownloadInfo? = withContext(Dispatchers.IO) {
        try {
            val buildNumber = buildId.ifBlank {
                val builds = getBuildInfos(version)
                builds.firstOrNull()?.id ?: return@withContext null
            }
            val url = "$baseUrl/versions/$version/builds/$buildNumber"
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().bodyOrThrow(url)
            val response = json.decodeFromString<PurpurBuildResponse>(body)

            val jarName = response.download
            val sha256 = response.sha256
            val downloadUrl = "$baseUrl/versions/$version/builds/$buildNumber/downloads/$jarName"
            DownloadInfo(downloadUrl, sha256, jarName)
        } catch (e: ServerProviderException) {
            Log.e(TAG, "getDownloadInfo: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "getDownloadInfo: ${e.message}")
            throw ServerProviderException("Failed to resolve Purpur download for $version build $buildId: ${e.message}", e)
        }
    }

    @Serializable
    private data class PurpurVersionsResponse(val versions: List<String>)
    @Serializable
    private data class PurpurBuildsResponse(val builds: List<PurpurBuildEntry>)
    @Serializable
    private data class PurpurBuildEntry(val build: Int, val download: String, val sha256: String)
    @Serializable
    private data class PurpurBuildResponse(
        val build: Int,
        val download: String,
        val sha256: String? = null
    )
}
