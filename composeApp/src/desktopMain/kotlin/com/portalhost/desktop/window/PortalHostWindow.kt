package com.portalhost.desktop.window

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Maximize
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import com.portalhost.desktop.util.rememberResourcePainter
import com.portalhost.desktop.screens.CreateServerScreen
import com.portalhost.desktop.screens.DashboardScreen
import com.portalhost.desktop.screens.AboutScreen
import com.portalhost.desktop.screens.MarketplaceDetailScreen
import com.portalhost.desktop.screens.MarketplaceScreen
import com.portalhost.desktop.screens.PlayerManagementScreen
import com.portalhost.desktop.screens.PlayerDetailScreen
import com.portalhost.desktop.screens.RecentActivityScreen
import com.portalhost.desktop.screens.ServerConsoleScreen
import com.portalhost.desktop.screens.ServerDetailScreen
import com.portalhost.desktop.screens.ServersScreen
import com.portalhost.desktop.screens.SettingsScreen
import com.portalhost.desktop.screens.ServerFilesScreen
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
    data class PlayerDetail(val serverId: String, val uuid: String) : Screen()
    object Marketplace : Screen()
    data class MarketplaceDetail(val projectId: String) : Screen()
    object About : Screen()
    object Activity : Screen()
}

@Composable
fun TitleBar(
    iconPainter: Painter? = null,
    window: java.awt.Frame? = null,
    onMinimize: () -> Unit = {},
    onMaximizeRestore: () -> Unit = {},
    onClose: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (iconPainter != null) {
                    Image(
                        painter = iconPainter,
                        contentDescription = "Portal Host",
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(8.dp))
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
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                IconButton(onClick = onMinimize) {
                    Icon(
                        imageVector = Icons.Default.Minimize,
                        contentDescription = "Minimize",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onMaximizeRestore) {
                    Icon(
                        imageVector = Icons.Default.Maximize,
                        contentDescription = "Maximize",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onClose) {
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

    val homeIcon = rememberResourcePainter("/icons/plains_small_house_3.png")
    val serversIcon = rememberResourcePainter("/icons/ender_dragon_portal.png")
    val marketplaceIcon = rememberResourcePainter("/icons/Chest.png")
    val settingsActive = rememberResourcePainter("/icons/armadillo.webp")
    val settingsInactive = rememberResourcePainter("/icons/armadillo_rolled.webp")

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(sidebarWidth)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .drawBehind {
                drawLine(
                    color = Color(0xFF2A3144),
                    start = Offset(size.width - 1.dp.toPx(), 0f),
                    end = Offset(size.width - 1.dp.toPx(), size.height),
                    strokeWidth = 1.dp.toPx()
                )
            },
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

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NavItem(
                    label = "Home",
                    iconPainter = homeIcon,
                    selected = currentScreen is Screen.Home,
                    expanded = expanded,
                    onClick = { onScreenChange(Screen.Home) }
                )
                NavItem(
                    label = "Servers",
                    iconPainter = serversIcon,
                    selected = currentScreen is Screen.Servers,
                    expanded = expanded,
                    onClick = { onScreenChange(Screen.Servers) }
                )
                NavItem(
                    label = "Add-ons",
                    iconPainter = marketplaceIcon,
                    selected = currentScreen is Screen.Marketplace,
                    expanded = expanded,
                    onClick = { onScreenChange(Screen.Marketplace) }
                )
                NavItem(
                    label = "Settings",
                    iconPainter = if (currentScreen is Screen.Settings) settingsActive else settingsInactive,
                    selected = currentScreen is Screen.Settings,
                    expanded = expanded,
                    onClick = { onScreenChange(Screen.Settings) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

            NavItem(
                label = "About",
                iconPainter = rememberVectorPainter(Icons.Default.Info),
                selected = currentScreen is Screen.About,
                expanded = expanded,
                onClick = { onScreenChange(Screen.About) }
            )
        }
    }
}

@Composable
fun NavItem(
    label: String,
    iconPainter: androidx.compose.ui.graphics.painter.Painter,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

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
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
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
        onNavigateToPlayers = { serverId -> onNavigate(Screen.Players(serverId)) },
        onNavigateToActivity = { onNavigate(Screen.Activity) }
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
        onBack = { onNavigate(Screen.Home) },
        onOpenPlayer = { uuid -> onNavigate(Screen.PlayerDetail((screen as Screen.Players).serverId, uuid)) }
    )
    is Screen.PlayerDetail -> PlayerDetailScreen(
        serverId = (screen as Screen.PlayerDetail).serverId,
        uuid = (screen as Screen.PlayerDetail).uuid,
        onBack = { onNavigate(Screen.Players((screen as Screen.PlayerDetail).serverId)) }
    )
    Screen.Settings -> SettingsScreen()
    Screen.Marketplace -> MarketplaceScreen(
        onNavigateToDetail = { projectId -> onNavigate(Screen.MarketplaceDetail(projectId)) }
    )
    is Screen.MarketplaceDetail -> MarketplaceDetailScreen(
        projectId = (screen as Screen.MarketplaceDetail).projectId,
        onBack = { onNavigate(Screen.Marketplace) }
    )
    Screen.About -> AboutScreen(
        onBack = { onNavigate(Screen.Home) }
    )
    Screen.Activity -> RecentActivityScreen(
        onBack = { onNavigate(Screen.Home) }
    )
}

@Composable
fun PortalHostWindow(
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit,
    serverManager: ServerManager,
    fileSystem: FileSystem,
    toastManager: ToastManager,
    preferences: Preferences,
    iconPainter: Painter? = null,
    window: java.awt.Frame? = null,
    onMinimize: () -> Unit = {},
    onMaximizeRestore: () -> Unit = {},
    onClose: () -> Unit = {},
    onQuit: () -> Unit = {}
) {
    var sidebarExpanded by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    Surface(
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
                            onQuit()
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
            },
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TitleBar(
                iconPainter = iconPainter,
                window = window,
                onMinimize = onMinimize,
                onMaximizeRestore = onMaximizeRestore,
                onClose = onClose
            )

            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                NavSidebar(
                    currentScreen = currentScreen,
                    onScreenChange = onScreenChange,
                    expanded = sidebarExpanded,
                    onExpandToggle = { sidebarExpanded = !sidebarExpanded }
                )

                Column(modifier = Modifier.fillMaxSize()) {
                    ScreenContent(
                        screen = currentScreen,
                        onNavigate = onScreenChange
                    )

                    ToastHost()
                }
            }
        }
    }
}