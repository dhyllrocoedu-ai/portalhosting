package com.portalhost.desktop

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.WindowState
import com.portalhost.uinotify.ToastManager
import com.portalhost.desktop.screens.CreateServerScreen
import com.portalhost.desktop.screens.DashboardScreen
import com.portalhost.desktop.screens.PlayerManagementScreen
import com.portalhost.desktop.screens.ServerConsoleScreen
import com.portalhost.desktop.screens.ServerDetailScreen
import com.portalhost.desktop.screens.ServersScreen
import com.portalhost.desktop.screens.SettingsScreen
import com.portalhost.desktop.screens.ToastHost
import com.portalhost.desktop.screens.WelcomeScreen
import com.portalhost.di.desktopModule
import com.portalhost.di.initKoin
import com.portalhost.log.setupLogging
import com.portalhost.preferences.Preferences
import com.portalhost.server.ServerManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.koin.compose.koinInject
import java.io.File

private val logger = KotlinLogging.logger {}

sealed class Screen {
    object Home : Screen()
    object Servers : Screen()
    data class ServerDetail(val serverId: String) : Screen()
    data class Console(val serverId: String) : Screen()
    object Create : Screen()
    object Settings : Screen()
    data class Players(val serverId: String) : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val preferences = koinInject<Preferences>()
    val toastManager = koinInject<ToastManager>()
    val fileSystem = koinInject<com.portalhost.filesystem.FileSystem>()
    val serverManager = koinInject<ServerManager>()
    val themePref by preferences.theme.collectAsState()
    val firstRunCompleted by preferences.firstRunCompleted.collectAsState()

    LaunchedEffect(firstRunCompleted) {
        logger.info { "firstRunCompleted = $firstRunCompleted" }
    }

