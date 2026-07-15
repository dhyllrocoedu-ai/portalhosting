package com.portalhost.model

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class ServerConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val version: String,
    val buildId: String = "",
    val serverType: ServerType,
    val source: ServerSource,
    val javaVersion: Int = 21,
    val memoryMin: Int = 1024,
    val memoryMax: Int = 4096,
    val properties: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastRunAt: Long? = null,
    val iconPath: String? = null,
    val port: Int = 25565,
    val autoRestart: Boolean = false,
    val autoBackup: Boolean = false,
    val backupIntervalHours: Int = 6,
    val rconEnabled: Boolean = false,
    val rconPassword: String? = null,
    val rconPort: Int = 25575,
)

enum class ServerType {
    VANILLA, PAPER, FABRIC, FORGE, NEOFORGE, PURPUR, FOLIA
}

enum class ServerSource {
    OFFICIAL, PAPERMC, FABRICMC, FORGE, NEOFORGE, PURPUR, FOLIA
}

@Serializable
data class ServerState(
    val id: String,
    val status: ServerStatus = ServerStatus.STOPPED,
    val pid: Int? = null,
    val memoryUsage: Long = 0,
    val cpuUsage: Double = 0.0,
    val playersOnline: Int = 0,
    val maxPlayers: Int = 20,
    val uptime: Long = 0,
    val lastError: String? = null,
)

enum class ServerStatus {
    STOPPED, STARTING, RUNNING, STOPPING, CRASHED, RESTARTING
}

@Serializable
data class BackupEntry(
    val id: String = UUID.randomUUID().toString(),
    val serverId: String,
    val path: String,
    val size: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val type: BackupType = BackupType.MANUAL,
)

enum class BackupType {
    MANUAL, AUTO, PRE_UPDATE
}

@Serializable
data class ServerVersion(
    val version: String,
    val stable: Boolean = true,
    val releaseDate: String? = null,
)

@Serializable
data class ServerBuild(
    val id: String,
    val url: String,
    val label: String = id,
    val sha256: String? = null,
    val size: Long? = null,
)

@Serializable
data class JavaInstallation(
    val version: Int,
    val path: String,
    val vendor: String = "Unknown",
    val isJre: Boolean = false,
)
