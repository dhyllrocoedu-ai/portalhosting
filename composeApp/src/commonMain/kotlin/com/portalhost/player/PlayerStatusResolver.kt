package com.portalhost.player

import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant

data class PlayerStatus(
    val isWhitelisted: Boolean = false,
    val isOp: Boolean = false,
    val opLevel: Int = 0,
    val isBanned: Boolean = false,
    val banReason: String? = null,
    val banExpires: Long? = null,
)

class PlayerStatusResolver {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun resolve(serverDir: File, uuid: String): PlayerStatus {
        val normalized = uuid.lowercase().replace("-", "")
        var whitelisted = false
        var isOp = false
        var opLevel = 0
        var isBanned = false
        var banReason: String? = null
        var banExpires: Long? = null

        runCatching {
            val wlFile = File(serverDir, "whitelist.json")
            if (wlFile.exists()) {
                whitelisted = json.decodeFromString<List<WhitelistEntry>>(wlFile.readText())
                    .any { it.uuid.replace("-", "").equals(normalized, ignoreCase = true) }
            }
        }

        runCatching {
            val opFile = File(serverDir, "ops.json")
            if (opFile.exists()) {
                val op = json.decodeFromString<List<OpEntry>>(opFile.readText())
                    .firstOrNull { it.uuid.replace("-", "").equals(normalized, ignoreCase = true) }
                if (op != null) {
                    isOp = true
                    opLevel = op.level
                }
            }
        }

        runCatching {
            val bpFile = File(serverDir, "banned-players.json")
            if (bpFile.exists()) {
                val ban = json.decodeFromString<List<BannedPlayerEntry>>(bpFile.readText())
                    .firstOrNull { it.uuid.replace("-", "").equals(normalized, ignoreCase = true) }
                if (ban != null) {
                    isBanned = true
                    banReason = ban.reason
                    banExpires = ban.expires?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
                }
            }
        }

        return PlayerStatus(
            isWhitelisted = whitelisted,
            isOp = isOp,
            opLevel = opLevel,
            isBanned = isBanned,
            banReason = banReason,
            banExpires = banExpires,
        )
    }
}
