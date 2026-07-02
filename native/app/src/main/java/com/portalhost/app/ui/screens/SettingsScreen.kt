package com.portalhost.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.portalhost.app.ui.components.CraftingIcon
import com.portalhost.app.ui.components.GrassIcon
import com.portalhost.app.ui.components.RedstoneIcon
import com.portalhost.app.ui.components.PickaxeIcon
import com.portalhost.app.ui.model.ServerConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    javaPath: String,
    jdkInstalled: Boolean,
    jdkInstalling: Boolean,
    onReinstallJava: () -> Unit,
    onUninstallJava: () -> Unit,
    onFixupJava: () -> Unit,
    onClearAppData: () -> Unit,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    activeServer: ServerConfig?,
    onUpdateServer: (ServerConfig) -> Unit
) {
    var showClearConfirm by remember { mutableStateOf(false) }
    var showRemoveJdkConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Appearance
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GrassIcon(size = 20.dp)
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

            // Java Runtime
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PickaxeIcon(size = 20.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Java Runtime", style = MaterialTheme.typography.titleSmall)
                            if (jdkInstalling) {
                                Text("Installing...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            } else if (jdkInstalled) {
                                Text("Installed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            } else {
                                Text("Not installed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(javaPath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (jdkInstalling) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = onReinstallJava, enabled = !jdkInstalling) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reinstall")
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

            // Storage
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null)
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
                            Text("PortalHost v2.1.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { /* TODO: open GitHub releases */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Check for Updates")
                    }
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { /* TODO: open GitHub issues */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Report Issue")
                    }
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { /* TODO: share intent */ },
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
}