    val isDark = when (themePref) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

val colorScheme = if (isDark) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colorScheme) {
        // Show welcome screen on first run
        if (!firstRunCompleted) {
            WelcomeScreen(onFinish = {
                preferences.firstRunCompleted.value = true
            })
        } else {
            val showTabs = currentScreen is Screen.Home || currentScreen is Screen.Servers || currentScreen is Screen.Settings
            val showFab = currentScreen is Screen.Servers

            val selectedTab = when (currentScreen) {
                Screen.Home -> 0
                Screen.Servers, is Screen.ServerDetail, is Screen.Console, Screen.Create, is Screen.Players -> 1
                Screen.Settings -> 2
            }

            val title = when (currentScreen) {
                Screen.Home -> "Home"
                Screen.Servers -> "Servers"
                is Screen.ServerDetail -> "Server Details"
                is Screen.Console -> "Console"
                Screen.Create -> "New Server"
                is Screen.Players -> "Player Management"
                Screen.Settings -> "Settings"
            }

            val scope = rememberCoroutineScope()

            // About dialog
        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = { Text("About Portal Host") },
                text = {
                    Column(Modifier.padding(16.dp)) {
                        Text("Portal Host")
                        Text("Version 5.0.19")
                        Spacer(Modifier.height(8.dp))
                        Text("Minecraft Java Edition Server Manager")
                        Spacer(Modifier.height(8.dp))
                        Text("https://github.com/portalhost/portalhost")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAboutDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Custom window chrome with drag region + controls
        Box(modifier = Modifier.fillMaxSize()) {
            // Main content area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.isCtrlPressed) {
                            when (keyEvent.key) {
                                Key.N -> {
                                    currentScreen = Screen.Create
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
                                    currentScreen = Screen.Settings
                                    true
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    },
            ) {
                if (showTabs) {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { currentScreen = Screen.Home },
                            icon = { Icon(Icons.Filled.Dashboard, contentDescription = null, modifier = Modifier.padding(end = 4.dp)) },
                            text = { Text("Home") },
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { currentScreen = Screen.Servers },
                            icon = { Icon(Icons.Filled.Dns, contentDescription = null, modifier = Modifier.padding(end = 4.dp)) },
                            text = { Text("Servers") },
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { currentScreen = Screen.Settings },
                            icon = { Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.padding(end = 4.dp)) },
                            text = { Text("Settings") },
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ToastHost()
                        when (currentScreen) {
                            Screen.Home -> DashboardScreen(
                                onNavigateToConsole = { serverId ->
                                    currentScreen = Screen.Console(serverId)
                                },
                                onNavigateToServer = { serverId ->
                                    currentScreen = Screen.ServerDetail(serverId)
                                },
                                onNavigateToCreate = { currentScreen = Screen.Create },
                                onNavigateToPlayers = { serverId ->
                                    currentScreen = Screen.Players(serverId)
                                },
                            )
                            Screen.Servers -> ServersScreen(
                                onNavigateToDetail = { serverId ->
                                    currentScreen = Screen.ServerDetail(serverId)
                                },
                                onNavigateToCreate = { currentScreen = Screen.Create },
                            )
                            is Screen.ServerDetail -> ServerDetailScreen(
                                serverId = (currentScreen as Screen.ServerDetail).serverId,
                                onBack = { currentScreen = Screen.Servers },
                            )
                            is Screen.Console -> ServerConsoleScreen(
                                serverId = (currentScreen as Screen.Console).serverId,
                                onBack = { currentScreen = Screen.Home },
                            )
                            Screen.Create -> CreateServerScreen(
                                onServerCreated = { serverId ->
                                    currentScreen = Screen.ServerDetail(serverId)
                                },
                                onBack = { currentScreen = Screen.Servers },
                            )
                            is Screen.Players -> PlayerManagementScreen(
                                serverId = (currentScreen as Screen.Players).serverId,
                                onBack = { currentScreen = Screen.Home }
                            )
Screen.Settings -> SettingsScreen()
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun AppContent() {
    MaterialTheme {
        DesktopApp()
    }
}

fun main() {
    try {
        initKoin(desktopModule())
        val prefs = org.koin.core.context.GlobalContext.get().get<com.portalhost.preferences.Preferences>()
        val dataDir = prefs.dataDirectory.value
        if (dataDir.isNotBlank()) {
            System.setProperty("portalhost.data.dir", dataDir)
        }
        val logRepo = org.koin.core.context.GlobalContext.get().get<com.portalhost.log.LogRepository>()
        setupLogging(logRepo)
        org.slf4j.LoggerFactory.getLogger("PortalHost").info("Application starting")
    } catch (e: Throwable) {
        System.err.println("FATAL: Failed to initialize application: ${e.message}")
        e.printStackTrace(System.err)
        return
    }

    val prefs = org.koin.core.context.GlobalContext.get().get<com.portalhost.preferences.Preferences>()
    val serverManager = org.koin.core.context.GlobalContext.get().get<com.portalhost.server.ServerManager>()
    val savedWidth = prefs.windowWidth.value
    val savedHeight = prefs.windowHeight.value

    application {
        var isWindowVisible by remember { mutableStateOf(true) }
        val windowState = rememberWindowState(
            width = savedWidth.dp,
            height = savedHeight.dp,
        )

        val trayManager = remember {
            SystemTrayManager(
                serverManager = serverManager,
                onShowWindow = { isWindowVisible = true },
                onExit = {
                    runBlocking {
                        val runningServers = serverManager.servers.value.entries
                            .filter { (id, _) ->
                                val state = serverManager.serverStates.value[id]
                                state?.status == com.portalhost.model.ServerStatus.RUNNING || state?.status == com.portalhost.model.ServerStatus.STARTING
                            }
                        for ((id, _) in runningServers) {
                            serverManager.stopServer(id)
                        }
                    }
                    exitApplication()
                },
            )
        }

        LaunchedEffect(isWindowVisible) {
            if (!isWindowVisible) {
                trayManager.install()
            } else {
                trayManager.remove()
            }
        }

        Window(
            onCloseRequest = {
                isWindowVisible = false
            },
            state = windowState,
            title = "Portal Host",
            visible = isWindowVisible,
        ) {
            AppContent()
        }
    }
}
