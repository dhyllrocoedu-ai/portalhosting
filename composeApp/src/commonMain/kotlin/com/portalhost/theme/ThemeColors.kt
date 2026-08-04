package com.portalhost.theme

import androidx.compose.ui.graphics.Color
import com.portalhost.model.ServerStatus
import com.portalhost.model.ServerType
import com.portalhost.uinotify.ToastType

object ThemeColors {

    // Semantic status colors
    val StatusSuccess = Color(0xFF4ADE80)
    val StatusWarning = Color(0xFFF59E0B)
    val StatusError = Color(0xFFEF4444)
    val StatusNeutral = Color(0xFF9CA3AF)
    val StatusInfo = Color(0xFF40C4FF)
    val StatusStarting = Color(0xFFFACC15)
    val StatusStopping = Color(0xFFFB923C)

    // Toast colors
    fun toastBackground(type: ToastType): Color = when (type) {
        ToastType.Info -> Color(0xFFE3F2FD)
        ToastType.Success -> Color(0xFFE8F5E9)
        ToastType.Warning -> Color(0xFFFFF3E0)
        ToastType.Error -> Color(0xFFFBE9E7)
    }

    fun toastIconColor(type: ToastType): Color = when (type) {
        ToastType.Info -> Color(0xFF2196F3)
        ToastType.Success -> StatusSuccess
        ToastType.Warning -> Color(0xFFFF9800)
        ToastType.Error -> StatusError
    }

    // Server status badge colors
    fun serverStatusColor(status: ServerStatus): Color = when (status) {
        ServerStatus.RUNNING -> StatusSuccess
        ServerStatus.STARTING -> StatusStarting
        ServerStatus.STOPPING -> StatusStopping
        ServerStatus.CRASHED -> StatusError
        ServerStatus.RESTARTING -> StatusStarting
        ServerStatus.STOPPED -> StatusNeutral
    }

    // Server type colors
    fun serverTypeColor(type: ServerType): Color = when (type) {
        ServerType.PAPER -> Color(0xFF4ADE80)
        ServerType.FABRIC -> Color(0xFF40C4FF)
        ServerType.FORGE -> Color(0xFFF97316)
        ServerType.NEOFORGE -> Color(0xFFD97706)
        ServerType.PURPUR -> Color(0xFFF4D03F)
        ServerType.FOLIA -> Color(0xFFA855F7)
        ServerType.VANILLA -> StatusNeutral
    }

    // Tunnel status colors
    fun tunnelStatusColor(status: com.portalhost.server.TunnelStatus, isDark: Boolean): Color = when (status) {
        com.portalhost.server.TunnelStatus.CONNECTED -> if (isDark) Color(0xFF81C784) else StatusSuccess
        com.portalhost.server.TunnelStatus.ERROR -> StatusError
        else -> if (isDark) Color(0xFFB0BEC5) else Color(0xFF757575)
    }

    // Activity log colors
    object ActivityLogColors {
        val Start = StatusSuccess
        val Stop = Color(0xFF9CA3AF)
        val Error = StatusError
        val Warning = StatusWarning
        val Leave = Color(0xFFFB923C)
        val Player = StatusInfo
        val Command = Color(0xFF7C4DFF)
        val Kill = Color(0xFFD500F9)
    }

    // Log viewer colors
    object LogViewerColors {
        val Error = StatusError
        val Warning = Color(0xFFFF9800)
        val Info = StatusSuccess
        val Default = Color(0xFFD4D4D4)
    }

    // RCON colors
    object RconColors {
        val Connecting = StatusWarning
        val Connected = StatusSuccess
        val Disconnected = StatusNeutral
        val Error = StatusError
        val SentMessage = Color(0xFF64B5F6)
        val Received = Color(0xFFE0E0E0)
    }

    // Performance stat colors
    object PerformanceColors {
        val Cpu = Color(0xFF7C4DFF)
        val Ram = Color(0xFF40C4FF)
        val Tps = Color(0xFFD500F9)
        val Players = Color(0xFF4ADE80)
        val NetworkRx = Color(0xFF40C4FF)
        val NetworkTx = Color(0xFFF59E0B)
    }

    // Storage breakdown colors
    object StorageColors {
        val World = Color(0xFF40C4FF)
        val Plugins = Color(0xFF7C4DFF)
        val Mods = Color(0xFFD500F9)
        val Datapacks = Color(0xFF4ADE80)
        val Resourcepacks = Color(0xFFF4D03F)
        val Other = Color(0xFF9CA3AF)
    }

    // Console line colors
    object ConsoleLineColors {
        val Error = Color(0xFFEF4444)
        val Warning = Color(0xFFF59E0B)
        val PlayerJoin = Color(0xFF4ADE80)
        val PlayerLeave = Color(0xFFF4D03F)
        val Chat = Color(0xFFD500F9)
        val Debug = Color(0xFF8B93A7)
        val Info = Color(0xFFE8EAF2)
        val Default = Color(0xFFC5CAD5)
        val Success = Color(0xFF4ADE80)
    }

    // MOTD color codes (Minecraft formatting codes)
    val MotdColors = mapOf(
        '0' to Color(0xFF000000),
        '1' to Color(0xFF0000AA),
        '2' to Color(0xFF00AA00),
        '3' to Color(0xFF00AAAA),
        '4' to Color(0xFFAA0000),
        '5' to Color(0xFFAA00AA),
        '6' to Color(0xFFFFAA00),
        '7' to Color(0xFFAAAAAA),
        '8' to Color(0xFF555555),
        '9' to Color(0xFF5555FF),
        'a' to Color(0xFF55FF55),
        'b' to Color(0xFF55FFFF),
        'c' to Color(0xFFFF5555),
        'd' to Color(0xFFFF55FF),
        'e' to Color(0xFFFFFF55),
        'f' to Color(0xFFFFFFFF),
    )
}