package com.portalhost.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ProcessStats(
    val cpuPercent: Float = 0f,
    val ramBytes: Long = 0,
    val maxRamBytes: Long = 0,
    val tps: Float = 20.0f,
    val rxBytesPerSec: Long = 0,
    val txBytesPerSec: Long = 0,
) {
    val ramFormatted: String get() = ProcessMonitor.formatBinaryBytes(ramBytes)
    val maxRamFormatted: String get() = ProcessMonitor.formatBinaryBytes(maxRamBytes)
    val rxFormatted: String get() = ProcessMonitor.formatBinaryBytes(rxBytesPerSec) + "/s"
    val txFormatted: String get() = ProcessMonitor.formatBinaryBytes(txBytesPerSec) + "/s"
}

class ProcessMonitor {
    private var lastCpuTime = 0L
    private var lastWallTime = 0L
    private var lastNetRx = 0L
    private var lastNetTx = 0L
    private var lastNetTime = 0L

    fun resetNetworkStats() {
        lastNetRx = 0L
        lastNetTx = 0L
        lastNetTime = 0L
    }

    suspend fun getStats(process: java.lang.Process?, maxRamMegabytes: Int = 2048): ProcessStats = withContext(Dispatchers.IO) {
        if (process == null || !process.isAlive) {
            return@withContext ProcessStats(tps = 0f)
        }

        val pid = getPid(process)
        val cpuPercent = if (pid != null) measureCpu(pid) else 0f
        val ramBytes = if (pid != null) readRss(pid) else 0L
        val (rxRate, txRate) = measureNetworkRate()

        ProcessStats(
            cpuPercent = cpuPercent,
            ramBytes = ramBytes,
            maxRamBytes = maxRamMegabytes * 1_048_576L,
            tps = 20.0f,
            rxBytesPerSec = rxRate,
            txBytesPerSec = txRate,
        )
    }

    private fun getPid(process: java.lang.Process): Int? {
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

    private fun measureNetworkRate(): Pair<Long, Long> {
        return try {
            var rx: Long
            var tx: Long
            val netFile = File("/proc/net/dev")
            if (netFile.exists()) {
                var rxTotal = 0L
                var txTotal = 0L
                netFile.readLines().forEach { line ->
                    if (line.contains(":") && !line.contains("Inter-|") && !line.contains(" face")) {
                        val parts = line.trim().split("\\s+".toRegex())
                        if (parts.size >= 10) {
                            val iface = parts[0].removeSuffix(":")
                            if (iface == "lo") return@forEach
                            rxTotal += parts[1].toLongOrNull() ?: 0
                            txTotal += parts[9].toLongOrNull() ?: 0
                        }
                    }
                }
                rx = rxTotal
                tx = txTotal
            } else {
                rx = 0L
                tx = 0L
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
        val regex = Regex("""TPS(?: from last \d+m)?[:\s]+(\d+\.?\d*)""", RegexOption.IGNORE_CASE)
        return regex.find(line)?.groupValues?.get(1)?.toFloatOrNull()
    }

    companion object {
        fun formatBinaryBytes(bytes: Long): String {
            return when {
                bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
                bytes >= 1_048_576L -> "%.0f MB".format(bytes / 1_048_576.0)
                bytes >= 1_024L -> "%.0f KB".format(bytes / 1_024.0)
                else -> "$bytes B"
            }
        }
    }
}