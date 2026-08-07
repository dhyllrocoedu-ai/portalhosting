package com.portalhost.app.server.providers

import kotlinx.serialization.Serializable
import okhttp3.Response

enum class ServerType(val displayName: String, val description: String) {
    PAPER("Paper", "High-performance, feature-rich server software"),
    VANILLA("Vanilla", "Official Mojang server jar"),
    FABRIC("Fabric", "Lightweight mod loader for Minecraft"),
    FORGE("Forge", "Popular mod loader with extensive mod support"),
    NEOFORGE("NeoForge", "Next-generation Forge fork for modern Minecraft"),
    FOLIA("Folia", "Region-based multithreading server fork of Paper"),
    PURPUR("Purpur", "Feature-rich fork of Paper with customizability")
}

@Serializable
data class BuildInfo(
    val label: String,
    val id: String,
    val isLatest: Boolean = false
)

class ServerProviderException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Reads the response body and throws a [ServerProviderException] with the HTTP status
 * (and a snippet of the body) when the request was not successful. This lets callers
 * surface real errors instead of silently returning empty lists.
 */
fun Response.bodyOrThrow(url: String): String {
    val body = this.body?.string() ?: ""
    if (!this.isSuccessful) {
        throw ServerProviderException("HTTP ${this.code} from $url: ${body.take(200)}")
    }
    return body
}

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
