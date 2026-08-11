package com.portalhost.player

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import java.util.Base64

private val logger = KotlinLogging.logger {}

@Serializable
private data class MojangProfileResponse(
    val id: String? = null,
    val name: String? = null,
    val properties: List<MojangProperty> = emptyList(),
)

@Serializable
private data class MojangProperty(
    val name: String,
    val value: String,
)

@Serializable
private data class MojangTexturesPayload(
    val textures: MojangTextures = MojangTextures(),
)

@Serializable
private data class MojangTextures(
    val SKIN: MojangTextureRef? = null,
    val CAPE: MojangTextureRef? = null,
    val ELYTRA: MojangTextureRef? = null,
)

@Serializable
private data class MojangTextureRef(
    val url: String,
)

class MojangSkinService(
    private val userAgent: String = "PortalHost/5.1.0",
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private val client: HttpClient by lazy {
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                connectTimeoutMillis = 5_000
                requestTimeoutMillis = 10_000
                socketTimeoutMillis = 10_000
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
            expectSuccess = false
        }
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val inFlightLocks = mutableMapOf<String, Mutex>()

    suspend fun fetchSkinUrl(uuid: String): String? {
        val normalized = uuid.replace("-", "")
        if (normalized.length != 32) return null
        val mutex = inFlightLocks.getOrPut(normalized) { Mutex() }
        return mutex.withLock { fetchSkinUrlInternal(normalized) }
    }

    private suspend fun fetchSkinUrlInternal(uuidNoDashes: String): String? {
        val url = "https://sessionserver.mojang.com/session/minecraft/profile/$uuidNoDashes"
        return try {
            val response = client.get(url) {
                header(HttpHeaders.UserAgent, userAgent)
            }
            if (response.status == HttpStatusCode.TooManyRequests) {
                logger.warn { "Mojang rate-limited for $uuidNoDashes" }
                return null
            }
            if (response.status != HttpStatusCode.OK) {
                logger.warn { "Mojang returned ${response.status} for $uuidNoDashes" }
                return null
            }
            val body = response.bodyAsText()
            val parsed = json.decodeFromString<MojangProfileResponse>(body)
            val texturesProp = parsed.properties.firstOrNull { it.name == "textures" } ?: return null
            val decoded = runCatching {
                String(Base64.getDecoder().decode(texturesProp.value), Charsets.UTF_8)
            }.getOrNull() ?: return null
            val payload = json.parseToJsonElement(decoded).jsonObject
            val texturesObj = payload["textures"]?.jsonObject ?: return null
            val skinObj = texturesObj["SKIN"]?.jsonObject ?: return null
            skinObj["url"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            logger.warn(e) { "Failed to fetch Mojang skin for $uuidNoDashes" }
            null
        }
    }

    fun now(): Long = clock()

    fun close() {
        runCatching { client.close() }
        inFlightLocks.clear()
    }
}
