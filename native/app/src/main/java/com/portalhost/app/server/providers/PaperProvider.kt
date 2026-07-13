package com.portalhost.app.server.providers

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class PaperProvider(
    private val client: OkHttpClient,
    private val json: Json
) : ServerProvider {
    override val type = ServerType.PAPER
    override val supportsBuilds = true
    private val TAG = "PaperProvider"

    override suspend fun getVersions(): List<String> = withContext(Dispatchers.IO) {
        try {
            // Try Fill API v3 first, then fallback to PaperMC API v3
            val endpoints = listOf(
                "https://fill.papermc.io/v3/projects/paper/versions",
                "https://api.papermc.io/v3/projects/paper/versions"
            )

            for (endpoint in endpoints) {
                val req = Request.Builder().url(endpoint).build()
                val body = client.newCall(req).execute().body?.string() ?: continue
                val versions = parseVersionsResponse(body)
                if (versions.isNotEmpty()) {
                    Log.d(TAG, "Got ${versions.size} versions from $endpoint")
                    return@withContext versions
                }
            }
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "getVersions: ${e.message}")
            emptyList()
        }
    }

    private fun parseVersionsResponse(body: String): List<String> {
        val obj = json.decodeFromString<JsonObject>(body)
        val versions = mutableListOf<String>()

        // Format 1: Fill API v3 - {"versions": [{"version": {"id": "1.21.4"}}, ...]}
        if (obj.containsKey("versions") && obj["versions"] is kotlinx.serialization.json.JsonArray) {
            val versionsArray = obj["versions"] as kotlinx.serialization.json.JsonArray
            for (elem in versionsArray) {
                if (elem is JsonObject) {
                    val versionObj = elem["version"]
                    if (versionObj is JsonObject) {
                        val id = versionObj["id"]
                        if (id is kotlinx.serialization.json.JsonPrimitive && id.content.matches(Regex("^\\d+(\\.\\d+)*$"))) {
                            versions.add(id.content)
                        }
                    }
                }
            }
        }
        // Format 2: Fill API v3 with version_groups - {"version_groups": [{"versions": [...]}]}
        else if (obj.containsKey("version_groups") && obj["version_groups"] is kotlinx.serialization.json.JsonArray) {
            val groupsArray = obj["version_groups"] as kotlinx.serialization.json.JsonArray
            for (group in groupsArray) {
                if (group is JsonObject) {
                    val versionsArray = group["versions"]
                    if (versionsArray is kotlinx.serialization.json.JsonArray) {
                        for (v in versionsArray) {
                            if (v is JsonObject) {
                                val versionObj = v["version"]
                                if (versionObj is JsonObject) {
                                    val id = versionObj["id"]
                                    if (id is kotlinx.serialization.json.JsonPrimitive && id.content.matches(Regex("^\\d+(\\.\\d+)*$"))) {
                                        versions.add(id.content)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // Format 3: Legacy PaperMC API - {"project_id": "paper", "versions": {"1.21.4": {...}}}
        else if (obj.containsKey("project_id") && obj.containsKey("versions") && obj["versions"] is JsonObject) {
            val versionsMap = obj["versions"] as JsonObject
            versions.addAll(versionsMap.keys.filter { it.matches(Regex("^\\d+(\\.\\d+)*$")) })
        }

        return versions.distinct()
            .filter { it.matches(Regex("^\\d+(\\.\\d+)*$")) }
            .sortedByDescending { it }
    }

    override suspend fun getBuildInfos(version: String): List<BuildInfo> = withContext(Dispatchers.IO) {
        try {
            // Try Fill API v3 first, then PaperMC API v3
            val endpoints = listOf(
                "https://fill.papermc.io/v3/projects/paper/versions/$version/builds",
                "https://api.papermc.io/v3/projects/paper/versions/$version/builds"
            )

            for (endpoint in endpoints) {
                val req = Request.Builder().url(endpoint).build()
                val body = client.newCall(req).execute().body?.string() ?: continue
                val builds = parseBuildsResponse(body)
                if (builds.isNotEmpty()) {
                    Log.d(TAG, "Got ${builds.size} builds for $version from $endpoint")
                    return@withContext builds
                }
            }
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "getBuildInfos for $version: ${e.message}")
            emptyList()
        }
    }

    private fun parseBuildsResponse(body: String): List<BuildInfo> {
        try {
            val response = json.decodeFromString<PaperBuildsResponse>(body)
            return response.builds
                .sortedByDescending { it.id }
                .map { BuildInfo("#${it.id}", it.id.toString()) }
        } catch (e: Exception) {
            Log.e(TAG, "parseBuildsResponse: ${e.message}")
            return emptyList()
        }
    }

    override suspend fun getDownloadInfo(version: String, buildId: String): DownloadInfo? = withContext(Dispatchers.IO) {
        try {
            val endpoints = listOf(
                "https://fill.papermc.io/v3/projects/paper/versions/$version/builds",
                "https://api.papermc.io/v3/projects/paper/versions/$version/builds"
            )

            val buildToUse = buildId.ifBlank {
                // Get latest build
                val builds = getBuildInfos(version)
                builds.firstOrNull()?.id ?: return@withContext null
            }

            for (endpoint in endpoints) {
                val url = "$endpoint/$buildToUse"
                val req = Request.Builder().url(url).build()
                val body = client.newCall(req).execute().body?.string() ?: continue
                val response = json.decodeFromString<PaperBuildResponse>(body)
                val serverDownload = response.downloads?.get("server:default") ?: continue
                val sha256 = serverDownload.checksums?.get("sha256")
                return@withContext DownloadInfo(serverDownload.url, sha256, "paper-$version.jar")
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "getDownloadInfo for $version: ${e.message}")
            null
        }
    }

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
