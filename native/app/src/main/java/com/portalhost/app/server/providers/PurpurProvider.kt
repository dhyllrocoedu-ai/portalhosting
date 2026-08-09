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
        val url = baseUrl
        try {
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().bodyOrThrow(url)
            val response = json.decodeFromString<PurpurRootResponse>(body)
            response.versions
                .distinct()
                .sortedByDescending { parseSemver(it) }
        } catch (e: ServerProviderException) {
            Log.e(TAG, "getVersions: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "getVersions: ${e.message}")
            throw ServerProviderException("Failed to load Purpur versions from $url: ${e.message}", e)
        }
    }

    override suspend fun getBuildInfos(version: String): List<BuildInfo> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/$version"
        try {
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().bodyOrThrow(url)
            val response = json.decodeFromString<PurpurBuildsWrapper>(body)
            response.builds.all
                .distinct()
                .sortedByDescending { it.toIntOrNull() ?: 0 }
                .map { BuildInfo("#$it", it) }
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
            DownloadInfo(
                url = "$baseUrl/$version/$buildNumber/download",
                sha256 = null,
                suggestedFileName = "purpur-$version-$buildNumber.jar"
            )
        } catch (e: ServerProviderException) {
            Log.e(TAG, "getDownloadInfo: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "getDownloadInfo: ${e.message}")
            throw ServerProviderException("Failed to resolve Purpur download for $version build $buildId: ${e.message}", e)
        }
    }

    @Serializable
    private data class PurpurRootResponse(
        val project: String,
        val versions: List<String>
    )

    @Serializable
    private data class PurpurBuildsWrapper(
        val builds: PurpurBuildList
    )

    @Serializable
    private data class PurpurBuildList(
        val latest: String,
        val all: List<String>
    )
}
