package com.portalhost.desktop

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.portalhost.uinotify.ToastManager
import com.portalhost.uinotify.ToastType
import com.portalhost.theme.AppTheme
import com.portalhost.desktop.screens.WelcomeScreen
import com.portalhost.desktop.util.SingleInstanceLock
import com.portalhost.desktop.util.UninstallHelper
import com.portalhost.desktop.window.PortalHostWindow
import com.portalhost.desktop.window.Screen
import com.portalhost.desktop.window.TitleBar
import com.portalhost.filesystem.defaultDataDir
import com.portalhost.di.desktopModule
import com.portalhost.di.initKoin
import com.portalhost.log.setupLogging
import com.portalhost.preferences.Preferences
import com.portalhost.server.ServerManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.koin.core.context.GlobalContext
import org.koin.compose.koinInject
import org.jetbrains.skia.Image
import java.awt.Frame
import java.io.File

private val resolvedDataDir: String = try {
    val early = java.util.prefs.Preferences.userRoot().node("com/portalhost").get("dataDirectory", "")
    val dir = early.ifBlank { defaultDataDir().absolutePath }
    System.setProperty("portalhost.data.dir", dir)
    dir
} catch (_: Throwable) {
    val fallback = defaultDataDir().absolutePath
    System.setProperty("portalhost.data.dir", fallback)
    fallback
}

private val logger by lazy { KotlinLogging.logger {} }

