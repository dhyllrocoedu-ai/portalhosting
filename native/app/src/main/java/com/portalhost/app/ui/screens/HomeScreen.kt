package com.portalhost.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.portalhost.app.server.TunnelState
import com.portalhost.app.storage.StorageStats
import com.portalhost.app.ui.components.GrassIcon
import com.portalhost.app.ui.model.ServerConfig
import com.portalhost.app.ui.screens.home.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    jdkInstalled: Boolean,
    jdkInstalling: Boolean,
    jdkProgress: Float = 0f,
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
    activeServer: ServerConfig? = null
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GrassIcon(size = 24.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("PortalHost", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (serverConfigs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GrassIcon(size = 64.dp)
                    Spacer(Modifier.height(16.dp))
                    Text("No servers yet", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Create your first Minecraft server",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onCreateServer) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create Server")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
            item {
                ServerCard(
                    activeServer = activeServer,
                    serverConfigs = serverConfigs,
                    serverState = serverState,
                    statusColor = statusColor,
                    networkInfo = networkInfo,
                    tunnelUrl = tunnelUrl,
                    tunnelState = tunnelState,
                    onSelectServer = onSelectServer,
                    onCreateServer = onCreateServer,
                    onDeleteServer = onDeleteServer
                )
            }

            if (jdkInstalling) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(12.dp))
                                Text("Installing Java runtime...")
                            }
                            if (jdkProgress > 0f) {
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { jdkProgress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            } else if (!jdkInstalled) {
                item {
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
            }

            serverState.error?.let { error ->
                item {
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
            }

            item {
                QuickActions(
                    serverState = serverState,
                    activeServer = activeServer,
                    onStart = onStart,
                    onStop = onStop,
                    onRestart = onRestart
                )
            }

            if (tunnelAvailable) {
                item {
                    TunnelCard(
                        tunnelState = tunnelState,
                        onStart = onTunnelStart,
                        onStop = onTunnelStop,
                        onReset = onTunnelReset,
                        onSaveSecretKey = onSaveSecretKey
                    )
                }
            }

            item {
                LiveStatsGrid(
                    processStats = processStats,
                    serverState = serverState,
                    maxPlayers = maxPlayers
                )
            }

            item {
                ConsolePreview(
                    consoleLines = consoleLines,
                    onOpenConsole = onOpenConsole,
                    onCommand = onCommand,
                    onClearConsole = onClearConsole,
                    isOnline = serverState.status == ServerStatus.ONLINE
                )
            }

            item {
                PlayerListCard(
                    players = serverState.players,
                    isOnline = serverState.status == ServerStatus.ONLINE,
                    onCommand = onCommand,
                    onOpenPlayers = onOpenPlayers,
                    maxPlayers = maxPlayers
                )
            }

            item {
                RecentActivityCard(activityLog = activityLog)
            }

            item {
                StorageCard(storageStats = storageStats)
            }

            item {
                ShortcutGrid(
                    onFiles = onOpenFiles,
                    onLogs = onOpenLogs,
                    onPerformance = onOpenPerformance
                )
            }
        }
    }
}
}
