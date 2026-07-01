package com.portalhost.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.portalhost.app.activity.ActivityLog
import com.portalhost.app.network.NetworkManager
import com.portalhost.app.server.ConsoleStreamer
import com.portalhost.app.server.ServerDownloader
import com.portalhost.app.server.ServerManager
import com.portalhost.app.server.ServerStatus
import com.portalhost.app.storage.StorageInfo
import com.portalhost.app.ui.model.ServerConfig
import com.portalhost.app.ui.model.ServerRepository
import com.portalhost.app.ui.screens.*
import com.portalhost.app.ui.screens.create.CreateServerScreen
import com.portalhost.app.ui.screens.server.ServerDetailScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun AppNavigation(
    serverManager: ServerManager,
    consoleStreamer: ConsoleStreamer,
    repository: ServerRepository,
    filesDir: File,
    jdkInstalled: Boolean,
    jdkInstalling: Boolean,
    javaPath: String,
    onReinstallJava: () -> Unit,
    onUninstallJava: () -> Unit,
    onFixupJava: () -> Unit,
    onClearAppData: () -> Unit,
    activityLog: ActivityLog,
    networkManager: NetworkManager,
    storageInfo: StorageInfo,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val state by serverManager.state.collectAsState()
    val processStats by serverManager.processStats.collectAsState()
    val tabs = AppTab.entries

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val showBottomBar = currentRoute in tabs.map { it.route }

    // Track active server
    var activeServerId by remember { mutableStateOf<String?>(null) }
    var servers by remember { mutableStateOf(repository.list()) }

    // Refresh server list on resume
    LaunchedEffect(currentRoute) {
        servers = repository.list()
        if (activeServerId == null && servers.isNotEmpty()) {
            activeServerId = servers.first().id
        } else if (activeServerId != null && servers.none { it.id == activeServerId }) {
            activeServerId = servers.firstOrNull()?.id
        }
    }

    // Network info + storage stats
    var networkInfo by remember { mutableStateOf(networkManager.getNetworkInfo()) }
    var storageStats by remember { mutableStateOf(storageInfo.getServerStorage(File(filesDir, "servers"))) }

    LaunchedEffect(state.status) {
        withContext(Dispatchers.IO) {
            networkInfo = networkManager.getNetworkInfo()
            if (activeServerId != null) {
                val serverDir = repository.getServerDir(activeServerId!!)
                storageStats = storageInfo.getServerStorage(serverDir)
            }
        }
    }

    val activeServer = servers.find { it.id == activeServerId }

    fun startServer(server: ServerConfig) {
        if (!jdkInstalled) return
        scope.launch {
            val serverDir = repository.getServerDir(server.id).absolutePath
            // Pre-seed Mojang jar for Paper servers to avoid Paperclip hash failure
            if (server.serverType == "paper" && server.mcVersion.isNotBlank()) {
                val mojangFile = File(serverDir, "mojang_${server.mcVersion}.jar")
                if (!mojangFile.exists()) {
                    val downloader = ServerDownloader()
                    val url = downloader.getVanillaDownloadUrl(server.mcVersion)
                    if (url != null) {
                        downloader.download(url, mojangFile, null).onFailure { e ->
                            android.util.Log.w("AppNavigation", "Failed to pre-seed Mojang jar: ${e.message}")
                        }
                    }
                }
            }
            serverManager.start(
                jarPath = server.jarPath,
                serverDir = serverDir,
                config = server
            )
        }
    }

    val onStart: () -> Unit = {
        val server = activeServer ?: servers.firstOrNull()
        if (server != null) {
            if (server.id != activeServerId) activeServerId = server.id
            startServer(server)
        }
    }

    val onStop: () -> Unit = { scope.launch { serverManager.stop() } }
    val onRestart: () -> Unit = { scope.launch { serverManager.restart() } }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
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
                HomeScreen(
                    serverConfigs = servers,
                    activeServerId = activeServerId,
                    serverState = state,
                    processStats = processStats,
                    consoleLines = consoleStreamer.lines,
                    activityLog = activityLog,
                    networkInfo = networkInfo,
                    storageStats = storageStats,
                    jdkInstalled = jdkInstalled,
                    jdkInstalling = jdkInstalling,
                    onStart = onStart,
                    onStop = onStop,
                    onRestart = onRestart,
                    onCommand = { serverManager.writeCommand(it) },
                    onOpenConsole = { navController.navigate(AppTab.CONSOLE.route) },
                    onOpenFiles = {
                        activeServer?.let { s ->
                            navController.navigate(Routes.serverFiles(s.id))
                        }
                    },
                    onOpenSettings = { navController.navigate(AppTab.SETTINGS.route) },
                    onSelectServer = { id -> activeServerId = id },
                    onCreateServer = { navController.navigate(Routes.CREATE_SERVER) },
                    onDeleteServer = { server ->
                        if (serverManager.state.value.status != ServerStatus.OFFLINE && server.id == activeServerId) {
                            scope.launch { serverManager.stop() }
                        }
                        repository.remove(server.id)
                    }
                )
            }

            composable(AppTab.SERVERS.route) {
                ServersScreen(
                    repository = repository,
                    onCreateServer = { navController.navigate(Routes.CREATE_SERVER) },
                    onServerClick = { server -> navController.navigate(Routes.serverDetail(server.id)) },
                    onDeleteServer = { server ->
                        if (serverManager.state.value.status != ServerStatus.OFFLINE && server.id == activeServerId) {
                            scope.launch { serverManager.stop() }
                        }
                        repository.remove(server.id)
                    }
                )
            }

            composable(AppTab.CONSOLE.route) {
                val serverDir = activeServer?.let { repository.getServerDir(it.id) }
                ConsoleScreen(
                    consoleLines = consoleStreamer.lines,
                    onCommand = { serverManager.writeCommand(it) },
                    isOnline = state.status == ServerStatus.ONLINE,
                    serverDir = serverDir
                )
            }

            composable(Routes.SERVER_FILES) { entry ->
                val serverId = entry.arguments?.getString("serverId") ?: return@composable
                val server = repository.getById(serverId)
                if (server != null) {
                    ServerFilesScreen(
                        serverName = server.name,
                        serverDir = repository.getServerDir(server.id),
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(AppTab.SETTINGS.route) {
                SettingsScreen(
                    javaPath = javaPath,
                    jdkInstalled = jdkInstalled,
                    jdkInstalling = jdkInstalling,
                    onReinstallJava = onReinstallJava,
                    onUninstallJava = onUninstallJava,
                    onFixupJava = onFixupJava,
                    onClearAppData = onClearAppData,
                    darkTheme = darkTheme,
                    onToggleTheme = onToggleTheme,
                    activeServer = activeServer,
                    onUpdateServer = { updated ->
                        repository.update(updated)
                        servers = repository.list()
                    }
                )
            }

            composable(Routes.CREATE_SERVER) {
                CreateServerScreen(
                    repository = repository,
                    onCreated = {
                        servers = repository.list()
                        activeServerId = it.id
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.SERVER_DETAIL) { entry ->
                val serverId = entry.arguments?.getString("serverId") ?: return@composable
                val server = repository.getById(serverId)
                if (server != null) {
                    ServerDetailScreen(
                        server = server,
                        serverState = state,
                        consoleLines = consoleStreamer.lines,
                        onStart = { startServer(server) },
                        onStop = { scope.launch { serverManager.stop() } },
                        onCommand = { serverManager.writeCommand(it) },
                        onBack = { navController.popBackStack() },
                        serverDir = repository.getServerDir(server.id)
                    )
                }
            }
        }
    }
}
