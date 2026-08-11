package com.portalhost.player

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.io.File
import java.net.URLEncoder

private val logger = KotlinLogging.logger {}

@Serializable
private data class MojangUuidResponse(val id: String? = null, val name: String? = null)

@Serializable
private data class ResolveUsercacheEntry(val name: String, val uuid: String)

/**
 * Resolves a player name to a real (Mojang) UUID. Checks the server's
 * usercache.json first, then falls back to the Mojang name->uuid API.
 * Rate-limit (429) responses return null instead of throwing.
 */
class NameToUuidResolver(
    private val userAgent: String = "PortalHost/5.1.0",
) {
    private val client by lazy {
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                connectTimeoutMillis = 5_000
                requestTimeoutMillis = 10_000
                socketTimeoutMillis = 10_000
            }
            expectSuccess = false
        }
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val usercacheLock = Any()

    suspend fun resolve(name: String, serverDir: File): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        val cached = withContext(Dispatchers.IO) {
            fromUsercache(serverDir)[trimmed.lowercase()]
        }
        if (cached != null) return cached
        return fetchMojang(trimmed)
    }

    private fun fromUsercache(serverDir: File): Map<String, String> = synchronized(usercacheLock) {
        val file = File(serverDir, "usercache.json")
        if (!file.exists()) return@synchronized emptyMap()
        val entries = runCatching { json.decodeFromString<List<ResolveUsercacheEntry>>(file.readText()) }
            .getOrElse { emptyList() }
        entries.associate { it.name.lowercase() to PlayerProfileRepository.normalizeUuid(it.uuid) }
    }

    private suspend fun fetchMojang(name: String): String? {
        val encoded = URLEncoder.encode(name, Charsets.UTF_8)
        val url = "https://api.mojang.com/users/profiles/minecraft/$encoded"
        return try {
            val response = client.get(url) { header(HttpHeaders.UserAgent, userAgent) }
            if (response.status == HttpStatusCode.TooManyRequests) {
                logger.warn { "Mojang rate-limited for name $name" }
                return null
            }
            if (response.status != HttpStatusCode.OK) {
                logger.warn { "Mojang name lookup ${response.status} for $name" }
                return null
            }
            val id = json.decodeFromString<MojangUuidResponse>(response.bodyAsText()).id ?: return null
            PlayerProfileRepository.normalizeUuid(id)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to resolve UUID for $name" }
            null
        }
    }

    fun close() {
        runCatching { client.close() }
    }
}
