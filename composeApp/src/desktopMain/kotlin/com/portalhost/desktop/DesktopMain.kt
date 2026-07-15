package com.portalhost.desktop

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import com.portalhost.uinotify.ToastManager
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
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File

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

    val isMainScreen = currentScreen is Screen.Dashboard ||
            currentScreen is Screen.Servers ||
            currentScreen is Screen.Create ||
            currentScreen is Screen.Settings
    val inSubScreen = !isMainScreen

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val showFab = currentScreen is Screen.Dashboard || currentScreen is Screen.Servers

    fun navigateTo(screen: Screen) {
        currentScreen = screen
        scope.launch { drawerState.close() }
    }

    MaterialTheme(colorScheme = colorScheme) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Portal Host",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    NavigationDrawerItem(
                        label = { Text("Dashboard") },
                        selected = currentScreen is Screen.Dashboard,
                        onClick = { navigateTo(Screen.Dashboard) },
                        icon = { Icon(Icons.Filled.Dashboard, contentDescription = "Dashboard") },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    )
                    NavigationDrawerItem(
                        label = { Text("Servers") },
                        selected = currentScreen is Screen.Servers,
                        onClick = { navigateTo(Screen.Servers) },
                        icon = { Icon(Icons.Filled.Dns, contentDescription = "Servers") },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    )
                    NavigationDrawerItem(
                        label = { Text("Settings") },
                        selected = currentScreen is Screen.Settings,
                        onClick = { navigateTo(Screen.Settings) },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    )
                }
            },
        ) {
            Scaffold(
                topBar = {
                    if (!inSubScreen || currentScreen is Screen.Create) {
                        val (title, showMenu, showBack) = when (currentScreen) {
                            Screen.Dashboard -> Triple("Dashboard", true, false)
                            Screen.Servers -> Triple("Servers", true, false)
                            Screen.Create -> Triple("New Server", false, true)
                            Screen.Settings -> Triple("Settings", true, false)
                            else -> Triple("", false, false)
                        }
                        TopAppBar(
                            title = { Text(title, fontWeight = FontWeight.SemiBold) },
                            navigationIcon = {
                                if (showMenu) {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                                    }
                                } else if (showBack) {
                                    IconButton(onClick = { currentScreen = Screen.Servers }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        )
                    } else {
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
                floatingActionButton = {
                    if (showFab) {
                        FloatingActionButton(
                            onClick = { currentScreen = Screen.Create },
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "New Server")
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
    singleWindowApplication(
        title = "Portal Host",
        state = WindowState(width = 1200.dp, height = 800.dp),
    ) {
        DesktopApp()
    }
}
