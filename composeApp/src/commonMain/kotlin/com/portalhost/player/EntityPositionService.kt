package com.portalhost.player

import com.portalhost.server.RconClient

data class PlayerPos3(
    val x: Double,
    val y: Double,
    val z: Double,
)

class EntityPositionService {
    private val posRegex = Regex("""Pos:\[(-?\d+\.?\d*)d,(-?\d+\.?\d*)d,(-?\d+\.?\d*)d]""")

    suspend fun positions(
        host: String,
        port: Int,
        password: String,
        names: List<String>,
    ): Map<String, PlayerPos3> {
        if (names.isEmpty()) return emptyMap()
        val client = RconClient(host, port, password)
        val connected = client.connect()
        if (connected.isFailure) return emptyMap()
        val results = mutableMapOf<String, PlayerPos3>()
        for (name in names) {
            val response = client.command("data get entity $name")
            response.getOrNull()?.let { body ->
                val match = posRegex.find(body)
                if (match != null) {
                    val x = match.groupValues[1].toDoubleOrNull()
                    val y = match.groupValues[2].toDoubleOrNull()
                    val z = match.groupValues[3].toDoubleOrNull()
                    if (x != null && y != null && z != null) {
                        results[name] = PlayerPos3(x, y, z)
                    }
                }
            }
        }
        client.disconnect()
        return results
    }
}
