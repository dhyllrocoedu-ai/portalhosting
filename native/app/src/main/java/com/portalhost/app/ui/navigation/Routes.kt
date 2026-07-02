package com.portalhost.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTab(
    val label: String,
    val icon: ImageVector,
    val route: String
) {
    HOME("Home", Icons.Default.Home, "home"),
    SERVERS("Servers", Icons.Default.Dns, "servers"),
    SETTINGS("Settings", Icons.Default.Settings, "settings")
}

object Routes {
    const val CREATE_SERVER = "create_server"
    const val SERVER_DETAIL = "server/{serverId}"
    fun serverDetail(id: String) = "server/$id"
    const val SERVER_FILES = "server_files/{serverId}"
    fun serverFiles(id: String) = "server_files/$id"
    const val FULL_CONSOLE = "console_full"
    const val PLAYER_MANAGEMENT = "player_management"
}
