package com.portalhost.desktop.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.portalhost.model.ServerStatus
import com.portalhost.server.ActivityLog
import com.portalhost.server.ServerManager
import org.koin.compose.koinInject
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun DashboardScreen(
    onNavigateToServer: (String) -> Unit = {},
    onNavigateToCreate: () -> Unit = {},
) {
    val serverManager = koinInject<ServerManager>()
    val activityLog = koinInject<ActivityLog>()
    val servers by serverManager.servers.collectAsState()
    val serverStates by serverManager.serverStates.collectAsState()
    val activities by activityLog.activities.collectAsState()
    val scope = rememberCoroutineScope()

    val runningCount = serverStates.count { it.value.status == ServerStatus.RUNNING }
    val stoppedCount = serverStates.count { it.value.status == ServerStatus.STOPPED }
    val totalCount = servers.size

    suspend fun startAllServers() {
        servers.keys.filter { serverStates[it]?.status != ServerStatus.RUNNING }.forEach { id ->
            serverManager.startServer(id)
        }
    }

    suspend fun stopAllServers() {
        servers.keys.filter { serverStates[it]?.status == ServerStatus.RUNNING }.forEach { id ->
            serverManager.stopServer(id)
        }
    }

    fun openServersFolder() {
        try {
            val folder = File("servers").absoluteFile
            if (folder.exists()) {
                java.awt.Desktop.getDesktop().open(folder)
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun launchInScope(block: suspend () -> Unit) {
        scope.launch { block() }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
    ) {
        Text("Dashboard", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatCard("Total Servers", totalCount.toString(), Icons.Filled.Dns, Color(0xFF5C6BC0))
            StatCard("Running", runningCount.toString(), Icons.Filled.PlayArrow, Color(0xFF4CAF50))
            StatCard("Stopped", stoppedCount.toString(), Icons.Filled.Stop, Color(0xFF9E9E9E))
            StatCard("Memory", "${serverStates.values.sumOf { it.memoryUsage } / 1048576}MB", Icons.Filled.Memory, Color(0xFFFF9800))
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Servers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    if (servers.isEmpty()) {
                        Text("No servers yet", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    } else {
                        servers.entries.take(5).forEach { (id, config) ->
                            val state = serverStates[id]
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier.size(8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val statusColor = when (state?.status) {
                                        ServerStatus.RUNNING -> Color(0xFF4CAF50)
                                        ServerStatus.STARTING -> Color(0xFFFFC107)
                                        ServerStatus.CRASHED -> Color(0xFFF44336)
                                        else -> Color(0xFF9E9E9E)
                                    }
                                    Text(
                                        text = "\u25CF",
                                        color = statusColor,
                                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(config.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val statusText = when (state?.status) {
                                    ServerStatus.RUNNING -> "Running"
                                    ServerStatus.STARTING -> "Starting"
                                    ServerStatus.STOPPING -> "Stopping"
                                    ServerStatus.CRASHED -> "Crashed"
                                    else -> "Stopped"
                                }
                                Text(statusText, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { onNavigateToServer(id) }) {
                                Text("Manage", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    val activities by activityLog.activities.collectAsState()
                    if (activities.isEmpty()) {
                        Text("No recent activity", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    } else {
                        activities.take(10).forEach { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(entry.formattedTime, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(80.dp))
                                Text(entry.action, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Cloud, contentDescription = null, tint = Color(0xFF42A5F5))
                        Spacer(Modifier.width(8.dp))
                        Text("Tunnels", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("No active tunnels", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Storage, contentDescription = null, tint = Color(0xFF66BB6A))
                        Spacer(Modifier.width(8.dp))
                        Text("Storage", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Calculating...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Quick Actions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        QuickActionButton(
                            icon = Icons.Filled.Add,
                            label = "New Server",
                            onClick = { onNavigateToCreate() },
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        QuickActionButton(
                            icon = Icons.Filled.PlayArrow,
                            label = "Start All",
                            onClick = { launchInScope { startAllServers() } },
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        QuickActionButton(
                            icon = Icons.Filled.Stop,
                            label = "Stop All",
                            onClick = { launchInScope { stopAllServers() } },
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        QuickActionButton(
                            icon = Icons.Filled.FolderOpen,
                            label = "Open Servers Folder",
                            onClick = { openServersFolder() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
        }
    }
}