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
        current = null
    }

    companion object {
        private const val LOCK_FILE_NAME = "portalhost.instance.lock"

        @Volatile
        private var current: SingleInstanceLock? = null

        /**
         * Attempts to acquire the single-instance lock. Returns null when
         * another live PortalHost instance holds the lock (or when a lock we
         * cannot safely recover is in the way).
         *
         * Stale locks are recovered automatically: the lock file stores the
         * owning process PID, so a leftover file from a process that died (e.g.
         * a machine restart while the app was minimized to the tray) is
         * detected as stale and replaced instead of blocking startup.
         */
        fun acquire(): SingleInstanceLock? {
            val existing = current
            if (existing != null) return existing
            synchronized(this) {
                val already = current
                if (already != null) return already
                val dataDir = runCatching { defaultDataDir() }.getOrNull()
                    ?: runCatching { File(System.getProperty("user.home") ?: ".") }.getOrNull()
                    ?: return null
                runCatching { dataDir.mkdirs() }
                return acquireAt(File(dataDir, LOCK_FILE_NAME))
            }
        }

        /** Test seam: acquire a lock on an explicit file. */
        internal fun acquireAt(file: File): SingleInstanceLock? {
            var instance = tryAcquire(file)
            if (instance == null && isStaleLock(file)) {
                logger.warn { "Removing stale single-instance lock file: ${file.absolutePath}" }
                runCatching { file.delete() }
                instance = tryAcquire(file)
            }

            if (instance == null) {
                logger.warn { "Another PortalHost instance is already running" }
                return null
            }
            writeOwnerInfo(instance.randomAccessFile)
            current = instance
            logger.info { "Acquired single-instance lock at ${file.absolutePath}" }
            return instance
        }

        private fun tryAcquire(file: File): SingleInstanceLock? {
            return try {
                val raf = RandomAccessFile(file, "rw")
                val channel = raf.channel
                val lock = channel.tryLock()
                if (lock == null) {
                    runCatching { channel.close() }
                    runCatching { raf.close() }
                    null
                } else {
                    SingleInstanceLock(file, raf, channel, lock)
                }
            } catch (e: Exception) {
                logger.warn(e) { "Failed to acquire single-instance lock at ${file.absolutePath}" }
                null
            }
        }

        private fun isStaleLock(file: File): Boolean {
            val pid = readOwnerPid(file) ?: return false
            val alive = try {
                ProcessHandle.of(pid).map { it.isAlive }.orElse(false)
            } catch (_: Exception) {
                false
            }
            return !alive
        }

        private fun readOwnerPid(file: File): Long? {
            return try {
                file.readLines().firstOrNull()?.trim()?.toLongOrNull()
            } catch (_: Exception) {
                null
            }
        }

        private fun writeOwnerInfo(raf: RandomAccessFile) {
            try {
                raf.setLength(0)
                raf.seek(0)
                raf.writeBytes(ProcessHandle.current().pid().toString() + "\n")
                raf.writeBytes(System.currentTimeMillis().toString() + "\n")
            } catch (_: Exception) { }
        }

        /**
         * Shown when a second instance is launched while the first is still
         * running (typically minimized to the system tray).
         */
        fun showAlreadyRunningDialog() {
            try {
                java.awt.EventQueue.invokeLater {
                    javax.swing.JOptionPane.showMessageDialog(
                        null,
                        "Portal Host is already running.\n\n" +
                            "Look for the Portal Host icon in the system tray (near the clock) and " +
                            "right-click it, then choose \"Show PortalHost\".\n\n" +
                            "If you are sure no other instance is running, delete the file\n" +
                            "<LOCALAPPDATA>\\PortalHost\\portalhost.instance.lock\n" +
                            "and start Portal Host again.",
                        "Portal Host - Already Running",
                        javax.swing.JOptionPane.WARNING_MESSAGE
                    )
                }
            } catch (_: Throwable) { }
        }
    }
}
