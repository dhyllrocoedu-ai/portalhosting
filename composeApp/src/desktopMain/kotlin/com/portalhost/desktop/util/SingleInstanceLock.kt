package com.portalhost.desktop.util

import com.portalhost.filesystem.defaultDataDir
import mu.KotlinLogging
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.FileLock

private val logger = KotlinLogging.logger {}

class SingleInstanceLock private constructor(
    private val lockFile: File,
    private val randomAccessFile: RandomAccessFile,
    private val channel: FileChannel,
    private val lock: FileLock,
) {
    fun release() {
        try { lock.release() } catch (_: Exception) {}
        try { channel.close() } catch (_: Exception) {}
        try { randomAccessFile.close() } catch (_: Exception) {}
        try { lockFile.delete() } catch (_: Exception) {}
    }

    companion object {
        private const val LOCK_FILE_NAME = "portalhost.instance.lock"

        @Volatile
        private var current: SingleInstanceLock? = null

        fun acquire(): SingleInstanceLock? {
            val existing = current
            if (existing != null) return existing
            synchronized(this) {
                val already = current
                if (already != null) return already
                val dataDir = runCatching { defaultDataDir() }.getOrNull() ?: return null
                dataDir.mkdirs()
                val file = File(dataDir, LOCK_FILE_NAME)
                val raf = runCatching { RandomAccessFile(file, "rw") }.getOrNull() ?: return null
                val channel = raf.channel
                val lock = runCatching { channel.tryLock() }.getOrNull()
                if (lock == null) {
                    runCatching { channel.close() }
                    runCatching { raf.close() }
                    logger.warn { "Another PortalHost instance is already running" }
                    return null
                }
                val instance = SingleInstanceLock(file, raf, channel, lock)
                current = instance
                logger.info { "Acquired single-instance lock at ${file.absolutePath}" }
                return instance
            }
        }
    }
}
