package com.portalhost.desktop

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import java.io.File
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import com.portalhost.uinotify.ToastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.portalhost.desktop.screens.CreateServerScreen
import com.portalhost.desktop.screens.DashboardScreen
import com.portalhost.desktop.screens.LogViewerScreen
import com.portalhost.desktop.screens.PerformanceScreen
import com.portalhost.desktop.screens.PlayerManagementScreen
import com.portalhost.desktop.screens.RconScreen
import com.portalhost.desktop.screens.ServerConsoleScreen
import com.portalhost.desktop.screens.ServerDetailScreen
import com.portalhost.desktop.screens.ServerFilesScreen
import com.portalhost.desktop.screens.ServersScreen
import com.portalhost.desktop.screens.SettingsScreen
import com.portalhost.desktop.screens.ToastHost
import com.portalhost.di.desktopModule
import com.portalhost.di.initKoin
import com.portalhost.log.setupLogging
import com.portalhost.preferences.Preferences
import org.koin.compose.koinInject

sealed class Screen {
    object Dashboard : Screen()
    object Servers : Screen()
    object Create : Screen()
    object Settings : Screen()
    data class ServerDetail(val serverId: String) : Screen()
    data class ServerConsole(val serverId: String) : Screen()
    data class ServerFiles(val serverId: String) : Screen()
    data class ServerPlayers(val serverId: String) : Screen()
    data class ServerPerformance(val serverId: String) : Screen()
    data class ServerLogs(val serverId: String) : Screen()
    data class Rcon(val serverId: String) : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    val preferences = koinInject<Preferences>()
    val toastManager = koinInject<ToastManager>()
    val themePref by preferences.theme.collectAsState()

