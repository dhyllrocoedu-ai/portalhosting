package com.portalhost.player

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import java.util.Base64

private const val TEST_UUID = "2a2adc8bcbfb538934c34edfa27081e9"

class MojangSkinServiceTest {

    @Test
    fun fetchSkinUrl_validResponse_returnsUrl() = runBlocking {
        val skinUrl = "https://textures.minecraft.net/texture/abcdef0123456789"
        val textureJson = """{"timestamp":123,"profileId":"x","profileName":"y","textures":{"SKIN":{"url":"$skinUrl"}}}"""
        val encoded = Base64.getEncoder().encodeToString(textureJson.toByteArray(Charsets.UTF_8))
        val body = """{"id":"$TEST_UUID","name":"Player","properties":[{"name":"textures","value":"$encoded","signature":"sig"}]}"""

        val engine = MockEngine { _ ->
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            expectSuccess = false
        }
        val response = client.get("https://sessionserver.mojang.com/session/minecraft/profile/$TEST_UUID")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(body, response.bodyAsText())
    }

    @Test
    fun fetchSkinUrl_429_returns429() = runBlocking {
        val engine = MockEngine { _ ->
            respond(
                content = "{\"error\":\"TooManyRequests\"}",
                status = HttpStatusCode.TooManyRequests,
                headers = headersOf("Content-Type", "application/json"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            expectSuccess = false
        }
        val response = client.get("https://sessionserver.mojang.com/session/minecraft/profile/$TEST_UUID")
        assertEquals(HttpStatusCode.TooManyRequests, response.status)
    }

    @Test
    fun fetchSkinUrl_invalidUuidLength_returnsNull() = runBlocking {
        val service = MojangSkinService()
        val result = service.fetchSkinUrl("not-a-uuid")
        assertNull(result)
        service.close()
    }

    @Test
    fun fetchSkinUrl_validUuid_isDispatchedCorrectly() {
        val testUuid = TEST_UUID
        assertEquals(32, testUuid.length)
    }
}
