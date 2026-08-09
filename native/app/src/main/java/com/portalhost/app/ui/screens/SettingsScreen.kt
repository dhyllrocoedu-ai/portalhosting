package com.portalhost.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.portalhost.app.AndroidUpdateInfo
import com.portalhost.app.BuildConfig
import com.portalhost.app.UpdateCheckResult
import com.portalhost.app.UpdateChecker
import com.portalhost.app.ui.components.CraftingIcon
import com.portalhost.app.ui.components.PortalHostLogo
import com.portalhost.app.ui.components.RedstoneIcon
import com.portalhost.app.ui.components.PickaxeIcon
import com.portalhost.app.ui.model.ServerConfig
import com.portalhost.app.update.AppUpdater
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    javaPath: String,
    jdkInstalled: Boolean,
    jdkInstalling: Boolean,
    jdkProgress: Float = 0f,
    jdkMessage: String = "",
    jdkError: String? = null,
    onReinstallJava: () -> Unit,
    onUninstallJava: () -> Unit,
    onFixupJava: () -> Unit,
    onClearAppData: () -> Unit,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    activeServer: ServerConfig?,
    onUpdateServer: (ServerConfig) -> Unit,
    tunnelUrl: String = "",
    onTunnelUrlChange: (String) -> Unit = {}
) {
    var showClearConfirm by remember { mutableStateOf(false) }
    var showRemoveJdkConfirm by remember { mutableStateOf(false) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<AndroidUpdateInfo?>(null) }
    var updateMessage by remember { mutableStateOf<String?>(null) }
    var downloadingUpdate by remember { mutableStateOf(false) }
    var updateProgress by remember { mutableStateOf(0f) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Appearance
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PortalHostLogo(size = 20.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Appearance", style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (darkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (darkTheme) "Dark Theme" else "Light Theme",
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = darkTheme, onCheckedChange = { onToggleTheme() })
                    }
                }
            }

            // Server Defaults
            if (activeServer != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RedstoneIcon(size = 20.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("Server Defaults (${activeServer.name})", style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Auto-restart on crash", modifier = Modifier.weight(1f))
                            Switch(checked = activeServer.autoRestart, onCheckedChange = { enabled ->
                                onUpdateServer(activeServer.copy(autoRestart = enabled))
                            })
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Auto-backup on stop", modifier = Modifier.weight(1f))
                            Switch(checked = activeServer.autoBackup, onCheckedChange = { enabled ->
                                onUpdateServer(activeServer.copy(autoBackup = enabled))
                            })
                        }
                    }
                }
            }

            // Tunnel (playit.gg / ngrok)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Tunnel Address", style = MaterialTheme.typography.titleSmall)
                            Text("Paste your playit.gg or ngrok URL here", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tunnelUrl,
                        onValueChange = onTunnelUrlChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. https://minecraft123.playit.gg") },
                        singleLine = true,
                        label = { Text("Tunnel URL") }
                    )
                }
            }

            // Java Runtime
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PickaxeIcon(size = 20.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Java Runtime", style = MaterialTheme.typography.titleSmall)
                            if (jdkInstalling) {
                                Text(
                                    if (jdkMessage.isNotBlank()) jdkMessage else "Installing...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (jdkProgress > 0f) {
                                    Text(
                                        "${(jdkProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else if (jdkInstalled) {
                                Text("Installed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            } else {
                                Text("Not installed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                            if (jdkError != null) {
                                Text(jdkError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(javaPath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (jdkInstalling) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { jdkProgress }, modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = onReinstallJava, enabled = !jdkInstalling) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (jdkInstalled) "Reinstall" else "Install")
                        }
                        OutlinedButton(onClick = onFixupJava, enabled = jdkInstalled && !jdkInstalling) {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Fix Libraries")
                        }
                        OutlinedButton(
                            onClick = { showRemoveJdkConfirm = true },
                            enabled = jdkInstalled && !jdkInstalling,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Remove")
                        }
                    }
                }
            }

            // Battery Optimization
            val context = LocalContext.current
            val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
            var isBatteryExempt by remember { mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName)) }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BatteryFull, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Background Keep-Alive", style = MaterialTheme.typography.titleSmall)
                            Text("Battery optimization: ${if (isBatteryExempt) "Disabled (server will survive in background)" else "Enabled (server may be killed when backgrounded)"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isBatteryExempt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        }
                    }
                    if (!isBatteryExempt) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Disable Battery Optimization")
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Tap the button above, then toggle \"Allow background operation\" on the next screen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("After toggling, come back here and the status will update.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Spacer(Modifier.height(8.dp))
                        Text("The server can stay alive in the background without being killed by the system.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Storage
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Storage", style = MaterialTheme.typography.titleSmall)
                            Text("Data stored in app files directory", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showClearConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear All Data")
                    }
                }
            }

            // App Info
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CraftingIcon(size = 20.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("App Info", style = MaterialTheme.typography.titleSmall)
                            Text("PortalHost v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (!checkingUpdate) {
                                coroutineScope.launch {
                                    checkingUpdate = true
                                    when (val result = UpdateChecker.checkForUpdate()) {
                                        is UpdateCheckResult.UpdateAvailable -> updateInfo = result.info
                                        UpdateCheckResult.UpToDate -> updateMessage = "You're up to date"
                                        UpdateCheckResult.Error -> updateMessage = "Couldn't check for updates. Make sure you're online and try again."
                                    }
                                    checkingUpdate = false
                                }
                            }
                        },
                        enabled = !checkingUpdate,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (checkingUpdate) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(if (checkingUpdate) "Checking..." else "Check for Updates")
                    }
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/dhyllrocoedu-ai/portalhosting/issues"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Report Issue")
                    }
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Check out PortalHost — run Minecraft servers on Android!\nhttps://github.com/dhyllrocoedu-ai/portalhosting")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share PortalHost"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Share App")
                    }
                }
            }
        }
    }

    // Clear all data confirmation
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Data") },
            text = { Text("This will delete all servers, worlds, and the Java runtime. The app will reinstall Java on next launch. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    onClearAppData()
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // Remove JDK confirmation
    if (showRemoveJdkConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveJdkConfirm = false },
            title = { Text("Remove Java") },
            text = { Text("Remove the Java runtime? Servers will not start until Java is reinstalled.") },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveJdkConfirm = false
                    onUninstallJava()
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveJdkConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // Update available
    updateInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { if (!downloadingUpdate) updateInfo = null },
            icon = { Icon(Icons.Default.Update, contentDescription = null) },
            title = { Text("Update Available") },
            text = {
                Column {
                    Text("PortalHost v${info.latestVersion} is available (you have v${BuildConfig.VERSION_NAME}).")
                    Spacer(Modifier.height(4.dp))
                    Text("Released ${info.releaseDate.takeIf { it.isNotBlank() } ?: "recently"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (info.sizeBytes > 0) {
                        Text("Size: %.1f MB".format(info.sizeBytes / 1024.0 / 1024.0), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Tap Download to fetch and install the new version right inside the app. Your servers and worlds are kept in the app files directory and won't be lost.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        downloadingUpdate = true
                        updateProgress = 0f
                        downloadedApk = null
                        coroutineScope.launch {
                            val result = AppUpdater.download(context, info) { frac ->
                                updateProgress = frac
                            }
                            result.onSuccess { file ->
                                downloadedApk = file
                                downloadingUpdate = false
                            }.onFailure { e ->
                                downloadingUpdate = false
                                updateInfo = null
                                updateMessage = "Download failed: ${e.message}. Tap Open in Browser to download manually."
                            }
                        }
                    },
                    enabled = !downloadingUpdate
                ) { Text("Download") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!downloadingUpdate) {
                            updateInfo = null
                            downloadedApk?.let { runCatching { it.delete() } }
                            downloadedApk = null
                        }
                    },
                    enabled = !downloadingUpdate
                ) { Text("Later") }
            }
        )
    }

    // Downloading progress
    if (downloadingUpdate && updateInfo != null) {
        AlertDialog(
            onDismissRequest = { },
            icon = { CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp) },
            title = { Text("Downloading PortalHost v${updateInfo?.latestVersion}…") },
            text = {
                Column {
                    if ((updateInfo?.sizeBytes ?: 0) > 0) {
                        LinearProgressIndicator(
                            progress = { updateProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${(updateProgress * 100).toInt()}%  ·  %.1f MB / %.1f MB".format(
                                updateInfo!!.sizeBytes * updateProgress / 1024.0 / 1024.0,
                                updateInfo!!.sizeBytes / 1024.0 / 1024.0
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Downloading…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Ready to install
    val apkReady = downloadedApk
    val infoForInstall = updateInfo
    if (apkReady != null && infoForInstall != null && !downloadingUpdate) {
        AlertDialog(
            onDismissRequest = {
                downloadedApk = null
                updateInfo = null
            },
            icon = { Icon(Icons.Default.SystemUpdate, contentDescription = null) },
            title = { Text("Ready to Install") },
            text = {
                Column {
                    Text("PortalHost v${infoForInstall.latestVersion} has finished downloading.")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap Install to open Android's installer. If the install dialog is blocked by 'Install unknown apps', grant the permission for PortalHost in Settings and try again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    runCatching { AppUpdater.install(context, apkReady) }
                        .onFailure {
                            updateMessage = "Couldn't start the installer: ${it.message}. Tap Open in Browser to download manually."
                        }
                    downloadedApk = null
                    updateInfo = null
                }) { Text("Install") }
            },
            dismissButton = {
                TextButton(onClick = {
                    AppUpdater.openInBrowser(context, infoForInstall)
                    downloadedApk = null
                    updateInfo = null
                }) { Text("Open in Browser") }
            }
        )
    }

    // Check result message
    updateMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { updateMessage = null },
            title = { Text("Update Check") },
            text = { Text(message) },
            confirmButton = {
                if (message.contains("Open in Browser", ignoreCase = true) && updateInfo != null) {
                    TextButton(onClick = {
                        updateInfo?.let { AppUpdater.openInBrowser(context, it) }
                        updateMessage = null
                    }) { Text("Open in Browser") }
                } else {
                    TextButton(onClick = { updateMessage = null }) { Text("OK") }
                }
            },
            dismissButton = {
                TextButton(onClick = { updateMessage = null }) { Text("Dismiss") }
            }
        )
    }
}
