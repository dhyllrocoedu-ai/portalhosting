package com.portalhost.app.ui.navigation

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.portalhost.app.server.ServerStatus
import com.portalhost.app.server.TunnelManager
import com.portalhost.app.server.TunnelState
import com.portalhost.app.service.MinecraftService
import com.portalhost.app.ui.components.GrassIcon
import com.portalhost.app.ui.components.CraftingIcon
import com.portalhost.app.ui.components.ChestIcon
import com.portalhost.app.ui.model.ServerConfig
import com.portalhost.app.ui.screens.*
import com.portalhost.app.ui.screens.create.CreateServerScreen
import com.portalhost.app.ui.screens.server.ServerDetailScreen
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AppNavigation(
    appState: AppState,
    tunnelManager: TunnelManager? = null,
    tunnelState: TunnelState? = null
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val tabs = AppTab.entries

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val showBottomBar = currentRoute in tabs.map { it.route }

    val state by appState.serverManager.state.collectAsState()
    val processStats by appState.serverManager.processStats.collectAsState()

    LaunchedEffect(currentRoute) {
        appState.refreshServers()
    }

    // Permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        appState.handlePermissionResult(granted, context)
    }

    val onStart: () -> Unit = {
        val server = appState.activeServer ?: appState.servers.firstOrNull()
        if (server != null) {
            if (server.id != appState.activeServerId) appState.selectServer(server.id)
            appState.startServer(server, context)
        }
    }

    val onStop: () -> Unit = {
        val stopIntent = Intent(context, MinecraftService::class.java).apply {
            action = MinecraftService.ACTION_STOP
        }
        context.startService(stopIntent)
    }
    val onRestart: () -> Unit = { scope.launch { appState.serverManager.restart() } }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            icon = {
                                when (tab) {
                                    AppTab.HOME -> GrassIcon(modifier = Modifier, size = 20.dp)
                                    AppTab.SERVERS -> ChestIcon(modifier = Modifier, size = 20.dp)
                                    AppTab.SETTINGS -> CraftingIcon(modifier = Modifier, size = 20.dp)
                                }
                            },
                            label = { Text(tab.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppTab.HOME.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppTab.HOME.route) {
                val consoleLines by appState.consoleStreamer.linesState
                    .sample(100)
                    .collectAsState(initial = appState.consoleStreamer.lines)
                val activeServer = appState.activeServer
                HomeScreen(
                    serverConfigs = appState.servers,
                    activeServerId = appState.activeServerId,
                    serverState = state,
                    processStats = processStats,
                    consoleLines = consoleLines,
                    activityLog = appState.activityLog,
                    networkInfo = appState.networkInfo,
                    storageStats = appState.storageStats,
                    jdkInstalled = appState.jdkInstalled,
                    publicIp = appState.publicIp,
                    tunnelUrl = appState.tunnelUrl,
                    jdkInstalling = appState.jdkInstalling,
                    jdkProgress = appState.jdkProgress,
                    tunnelState = tunnelState,
                    onStart = onStart,
                    onStop = onStop,
                    onRestart = onRestart,
                    onCommand = { appState.serverManager.writeCommand(it) },
                    onClearConsole = { appState.consoleStreamer.clear() },
                    onOpenConsole = { navController.navigate(Routes.FULL_CONSOLE) },
                    onOpenFiles = {
                        appState.activeServer?.let { s ->
                            navController.navigate(Routes.serverFiles(s.id))
                        }
                    },
                    onOpenPlayers = {
                        navController.navigate(Routes.PLAYER_MANAGEMENT)
                    },
                    onSelectServer = { id -> appState.selectServer(id) },
                    onCreateServer = { navController.navigate(Routes.CREATE_SERVER) },
                    onDeleteServer = { server ->
                        val s = state.status
                        if (s != ServerStatus.OFFLINE && s != ServerStatus.STOPPED && s != ServerStatus.CRASHED && server.id == appState.activeServerId) {
                            scope.launch { appState.serverManager.stop() }
                        }
                        appState.deleteServer(server)
                    },
                    onTunnelStart = {
                        tunnelManager?.let { tm ->
                            scope.launch { tm.start(25565) }
                        }
                    },
                    onTunnelStop = { tunnelManager?.stop() },
                    onTunnelReset = { tunnelManager?.resetClaim() },
                    onSaveSecretKey = { key -> tunnelManager?.setSecretKey(key) },
                    serverDir = activeServer?.let { appState.repository.getServerDir(it.id) },
                    activeServer = activeServer
                )
            }

            composable(AppTab.SERVERS.route) {
                ServersScreen(
                    repository = appState.repository,
                    onCreateServer = { navController.navigate(Routes.CREATE_SERVER) },
                    onServerClick = { server -> navController.navigate(Routes.serverDetail(server.id)) },
                    onDeleteServer = { server ->
                        val s = state.status
                        if (s != ServerStatus.OFFLINE && s != ServerStatus.STOPPED && s != ServerStatus.CRASHED && server.id == appState.activeServerId) {
                            scope.launch { appState.serverManager.stop() }
                        }
                        appState.deleteServer(server)
                    }
                )
            }

            composable(Routes.FULL_CONSOLE) {
                val serverDir = appState.activeServer?.let { appState.repository.getServerDir(it.id) }
                val consoleLines by appState.consoleStreamer.linesState
                    .sample(100)
                    .collectAsState(initial = appState.consoleStreamer.lines)
                ConsoleScreen(
                    consoleLines = consoleLines,
                    onCommand = { appState.serverManager.writeCommand(it) },
                    isOnline = state.status == ServerStatus.ONLINE,
                    serverDir = serverDir,
                    onBack = { navController.popBackStack() },
                    isFullScreen = true
                )
            }

            composable(Routes.SERVER_FILES) { entry ->
                val serverId = entry.arguments?.getString("serverId") ?: return@composable
                val server = appState.repository.getById(serverId)
                if (server != null) {
                    ServerFilesScreen(
                        serverName = server.name,
                        serverDir = appState.repository.getServerDir(server.id),
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(AppTab.SETTINGS.route) {
                SettingsScreen(
                    javaPath = appState.javaPath,
                    jdkInstalled = appState.jdkInstalled,
                    jdkInstalling = appState.jdkInstalling,
                    jdkProgress = appState.jdkProgress,
                    onReinstallJava = appState.onReinstallJava,
                    onUninstallJava = appState.onUninstallJava,
                    onFixupJava = appState.onFixupJava,
                    onClearAppData = appState.onClearAppData,
                    darkTheme = appState.darkTheme,
                    onToggleTheme = appState.onToggleTheme,
                    activeServer = appState.activeServer,
                    onUpdateServer = { updated -> appState.updateServer(updated) },
                    tunnelUrl = appState.tunnelUrl,
                    onTunnelUrlChange = { url -> appState.tunnelUrlChanged(url) }
                )
            }

            composable(Routes.CREATE_SERVER) {
                CreateServerScreen(
                    repository = appState.repository,
                    onCreated = { server ->
                        appState.serverCreated(server)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.SERVER_DETAIL) { entry ->
                val serverId = entry.arguments?.getString("serverId") ?: return@composable
                val server = appState.repository.getById(serverId)
                if (server != null) {
                    ServerDetailScreen(
                        server = server,
                        serverState = state,
                        onBack = { navController.popBackStack() },
                        onUpdateServer = { updated -> appState.updateServer(updated) },
                        serverDir = appState.repository.getServerDir(server.id)
                    )
                }
            }

            composable(Routes.PLAYER_MANAGEMENT) {
                val serverDir = appState.activeServer?.let { appState.repository.getServerDir(it.id) }
                PlayersScreen(
                    serverDir = serverDir,
                    onCommand = { appState.serverManager.writeCommand(it) },
                    isOnline = state.status == ServerStatus.ONLINE,
                    currentPlayers = state.players,
                    status = state.status,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

    // ---- Permission dialogs ----
    if (appState.showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { appState.showPermissionRationale = false },
            title = { Text("Enable Notifications") },
            text = { Text("PortalHost needs notification permission to show the server status and keep your Minecraft server running in the background.") },
            confirmButton = {
                TextButton(onClick = {
                    appState.showPermissionRationale = false
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = { appState.showPermissionRationale = false }) { Text("Not Now") }
            }
        )
    }

    if (appState.showPermissionSettings) {
        AlertDialog(
            onDismissRequest = { appState.showPermissionSettings = false },
            title = { Text("Permission Required") },
            text = { Text("Notification permission was previously denied. Without it, the server cannot stay running in the background.\n\nOpen Settings > Apps > PortalHost > Notifications and enable notifications.") },
            confirmButton = {
                TextButton(onClick = {
                    appState.showPermissionSettings = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { appState.showPermissionSettings = false }) { Text("Cancel") }
            }
        )
    }
}
