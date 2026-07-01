package com.portalhost.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.portalhost.app.activity.ActivityLog
import com.portalhost.app.network.NetworkManager
import com.portalhost.app.server.ConsoleStreamer
import com.portalhost.app.server.JavaRuntimeManager
import com.portalhost.app.server.ProcessMonitor
import com.portalhost.app.server.ServerManager
import com.portalhost.app.service.MinecraftService
import com.portalhost.app.storage.StorageInfo
import com.portalhost.app.ui.model.ServerRepository
import com.portalhost.app.ui.navigation.AppNavigation
import com.portalhost.app.ui.theme.PortalHostTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
        serverManager = ServerManager(javaRuntimeManager, consoleStreamer, activityLog)
        repository = ServerRepository(this)
        networkManager = NetworkManager(this)
        storageInfo = StorageInfo()

        MinecraftService.ServerManagerHolder.manager = serverManager

        // Wire console streaming
        val consoleJob = serverScope.launch {
            serverManager.consoleLines.collect { line ->
                consoleStreamer.append(line)
            }
        }

        setContent {
            var darkTheme by remember { mutableStateOf(true) }
            PortalHostTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppEntry(
                        serverManager = serverManager,
                        consoleStreamer = consoleStreamer,
                        repository = repository,
                        filesDir = filesDir,
                        javaRuntimeManager = javaRuntimeManager,
                        activityLog = activityLog,
                        networkManager = networkManager,
                        storageInfo = storageInfo,
                        darkTheme = darkTheme,
                        onToggleTheme = { darkTheme = !darkTheme }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serverManager.destroy()
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
    darkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var jdkInstalled by remember { mutableStateOf(javaRuntimeManager.isInstalled) }
    var jdkInstalling by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            javaRuntimeManager.fixupLibraries()
        }

        if (!javaRuntimeManager.isInstalled) {
            jdkInstalling = true
            val result = javaRuntimeManager.install()
            jdkInstalling = false
            jdkInstalled = result.isSuccess
            if (result.isSuccess) {
                Log.i(TAG, "JDK installed successfully")
            } else {
                Log.e(TAG, "JDK install failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    val onReinstallJava: () -> Unit = {
        javaRuntimeManager.uninstall()
        jdkInstalled = false
        scope.launch {
            jdkInstalling = true
            val result = javaRuntimeManager.install()
            jdkInstalling = false
            jdkInstalled = result.isSuccess
        }
    }

    val onUninstallJava: () -> Unit = {
        javaRuntimeManager.uninstall()
        jdkInstalled = false
    }

    val onFixupJava: () -> Unit = {
        scope.launch(Dispatchers.IO) {
            javaRuntimeManager.fixupLibraries()
        }
    }

    val onClearAppData: () -> Unit = {
        File(filesDir, "servers").deleteRecursively()
        javaRuntimeManager.uninstall()
        jdkInstalled = false
        repository.clear()
    }

    AppNavigation(
        serverManager = serverManager,
        consoleStreamer = consoleStreamer,
        repository = repository,
        filesDir = filesDir,
        jdkInstalled = jdkInstalled,
        jdkInstalling = jdkInstalling,
        javaPath = javaRuntimeManager.resolveJavaPath(),
        onReinstallJava = onReinstallJava,
        onUninstallJava = onUninstallJava,
        onFixupJava = onFixupJava,
        onClearAppData = onClearAppData,
        activityLog = activityLog,
        networkManager = networkManager,
        storageInfo = storageInfo,
        darkTheme = darkTheme,
        onToggleTheme = onToggleTheme
    )
}
