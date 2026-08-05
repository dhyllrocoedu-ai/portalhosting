package com.portalhost.desktop.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.portalhost.desktop.util.UninstallHelper
import com.portalhost.desktop.util.UpdateChecker
import com.portalhost.desktop.util.UpdateInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.io.File
import java.net.URI

sealed class UpdateDownloadState {
    object Idle : UpdateDownloadState()
    data class Downloading(val progress: Double, val downloaded: Long, val total: Long, val speed: Long) : UpdateDownloadState()
    object Installing : UpdateDownloadState()
    data class Error(val message: String, val retriesLeft: Int) : UpdateDownloadState()
}

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit,
    onNoLongerNeeded: () -> Unit = {},
) {
    var downloadState by remember { mutableStateOf<UpdateDownloadState>(UpdateDownloadState.Idle) }
    var changelogEntries by remember { mutableStateOf(updateInfo.changelog) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(updateInfo) {
        if (changelogEntries.isEmpty()) {
            changelogEntries = try { UpdateChecker.fetchChangelog() } catch (_: Exception) { emptyList() }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (downloadState !is UpdateDownloadState.Downloading) onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Update Available")
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "v${updateInfo.latestVersion}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            updateInfo.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (changelogEntries.isNotEmpty()) {
                    HorizontalDivider()
                    Text("What's New", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    changelogEntries.take(1).forEach { entry ->
                        val changes = entry.items
                        changes.take(8).forEach { change ->
                            Text("• $change", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (changes.size > 8) {
                            Text("... and ${changes.size - 8} more changes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                when (val state = downloadState) {
                    is UpdateDownloadState.Downloading -> {
                        HorizontalDivider()
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LinearProgressIndicator(
                                progress = { state.progress.toFloat() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "${formatBytes(state.downloaded)} / ${formatBytes(state.total)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    "${formatSpeed(state.speed)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                "Downloading update...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    is UpdateDownloadState.Error -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(state.message, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            when (val state = downloadState) {
                is UpdateDownloadState.Idle -> {
                    Button(onClick = {
                        scope.launch {
                            downloadState = UpdateDownloadState.Downloading(0.0, 0, 0, 0)
                            val ext = if (updateInfo.downloadUrl.endsWith(".exe")) "exe" else "msi"
                            val installDir = File(System.getProperty("portalhost.install.dir") ?: System.getProperty("user.home") ?: ".")
                            val destFile = File(installDir, "PortalHost-${updateInfo.latestVersion}-update.$ext")

                            var attempt = 0
                            val maxRetries = 3
                            while (attempt < maxRetries) {
                                attempt++
                                val result = UpdateChecker.downloadUpdate(
                                    updateInfo.downloadUrl,
                                    destFile,
                                ) { downloaded, total, speed ->
                                    val progress = if (total > 0) downloaded.toDouble() / total else 0.0
                                    downloadState = UpdateDownloadState.Downloading(progress, downloaded, total, speed)
                                }

                                result.onSuccess { file ->
                                    downloadState = UpdateDownloadState.Installing
                                    if (launchSilentUpdateAndRestart(file)) {
                                        onNoLongerNeeded()
                                        onDismiss()
                                        kotlin.system.exitProcess(0)
                                    } else {
                                        try {
                                            Desktop.getDesktop().open(file)
                                        } catch (_: Exception) {}
                                        delay(2000)
                                        onNoLongerNeeded()
                                        onDismiss()
                                    }
                                    return@launch
                                }.onFailure { e ->
                                    if (attempt >= maxRetries) {
                                        downloadState = UpdateDownloadState.Error(
                                            "${e.message ?: "Download failed"} (${maxRetries}/${maxRetries} attempts)",
                                            retriesLeft = 0,
                                        )
                                    } else {
                                        downloadState = UpdateDownloadState.Downloading(0.0, 0, 0, 0)
                                        delay(1000)
                                    }
                                }
                            }
                        }
                    }) {
                        Text("Download & Install")
                    }
                }
                is UpdateDownloadState.Downloading -> {
                    TextButton(onClick = {
                        downloadState = UpdateDownloadState.Idle
                    }) {
                        Text("Cancel")
                    }
                }
                is UpdateDownloadState.Installing -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Installing...")
                    }
                }
                is UpdateDownloadState.Error -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            try { Desktop.getDesktop().browse(URI(updateInfo.downloadUrl)) } catch (_: Exception) {}
                            onDismiss()
                        }) {
                            Text("Open in Browser")
                        }
                        if (state.retriesLeft > 0) {
                            Button(onClick = {
                                downloadState = UpdateDownloadState.Idle
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        },
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIdx = 0
    while (value >= 1024 && unitIdx < units.size - 1) {
        value /= 1024
        unitIdx++
    }
    return "%.1f %s".format(value, units[unitIdx])
}

private fun formatSpeed(bytesPerSecond: Long): String {
    if (bytesPerSecond <= 0) return ""
    return "${formatBytes(bytesPerSecond)}/s"
}

/**
 * Silently installs the downloaded update and relaunches the app into the new
 * version, without showing the installer UI.
 *
 * A detached PowerShell process runs the installer (elevated, quiet) and starts
 * the freshly installed PortalHost.exe afterwards. Because it is an OS process
 * (not a JVM thread) it survives this app terminating, which is required so the
 * MSI can replace files that are locked while the app is running.
 */
private fun launchSilentUpdateAndRestart(installerFile: File): Boolean {
    return try {
        val installDir = UninstallHelper.installDirectory() ?: return false
        val appExe = File(installDir, "PortalHost.exe")
        if (!appExe.exists()) return false

        val installerPath = installerFile.absolutePath.replace("'", "''")
        val appExePath = appExe.absolutePath.replace("'", "''")

        val installCmd = if (installerFile.name.endsWith(".msi", ignoreCase = true)) {
            "Start-Process -FilePath msiexec -ArgumentList '/i', '$installerPath', '/qn', '/norestart' -Verb RunAs -Wait"
        } else {
            "Start-Process -FilePath '$installerPath' -ArgumentList '/qn', '/norestart' -Verb RunAs -Wait"
        }

        val psCommand =
            "Start-Sleep -Seconds 2; " +
                "$installCmd; " +
                "Start-Sleep -Seconds 2; " +
                "Start-Process -FilePath '$appExePath'"

        ProcessBuilder("powershell", "-NoProfile", "-WindowStyle", "Hidden", "-Command", psCommand)
            .redirectErrorStream(true)
            .start()
        true
    } catch (_: Exception) {
        false
    }
}
