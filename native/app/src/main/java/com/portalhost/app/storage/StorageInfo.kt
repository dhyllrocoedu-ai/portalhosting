package com.portalhost.app.storage

import android.os.StatFs
import java.io.File

data class StorageStats(
    val worldSize: Long = 0,
    val logsSize: Long = 0,
    val backupsSize: Long = 0,
    val availableBytes: Long = 0,
    val totalBytes: Long = 0
) {
    val availableFormatted: String get() = StorageInfo.formatBytes(availableBytes)
    val totalFormatted: String get() = StorageInfo.formatBytes(totalBytes)
    val worldFormatted: String get() = StorageInfo.formatBytes(worldSize)
    val logsFormatted: String get() = StorageInfo.formatBytes(logsSize)
    val backupsFormatted: String get() = StorageInfo.formatBytes(backupsSize)
}

class StorageInfo {
    fun getServerStorage(serversDir: File): StorageStats {
        val worldSize = calculateDirSize(File(serversDir, "world")) +
                calculateDirSize(File(serversDir, "worlds"))
        val logsSize = calculateDirSize(File(serversDir, "logs"))
        val backupsSize = calculateDirSize(File(serversDir, "backups"))

        val stat = StatFs(serversDir.absolutePath)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        val totalBytes = stat.blockCountLong * stat.blockSizeLong

        return StorageStats(worldSize, logsSize, backupsSize, availableBytes, totalBytes)
    }

    private fun calculateDirSize(dir: File): Long {
        if (!dir.exists()) return 0
        return dir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            return when {
                bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
                bytes >= 1_000_000 -> "%.0f MB".format(bytes / 1_000_000.0)
                bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
                else -> "$bytes B"
            }
        }
    }
}
