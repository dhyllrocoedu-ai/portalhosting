package com.portalhost.player

import com.portalhost.server.ServerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant

@Serializable
private data class UserCacheEntry(
    val name: String,
    val uuid: String,
    val expiresOn: String? = null,
)

class PlayerProfileRepository(
    private val serverManager: ServerManager,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun load(serverDir: File, serverId: String? = null): List<PlayerProfile> = withContext(Dispatchers.IO) {
        val userCache = File(serverDir, "usercache.json")
        if (!userCache.exists()) return@withContext emptyList()

        val entries = runCatching { json.decodeFromString<List<UserCacheEntry>>(userCache.readText()) }
            .getOrElse { emptyList() }

        entries.map { entry ->
            val uuid = normalizeUuid(entry.uuid)
            val expiresAt = entry.expiresOn?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
            val resolvedServerId = serverId ?: serverDir.name

            val joinPattern = "%${entry.name}%joined the game%"
            val firstSeen = serverManager.firstConsoleLogTimestamp(resolvedServerId, joinPattern)
            val lastSeen = serverManager.lastConsoleLogTimestamp(resolvedServerId, joinPattern)

            val effectiveFirst = firstSeen ?: expiresAt
            val effectiveLast = lastSeen ?: expiresAt

            PlayerProfile(
                uuid = uuid,
                currentName = entry.name,
                nameHistory = listOf(NameChange(entry.name, effectiveLast)),
                firstSeen = effectiveFirst,
                lastSeen = effectiveLast,
            )
        }
    }

    suspend fun findByUuid(serverDir: File, uuid: String, serverId: String? = null): PlayerProfile? = withContext(Dispatchers.IO) {
        val normalized = normalizeUuid(uuid)
        load(serverDir, serverId).firstOrNull { it.uuid.equals(normalized, ignoreCase = true) }
    }

    companion object {
        fun normalizeUuid(uuid: String): String {
            val stripped = uuid.lowercase().replace("-", "")
            if (stripped.length != 32) return uuid.lowercase()
            return buildString(36) {
                append(stripped.substring(0, 8)).append('-')
                append(stripped.substring(8, 12)).append('-')
                append(stripped.substring(12, 16)).append('-')
                append(stripped.substring(16, 20)).append('-')
                append(stripped.substring(20, 32))
            }
        }

        fun stripDashes(uuid: String): String = uuid.replace("-", "")
    }
}