@Composable
fun DesktopApp(
    iconPainter: Painter? = null,
    window: java.awt.Frame? = null,
    onMinimize: () -> Unit = {},
    onMaximizeRestore: () -> Unit = {},
    onClose: () -> Unit = {},
    onQuit: () -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val preferences = koinInject<Preferences>()
    val toastManager = koinInject<ToastManager>()
    val fileSystem = koinInject<com.portalhost.filesystem.FileSystem>()
    val serverManager = koinInject<ServerManager>()
    val firstRunCompleted by preferences.firstRunCompleted.collectAsState()
    val showWelcome = !firstRunCompleted

    // Check if data dir is configured for returning users (non-blocking toast)
    var dataDirConfigured by remember { mutableStateOf(false) }
    LaunchedEffect(firstRunCompleted, preferences.dataDirectory.value) {
        if (firstRunCompleted) {
            val hasDataDir = preferences.dataDirectory.value.isNotBlank() ||
                com.portalhost.filesystem.readInstallConfigDataDir() != null
            dataDirConfigured = hasDataDir
            if (!hasDataDir) {
                // Delay toast slightly to ensure UI is ready
                delay(1000)
                toastManager.warningWithAction(
                    message = "Data directory not configured. Click to set it up.",
                    actionLabel = "Open Settings",
                    onAction = { currentScreen = Screen.Settings },
                    duration = 10000
                )
            }
        }
    }

    LaunchedEffect(showWelcome) {
        logger.info { "firstRunCompleted=$firstRunCompleted, showWelcome=$showWelcome" }
    }

    if (showWelcome) {
        Column(Modifier.fillMaxSize()) {
            TitleBar(
                iconPainter = iconPainter,
                window = window,
                onMinimize = onMinimize,
                onMaximizeRestore = onMaximizeRestore,
                onClose = onClose
            )
            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                WelcomeScreen(onFinish = {
                    preferences.firstRunCompleted.value = true
                })
            }
        }
    } else {
        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = { Text("About Portal Host") },
                text = {
                    Column(Modifier.padding(16.dp)) {
                        Text("Portal Host")
                        Text(com.portalhost.BuildConfig.DISPLAY_NAME)
                        Spacer(Modifier.height(8.dp))
                        Text("Minecraft Java Edition Server Manager")
                        Spacer(Modifier.height(8.dp))
                        Text("https://github.com/dhyllrocoedu-ai/portalhosting")
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

        PortalHostWindow(
            currentScreen = currentScreen,
            onScreenChange = { currentScreen = it },
            serverManager = serverManager,
            fileSystem = fileSystem,
            toastManager = toastManager,
            preferences = preferences,
            iconPainter = iconPainter,
            window = window,
            onMinimize = onMinimize,
            onMaximizeRestore = onMaximizeRestore,
            onClose = onClose,
            onQuit = onQuit
        )
    }
}

fun main() {
    // Acquire a single-instance lock so two PortalHost processes cannot run at
    // the same time. This prevents the silent-update restart from spawning a
    // second PortalHost.exe that fights with the old (still-shutting-down) one
    // for file locks on the install directory.
    val instanceLock = SingleInstanceLock.acquire()
    if (instanceLock == null) {
        System.err.println("PortalHost is already running. Exiting duplicate instance.")
        return
    }

    // Release the lock on any JVM exit path (normal exit, exitProcess from
    // update flow, crash). PowerShell's taskkill gives us 2-5s; releasing the
    // lock here lets the new process acquire it instantly.
    Runtime.getRuntime().addShutdownHook(Thread {
        instanceLock.release()
    })

    try {
        // Runs before the database is opened: if the data directory lives inside
        // the install folder, offer to move it out so an uninstall cannot wipe it.
        val migratedDataDir = UninstallHelper.migrateDataDirBeforeStart()
        System.setProperty("portalhost.data.dir", migratedDataDir ?: resolvedDataDir)

        initKoin(desktopModule())

        val logRepo = GlobalContext.get().get<com.portalhost.log.LogRepository>()
        setupLogging(logRepo)
        org.slf4j.LoggerFactory.getLogger("PortalHost").info("Application starting")

        // Warm up provider host connections (DNS + TLS) so the version list loads faster.
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            for (host in listOf(
                "https://fill.papermc.io",
                "https://meta.fabricmc.net",
                "https://maven.minecraftforge.net",
                "https://maven.neoforged.net",
                "https://api.purpurmc.org",
                "https://launchermeta.mojang.com"
            )) {
                try {
                    val conn = java.net.URL(host).openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 3000
                    conn.readTimeout = 3000
                    conn.requestMethod = "HEAD"
                    conn.setRequestProperty("User-Agent", "PortalHost/1.0")
                    conn.connect()
                    conn.disconnect()
                } catch (_: Exception) { }
            }
        }
    } catch (e: Throwable) {
        System.err.println("FATAL: Failed to initialize application: ${e.message}")
        e.printStackTrace(System.err)
        instanceLock.release()
        return
    }

        val prefs = GlobalContext.get().get<Preferences>()
        val serverManager = GlobalContext.get().get<ServerManager>()
    val savedWidth = prefs.windowWidth.value
    val savedHeight = prefs.windowHeight.value

    application {
        var isWindowVisible by remember { mutableStateOf(true) }
        var showCloseDialog by remember { mutableStateOf(false) }
        var quitting by remember { mutableStateOf(false) }
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
            if (!isWindowVisible && !quitting) {
                trayManager.install()
            } else {
                trayManager.remove()
            }
        }

        val iconPainter = remember {
            try {
                val resource = this::class.java.classLoader.getResource("portalhost_logo.png")
                if (resource != null) {
                    val bytes = resource.openStream().readAllBytes()
                    BitmapPainter(Image.makeFromEncoded(bytes).toComposeImageBitmap())
                } else {
                    null
                }
            } catch (_: Exception) { null }
        }

        val preferences = remember { GlobalContext.get().get<Preferences>() }
        val themePref by preferences.theme.collectAsState()

        val isDark = when (themePref) {
            "dark" -> true
            "light" -> false
            else -> isSystemInDarkTheme()
        }

        Window(
            onCloseRequest = { showCloseDialog = true },
            state = windowState,
            title = "Portal Host",
            visible = isWindowVisible,
            icon = iconPainter,
            undecorated = true,
        ) {
            val awtWindow = this.window

            DisposableEffect(awtWindow) {
                val scale = try { awtWindow.graphicsConfiguration.defaultTransform.scaleX } catch (_: Exception) { 1.0 }
                val titleBarHeightPx = (40 * scale).toInt()
                val handler = com.portalhost.desktop.window.NativeTitleBarDragHandler(awtWindow, titleBarHeightPx)
                handler.install()
                onDispose { handler.uninstall() }
            }

            AppTheme.AppTheme(darkTheme = isDark) {
                Box(modifier = Modifier.fillMaxSize()) {
                    DesktopApp(
                        iconPainter = iconPainter,
                        window = awtWindow,
                        onMinimize = { awtWindow.setState(Frame.ICONIFIED) },
                        onMaximizeRestore = {
                            if (awtWindow.extendedState and Frame.MAXIMIZED_BOTH != 0)
                                awtWindow.extendedState = Frame.NORMAL
                            else
                                awtWindow.extendedState = Frame.MAXIMIZED_BOTH
                        },
                        onClose = { showCloseDialog = true },
                        onQuit = { quitting = true; isWindowVisible = false }
                    )

                    if (showCloseDialog) {
                        AlertDialog(
                            onDismissRequest = { },
                            title = { Text("Close Portal Host") },
                            text = { Text("Do you want to minimize to system tray or quit the application?") },
                            confirmButton = {
                                Button(onClick = {
                                    showCloseDialog = false
                                    isWindowVisible = false
                                }) {
                                    Text("Minimize to Tray")
                                }
                            },
                            dismissButton = {
                                Row {
                                    TextButton(onClick = { showCloseDialog = false }) {
                                        Text("Cancel")
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            quitting = true
                                            isWindowVisible = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Quit")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        LaunchedEffect(isWindowVisible, showCloseDialog) {
            if (!isWindowVisible && showCloseDialog) {
                trayManager.remove()
                kotlinx.coroutines.delay(300)
                val runningServers = serverManager.servers.value.entries
                    .filter { (id, _) ->
                        val state = serverManager.serverStates.value[id]
                        state?.status == com.portalhost.model.ServerStatus.RUNNING || state?.status == com.portalhost.model.ServerStatus.STARTING
                    }
                for ((id, _) in runningServers) {
                    serverManager.stopServer(id)
                }
                exitApplication()
            }
        }
    }
}