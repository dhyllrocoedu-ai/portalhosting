package com.portalhost.marketplace

import com.portalhost.model.*
import com.portalhost.server.providers.HttpCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.net.HttpURLConnection
import java.net.URL

class ModrinthApi {
    private val baseUrl = "https://api.modrinth.com/v2"
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    suspend fun searchProjects(
        query: String = "",
        version: String? = null,
        loader: String? = null,
        projectType: String? = null,
        categories: Set<String> = emptySet(),
        sort: MarketplaceSort = MarketplaceSort.Downloads,
        offset: Int = 0,
        limit: Int = 20
    ): Result<ModrinthSearchResult> = withContext(Dispatchers.IO) {
        try {
            val facets = buildFacets(version, loader, projectType, categories)
            val params = mutableListOf<String>()
            if (query.isNotBlank()) params.add("query=${encodeUrl(query)}")
            if (facets.isNotEmpty()) {
                val facetsJson = json.encodeToString(JsonArray.serializer(), facets)
                params.add("facets=${encodeUrl(facetsJson)}")
            }
            params.add("index=${sort.apiValue}")
            if (offset > 0) params.add("offset=$offset")
            params.add("limit=$limit")
            val url = "$baseUrl/search?${params.joinToString("&")}"

            val response = httpGet(url)
            Result.success(json.decodeFromString<ModrinthSearchResult>(response))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun encodeUrl(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    suspend fun getProject(projectId: String): Result<ModrinthProject> = withContext(Dispatchers.IO) {
        HttpCache.fetchWithCacheSuspend("$baseUrl/project/$projectId")
            .mapCatching { json.decodeFromString<ModrinthProject>(it) }
    }

    suspend fun getProjectVersions(projectId: String): Result<List<ModrinthVersion>> = withContext(Dispatchers.IO) {
        HttpCache.fetchWithCacheSuspend("$baseUrl/project/$projectId/version")
            .mapCatching { json.decodeFromString<List<ModrinthVersion>>(it) }
    }

    suspend fun getProjectVersion(versionId: String): Result<ModrinthVersion> = withContext(Dispatchers.IO) {
        HttpCache.fetchWithCacheSuspend("$baseUrl/version/$versionId")
            .mapCatching { json.decodeFromString<ModrinthVersion>(it) }
    }

    suspend fun getCategories(): Result<List<ModrinthCategory>> = withContext(Dispatchers.IO) {
        HttpCache.fetchWithCacheSuspend("$baseUrl/tag/category")
            .mapCatching { json.decodeFromString<List<ModrinthCategory>>(it) }
    }

    suspend fun getLoaders(): Result<List<ModrinthLoader>> = withContext(Dispatchers.IO) {
        HttpCache.fetchWithCacheSuspend("$baseUrl/tag/loader")
            .mapCatching { json.decodeFromString<List<ModrinthLoader>>(it) }
    }

    suspend fun getGameVersions(): Result<List<String>> = withContext(Dispatchers.IO) {
        HttpCache.fetchWithCacheSuspend("$baseUrl/tag/game_version")
            .mapCatching {
                val versions: List<JsonObject> = json.decodeFromString(it)
                versions.mapNotNull { v -> v["version"]?.jsonPrimitive?.content }
                    .filter { ver -> ver.startsWith("1.") }
            }
    }

    private fun buildFacets(
        version: String?,
        loader: String?,
        projectType: String?,
        categories: Set<String>
    ): JsonArray {
        val facetList = mutableListOf<JsonArray>()

        projectType?.let {
            facetList.add(buildJsonArray { add(buildJsonArray { add("project_type:$it") }) })
        }

        loader?.let {
            val normalized = loader.lowercase()
            val loaderFacets = when (normalized) {
                "paper" -> listOf("paper", "purpur", "folia")
                "spigot" -> listOf("spigot")
                "forge" -> listOf("forge")
                "neoforge" -> listOf("neoforge")
                "fabric" -> listOf("fabric")
                "quilt" -> listOf("quilt")
                "vanilla" -> listOf("vanilla")
                else -> listOf(normalized)
            }
            facetList.add(buildJsonArray {
                add(buildJsonArray {
                    loaderFacets.forEach { add("loader:$it") }
                })
            })
        }

        version?.let {
            facetList.add(buildJsonArray { add(buildJsonArray { add("game_version:$it") }) })
        }

        if (categories.isNotEmpty()) {
            facetList.add(buildJsonArray {
                add(buildJsonArray {
                    categories.forEach { add("categories:$it") }
                })
            })
        }

        return buildJsonArray { facetList.forEach { add(it) } }
    }

    private fun httpGet(urlString: String): String {
        var lastException: Exception? = null
        for (attempt in 0 until 3) {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("User-Agent", "PortalHost/5.0.62")

                val responseCode = conn.responseCode

                if (responseCode == 429) {
                    val retryAfter = conn.getHeaderField("Retry-After")?.toLongOrNull() ?: 30L
                    if (attempt < 2) {
                        try { Thread.sleep(retryAfter * 1000L) } catch (_: InterruptedException) { }
                        lastException = Exception("Rate limited (HTTP 429). Retry after ${retryAfter}s")
                        continue
                    }
                    throw Exception("Rate limited (HTTP 429). Retry after ${retryAfter}s")
                }

                if (responseCode !in 200..299) {
                    val errorBody = try { conn.errorStream?.bufferedReader()?.readText() ?: "" } catch (_: Exception) { "" }
                    throw Exception("HTTP $responseCode: $errorBody")
                }

                val response = conn.inputStream.bufferedReader().readText()
                return response
            } catch (e: Exception) {
                lastException = e
                if (attempt < 2) {
                    try { Thread.sleep((attempt + 1) * 1000L) } catch (_: InterruptedException) { }
                }
            }
        }
        throw lastException ?: Exception("HTTP request failed")
    }

    private fun httpPost(urlString: String, body: JsonObject): String {
        var lastException: Exception? = null
        for (attempt in 0 until 3) {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("User-Agent", "PortalHost/5.0.61")
                conn.doOutput = true

                conn.outputStream.use { os ->
                    os.write(json.encodeToString(body).toByteArray())
                }

                val responseCode = conn.responseCode

                if (responseCode !in 200..299) {
                    val errorBody = try { conn.errorStream?.bufferedReader()?.readText() ?: "" } catch (_: Exception) { "" }
                    throw Exception("HTTP $responseCode: $errorBody")
                }

                val response = conn.inputStream.bufferedReader().readText()
                return response
            } catch (e: Exception) {
                lastException = e
                if (attempt < 2) {
                    try { Thread.sleep((attempt + 1) * 1000L) } catch (_: InterruptedException) { }
                }
            }
        }
        throw lastException ?: Exception("HTTP request failed")
    }
}