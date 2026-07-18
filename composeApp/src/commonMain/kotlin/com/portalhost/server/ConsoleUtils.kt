package com.portalhost.server

import androidx.compose.ui.graphics.Color

enum class LogLevel { ALL, ERROR, WARN, INFO, PLAYER, CHAT, OTHER }

private val ALL_LEVELS = LogLevel.entries.toList()

fun classifyLogLevel(line: String): LogLevel {
    val upper = line.uppercase()
    return when {
        upper.contains("[SEVERE]") || upper.contains("[ERROR]") || upper.contains("[FATAL]") -> LogLevel.ERROR
        upper.contains("[WARN]") || upper.contains("[WARNING]") -> LogLevel.WARN
        upper.contains("JOINED THE GAME") || upper.contains("LEFT THE GAME") -> LogLevel.PLAYER
        line.contains("<") && line.contains(">") -> LogLevel.CHAT
        upper.contains("[DEBUG]") || upper.contains("[FINE]") || upper.contains("[FINER]") ||
            upper.contains("[FINEST]") || upper.contains("[TRACE]") || upper.contains("[VERBOSE]") ||
            upper.contains("[INFO]") || upper.contains("[NOTICE]") || upper.contains("[CONFIG]") ||
            upper.contains("]: ") -> LogLevel.INFO
        else -> LogLevel.OTHER
    }
}

fun consoleLineColor(line: String): Color {
    val upper = line.uppercase()
    return when {
        upper.contains("[SEVERE]") || upper.contains("[ERROR]") || upper.contains("[FATAL]") ->
            Color(0xFFFF5555)
        upper.contains("[WARN]") || upper.contains("[WARNING]") -> Color(0xFFFFAA00)
        upper.contains("JOINED THE GAME") -> Color(0xFF55FF55)
        upper.contains("LEFT THE GAME") -> Color(0xFFFFFF55)
        line.contains("<") && line.contains(">") -> Color(0xFFAA55FF)
        upper.contains("[DEBUG]") || upper.contains("[FINE]") || upper.contains("[FINER]") ||
            upper.contains("[FINEST]") || upper.contains("[TRACE]") || upper.contains("[VERBOSE]") ->
            Color(0xFF888888)
        upper.contains("[INFO]") || upper.contains("[NOTICE]") || upper.contains("[CONFIG]") ||
            upper.contains("]: ") -> Color(0xFFE0E0E0)
        else -> Color(0xFFCCCCCC)
    }
}

val ALL_LOG_LEVELS = LogLevel.entries.toList()