package com.portalhost.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.portalhost.app.activity.ActivityLog
import com.portalhost.app.network.NetworkInfo
import com.portalhost.app.server.ProcessStats
import com.portalhost.app.server.ServerState
import com.portalhost.app.server.ServerStatus
import com.portalhost.app.server.SkinService
import com.portalhost.app.server.TunnelState
import com.portalhost.app.storage.StorageStats
import com.portalhost.app.ui.components.PortalHostLogo
import com.portalhost.app.ui.model.ServerConfig
import com.portalhost.app.ui.model.ServerRepository
import com.portalhost.app.ui.screens.home.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    serverConfigs: List<ServerConfig>,
    activeServerId: String?,
    serverState: ServerState,
    processStats: ProcessStats,
    consoleLines: List<String>,
    activityLog: ActivityLog,
    networkInfo: NetworkInfo,
    storageStats: StorageStats,
    currentPlayers: List<String> = emptyList(),
    skinService: SkinService? = null,
    jdkInstalled: Boolean,
    jdkInstalling: Boolean,
    jdkProgress: Float = 0f,
    jdkMessage: String = "",
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onCommand: (String) -> Unit,
    onClearConsole: () -> Unit = {},
    onOpenConsole: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenPlayers: () -> Unit,
    onOpenLogs: () -> Unit = {},
    onOpenPerformance: () -> Unit = {},
    onViewActivity: () -> Unit = {},
    onSelectServer: (String) -> Unit,
    onCreateServer: () -> Unit,
    onDeleteServer: (ServerConfig) -> Unit,
    tunnelUrl: String = "",
    tunnelState: TunnelState? = null,
    onTunnelStart: () -> Unit = {},
    onTunnelStop: () -> Unit = {},
    onTunnelReset: () -> Unit = {},
    onSaveSecretKey: (String) -> Unit = {},
    tunnelAvailable: Boolean = true,
    serverDir: File? = null,
    activeServer: ServerConfig? = null,
    repository: ServerRepository
) {
    val activeServer = activeServer ?: serverConfigs.find { it.id == activeServerId }
    val maxPlayers = remember(serverDir) { readMaxPlayers(serverDir) }

    val statusColor by animateColorAsState(
        targetValue = when (serverState.status) {
            ServerStatus.ONLINE -> Color(0xFF4CAF50)
            ServerStatus.STARTING -> Color(0xFFFFC107)
            ServerStatus.STOPPING -> Color(0xFFFF9800)
            ServerStatus.STOPPED -> Color(0xFFA5D6A7)
            ServerStatus.CRASHED -> Color(0xFFF44336)
            ServerStatus.OFFLINE -> Color(0xFF9E9E9E)
        }, label = "statusColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // ─── Header ────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PortalHostLogo(size = 28.dp)
            Spacer(Modifier.width(10.dp))
            Text("PortalHost", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {}) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
        }

        // ─── Scrollable Content ────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (serverConfigs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PortalHostLogo(size = 64.dp)
                        Spacer(Modifier.height(16.dp))
                        Text("No servers yet", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("Create your first Minecraft server", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = onCreateServer) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Create Server")
                        }
                    }
                }
            } else {
                // Server Overview Card (with controls integrated)
                ServerCard(
                    activeServer = activeServer,
                    serverConfigs = serverConfigs,
                    serverState = serverState,
                    statusColor = statusColor,
                    networkInfo = networkInfo,
                    repository = repository,
                    onSelectServer = onSelectServer,
                    onCreateServer = onCreateServer,
                    onDeleteServer = onDeleteServer,
                    onStart = onStart,
                    onStop = onStop,
                    onRestart = onRestart
                )

                // JDK install progress
                if (jdkInstalling) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        if (jdkMessage.isNotBlank()) jdkMessage else "Installing Java runtime...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (jdkProgress > 0f) {
                                        Text(
                                            "${(jdkProgress * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            if (jdkProgress > 0f) {
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(progress = { jdkProgress }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                } else if (!jdkInstalled) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(12.dp))
                            Text("Java runtime not installed. Restart app to retry.")
                        }
                    }
                }

                // Error banner
                serverState.error?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(error, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Tunnel
                if (tunnelAvailable) {
                    TunnelCard(
                        tunnelState = tunnelState,
                        onStart = onTunnelStart,
                        onStop = onTunnelStop
                    )
                }

                // Performance
                PerformanceCard(
                    processStats = processStats,
                    serverState = serverState,
                    maxPlayers = maxPlayers,
                    onOpenPerformance = onOpenPerformance
                )

                // Storage
                StorageCard(storageStats = storageStats)

                // Console
                ConsoleCard(
                    consoleLines = consoleLines,
                    onOpenConsole = onOpenConsole,
                    onCommand = onCommand,
                    onClearConsole = onClearConsole,
                    isOnline = serverState.status == ServerStatus.ONLINE
                )

                // Activity
                ActivityCard(activityLog = activityLog, onViewAll = onViewActivity)

                // Players (mirrors desktop dashboard)
                PlayerListCard(
                    players = currentPlayers,
                    isOnline = serverState.status == ServerStatus.ONLINE,
                    onCommand = onCommand,
                    onOpenPlayers = onOpenPlayers,
                    skinService = skinService,
                    maxPlayers = maxPlayers
                )

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
