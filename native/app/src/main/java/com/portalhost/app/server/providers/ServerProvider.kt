package com.portalhost.app.server.providers

enum class ServerType(val displayName: String, val description: String) {
    PAPER("Paper", "High-performance, feature-rich server software"),
    VANILLA("Vanilla", "Official Mojang server jar"),
    FABRIC("Fabric", "Lightweight mod loader for Minecraft"),
    FORGE("Forge", "Popular mod loader with extensive mod support")
}

data class BuildInfo(
    val label: String,
    val id: String,
    val isLatest: Boolean = false
)

data class DownloadInfo(
    val url: String,
    val sha256: String? = null,
    val suggestedFileName: String
)

interface ServerProvider {
    val type: ServerType
    val supportsBuilds: Boolean
    suspend fun getVersions(): List<String>
    suspend fun getBuildInfos(version: String): List<BuildInfo>
    suspend fun getDownloadInfo(version: String, buildId: String): DownloadInfo?
}
