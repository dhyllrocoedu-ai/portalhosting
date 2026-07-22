package com.portalhost.theme

import androidx.compose.ui.graphics.Color
import com.portalhost.model.ServerStatus
import com.portalhost.model.ServerType
import com.portalhost.uinotify.ToastType

object ThemeColors {

    // Semantic status colors
    val StatusSuccess = Color(0xFF4CAF50)
    val StatusWarning = Color(0xFFFFC107)
    val StatusError = Color(0xFFF44336)
    val StatusNeutral = Color(0xFF9E9E9E)
    val StatusInfo = Color(0xFF2196F3)

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
        ServerStatus.STARTING -> StatusWarning
        ServerStatus.STOPPING -> Color(0xFFFF9800)
        ServerStatus.CRASHED -> StatusError
        ServerStatus.RESTARTING -> StatusWarning
        ServerStatus.STOPPED -> StatusNeutral
    }

    // Server type colors
    fun serverTypeColor(type: ServerType): Color = when (type) {
        ServerType.PAPER -> Color(0xFF4CAF50)
        ServerType.FABRIC -> Color(0xFF2196F3)
        ServerType.FORGE -> Color(0xFFFF9800)
        ServerType.NEOFORGE -> Color(0xFF9C27B0)
        ServerType.PURPUR -> Color(0xFFFFEB3B)
        ServerType.FOLIA -> Color(0xFF7B1FA2)
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
        val Error = StatusError
        val Warning = StatusWarning
        val Leave = Color(0xFFFF9800)
        val Player = StatusInfo
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
        val Cpu = Color(0xFF5C6BC0)
        val Ram = StatusSuccess
        val Tps = Color(0xFFFF9800)
        val Players = Color(0xFF42A5F5)
        val NetworkRx = Color(0xFF42A5F5)
        val NetworkTx = Color(0xFFFF9800)
    }

    // Storage breakdown colors
    object StorageColors {
        val World = Color(0xFF42A5F5)
        val Plugins = Color(0xFFAB47BC)
        val Mods = Color(0xFFFF9800)
        val Datapacks = Color(0xFF66BB6A)
        val Resourcepacks = Color(0xFFFFEE58)
        val Other = Color(0xFF78909C)
    }

    // Console line colors
    object ConsoleLineColors {
        val Error = Color(0xFFFF5555)
        val Warning = Color(0xFFFFAA00)
        val PlayerJoin = Color(0xFF55FF55)
        val PlayerLeave = Color(0xFFFFFF55)
        val Chat = Color(0xFFAA55FF)
        val Debug = Color(0xFF888888)
        val Info = Color(0xFFE0E0E0)
        val Default = Color(0xFFCCCCCC)
        val Success = Color(0xFF4CAF50)
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