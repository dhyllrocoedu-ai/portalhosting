package com.portalhost.app.ui.screens.home

import java.io.File

fun serverTypeLabel(type: String): String = when (type) {
    "paper" -> "Paper"
    "vanilla" -> "Vanilla"
    "fabric" -> "Fabric"
    else -> ""
}

fun formatRelativeTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m"
        else -> "${seconds}s"
    }
}

fun Float.roundToInt(): Int = (this + 0.5f).toInt()

fun readMaxPlayers(serverDir: File?): Int {
    if (serverDir == null) return 20
    return try {
        val props = java.util.Properties()
        val file = File(serverDir, "server.properties")
        if (!file.exists()) return 20
        file.inputStream().use { props.load(it) }
        props.getProperty("max-players")?.toIntOrNull() ?: 20
    } catch (_: Exception) { 20 }
}
