package com.portalhost.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModrinthSearchResult(
    @SerialName("hits") val projects: List<ModrinthProject>,
    @SerialName("offset") val offset: Int,
    @SerialName("limit") val limit: Int,
    @SerialName("total_hits") val totalHits: Int
)

@Serializable
data class ModrinthProject(
    @SerialName("id") val id: String,
    @SerialName("slug") val slug: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("categories") val categories: List<String>,
    @SerialName("client_side") val clientSide: String = "optional",
    @SerialName("server_side") val serverSide: String = "optional",
    @SerialName("project_type") val projectType: String,
    @SerialName("downloads") val downloads: Int,
    @SerialName("icon_url") val iconUrl: String? = null,
    @SerialName("latest_version") val latestVersion: String? = null,
    @SerialName("versions") val versions: List<String> = emptyList(),
    @SerialName("loaders") val loaders: List<String> = emptyList(),
    @SerialName("created") val created: String? = null,
    @SerialName("updated") val updated: String? = null,
    @SerialName("followers") val followers: Int = 0,
    @SerialName("featured") val featured: Boolean = false,
    @SerialName("color") val color: Int? = null,
    @SerialName("installed") val installed: Boolean = false,
    @SerialName("installed_version") val installedVersion: String? = null
)

@Serializable
data class ModrinthVersion(
    @SerialName("id") val id: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("name") val name: String,
    @SerialName("version_number") val versionNumber: String,
    @SerialName("version_type") val versionType: String,
    @SerialName("loaders") val loaders: List<String> = emptyList(),
    @SerialName("game_versions") val gameVersions: List<String> = emptyList(),
    @SerialName("downloads") val downloads: Int,
    @SerialName("changelog") val changelog: String? = null,
    @SerialName("changelog_url") val changelogUrl: String? = null,
    @SerialName("files") val files: List<ModrinthFile> = emptyList(),
    @SerialName("date_published") val datePublished: String,
    @SerialName("featured") val featured: Boolean = false
)

@Serializable
data class ModrinthFile(
    @SerialName("url") val url: String,
    @SerialName("filename") val filename: String,
    @SerialName("size") val size: Long,
    @SerialName("sha1") val sha1: String? = null,
    @SerialName("sha512") val sha512: String? = null,
    @SerialName("release_type") val releaseType: String? = null,
    @SerialName("game_versions") val gameVersions: List<String> = emptyList()
)

@Serializable
data class ModrinthCategory(
    @SerialName("name") val name: String,
    @SerialName("project_type") val projectType: String,
    @SerialName("count") val count: Int
)

@Serializable
data class ModrinthLoader(
    @SerialName("name") val name: String,
    @SerialName("supported_project_types") val supportedProjectTypes: List<String>,
    @SerialName("icon") val icon: String,
    @SerialName("supported_game_versions") val supportedGameVersions: List<String>
)

data class MarketplaceFilters(
    val query: String = "",
    val version: String? = null,
    val loader: String? = null,
    val projectType: String? = null,
    val categories: Set<String> = emptySet(),
    val sort: MarketplaceSort = MarketplaceSort.Downloads
)

enum class MarketplaceSort(val apiValue: String, val displayName: String) {
    Downloads("downloads", "Most Downloads"),
    Updated("updated", "Recently Updated"),
    Created("newest", "Newest"),
    NameAZ("name", "Name A-Z"),
    Follows("followers", "Most Followers")
}

enum class MarketplaceProjectType(val apiValue: String, val displayName: String) {
    Plugin("plugin", "Plugin"),
    Mod("mod", "Mod"),
    Datapack("datapack", "Datapack"),
    Shader("shader", "Shader"),
    Modpack("modpack", "Modpack"),
    ResourcePack("resourcepack", "Resource Pack")
}

sealed interface MarketplaceUiState {
    data object Loading : MarketplaceUiState
    data object Initial : MarketplaceUiState
    data class Success(
        val projects: List<ModrinthProject>,
        val totalHits: Int,
        val hasMore: Boolean
    ) : MarketplaceUiState
    data class Error(val message: String) : MarketplaceUiState
}

data class ServerInstallTarget(
    val serverId: String,
    val serverName: String,
    val serverVersion: String,
    val serverType: String,
    val compatible: Boolean,
    val folderHint: String
)