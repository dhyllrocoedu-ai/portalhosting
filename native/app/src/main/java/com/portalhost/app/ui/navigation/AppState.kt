package com.portalhost.app.ui.navigation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import android.app.Activity
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.portalhost.app.activity.ActivityLog
import com.portalhost.app.network.NetworkManager
import com.portalhost.app.notifications.AppNotifier
import com.portalhost.app.server.ConsoleStreamer
import com.portalhost.app.server.DeviceDetector
import com.portalhost.app.server.JavaRuntimeManager
import com.portalhost.app.server.JdkInstallPhase
import com.portalhost.app.server.ServerDownloader
import com.portalhost.app.server.ServerManager
import com.portalhost.app.server.ServerState
import com.portalhost.app.server.ServerStatus
import com.portalhost.app.server.SkinService
import com.portalhost.app.server.providers.ServerType
import com.portalhost.app.service.MinecraftService
import com.portalhost.app.storage.StorageInfo
import com.portalhost.app.ui.model.ServerConfig
import com.portalhost.app.ui.model.ServerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AppState(
    val serverManager: ServerManager,
    val consoleStreamer: ConsoleStreamer,
    val repository: ServerRepository,
    val filesDir: File,
    val javaRuntimeManager: JavaRuntimeManager,
    val javaPath: String,
    val onReinstallJava: () -> Unit,
    val onUninstallJava: () -> Unit,
    val onFixupJava: () -> Unit,
    val onClearAppData: () -> Unit,
    val activityLog: ActivityLog,
    val notifier: AppNotifier,
    val networkManager: NetworkManager,
    val storageInfo: StorageInfo,
    val skinService: SkinService,
    val darkTheme: Boolean,
    val onToggleTheme: () -> Unit,
    val tunnelUrl: String,
    val onTunnelUrlChange: (String) -> Unit
) {
    private val scope = MainScope()
    var servers by mutableStateOf(repository.list())
        private set
    var activeServerId by mutableStateOf<String?>(null)
        private set
    var networkInfo by mutableStateOf(networkManager.getNetworkInfo())
    var storageStats by mutableStateOf(storageInfo.getServerStorage(File(filesDir, "servers")))
        private set
    var performanceHistory by mutableStateOf(listOf<com.portalhost.app.ui.screens.StatsSnapshot>())
        private set

    var jdkInstalled by mutableStateOf(javaRuntimeManager.isInstalled)
        private set
    var jdkInstalling by mutableStateOf(false)
        private set
    var jdkProgress by mutableStateOf(0f)
        private set
    var jdkMessage by mutableStateOf("")
        private set
    var jdkError by mutableStateOf<String?>(null)
        private set

    private var lastServerStatus = serverManager.state.value.status
    private var lastJdkPhase = javaRuntimeManager.installState.value.phase

    init {
        scope.launch {
            javaRuntimeManager.installState.collect { state ->
                jdkProgress = state.progress
                jdkMessage = state.message
                jdkError = state.error
                jdkInstalling = state.phase == JdkInstallPhase.CONNECTING ||
                    state.phase == JdkInstallPhase.DOWNLOADING ||
                    state.phase == JdkInstallPhase.EXTRACTING ||
                    state.phase == JdkInstallPhase.VERIFYING
                jdkInstalled = javaRuntimeManager.isInstalled

                val prevPhase = lastJdkPhase
                lastJdkPhase = state.phase
                if (state.phase == JdkInstallPhase.COMPLETE && prevPhase != JdkInstallPhase.COMPLETE) {
                    notifier.notify(
                        message = "Java runtime is ready to start servers.",
                        success = true,
                        title = "JDK installed"
                    )
                } else if (state.phase == JdkInstallPhase.ERROR && prevPhase != JdkInstallPhase.ERROR) {
                    notifier.notify(
                        message = state.error ?: "Failed to install Java runtime.",
                        success = false,
                        title = "JDK install failed"
                    )
                }
            }
        }
        scope.launch {
            serverManager.state.collect { state ->
                notifyServerStatus(state)
            }
        }
    }

    private fun notifyServerStatus(state: ServerState) {
        val prev = lastServerStatus
        lastServerStatus = state.status
        if (state.status == prev) return
        when (state.status) {
            ServerStatus.ONLINE -> notifier.notify(
                message = "Your server is now online.",
                success = true,
                title = "Server online"
            )
            ServerStatus.STOPPED -> {
                if (state.exitCode != 0) {
                    notifier.notify(
                        message = "The server crashed (exit code ${state.exitCode}).",
                        success = false,
                        title = "Server crashed"
                    )
                } else {
                    notifier.notify(
                        message = "The server was stopped.",
                        success = true,
                        title = "Server stopped"
                    )
                }
            }
            ServerStatus.CRASHED -> notifier.notify(
                message = state.error ?: "The server failed to start.",
                success = false,
                title = "Server crashed"
            )
            ServerStatus.OFFLINE -> {
                if (prev == ServerStatus.ONLINE || prev == ServerStatus.STARTING) {
                    notifier.notify(
                        message = "The server is no longer running.",
                        success = true,
                        title = "Server stopped"
                    )
                }
            }
            else -> {}
        }
    }

    // Debounced StateFlows to reduce recomposition frequency
    private val _processStatsFlow = MutableStateFlow(serverManager.processStats.value)
    val processStatsDebounced: StateFlow<com.portalhost.app.server.ProcessStats> = _processStatsFlow

    private val _storageStatsFlow = MutableStateFlow(storageStats)
    val storageStatsDebounced: StateFlow<com.portalhost.app.storage.StorageStats> = _storageStatsFlow.asStateFlow()

    private val _networkInfoFlow = MutableStateFlow(networkInfo)
    val networkInfoDebounced: StateFlow<com.portalhost.app.network.NetworkInfo> = _networkInfoFlow.asStateFlow()
    var pendingServerForPermission by mutableStateOf<ServerConfig?>(null)
        private set
    var showPermissionRationale by mutableStateOf(false)
    var showPermissionSettings by mutableStateOf(false)

    val activeServer: ServerConfig? get() = servers.find { it.id == activeServerId }

    fun refreshServers() {
        servers = repository.list()
        if (activeServerId == null && servers.isNotEmpty()) {
            activeServerId = servers.first().id
        } else if (activeServerId != null && servers.none { it.id == activeServerId }) {
            activeServerId = servers.firstOrNull()?.id
        }
    }

    fun selectServer(id: String) {
        activeServerId = id
    }

    fun refreshStorageStats() {
        if (activeServerId != null) {
            val serverDir = repository.getServerDir(activeServerId!!)
            storageStats = storageInfo.getServerStorage(serverDir)
        }
        _storageStatsFlow.value = storageStats
    }

    fun updateProcessStats(stats: com.portalhost.app.server.ProcessStats) {
        _processStatsFlow.value = stats
    }

    fun appendPerformanceSnapshot(snapshot: com.portalhost.app.ui.screens.StatsSnapshot) {
        val history = performanceHistory.toMutableList()
        history.add(snapshot)
        if (history.size > 60) history.removeAt(0)
        performanceHistory = history
    }

    fun updateNetworkInfo(info: com.portalhost.app.network.NetworkInfo) {
        _networkInfoFlow.value = info
    }

    fun handleMemoryTrim(level: Int) {
        when (level) {
            Activity.TRIM_MEMORY_RUNNING_LOW -> {
                consoleStreamer.clear()
            }
            Activity.TRIM_MEMORY_RUNNING_CRITICAL -> {
                consoleStreamer.clear()
                serverManager.kill()
            }
        }
    }

    fun tunnelUrlChanged(url: String) {
        onTunnelUrlChange(url)
        refreshNetworkInfo()
    }

    private fun refreshNetworkInfo() {
        networkInfo = networkManager.getNetworkInfo()
    }

    suspend fun deleteServer(server: ServerConfig, context: Context) {
        // Stop the server if it's running
        val serverState = serverManager.state.value
        if (serverState.status != ServerStatus.OFFLINE && serverState.status != ServerStatus.STOPPED && serverState.status != ServerStatus.CRASHED && server.id == activeServerId) {
            serverManager.stop()
        }
        // Delete from repository
        repository.remove(server.id)
        // Delete server directory
        try {
            val serverDir = repository.getServerDir(server.id)
            if (serverDir.exists()) {
                serverDir.deleteRecursively()
            }
        } catch (e: Exception) {
            android.util.Log.e("AppState", "Failed to delete server directory", e)
        }
        refreshServers()
        // Show confirmation toast
        Toast.makeText(context, "Server deleted", Toast.LENGTH_SHORT).show()
    }

    fun updateServer(updated: ServerConfig) {
        repository.update(updated)
        refreshServers()
    }

    fun serverCreated(server: ServerConfig) {
        refreshServers()
        activeServerId = server.id
    }

    fun requestNotificationPermissionOrSettings(server: ServerConfig, context: android.content.Context) {
        pendingServerForPermission = server
        val showRationale = try {
            ActivityCompat.shouldShowRequestPermissionRationale(
                context as android.app.Activity,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } catch (_: Exception) { false }
        if (showRationale) {
            showPermissionRationale = true
        } else {
            showPermissionSettings = true
        }
    }

    fun startServer(server: ServerConfig, context: android.content.Context) {
        if (!jdkInstalled) {
            Toast.makeText(context, "JDK not installed yet. Please wait.", Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermissionOrSettings(server, context)
            return
        }
        doStartServer(server, context)
    }

    fun doStartServer(server: ServerConfig, context: android.content.Context) {
        val fgIntent = Intent(context, MinecraftService::class.java).apply {
            action = MinecraftService.ACTION_FOREGROUND
        }
        ContextCompat.startForegroundService(context, fgIntent)
        MainScope().launch(Dispatchers.IO) {
            val serverDir = repository.getServerDir(server.id).absolutePath
            if (server.serverType == "paper" && server.mcVersion.isNotBlank()) {
                val mojangFile = File(serverDir, "mojang_${server.mcVersion}.jar")
                if (!mojangFile.exists()) {
                    val downloader = ServerDownloader()
                    val vanProv = downloader.getProvider(ServerType.VANILLA)
                    val info = vanProv.getDownloadInfo(server.mcVersion, "")
                    if (info != null) {
                        downloader.download(info.url, mojangFile, info.sha256).onFailure { e ->
                            android.util.Log.w("AppState", "Failed to pre-seed Mojang jar: ${e.message}")
                        }
                    }
                }
            }
            val spec = DeviceDetector.detect(context)
            val deviceCfg = DeviceDetector.generateConfig(spec)
            val userMaxMb = DeviceDetector.parseRamMb(server.maxRam)
            val userMinMb = DeviceDetector.parseRamMb(server.minRam)
            val safeMinMb = minOf(userMinMb, userMaxMb)
            DeviceDetector.enforceServerProfile(serverDir, deviceCfg.serverProps)
            val javaArgs = listOf("-Xms${safeMinMb}M", "-Xmx${userMaxMb}M") + deviceCfg.gcFlags
            serverManager.start(
                jarPath = server.jarPath,
                javaArgs = javaArgs,
                serverDir = serverDir,
                config = server
            )
        }
    }

    fun handlePermissionResult(granted: Boolean, context: android.content.Context) {
        val server = pendingServerForPermission
        pendingServerForPermission = null
        if (granted && server != null) {
            doStartServer(server, context)
        } else if (!granted && server != null) {
            showPermissionSettings = true
        }
    }
}

@Composable
fun rememberAppState(
    serverManager: ServerManager,
    consoleStreamer: ConsoleStreamer,
    repository: ServerRepository,
    filesDir: File,
    javaRuntimeManager: JavaRuntimeManager,
    onReinstallJava: () -> Unit,
    onUninstallJava: () -> Unit,
    onFixupJava: () -> Unit,
    onClearAppData: () -> Unit,
    activityLog: ActivityLog,
    notifier: AppNotifier,
    networkManager: NetworkManager,
    storageInfo: StorageInfo,
    skinService: SkinService,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    tunnelUrl: String,
    onTunnelUrlChange: (String) -> Unit
): AppState {
    val appState = remember {
        AppState(
            serverManager = serverManager,
            consoleStreamer = consoleStreamer,
            repository = repository,
            filesDir = filesDir,
            javaRuntimeManager = javaRuntimeManager,
            javaPath = javaRuntimeManager.resolveJavaPath(),
            onReinstallJava = onReinstallJava,
            onUninstallJava = onUninstallJava,
            onFixupJava = onFixupJava,
            onClearAppData = onClearAppData,
            activityLog = activityLog,
            notifier = notifier,
            networkManager = networkManager,
            storageInfo = storageInfo,
            skinService = skinService,
            darkTheme = darkTheme,
            onToggleTheme = onToggleTheme,
            tunnelUrl = tunnelUrl,
            onTunnelUrlChange = onTunnelUrlChange
        )
    }

    LaunchedEffect(appState.activeServerId) {
        withContext(Dispatchers.IO) {
            appState.refreshStorageStats()
        }
    }

    LaunchedEffect(Unit) {
        networkManager.networkInfo.collect { info ->
            appState.networkInfo = info.copy(tunnelUrl = appState.tunnelUrl)
        }
    }

    LaunchedEffect(Unit) {
        serverManager.processStats.collect { stats ->
            val snapshot = com.portalhost.app.ui.screens.StatsSnapshot(
                timestamp = System.currentTimeMillis(),
                cpuPercent = stats.cpuPercent,
                ramMb = stats.ramBytes / (1024f * 1024f),
                tps = stats.tps,
                rxBytesPerSec = stats.rxBytesPerSec,
                txBytesPerSec = stats.txBytesPerSec
            )
            appState.appendPerformanceSnapshot(snapshot)
        }
    }

    return appState
}
