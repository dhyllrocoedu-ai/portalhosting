package com.portalhost.app.server

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.util.Properties

data class DeviceSpec(
    val manufacturer: String,
    val model: String,
    val totalRamMb: Long,
    val isAggressiveBrand: Boolean
)

data class DeviceConfig(
    val recommendedMinRam: String,
    val recommendedMaxRam: String,
    val gcFlags: List<String>,
    val serverProps: Map<String, String>
)

object DeviceDetector {
    private val TAG = "DeviceDetector"
    private val aggressiveBrands = listOf("samsung", "xiaomi", "oppo", "vivo", "realme", "huawei", "tecno")

    fun detect(context: Context): DeviceSpec {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            val totalRamMb = memoryInfo.totalMem / (1024 * 1024)
            val brand = Build.MANUFACTURER.lowercase()
            val isAggressive = aggressiveBrands.any { brand.contains(it) }
            val spec = DeviceSpec(Build.MANUFACTURER, Build.MODEL, totalRamMb, isAggressive)
            Log.i(TAG, "Detected: $spec")
            spec
        } catch (e: Exception) {
            Log.w(TAG, "Detection failed: ${e.message}")
            DeviceSpec(Build.MANUFACTURER, Build.MODEL, 6000, false)
        }
    }

    fun generateConfig(spec: DeviceSpec): DeviceConfig {
        val maxRamMb = when {
            spec.totalRamMb <= 4500 -> 1200
            spec.totalRamMb <= 8500 -> if (spec.isAggressiveBrand) 2200 else 2800
            spec.totalRamMb <= 12500 -> 4500
            else -> 6000
        }
        val minRamMb = maxRamMb / 2

        val gcFlags = listOf(
            "-XX:+UseG1GC",
            "-XX:MaxGCPauseMillis=15",
            "-XX:ParallelGCThreads=2",
            "-XX:ConcGCThreads=1"
        )

        val serverProps = if (spec.totalRamMb <= 8500 && spec.isAggressiveBrand) {
            mapOf(
                "view-distance" to "4",
                "simulation-distance" to "4",
                "max-players" to "6",
                "network-compression-threshold" to "512"
            )
        } else {
            mapOf(
                "view-distance" to "6",
                "simulation-distance" to "5",
                "max-players" to "12"
            )
        }

        return DeviceConfig(
            recommendedMinRam = "${minRamMb}M",
            recommendedMaxRam = "${maxRamMb}M",
            gcFlags = gcFlags,
            serverProps = serverProps
        )
    }

    /** Parse a RAM value like "512M" or "2G" into MB. */
    fun parseRamMb(ram: String): Int {
        return when {
            ram.endsWith("G", ignoreCase = true) -> {
                val num = ram.dropLast(1).toFloatOrNull() ?: return 2048
                (num * 1024).toInt()
            }
            ram.endsWith("M", ignoreCase = true) -> {
                ram.dropLast(1).toIntOrNull() ?: 2048
            }
            else -> ram.toIntOrNull() ?: 2048
        }
    }

    /** Apply server property overrides to the server.properties file. */
    fun enforceServerProfile(serverDir: String, props: Map<String, String>) {
        try {
            val file = File(serverDir, "server.properties")
            if (!file.exists()) {
                Log.w(TAG, "server.properties not found, skipping profile enforcement")
                return
            }
            val p = Properties()
            file.inputStream().use { p.load(it) }
            for ((key, value) in props) {
                p.setProperty(key, value)
            }
            file.outputStream().use { p.store(it, "Configured by PortalHost hardware profile") }
            Log.i(TAG, "Applied server props: $props")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to enforce server profile: ${e.message}")
        }
    }
}
