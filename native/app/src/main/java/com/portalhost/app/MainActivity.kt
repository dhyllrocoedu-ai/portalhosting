package com.portalhost.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.portalhost.app.activity.ActivityLog
import com.portalhost.app.network.NetworkManager
import com.portalhost.app.notifications.AppNotifier
import com.portalhost.app.server.ConsoleStreamer
import com.portalhost.app.server.JavaRuntimeManager
import com.portalhost.app.server.ProcessMonitor
import com.portalhost.app.server.ServerDownloader
import com.portalhost.app.server.ServerManager
import com.portalhost.app.server.ServerStatus
import com.portalhost.app.server.SkinService
import com.portalhost.app.server.TunnelManager
import com.portalhost.app.service.MinecraftService
import com.portalhost.app.storage.StorageInfo
import com.portalhost.app.ui.model.ServerRepository
import com.portalhost.app.ui.navigation.AppNavigation
import com.portalhost.app.ui.navigation.rememberAppState
import com.portalhost.app.ui.theme.PortalHostTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private lateinit var javaRuntimeManager: JavaRuntimeManager
    private lateinit var consoleStreamer: ConsoleStreamer
    private lateinit var serverManager: ServerManager
    private lateinit var repository: ServerRepository
    private lateinit var activityLog: ActivityLog
    private lateinit var networkManager: NetworkManager
    private lateinit var storageInfo: StorageInfo
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        javaRuntimeManager = JavaRuntimeManager(this)
        consoleStreamer = ConsoleStreamer()
        activityLog = ActivityLog()
        val skinService = SkinService()

        // Reuse existing ServerManager if a server is still running in the background
        val existing = MinecraftService.ServerManagerHolder.manager
        if (existing != null && existing.isRunning) {
            serverManager = existing
            Log.i(TAG, "Reusing existing ServerManager (server still running)")
        } else {
            serverManager = ServerManager(javaRuntimeManager, consoleStreamer, activityLog)
            MinecraftService.ServerManagerHolder.manager = serverManager
        }

        repository = ServerRepository(this)
        networkManager = NetworkManager(this)
        storageInfo = StorageInfo()

        // Wire console streaming
        val consoleJob = serverScope.launch {
            serverManager.consoleLines.collect { line ->
                consoleStreamer.append(line)
            }
        }

        // Warm up provider connections (DNS + TLS) so version lists load faster
        // when the user opens the create-server wizard. Fire-and-forget; failures ignored.
        val downloaderForWarmup = ServerDownloader()
        serverScope.launch {
            for (host in ServerDownloader.PROVIDER_HOSTS) {
                try {
                    val req = okhttp3.Request.Builder().url(host).head().build()
                    downloaderForWarmup.fastClient.newCall(req).execute().close()
                } catch (_: Exception) { }
            }
        }

        setContent {
            var darkTheme by remember { mutableStateOf(true) }
            var isStarting by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) { isStarting = false }

            PortalHostTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isStarting) {
                        NativeSplashScreen()
                    } else {
                        AppEntry(
                            serverManager = serverManager,
                            consoleStreamer = consoleStreamer,
                            repository = repository,
                            filesDir = filesDir,
                            javaRuntimeManager = javaRuntimeManager,
                            activityLog = activityLog,
                            networkManager = networkManager,
                            storageInfo = storageInfo,
                            skinService = skinService,
                            darkTheme = darkTheme,
                            onToggleTheme = { darkTheme = !darkTheme }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serverManager.destroy()
        serverScope.cancel()
    }
}

@Composable
private fun AppEntry(
    serverManager: ServerManager,
    consoleStreamer: ConsoleStreamer,
    repository: ServerRepository,
    filesDir: File,
    javaRuntimeManager: JavaRuntimeManager,
    activityLog: ActivityLog,
    networkManager: NetworkManager,
    storageInfo: StorageInfo,
    skinService: SkinService,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val jdkInstallState by javaRuntimeManager.installState.collectAsState()
    val notifier = remember { AppNotifier(context.applicationContext) }

    // TunnelManager for playit.gg testing
    val tunnelManager = remember { TunnelManager(context) }
    val tunnelState by tunnelManager.state.collectAsState()

    KeepScreenOnWhileProcessing(
        serverManager = serverManager,
        jdkInstalling = jdkInstallState.phase == com.portalhost.app.server.JdkInstallPhase.CONNECTING ||
            jdkInstallState.phase == com.portalhost.app.server.JdkInstallPhase.DOWNLOADING ||
            jdkInstallState.phase == com.portalhost.app.server.JdkInstallPhase.EXTRACTING ||
            jdkInstallState.phase == com.portalhost.app.server.JdkInstallPhase.VERIFYING
    )

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            javaRuntimeManager.fixupLibraries()
        }

        if (!javaRuntimeManager.isInstalled) {
            val result = javaRuntimeManager.install()
            if (result.isSuccess) {
                Log.i(TAG, "JDK installed successfully")
            } else {
                Log.e(TAG, "JDK install failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    val onReinstallJava: () -> Unit = {
        scope.launch {
            javaRuntimeManager.uninstall()
            javaRuntimeManager.install()
        }
    }

    val onUninstallJava: () -> Unit = {
        javaRuntimeManager.uninstall()
    }

    val onFixupJava: () -> Unit = {
        scope.launch(Dispatchers.IO) {
            javaRuntimeManager.fixupLibraries()
        }
    }

    val onClearAppData: () -> Unit = {
        File(filesDir, "servers").deleteRecursively()
        javaRuntimeManager.uninstall()
        repository.clear()
    }

    var tunnelUrl by remember { mutableStateOf(networkManager.loadTunnelUrl()) }
    val onTunnelUrlChange: (String) -> Unit = {
        tunnelUrl = it
        networkManager.saveTunnelUrl(it)
    }

    val appState = rememberAppState(
        serverManager = serverManager,
        consoleStreamer = consoleStreamer,
        repository = repository,
        filesDir = filesDir,
        javaRuntimeManager = javaRuntimeManager,
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

    AppNavigation(
        appState = appState,
        tunnelManager = tunnelManager,
        tunnelState = tunnelState
    )
}

@Composable
private fun NativeSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0B2E), Color(0xFF1A1040))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher),
                contentDescription = "Portal Host",
                modifier = Modifier.size(96.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Portal Host",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB388FF)
            )
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(
                color = Color(0xFF7C4DFF),
                trackColor = Color(0xFF2A2040),
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
private fun KeepScreenOnWhileProcessing(
    serverManager: ServerManager,
    jdkInstalling: Boolean
) {
    val view = LocalView.current
    val serverState by serverManager.state.collectAsState()
    val isProcessing = jdkInstalling ||
        serverState.status == ServerStatus.STARTING ||
        serverState.status == ServerStatus.ONLINE ||
        serverState.status == ServerStatus.STOPPING

    DisposableEffect(isProcessing) {
        view.keepScreenOn = isProcessing
        onDispose {
            view.keepScreenOn = false
        }
    }
}
