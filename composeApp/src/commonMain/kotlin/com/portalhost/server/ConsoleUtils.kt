package com.portalhost.server

import androidx.compose.ui.graphics.Color
import com.portalhost.theme.ThemeColors

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
            ThemeColors.ConsoleLineColors.Error
        upper.contains("[WARN]") || upper.contains("[WARNING]") -> ThemeColors.ConsoleLineColors.Warning
        upper.contains("JOINED THE GAME") -> ThemeColors.ConsoleLineColors.PlayerJoin
        upper.contains("LEFT THE GAME") -> ThemeColors.ConsoleLineColors.PlayerLeave
        line.contains("<") && line.contains(">") -> ThemeColors.ConsoleLineColors.Chat
        upper.contains("[DEBUG]") || upper.contains("[FINE]") || upper.contains("[FINER]") ||
            upper.contains("[FINEST]") || upper.contains("[TRACE]") || upper.contains("[VERBOSE]") ->
            ThemeColors.ConsoleLineColors.Debug
        upper.contains("[INFO]") || upper.contains("[NOTICE]") || upper.contains("[CONFIG]") ||
            upper.contains("]: ") -> ThemeColors.ConsoleLineColors.Info
        else -> ThemeColors.ConsoleLineColors.Default
    }
}

val ALL_LOG_LEVELS = LogLevel.entries.toList()