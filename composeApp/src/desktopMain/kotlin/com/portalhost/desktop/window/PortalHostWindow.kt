package com.portalhost.desktop.window

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Maximize
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.portalhost.desktop.screens.CreateServerScreen
import com.portalhost.desktop.screens.DashboardScreen
import com.portalhost.desktop.screens.PlayerManagementScreen
import com.portalhost.desktop.screens.ServerConsoleScreen
import com.portalhost.desktop.screens.ServerDetailScreen
import com.portalhost.desktop.screens.ServersScreen
import com.portalhost.desktop.screens.SettingsScreen
import com.portalhost.desktop.screens.ToastHost
import com.portalhost.preferences.Preferences
import com.portalhost.server.ServerManager
import com.portalhost.filesystem.FileSystem
import com.portalhost.uinotify.ToastManager
import kotlinx.coroutines.launch

sealed class Screen {
    object Home : Screen()
    object Servers : Screen()
    data class ServerDetail(val serverId: String) : Screen()
    data class Console(val serverId: String) : Screen()
    object Create : Screen()
    object Settings : Screen()
    data class Players(val serverId: String) : Screen()
}

@Composable
fun TitleBar(
    sidebarExpanded: Boolean,
    onSidebarToggle: () -> Unit
) {
    val isDark = (MaterialTheme.colorScheme.surface.red * 0.299 + MaterialTheme.colorScheme.surface.green * 0.587 + MaterialTheme.colorScheme.surface.blue * 0.114) < 128
    val titleBarColor = if (isDark) Color(0xFF1E1E2E) else Color.White

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        color = if (isDark) Color(0xFF1E1E2E) else Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onSidebarToggle) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Toggle sidebar",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Portal Host",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.Minimize,
                        contentDescription = "Minimize",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.Maximize,
                        contentDescription = "Maximize",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun NavSidebar(
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit,
    expanded: Boolean,
    onExpandToggle: () -> Unit
) {
    val sidebarWidth = if (expanded) 240.dp else 64.dp
    
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(sidebarWidth)
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onExpandToggle) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = if (expanded) "Collapse sidebar" else "Expand sidebar",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                NavItem(
                    label = "Home",
                    icon = Icons.Default.Dashboard,
                    selected = currentScreen is Screen.Home,
                    expanded = expanded,
                    onClick = { onScreenChange(Screen.Home) }
                )
                NavItem(
                    label = "Servers",
                    icon = Icons.Default.Dns,
                    selected = currentScreen is Screen.Servers,
                    expanded = expanded,
                    onClick = { onScreenChange(Screen.Servers) }
                )
                NavItem(
                    label = "Create",
                    icon = Icons.Default.Add,
                    selected = currentScreen is Screen.Create,
                    expanded = expanded,
                    onClick = { onScreenChange(Screen.Create) }
                )
                NavItem(
                    label = "Settings",
                    icon = Icons.Default.Settings,
                    selected = currentScreen is Screen.Settings,
                    expanded = expanded,
                    onClick = { onScreenChange(Screen.Settings) }
                )
            }
        }
    }
}

@Composable
fun NavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val isDark = (MaterialTheme.colorScheme.surface.red * 0.299 + MaterialTheme.colorScheme.surface.green * 0.587 + MaterialTheme.colorScheme.surface.blue * 0.114) < 128
    val backgroundColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onClick() }
            .animateContentSize(),
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (expanded) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ScreenContent(
    screen: Screen,
    onNavigate: (Screen) -> Unit
) = when (screen) {
    Screen.Home -> DashboardScreen(
        onNavigateToConsole = { serverId -> onNavigate(Screen.Console(serverId)) },
        onNavigateToServer = { serverId -> onNavigate(Screen.ServerDetail(serverId)) },
        onNavigateToCreate = { onNavigate(Screen.Create) },
        onNavigateToPlayers = { serverId -> onNavigate(Screen.Players(serverId)) }
    )
    Screen.Servers -> ServersScreen(
        onNavigateToDetail = { serverId -> onNavigate(Screen.ServerDetail(serverId)) },
        onNavigateToCreate = { onNavigate(Screen.Create) }
    )
    is Screen.ServerDetail -> ServerDetailScreen(
        serverId = (screen as Screen.ServerDetail).serverId,
        onBack = { onNavigate(Screen.Servers) }
    )
    is Screen.Console -> ServerConsoleScreen(
        serverId = (screen as Screen.Console).serverId,
        onBack = { onNavigate(Screen.Home) }
    )
    Screen.Create -> CreateServerScreen(
        onServerCreated = { serverId -> onNavigate(Screen.ServerDetail(serverId)) },
        onBack = { onNavigate(Screen.Servers) }
    )
    is Screen.Players -> PlayerManagementScreen(
        serverId = (screen as Screen.Players).serverId,
        onBack = { onNavigate(Screen.Home) }
    )
    Screen.Settings -> SettingsScreen()
}

@Composable
fun PortalHostWindow(
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit,
    serverManager: ServerManager,
    fileSystem: FileSystem,
    toastManager: ToastManager,
    preferences: Preferences
) {
    var sidebarExpanded by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onKeyEvent { keyEvent ->
                if (keyEvent.isCtrlPressed) {
                    when (keyEvent.key) {
                        Key.N -> {
                            onScreenChange(Screen.Create)
                            true
                        }
                        Key.Q -> {
                            System.exit(0)
                            true
                        }
                        Key.R -> {
                            scope.launch { serverManager.refreshServers() }
                            toastManager.success("Server list refreshed")
                            true
                        }
                        Key.O -> {
                            try {
                                val folder = fileSystem.getServersDirBlocking()
                                java.awt.Desktop.getDesktop().open(folder)
                                true
                            } catch (_: Exception) {
                                false
                            }
                        }
                        Key.Comma -> {
                            onScreenChange(Screen.Settings)
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavSidebar(
                currentScreen = currentScreen,
                onScreenChange = onScreenChange,
                expanded = sidebarExpanded,
                onExpandToggle = { sidebarExpanded = !sidebarExpanded }
            )

            Column(modifier = Modifier.fillMaxSize()) {
                TitleBar(
                    sidebarExpanded = sidebarExpanded,
                    onSidebarToggle = { sidebarExpanded = !sidebarExpanded }
                )

                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    ScreenContent(
                        screen = currentScreen,
                        onNavigate = onScreenChange
                    )
                }

                ToastHost()
            }
        }
    }
}
