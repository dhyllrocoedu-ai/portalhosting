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

private val PLUGIN_SERVER_TYPES = setOf("paper", "spigot", "purpur", "folia", "vanilla")
private val MOD_SERVER_TYPES = setOf("fabric", "quilt", "forge", "neoforge")

/**
 * Suggests the install subfolder inside a server directory.
 *
 * The target server type takes precedence over Modrinth's project metadata so a
 * plugin installed to a Forge/Fabric server lands in `mods` and a mod installed
 * to a Paper/Purpur/Folia/Vanilla server lands in `plugins`. Datapacks always go
 * to `world/datapacks`. Falls back to project metadata when no server type is given.
 */
fun getSuggestedFolder(project: ModrinthProject, serverType: String? = null): String {
    val projectType = project.projectType.lowercase()
    val loaders = project.loaders.map { it.lowercase() }
    val hasDatapackLoader = "datapack" in loaders
    val serverTypeLower = serverType?.lowercase()
    val isPluginServer = serverTypeLower in PLUGIN_SERVER_TYPES
    val isModServer = serverTypeLower in MOD_SERVER_TYPES

    return when (projectType) {
        "datapack" -> "world/datapacks"
        "resourcepack" -> "resourcepacks"
        "shader" -> "shaderpacks" // client-side only, will be disabled in UI
        "modpack" -> "modpacks"   // not a single-file install, will be disabled in UI
        "mod" -> {
            // Datapacks take precedence - some "mod" projects are actually datapacks
            if (hasDatapackLoader) "world/datapacks"
            // Server type takes precedence - plugin servers load these as plugins
            else if (isPluginServer) "plugins"
            else "mods"
        }
        "plugin" -> {
            // Server type takes precedence - mod servers load these as mods
            if (isModServer) "mods"
            else "plugins"
        }
        else -> {
            // Fallback: infer from the target server type, then from project loaders
            when {
                hasDatapackLoader -> "world/datapacks"
                isPluginServer -> "plugins"
                isModServer -> "mods"
                loaders.any { it in PLUGIN_SERVER_TYPES } -> "plugins"
                loaders.any { it in MOD_SERVER_TYPES } -> "mods"
                else -> "plugins" // safe default
            }
        }
    }
}

fun isProjectCompatible(project: ModrinthProject, serverVersion: String, serverLoader: String): Boolean {
    val versionMatch = project.versions.any { ver ->
        ver.startsWith(serverVersion) || serverVersion.startsWith(ver)
    }

    return versionMatch && loadersMatch(project.loaders, serverLoader)
}

/**
 * Compatibility check against a specific Modrinth version's game versions and loaders.
 * This is more accurate than [isProjectCompatible] because the search hit only exposes
 * coarse project-level versions/loaders, while a selected version has precise
 * [ModrinthVersion.gameVersions] and [ModrinthVersion.loaders].
 */
fun isVersionCompatible(version: ModrinthVersion, serverVersion: String, serverLoader: String): Boolean {
    val versionMatch = version.gameVersions.any { gameVersion ->
        gameVersion.startsWith(serverVersion) || serverVersion.startsWith(gameVersion)
    }

    return versionMatch && loadersMatch(version.loaders, serverLoader)
}

private fun loadersMatch(loaders: List<String>, serverLoader: String): Boolean {
    val serverLoaderLower = serverLoader.lowercase()
    return loaders.any { loader ->
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
}
