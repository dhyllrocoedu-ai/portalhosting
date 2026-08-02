package com.portalhost.marketplace

import com.portalhost.model.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MarketplaceRepository(
    private val api: ModrinthApi = ModrinthApi()
) {
    private val searchCache = mutableMapOf<String, CachedResult>()
    private val projectCache = mutableMapOf<String, CachedResult>()
    private val versionsCache = mutableMapOf<String, CachedResult>()
    private val cacheMutex = Mutex()
    private val cacheTtlMs = 5 * 60 * 1000L

    data class CachedResult(val data: String, val timestamp: Long)

    private fun cacheKey(
        query: String,
        version: String?,
        loader: String?,
        projectType: String?,
        categories: Set<String>,
        sort: MarketplaceSort,
        offset: Int,
        limit: Int
    ) = "$query|$version|$loader|$projectType|${categories.sorted()}|${sort.apiValue}|$offset|$limit"

    suspend fun searchProjects(
        query: String = "",
        version: String? = null,
        loader: String? = null,
        projectType: String? = null,
        categories: Set<String> = emptySet(),
        sort: MarketplaceSort = MarketplaceSort.Downloads,
        offset: Int = 0,
        limit: Int = 20
    ): Result<ModrinthSearchResult> = cacheMutex.withLock {
        val key = cacheKey(query, version, loader, projectType, categories, sort, offset, limit)
        val cached = searchCache[key]
        if (cached != null && System.currentTimeMillis() - cached.timestamp < cacheTtlMs) {
            return@withLock Result.success(parseSearchResult(cached.data))
        }
        // Default to server-relevant project types when no explicit projectType filter
        val effectiveProjectType = projectType
        val effectiveCategories = categories
        if (projectType == null && loader == null && categories.isEmpty()) {
            // No explicit filter: auto-restrict to server-relevant types
            // This is handled by passing a special marker to the API
        }
        val result = api.searchProjects(query, version, loader, projectType, categories, sort, offset, limit, serverAddonsOnly = (projectType == null && loader == null && categories.isEmpty()))
        result.onSuccess {
            searchCache[key] = CachedResult(serializeSearchResult(it), System.currentTimeMillis())
        }
        result
    }

    suspend fun getProject(projectId: String): Result<ModrinthProject> = cacheMutex.withLock {
        val cached = projectCache[projectId]
        if (cached != null && System.currentTimeMillis() - cached.timestamp < cacheTtlMs) {
            return@withLock Result.success(parseProject(cached.data))
        }
        val result = api.getProject(projectId)
        result.onSuccess {
            projectCache[projectId] = CachedResult(serializeProject(it), System.currentTimeMillis())
        }
        result
    }

    suspend fun getProjectVersions(projectId: String): Result<List<ModrinthVersion>> = cacheMutex.withLock {
        val cached = versionsCache[projectId]
        if (cached != null && System.currentTimeMillis() - cached.timestamp < cacheTtlMs) {
            return@withLock Result.success(parseVersions(cached.data))
        }
        val result = api.getProjectVersions(projectId)
        result.onSuccess {
            versionsCache[projectId] = CachedResult(serializeVersions(it), System.currentTimeMillis())
        }
        result
    }

    fun clearCache() {
        searchCache.clear()
        projectCache.clear()
        versionsCache.clear()
    }

    private fun parseSearchResult(data: String): ModrinthSearchResult {
        val json = createJson()
        return json.decodeFromString<ModrinthSearchResult>(data)
    }

    private fun parseProject(data: String): ModrinthProject {
        return createJson().decodeFromString<ModrinthProject>(data)
    }

    private fun parseVersions(data: String): List<ModrinthVersion> {
        return createJson().decodeFromString(data)
    }

    private fun serializeSearchResult(result: ModrinthSearchResult): String {
        return createJson().encodeToString(ModrinthSearchResult.serializer(), result)
    }

    private fun serializeProject(project: ModrinthProject): String {
        return createJson().encodeToString(ModrinthProject.serializer(), project)
    }

    private fun serializeVersions(versions: List<ModrinthVersion>): String {
        return createJson().encodeToString(versions)
    }

    private fun createJson() = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
}

fun formatDownloads(downloads: Int): String {
    return when {
        downloads >= 1_000_000 -> "%.1fM".format(downloads / 1_000_000.0)
        downloads >= 1_000 -> "%.1fK".format(downloads / 1_000.0)
        else -> downloads.toString()
    }
}

fun getSuggestedFolder(project: ModrinthProject): String {
    val projectType = project.projectType.lowercase()
    val loaders = project.loaders.map { it.lowercase() }
    val hasDatapackLoader = "datapack" in loaders

    // Project type takes precedence — Modrinth's project_type is the primary discriminator
    return when (projectType) {
        "datapack" -> "world/datapacks"
        "resourcepack" -> "resourcepacks"
        "shader" -> "shaderpacks" // client-side only, will be disabled in UI
        "modpack" -> "modpacks"   // not a single-file install, will be disabled in UI
        "mod" -> {
            // Mods: loader determines fabric/forge/neoforge/quilt -> mods
            if (loaders.any { it in listOf("fabric", "quilt", "forge", "neoforge") }) "mods"
            else if (hasDatapackLoader) "world/datapacks" // some "mod" projects are actually datapacks
            else "mods"
        }
        "plugin" -> {
            // Plugins: loader determines paper/spigot/purpur/folia -> plugins
            if (loaders.any { it in listOf("paper", "spigot", "purpur", "folia") }) "plugins"
            else "plugins"
        }
        else -> {
            // Fallback: infer from loaders for any unrecognized project_type
            when {
                hasDatapackLoader -> "world/datapacks"
                loaders.any { it in listOf("paper", "spigot", "purpur", "folia") } -> "plugins"
                loaders.any { it in listOf("forge", "neoforge", "fabric", "quilt") } -> "mods"
                else -> "plugins" // safe default
            }
        }
    }
}

fun isProjectCompatible(project: ModrinthProject, serverVersion: String, serverLoader: String): Boolean {
    val versionMatch = project.versions.any { ver ->
        ver.startsWith(serverVersion) || serverVersion.startsWith(ver)
    }

    val serverLoaderLower = serverLoader.lowercase()
    val loaderMatch = project.loaders.any { loader ->
        val l = loader.lowercase()
        when {
            serverLoaderLower in listOf("paper", "spigot", "purpur", "folia") -> l in listOf("paper", "spigot", "purpur", "folia", "datapack")
            serverLoaderLower == "forge" -> l == "forge"
            serverLoaderLower == "neoforge" -> l == "neoforge"
            serverLoaderLower in listOf("fabric", "quilt") -> l in listOf("fabric", "quilt", "datapack")
            serverLoaderLower == "vanilla" -> l == "vanilla" || l.isEmpty() || l == "datapack"
            else -> true
        }
    }

    return versionMatch && loaderMatch
}