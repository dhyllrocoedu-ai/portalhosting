package com.portalhost.desktop

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.portalhost.desktop.screens.ServerDetailScreen
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
    object Home : Screen()
    object Servers : Screen()
    data class ServerDetail(val serverId: String) : Screen()
    object Create : Screen()
    object Settings : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    val preferences = koinInject<Preferences>()
    val toastManager = koinInject<ToastManager>()
    val themePref by preferences.theme.collectAsState()

    val isDark = when (themePref) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) darkColorScheme() else lightColorScheme()

    val showTabs = currentScreen is Screen.Home || currentScreen is Screen.Servers || currentScreen is Screen.Settings
    val showFab = currentScreen is Screen.Servers

    val selectedTab = when (currentScreen) {
        Screen.Home -> 0
        Screen.Servers, is Screen.ServerDetail, Screen.Create -> 1
        Screen.Settings -> 2
    }

    val title = when (currentScreen) {
        Screen.Home -> "Home"
        Screen.Servers -> "Servers"
        is Screen.ServerDetail -> "Server Details"
        Screen.Create -> "New Server"
        Screen.Settings -> "Settings"
    }

    MaterialTheme(colorScheme = colorScheme) {
        Scaffold(
            topBar = {
                if (showTabs) {
                    TopAppBar(
                        title = { Text(title, fontWeight = FontWeight.SemiBold) },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                    )
                } else {
                    TopAppBar(
                        title = { Text(title, fontWeight = FontWeight.SemiBold) },
                        navigationIcon = {
                            IconButton(onClick = {
                                currentScreen = when (currentScreen) {
                                    is Screen.ServerDetail -> Screen.Servers
                                    else -> Screen.Servers
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
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
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                    modifier = Modifier
                        .fillMaxSize()
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
                            Screen.Home -> DashboardScreen(
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
                            is Screen.ServerDetail -> ServerDetailScreen(
                                serverId = (currentScreen as Screen.ServerDetail).serverId,
                                onBack = { currentScreen = Screen.Servers },
                            )
                            Screen.Create -> CreateServerScreen()
                            Screen.Settings -> SettingsScreen()
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
