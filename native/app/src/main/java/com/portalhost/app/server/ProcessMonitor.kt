package com.portalhost.app.server

import android.util.Log
import java.io.File
import kotlin.math.roundToInt

data class ProcessStats(
    val cpuPercent: Float = 0f,
    val ramBytes: Long = 0,
    val maxRamBytes: Long = 0,
    val tps: Float = 20.0f,
    val rxBytesPerSec: Long = 0,
    val txBytesPerSec: Long = 0
) {
    val ramFormatted: String get() = ProcessMonitor.formatBytes(ramBytes)
    val maxRamFormatted: String get() = ProcessMonitor.formatBytes(maxRamBytes)
    val rxFormatted: String get() = ProcessMonitor.formatBytes(rxBytesPerSec) + "/s"
    val txFormatted: String get() = ProcessMonitor.formatBytes(txBytesPerSec) + "/s"
}

class ProcessMonitor {
    private val TAG = "ProcessMonitor"
    private var lastCpuTime = 0L
    private var lastWallTime = 0L
    private var lastNetRx = 0L
    private var lastNetTx = 0L
    private var lastNetTime = 0L

    fun getStats(process: Process?, maxRamMegabytes: Int = 2048): ProcessStats {
        if (process == null || !process.isAlive) {
            return ProcessStats(tps = 0f)
        }

        val pid = getPid(process)
        val cpuPercent = if (pid != null) measureCpu(pid) else 0f
        val ramBytes = if (pid != null) readRss(pid) else 0L
        val (rxRate, txRate) = if (pid != null) measureNetworkRate(pid) else 0L to 0L

        return ProcessStats(
            cpuPercent = cpuPercent,
            ramBytes = ramBytes,
            maxRamBytes = maxRamMegabytes * 1_000_000L,
            tps = 20.0f,
            rxBytesPerSec = rxRate,
            txBytesPerSec = txRate
        )
    }

    private fun getPid(process: Process): Int? {
        return try {
            val pidField = process::class.java.getDeclaredField("pid")
            pidField.isAccessible = true
            pidField.getInt(process)
        } catch (_: Exception) {
            null
        }
    }

    private fun measureCpu(pid: Int): Float {
        return try {
            val statFile = File("/proc/$pid/stat")
            if (!statFile.exists()) return 0f

            val parts = statFile.readText().split(" ")
            val utime = parts[13].toLong()
            val stime = parts[14].toLong()
            val cpuTime = utime + stime
            val wallTime = System.nanoTime()

            if (lastCpuTime == 0L) {
                lastCpuTime = cpuTime
                lastWallTime = wallTime
                return 0f
            }

            val elapsedCpu = cpuTime - lastCpuTime
            val elapsedWall = (wallTime - lastWallTime) / 10_000_000L // to centiseconds

            lastCpuTime = cpuTime
            lastWallTime = wallTime

            if (elapsedWall <= 0) return 0f
            val cores = Runtime.getRuntime().availableProcessors()
            ((elapsedCpu.toFloat() / elapsedWall.toFloat()) * 100f / cores.toFloat())
                .coerceIn(0f, 100f)
        } catch (_: Exception) {
            0f
        }
    }

    private fun readRss(pid: Int): Long {
        return try {
            val statusFile = File("/proc/$pid/status")
            if (!statusFile.exists()) return 0L
            val lines = statusFile.readLines()
            for (line in lines) {
                if (line.startsWith("VmRSS:")) {
                    val parts = line.split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        return parts[1].toLongOrNull()?.times(1024) ?: 0L
                    }
                }
            }
            0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun measureNetworkRate(pid: Int): Pair<Long, Long> {
        return try {
            val netFile = File("/proc/$pid/net/dev")
            if (!netFile.exists()) return 0L to 0L
            var rx: Long = 0
            var tx: Long = 0
            netFile.readLines().forEach { line ->
                // Lines: "  eth0: 12345 0 0 0 0 0 0 0 54321 ..."
                if (line.contains(":") && !line.contains("Inter-|") && !line.contains(" face")) {
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size >= 10) {
                        rx += parts[1].toLongOrNull() ?: 0
                        tx += parts[9].toLongOrNull() ?: 0
                    }
                }
            }
            val now = System.nanoTime()
            val elapsedNs = now - lastNetTime
            if (lastNetTime == 0L || elapsedNs <= 0) {
                lastNetRx = rx
                lastNetTx = tx
                lastNetTime = now
                return 0L to 0L
            }
            val elapsedSec = elapsedNs / 1_000_000_000.0
            val rxRate = if (elapsedSec > 0) ((rx - lastNetRx) / elapsedSec).toLong().coerceAtLeast(0) else 0L
            val txRate = if (elapsedSec > 0) ((tx - lastNetTx) / elapsedSec).toLong().coerceAtLeast(0) else 0L
            lastNetRx = rx
            lastNetTx = tx
            lastNetTime = now
            rxRate to txRate
        } catch (_: Exception) {
            0L to 0L
        }
    }

    fun parseTps(line: String): Float? {
        // Match: "TPS: 20.0" or "TPS from last 1m: 20.0" or similar
        val regex = Regex("""TPS(?: from last \d+m)?[:\s]+(\d+\.?\d*)""", RegexOption.IGNORE_CASE)
        return regex.find(line)?.groupValues?.get(1)?.toFloatOrNull()
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