    val isDark = when (themePref) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) darkColorScheme() else lightColorScheme()

    val inSubScreen = currentScreen !is Screen.Dashboard &&
            currentScreen !is Screen.Servers &&
            currentScreen !is Screen.Create &&
            currentScreen !is Screen.Settings

    MaterialTheme(colorScheme = colorScheme) {
        Scaffold(
            topBar = {
                if (inSubScreen && currentScreen !is Screen.Servers) {
                    val title = when (currentScreen) {
                        is Screen.ServerDetail -> "Server Details"
                        is Screen.ServerConsole -> "Console"
                        is Screen.ServerFiles -> "Files"
                        is Screen.ServerPlayers -> "Players"
                        is Screen.ServerPerformance -> "Performance"
                        is Screen.ServerLogs -> "Logs"
                        is Screen.Rcon -> "RCON"
                        else -> ""
                    }
                    TopAppBar(
                        title = { Text(title, fontWeight = FontWeight.SemiBold) },
                        navigationIcon = {
                            IconButton(onClick = {
                                val serverId = when (currentScreen) {
                                    is Screen.ServerDetail -> (currentScreen as Screen.ServerDetail).serverId
                                    is Screen.ServerConsole -> (currentScreen as Screen.ServerConsole).serverId
                                    is Screen.ServerFiles -> (currentScreen as Screen.ServerFiles).serverId
                                    is Screen.ServerPlayers -> (currentScreen as Screen.ServerPlayers).serverId
                                    is Screen.ServerPerformance -> (currentScreen as Screen.ServerPerformance).serverId
                                    is Screen.ServerLogs -> (currentScreen as Screen.ServerLogs).serverId
                                    is Screen.Rcon -> (currentScreen as Screen.Rcon).serverId
                                    else -> null
                                }
                                currentScreen = if (serverId != null) Screen.ServerDetail(serverId) else Screen.Servers
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
                }
            },
            bottomBar = {
                if (!inSubScreen) {
                    val tabs = listOf("Dashboard", "Servers", "Create", "Settings")
                    val selectedIndex = when (currentScreen) {
                        Screen.Dashboard -> 0
                        Screen.Servers -> 1
                        Screen.Create -> 2
                        Screen.Settings -> 3
                        else -> 0
                    }
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        tabs.forEachIndexed { index, title ->
                            NavigationBarItem(
                                selected = selectedIndex == index,
                                onClick = {
                                    currentScreen = when (index) {
                                        0 -> Screen.Dashboard
                                        1 -> Screen.Servers
                                        2 -> Screen.Create
                                        3 -> Screen.Settings
                                        else -> Screen.Dashboard
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = when (index) {
                                            0 -> Icons.Filled.Dashboard
                                            1 -> Icons.Filled.Dns
                                            2 -> Icons.Filled.Dns
                                            3 -> Icons.Filled.Settings
                                            else -> Icons.Filled.Dashboard
                                        },
                                        contentDescription = title,
                                    )
                                },
                                label = { Text(title) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
.onKeyEvent { keyEvent ->
                        if (keyEvent.isCtrlPressed) {
                            when (keyEvent.key) {
                                Key.O -> {
                                    try {
                                        val folder = File("servers").absoluteFile
                                        if (!folder.exists()) folder.mkdirs()
                                        java.awt.Desktop.getDesktop().open(folder)
                                        true
                                    } catch (_: Exception) {
                                        false
                                    }
                                }
                                Key.S -> {
                                    toastManager.success("Saved!")
                                    true
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    },
                color = MaterialTheme.colorScheme.background,
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ToastHost()
                    when (currentScreen) {
                        Screen.Dashboard -> DashboardScreen(
                            onNavigateToServer = { serverId ->
                                currentScreen = Screen.ServerDetail(serverId)
                            },
                            onNavigateToCreate = { currentScreen = Screen.Create },
                        )
                        Screen.Servers -> ServersScreen(
                            onNavigateToDetail = { serverId ->
                                currentScreen = Screen.ServerDetail(serverId)
                            },
                        )
                        Screen.Create -> CreateServerScreen()
                        Screen.Settings -> SettingsScreen()
                        is Screen.ServerDetail -> ServerDetailScreen(
                            serverId = (currentScreen as Screen.ServerDetail).serverId,
                            onNavigateToConsole = { currentScreen = Screen.ServerConsole(it) },
                            onNavigateToFiles = { currentScreen = Screen.ServerFiles(it) },
                            onNavigateToPlayers = { currentScreen = Screen.ServerPlayers(it) },
                            onNavigateToPerformance = { currentScreen = Screen.ServerPerformance(it) },
                            onNavigateToLogs = { currentScreen = Screen.ServerLogs(it) },
                            onNavigateToRcon = { currentScreen = Screen.Rcon(it) },
                            onBack = { currentScreen = Screen.Servers },
                        )
                        is Screen.ServerConsole -> ServerConsoleScreen(
                            serverId = (currentScreen as Screen.ServerConsole).serverId,
                            onBack = { currentScreen = Screen.Servers },
                        )
                        is Screen.ServerFiles -> ServerFilesScreen(
                            serverId = (currentScreen as Screen.ServerFiles).serverId,
                            onBack = { currentScreen = Screen.Servers },
                        )
                        is Screen.ServerPlayers -> PlayerManagementScreen(
                            serverId = (currentScreen as Screen.ServerPlayers).serverId,
                            onBack = { currentScreen = Screen.Servers },
                        )
                        is Screen.ServerPerformance -> PerformanceScreen(
                            serverId = (currentScreen as Screen.ServerPerformance).serverId,
                            onBack = { currentScreen = Screen.Servers },
                        )
                        is Screen.ServerLogs -> LogViewerScreen(
                            serverId = (currentScreen as Screen.ServerLogs).serverId,
                            onBack = { currentScreen = Screen.Servers },
                        )
                        is Screen.Rcon -> RconScreen(
                            serverId = (currentScreen as Screen.Rcon).serverId,
                            onBack = { currentScreen = Screen.Servers },
                        )
                    }
                }
            }
        }
    }
}

fun main() = singleWindowApplication(
    title = "Portal Host",
    state = WindowState(width = 1200.dp, height = 800.dp),
) {
    initKoin(desktopModule())
    setupLogging(org.koin.core.context.GlobalContext.get().get<com.portalhost.log.LogRepository>())
    DesktopApp()
}