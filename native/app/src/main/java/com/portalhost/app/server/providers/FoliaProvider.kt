package com.portalhost.app.server.providers

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray

class FoliaProvider(
    private val client: OkHttpClient,
    private val json: Json
) : ServerProvider {
    override val type = ServerType.FOLIA
    override val supportsBuilds = true
    private val TAG = "FoliaProvider"

    override suspend fun getVersions(): List<String> = withContext(Dispatchers.IO) {
        // Try Fill API v3 first, then fallback to api.papermc.io
        val endpoints = listOf(
            "https://fill.papermc.io/v3/projects/folia/versions",
            "https://api.papermc.io/v3/projects/folia/versions"
        )

        for (endpoint in endpoints) {
            try {
                val url = endpoint
                val req = Request.Builder().url(url).build()
                val body = client.newCall(req).execute().body?.string() ?: continue
                val versions = parseVersionsResponse(body)
                if (versions.isNotEmpty()) return@withContext versions
            } catch (e: Exception) {
                Log.w(TAG, "getVersions failed for $endpoint: ${e.message}")
            }
        }
        emptyList()
    }

    private fun parseVersionsResponse(body: String): List<String> {
        val data = json.decodeFromString<JsonElement>(body) as JsonObject

        // Format 1: Fill API v3 - {"versions": [{"version": {"id": "1.21.4"}}, ...]}
        if (data.containsKey("versions") && data["versions"] is JsonArray) {
            val versionsList = data["versions"] as JsonArray
            return versionsList.mapNotNull { element ->
                val obj = element as JsonObject
                val versionObj = obj["version"] as? JsonObject
                val id = versionObj?.get("id")
                if (id is kotlinx.serialization.json.JsonPrimitive) id.content else null
            }.filter { it.matches(Regex("^\\d+(\\.\\d+)*$")) }.distinct().sortedByDescending { it }
        }

        // Format 2: version_groups - {"version_groups": [{"versions": [{"version": {"id": "1.21.4"}}, ...]}, ...]}
        if (data.containsKey("version_groups") && data["version_groups"] is JsonArray) {
            val groups = data["version_groups"] as JsonArray
            val versions = mutableListOf<String>()
            for (group in groups) {
                val groupObj = group as JsonObject
                val groupVersions = groupObj["versions"] as? JsonArray ?: continue
                for (v in groupVersions) {
                    val vObj = v as JsonObject
                    val versionObj = vObj["version"] as? JsonObject
                    val id = versionObj?.get("id")
                    if (id is kotlinx.serialization.json.JsonPrimitive) versions.add(id.content)
                }
            }
            return versions.filter { it.matches(Regex("^\\d+(\\.\\d+)*$")) }.distinct().sortedByDescending { it }
        }

        // Format 3: Legacy - {"project_id": "folia", "versions": {"1.21.4": {...}, ...}}
        if (data.containsKey("project_id") && data.containsKey("versions") && data["versions"] is JsonObject) {
            val versionsMap = data["versions"] as JsonObject
            return versionsMap.keys.filter { it.matches(Regex("^\\d+(\\.\\d+)*$")) }.sortedByDescending { it }.toList()
        }

        return emptyList()
    }

    override suspend fun getBuildInfos(version: String): List<BuildInfo> = withContext(Dispatchers.IO) {
        val endpoints = listOf(
            "https://fill.papermc.io/v3/projects/folia/versions/$version/builds",
            "https://api.papermc.io/v3/projects/folia/versions/$version/builds"
        )

        for (endpoint in endpoints) {
            try {
                val req = Request.Builder().url(endpoint).build()
                val body = client.newCall(req).execute().body?.string() ?: continue
                val response = json.decodeFromString<FoliaBuildsResponse>(body)
                return@withContext response.builds
                    .sortedByDescending { it.id }
                    .map { BuildInfo("#${it.id}", it.id.toString()) }
            } catch (e: Exception) {
                Log.w(TAG, "getBuildInfos failed for $endpoint: ${e.message}")
            }
        }
        emptyList()
    }

    override suspend fun getDownloadInfo(version: String, buildId: String): DownloadInfo? = withContext(Dispatchers.IO) {
        val endpoints = listOf(
            "https://fill.papermc.io/v3/projects/folia/versions/$version/builds/${if (buildId.isBlank()) "latest" else buildId}",
            "https://api.papermc.io/v3/projects/folia/versions/$version/builds/${if (buildId.isBlank()) "latest" else buildId}"
        )

        for (endpoint in endpoints) {
            try {
                val req = Request.Builder().url(endpoint).build()
                val body = client.newCall(req).execute().body?.string() ?: continue
                val response = json.decodeFromString<FoliaBuildResponse>(body)
                val serverDownload = response.downloads?.get("server:default") ?: continue
                return@withContext DownloadInfo(serverDownload.url, null, "folia-$version.jar")
            } catch (e: Exception) {
                Log.w(TAG, "getDownloadInfo failed for $endpoint: ${e.message}")
            }
        }
        null
    }

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
