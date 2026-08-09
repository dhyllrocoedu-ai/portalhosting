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
    private val baseUrl = "https://fill.papermc.io/v3/projects/paper"

    override suspend fun getVersions(): List<String> = withContext(Dispatchers.IO) {
        val url = baseUrl
        try {
            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().bodyOrThrow(url)
            val response = json.decodeFromString<PaperProjectV3>(body)
            response.versions.values.flatten()
                .filter { !it.contains("-") }
                .distinct()
                .sortedByDescending { parseSemver(it) }
                .map { it }
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
            val builds = json.decodeFromString<List<PaperBuildEntryV3>>(body)
            builds
                .filter { it.channel == "STABLE" }
                .sortedByDescending { it.id }
                .map { BuildInfo("#${it.id}", it.id.toString()) }
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
            val response = json.decodeFromString<PaperBuildResponseV3>(body)

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
            throw ServerProviderException("Failed to resolve Paper download for $version build $buildId: ${e.message}", e)
        }
    }

    @Serializable
    private data class PaperProjectV3(
        val project: PaperProjectInfoV3,
        val versions: Map<String, List<String>>
    )

    @Serializable
    private data class PaperProjectInfoV3(val id: String, val name: String)

    @Serializable
    private data class PaperBuildEntryV3(
        val id: Int,
        val time: String,
        val channel: String,
        val downloads: Map<String, PaperDownloadV3>
    )

    @Serializable
    private data class PaperBuildResponseV3(
        val id: Int,
        val downloads: Map<String, PaperDownloadV3>? = null
    )

    @Serializable
    private data class PaperDownloadV3(
        val name: String,
        val url: String,
        val checksums: Map<String, String>? = null,
        val size: Long? = null
    )
}
