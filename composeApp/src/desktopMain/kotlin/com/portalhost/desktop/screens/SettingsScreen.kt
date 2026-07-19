package com.portalhost.desktop.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.portalhost.java.JdkManager
import com.portalhost.preferences.Preferences
import com.portalhost.server.TunnelManager
import com.portalhost.server.TunnelStatus
import com.portalhost.util.pickDirectory
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val preferences = koinInject<Preferences>()
    val scope = rememberCoroutineScope()

    val theme by preferences.theme.collectAsState()
    val language by preferences.language.collectAsState()
    val autoCheckUpdates by preferences.autoCheckUpdates.collectAsState()
    val showConsoleColors by preferences.showConsoleColors.collectAsState()
    val maxConsoleLines by preferences.maxConsoleLines.collectAsState()
    val defaultMemoryMin by preferences.defaultMemoryMin.collectAsState()
    val defaultMemoryMax by preferences.defaultMemoryMax.collectAsState()
    val autoBackupEnabled by preferences.autoBackupEnabled.collectAsState()
    val backupIntervalHours by preferences.backupIntervalHours.collectAsState()
    val rconEnabledByDefault by preferences.rconEnabledByDefault.collectAsState()
    val serverAutoRestart by preferences.serverAutoRestart.collectAsState()
    val confirmServerDelete by preferences.confirmServerDelete.collectAsState()
    val showAdvancedSettings by preferences.showAdvancedSettings.collectAsState()
    val logLevel by preferences.logLevel.collectAsState()

    var themeExpanded by remember { mutableStateOf(false) }
    var logLevelExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        SettingsSection("General") {
            ExposedDropdownMenuBox(expanded = themeExpanded, onExpandedChange = { themeExpanded = it }) {
                OutlinedTextField(
                    value = theme,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Theme") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(expanded = themeExpanded, onDismissRequest = { themeExpanded = false }) {
                    listOf("system", "light", "dark").forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t.replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                preferences.theme.value = t
                                themeExpanded = false
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            SettingToggle("Auto-check updates", autoCheckUpdates) { preferences.autoCheckUpdates.value = it }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Data Directory", style = MaterialTheme.typography.bodyMedium)
                    val displayPath = preferences.dataDirectory.value.takeIf { it.isNotBlank() }
                        ?: System.getProperty("user.home") + File.separator + ".portalhost"
                    Text(
                        displayPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Button(onClick = {
                    val chosen = pickDirectory(title = "Select Data Directory")
                    if (chosen != null) {
                        preferences.dataDirectory.value = chosen.absolutePath
                        System.setProperty("portalhost.data.dir", chosen.absolutePath)
                    }
                }) {
                    Text("Change")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "All application data (database, servers, JDKs, tunnels) will be stored here. Restart required.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection("Console") {
            SettingToggle("Show color codes", showConsoleColors) { preferences.showConsoleColors.value = it }
            Spacer(Modifier.height(12.dp))
            Text("Max console lines: $maxConsoleLines", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = maxConsoleLines.toFloat(),
                onValueChange = { preferences.maxConsoleLines.value = it.toInt() },
                valueRange = 100f..20000f,
                steps = 199,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection("Defaults") {
            Text("Default memory: ${defaultMemoryMin}MB - ${defaultMemoryMax}MB", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text("Min", style = MaterialTheme.typography.labelSmall)
            Slider(
                value = defaultMemoryMin.toFloat(),
                onValueChange = { preferences.defaultMemoryMin.value = it.toInt() },
                valueRange = 256f..16384f,
                steps = 63,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text("Max", style = MaterialTheme.typography.labelSmall)
            Slider(
                value = defaultMemoryMax.toFloat(),
                onValueChange = { preferences.defaultMemoryMax.value = it.toInt() },
                valueRange = 256f..16384f,
                steps = 63,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            SettingToggle("Auto-restart on crash", serverAutoRestart) { preferences.serverAutoRestart.value = it }
            SettingToggle("RCON enabled by default", rconEnabledByDefault) { preferences.rconEnabledByDefault.value = it }
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection("Backup") {
            SettingToggle("Auto-backup enabled", autoBackupEnabled) { preferences.autoBackupEnabled.value = it }
            if (autoBackupEnabled) {
                Spacer(Modifier.height(8.dp))
                Text("Backup interval: $backupIntervalHours hours", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = backupIntervalHours.toFloat(),
                    onValueChange = { preferences.backupIntervalHours.value = it.toInt() },
                    valueRange = 1f..48f,
                    steps = 47,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text("Auto-backup runs for each server using its configured interval. Configure per-server in Server Details > Backups tab.", 
                     style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection("Tunnel (playit.gg)") {
            val tunnelManager = koinInject<TunnelManager>()
            val tunnelState by tunnelManager.state.collectAsState()

            // Connection status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    val statusLabel = when (tunnelState.status) {
                        TunnelStatus.IDLE -> "Not Connected"
                        TunnelStatus.DOWNLOADING -> "Downloading..."
                        TunnelStatus.CLAIM_REQUIRED -> "Claim Required"
                        TunnelStatus.CONNECTING -> "Connecting..."
                        TunnelStatus.CONNECTED -> "Connected"
                        TunnelStatus.ERROR -> "Error"
                    }
                    Text("Status: $statusLabel", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    if (tunnelState.status == TunnelStatus.CONNECTED) {
                        tunnelState.tunnels.forEach { tunnel ->
                            Text("Tunnel: ${tunnel.publicAddress} (port ${tunnel.localPort})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (tunnelState.status == TunnelStatus.CLAIM_REQUIRED && tunnelState.claimUrl != null) {
                        Text("Claim URL: ${tunnelState.claimUrl}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(tunnelState.error ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(8.dp))

            // Actions based on status
            when (tunnelState.status) {
                TunnelStatus.IDLE, TunnelStatus.ERROR -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = tunnelManager.getSecretKey() ?: "",
                            onValueChange = { tunnelManager.setSecretKey(it) },
                            label = { Text("Secret Key (optional)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = { scope.launch { tunnelManager.start(25565) } },
                            enabled = tunnelState.status != TunnelStatus.CONNECTING,
                        ) {
                            Text("Start Tunnel")
                        }
                    }
                }
                TunnelStatus.DOWNLOADING -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Downloading playit agent...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                TunnelStatus.CLAIM_REQUIRED -> {
                    if (tunnelState.claimUrl != null) {
                        Button(
                            onClick = { java.awt.Desktop.getDesktop().browse(java.net.URI(tunnelState.claimUrl)) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        ) {
                            Text("Open Claim URL")
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Claim your tunnel in the browser, then reconnect.",
                             style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { tunnelManager.stop() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    ) {
                        Text("Cancel")
                    }
                }
                TunnelStatus.CONNECTING, TunnelStatus.CONNECTED -> {
                    Button(
                        onClick = { tunnelManager.stop() },
                        enabled = tunnelState.status != TunnelStatus.CONNECTING,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    ) {
                        Text("Stop Tunnel")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Secret key management
            if (tunnelManager.getSecretKey() != null) {
                Button(
                    onClick = { tunnelManager.resetKey() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Reset Secret Key")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection("Java / JDK") {
            val jdkManager = koinInject<JdkManager>()
            val installations by jdkManager.knownInstallations.collectAsState()
            val isInstalling by jdkManager.isInstalling.collectAsState()
            val installProgress by jdkManager.installProgress.collectAsState()

            LaunchedEffect(Unit) { jdkManager.refresh() }

            if (installations.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Memory, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text("No JDK installations found", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                installations.forEach { inst ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Java ${inst.version} - ${inst.vendor}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(inst.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        Text(if (inst.isJre) "JRE" else "JDK", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (installations.isEmpty()) {
                    Button(
                        onClick = { scope.launch { jdkManager.installJdk(21) } },
                        enabled = !isInstalling,
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Install Java 21")
                    }
                } else {
                    Button(
                        onClick = { scope.launch { jdkManager.installJdk(21) } },
                        enabled = !isInstalling,
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reinstall Java 21")
                    }
                }
            }
            if (isInstalling) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { installProgress.toFloat() }, modifier = Modifier.fillMaxWidth())
                Text("Installing JDK... ${(installProgress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection("Advanced") {
            SettingToggle("Confirm before deleting servers", confirmServerDelete) { preferences.confirmServerDelete.value = it }
            SettingToggle("Show advanced settings", showAdvancedSettings) { preferences.showAdvancedSettings.value = it }
            Spacer(Modifier.height(12.dp))
            ExposedDropdownMenuBox(expanded = logLevelExpanded, onExpandedChange = { logLevelExpanded = it }) {
                OutlinedTextField(
                    value = logLevel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Log Level") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = logLevelExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(expanded = logLevelExpanded, onDismissRequest = { logLevelExpanded = false }) {
                    listOf("TRACE", "DEBUG", "INFO", "WARN", "ERROR").forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level) },
                            onClick = {
                                preferences.logLevel.value = level
                                logLevelExpanded = false
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        var aboutExpanded by remember { mutableStateOf(false) }
        SettingsSection("About") {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { aboutExpanded = !aboutExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Portal Host", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("Version 5.0.18", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (aboutExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (aboutExpanded) "Collapse" else "Expand",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (aboutExpanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text("Desktop Minecraft Server Manager", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("Built with Compose Multiplatform", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("Portal Host is a cross-platform Minecraft server manager that allows you to create, configure, start, and stop Minecraft Java Edition servers from a modern desktop interface.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp))
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { preferences.resetToDefaults() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
            modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp).height(48.dp),
        ) {
            Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Reset to Defaults")
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingToggle(label: String, value: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = value, onCheckedChange = onChanged)
    }
}