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
    private val baseUrl = "https://fill.papermc.io/v3/projects/folia"

    override suspend fun getVersions(): List<String> = withContext(Dispatchers.IO) {
        val url = baseUrl
        try {
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().bodyOrThrow(url)
            val response = json.decodeFromString<FoliaProjectV3>(body)
            response.versions.values.flatten()
                .filter { !it.contains("-") }
                .distinct()
                .sortedByDescending { parseSemver(it) }
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
            val builds = json.decodeFromString<List<FoliaBuildEntryV3>>(body)
            builds
                .filter { it.channel == "STABLE" }
                .sortedByDescending { it.id }
                .map { BuildInfo("#${it.id}", it.id.toString()) }
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
            val response = json.decodeFromString<FoliaBuildResponseV3>(body)

            val download = response.downloads?.get("server:default") ?: return@withContext null
            DownloadInfo(
                url = download.url,
                sha256 = download.checksums?.get("sha256"),
                suggestedFileName = download.name
            )
        } catch (e: ServerProviderException) {
            Log.e(TAG, "getDownloadInfo: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "getDownloadInfo: ${e.message}")
            throw ServerProviderException("Failed to resolve Folia download for $version build $buildId: ${e.message}", e)
        }
    }

    @Serializable
    private data class FoliaProjectV3(
        val project: FoliaProjectInfoV3,
        val versions: Map<String, List<String>>
    )

    @Serializable
    private data class FoliaProjectInfoV3(val id: String, val name: String)

    @Serializable
    private data class FoliaBuildEntryV3(
        val id: Int,
        val time: String,
        val channel: String,
        val downloads: Map<String, FoliaDownloadV3>
    )

    @Serializable
    private data class FoliaBuildResponseV3(
        val id: Int,
        val downloads: Map<String, FoliaDownloadV3>? = null
    )

    @Serializable
    private data class FoliaDownloadV3(
        val name: String,
        val url: String,
        val checksums: Map<String, String>? = null,
        val size: Long? = null
    )
}
