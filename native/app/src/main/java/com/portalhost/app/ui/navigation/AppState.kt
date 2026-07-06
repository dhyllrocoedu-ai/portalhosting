package com.portalhost.app.ui.navigation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import android.app.Activity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.portalhost.app.activity.ActivityLog
import com.portalhost.app.network.NetworkManager
import com.portalhost.app.server.ConsoleStreamer
import com.portalhost.app.server.DeviceDetector
import com.portalhost.app.server.ServerDownloader
import com.portalhost.app.server.ServerManager
import com.portalhost.app.server.ServerStatus
import com.portalhost.app.service.MinecraftService
import com.portalhost.app.storage.StorageInfo
import com.portalhost.app.ui.model.ServerConfig
import com.portalhost.app.ui.model.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AppState(
    val serverManager: ServerManager,
    val consoleStreamer: ConsoleStreamer,
    val repository: ServerRepository,
    val filesDir: File,
    val jdkInstalled: Boolean,
    val jdkInstalling: Boolean,
    val jdkProgress: Float,
    val javaPath: String,
    val onReinstallJava: () -> Unit,
    val onUninstallJava: () -> Unit,
    val onFixupJava: () -> Unit,
    val onClearAppData: () -> Unit,
    val activityLog: ActivityLog,
    val networkManager: NetworkManager,
    val storageInfo: StorageInfo,
    val darkTheme: Boolean,
    val onToggleTheme: () -> Unit,
    val tunnelUrl: String,
    val onTunnelUrlChange: (String) -> Unit
) {
    var servers by mutableStateOf(repository.list())
        private set
    var activeServerId by mutableStateOf<String?>(null)
        private set
    var networkInfo by mutableStateOf(networkManager.getNetworkInfo())
        private set
    var storageStats by mutableStateOf(storageInfo.getServerStorage(File(filesDir, "servers")))
        private set

    // Debounced StateFlows to reduce recomposition frequency
    private val _processStatsFlow = MutableStateFlow(serverManager.processStats.value)
    val processStatsDebounced: StateFlow<com.portalhost.app.server.ProcessStats> = _processStatsFlow

    private val _storageStatsFlow = MutableStateFlow(storageStats)
    val storageStatsDebounced: StateFlow<com.portalhost.app.storage.StorageStats> = _storageStatsFlow.asStateFlow()

    private val _networkInfoFlow = MutableStateFlow(networkInfo)
    val networkInfoDebounced: StateFlow<com.portalhost.app.network.NetworkInfo> = _networkInfoFlow.asStateFlow()
    var publicIp by mutableStateOf("")
        private set
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

    fun refreshNetworkInfo() {
        networkInfo = networkManager.getNetworkInfo().copy(tunnelUrl = tunnelUrl)
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

    fun refreshPublicIp(ip: String) {
        if (ip.isNotBlank()) publicIp = ip
    }

    fun tunnelUrlChanged(url: String) {
        onTunnelUrlChange(url)
        networkInfo = networkInfo.copy(tunnelUrl = url)
    }

    fun deleteServer(server: ServerConfig) {
        repository.remove(server.id)
        refreshServers()
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
                    val url = downloader.getVanillaDownloadUrl(server.mcVersion)
                    if (url != null) {
                        downloader.download(url, mojangFile, null).onFailure { e ->
                            android.util.Log.w("AppState", "Failed to pre-seed Mojang jar: ${e.message}")
                        }
                    }
                }
            }
            val spec = DeviceDetector.detect(context)
            val deviceCfg = DeviceDetector.generateConfig(spec)
            val userMaxMb = DeviceDetector.parseRamMb(server.maxRam)
            val recommendedMaxMb = DeviceDetector.parseRamMb(deviceCfg.recommendedMaxRam)
            val safeMaxMb = minOf(userMaxMb, recommendedMaxMb)
            val safeMinMb = minOf(DeviceDetector.parseRamMb(server.minRam), safeMaxMb)
            DeviceDetector.enforceServerProfile(serverDir, deviceCfg.serverProps)
            val javaArgs = listOf("-Xms${safeMinMb}M", "-Xmx${safeMaxMb}M") + deviceCfg.gcFlags
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
    jdkInstalled: Boolean,
    jdkInstalling: Boolean,
    jdkProgress: Float,
    javaPath: String,
    onReinstallJava: () -> Unit,
    onUninstallJava: () -> Unit,
    onFixupJava: () -> Unit,
    onClearAppData: () -> Unit,
    activityLog: ActivityLog,
    networkManager: NetworkManager,
    storageInfo: StorageInfo,
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
            jdkInstalled = jdkInstalled,
            jdkInstalling = jdkInstalling,
            javaPath = javaPath,
            onReinstallJava = onReinstallJava,
            onUninstallJava = onUninstallJava,
            onFixupJava = onFixupJava,
            onClearAppData = onClearAppData,
            activityLog = activityLog,
            networkManager = networkManager,
            storageInfo = storageInfo,
            darkTheme = darkTheme,
            onToggleTheme = onToggleTheme,
            tunnelUrl = tunnelUrl,
            onTunnelUrlChange = onTunnelUrlChange,
            jdkProgress = jdkProgress
        )
    }

    LaunchedEffect(appState.activeServerId) {
        withContext(Dispatchers.IO) {
            appState.refreshNetworkInfo()
            if (appState.publicIp.isEmpty()) {
                val ip = appState.networkManager.fetchPublicIp()
                if (ip.isNotBlank()) appState.refreshPublicIp(ip)
            }
            appState.refreshStorageStats()
        }
    }

    return appState
}
