package com.portalhost.desktop.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.filesystem.FileSystem
import com.portalhost.java.JdkManager
import com.portalhost.model.ServerConfig
import com.portalhost.model.ServerState
import com.portalhost.model.ServerStatus
import com.portalhost.server.ActivityEntry
import com.portalhost.server.ActivityLog
import com.portalhost.server.ServerManager
import com.portalhost.server.TunnelManager
import com.portalhost.server.TunnelStatus
import com.portalhost.server.getServerIconFile
import com.portalhost.server.loadServerIcon
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File

@Composable
fun DashboardScreen(
    onNavigateToConsole: (String) -> Unit = {},
    onNavigateToServer: (String) -> Unit = {},
    onNavigateToCreate: () -> Unit = {},
) {
    val serverManager = koinInject<ServerManager>()
    val fileSystem = koinInject<com.portalhost.filesystem.FileSystem>()
    val jdkManager = koinInject<JdkManager>()
    val activityLog = koinInject<ActivityLog>()
    val tunnelManager = koinInject<TunnelManager>()
    val servers by serverManager.servers.collectAsState()
    val serverStates by serverManager.serverStates.collectAsState()
    val consoleOutputs by serverManager.consoleOutputs.collectAsState()
    val tunnelState by tunnelManager.state.collectAsState()
    val activities by activityLog.activities.collectAsState()
    val jdkInstalling by jdkManager.isInstalling.collectAsState()
    val jdkProgress by jdkManager.installProgress.collectAsState()
    val jdkInstallations by jdkManager.knownInstallations.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedServerId by remember { mutableStateOf<String?>(null) }
    val previousServerCount by remember { mutableStateOf(servers.size) }
    var expanded by remember { mutableStateOf(false) }
    var commandInput by remember { mutableStateOf("") }

    LaunchedEffect(servers) {
        if (selectedServerId == null && servers.isNotEmpty()) {
            selectedServerId = servers.keys.first()
        }
    }

    val activeServer = selectedServerId?.let { servers[it] }
    val activeState = selectedServerId?.let { serverStates[it] }
    val activeConsole = selectedServerId?.let { consoleOutputs[it] } ?: emptyList()
    val serverIcon = remember(activeServer?.id) {
        activeServer?.id?.let { id ->
            val iconFile = getServerIconFile(java.io.File(fileSystem.getServersDirBlocking(), id))
            loadServerIcon(iconFile)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
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

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (servers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(400.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("No servers yet", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("Create your first Minecraft server", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = onNavigateToCreate) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Create Server")
                        }
                    }
                }
            } else {
                val statusColor = when (activeState?.status) {
                    ServerStatus.RUNNING -> Color(0xFF4CAF50)
                    ServerStatus.STARTING -> Color(0xFFFFC107)
                    ServerStatus.STOPPING -> Color(0xFFFF9800)
                    ServerStatus.CRASHED -> Color(0xFFF44336)
                    else -> Color(0xFF9E9E9E)
                }

                ServerCard(
                    serverIcon = serverIcon,
                    activeServer = activeServer,
                    serverConfigs = servers.values.toList(),
                    activeState = activeState,
                    statusColor = statusColor,
                    expanded = expanded,
                    onToggleExpand = { expanded = !expanded },
                    onSelectServer = { id ->
                        selectedServerId = id
                        expanded = false
                    },
                    onCreateServer = onNavigateToCreate,
                    onDeleteServer = { config ->
                        scope.launch { serverManager.deleteServer(config.id) }
                        if (config.id == selectedServerId) {
                            val remaining = servers.keys.firstOrNull { it != config.id }
                            selectedServerId = remaining
                        }
                    },
                    onStart = { scope.launch { selectedServerId?.let { serverManager.startServer(it) } } },
                    onStop = { scope.launch { selectedServerId?.let { serverManager.stopServer(it) } } },
                    onRestart = { scope.launch { selectedServerId?.let { serverManager.restartServer(it) } } }
                )

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
                                Text("Installing Java runtime...")
                            }
                            if (jdkProgress > 0.0) {
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(progress = { jdkProgress.toFloat() }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                } else if (jdkInstallations.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(12.dp))
                            Text("Java runtime not installed. Install from Settings or place JDK in the data directory.")
                        }
                    }
                }

                activeState?.lastError?.let { error ->
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

                TunnelCard(
                    tunnelState = tunnelState,
                    onStart = { scope.launch { tunnelManager.start(activeServer?.port ?: 25565) } },
                    onStop = { scope.launch { tunnelManager.stop() } }
                )

                PerformanceCard(
                    activeState = activeState,
                    onOpenPerformance = { selectedServerId?.let { onNavigateToServer(it) } }
                )

                ConsoleCard(
                    consoleLines = activeConsole,
                    onOpenConsole = { selectedServerId?.let { onNavigateToConsole(it) } },
                    onCommand = { cmd ->
                        scope.launch {
                            selectedServerId?.let { id ->
                                val handle = serverManager.getProcessForServer(id)
                                if (handle != null) {
                                    val writer = handle.outputStream.bufferedWriter()
                                    writer.write("$cmd\n")
                                    writer.flush()
                                }
                            }
                        }
                    },
                    onClearConsole = {},
                    isOnline = activeState?.status == ServerStatus.RUNNING,
                    commandInput = commandInput,
                    onCommandInputChange = { commandInput = it }
                )

                ActivityCard(activities = activities)
            }
        }
    }
}

@Composable
private fun ServerCard(
    serverIcon: ImageBitmap?,
    activeServer: ServerConfig?,
    serverConfigs: List<ServerConfig>,
    activeState: ServerState?,
    statusColor: Color,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectServer: (String) -> Unit,
    onCreateServer: () -> Unit,
    onDeleteServer: (ServerConfig) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit
) {
    val status = activeState?.status ?: ServerStatus.STOPPED
    val statusLabel = status.name.lowercase().replaceFirstChar { it.uppercase() }
    val statusColorForBadge = when (status) {
        ServerStatus.RUNNING -> Color(0xFF4CAF50)
        ServerStatus.STARTING -> Color(0xFFFFC107)
        ServerStatus.STOPPING -> Color(0xFFFF9800)
        ServerStatus.CRASHED -> Color(0xFFF44336)
        else -> Color(0xFFA5D6A7)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (serverIcon != null) {
                            Image(bitmap = serverIcon, contentDescription = "Server Icon", modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)))
                        } else {
                            Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = activeServer?.name ?: "No Server",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusColorForBadge.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = statusLabel,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColorForBadge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (activeServer != null) {
                        Text(
                            text = "${activeServer.serverType.name} ${activeServer.version}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (activeServer != null) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("MC ${activeServer.version.ifBlank { "?" }}", fontSize = 10.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Java ${activeServer.javaVersion}", fontSize = 10.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                            SuggestionChip(
                                onClick = {},
                                label = { Text("RAM ${activeServer.memoryMax}M", fontSize = 10.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
                if (serverConfigs.isNotEmpty()) {
                    IconButton(onClick = onToggleExpand) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Switch server"
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val canStart = (status == ServerStatus.STOPPED || status == ServerStatus.CRASHED) && activeServer != null
                val canStop = status == ServerStatus.RUNNING
                val canRestart = status == ServerStatus.RUNNING

                ControlButton(
                    icon = Icons.Default.PlayArrow,
                    label = "Start",
                    onClick = onStart,
                    enabled = canStart,
                    color = Color(0xFF4CAF50)
                )
                ControlButton(
                    icon = Icons.Default.Stop,
                    label = "Stop",
                    onClick = onStop,
                    enabled = canStop,
                    color = Color(0xFFF44336)
                )
                ControlButton(
                    icon = Icons.Default.Refresh,
                    label = "Restart",
                    onClick = onRestart,
                    enabled = canRestart,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                serverConfigs.forEach { config ->
                    val isActive = config.id == activeServer?.id
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable {
                            onSelectServer(config.id)
                        },
                        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(config.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable {
                        activeServer?.let { onDeleteServer(it) }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Text("Delete Server", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.ControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    color: Color
) {
    ElevatedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.weight(1f).height(56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = color.copy(alpha = 0.12f),
            contentColor = color,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun TunnelCard(
    tunnelState: com.portalhost.server.TunnelState,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val status = tunnelState.status
    val connected = status == TunnelStatus.CONNECTED
    val statusText = when (status) {
        TunnelStatus.IDLE -> "Not Connected"
        TunnelStatus.DOWNLOADING -> "Downloading..."
        TunnelStatus.CLAIM_REQUIRED -> "Claim Required"
        TunnelStatus.CONNECTING -> "Connecting..."
        TunnelStatus.CONNECTED -> "Connected"
        TunnelStatus.ERROR -> "Error"
    }
    val statusColor = when (status) {
        TunnelStatus.CONNECTED -> MaterialTheme.colorScheme.primary
        TunnelStatus.ERROR -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val enabled = status != TunnelStatus.DOWNLOADING

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Tunnel", style = MaterialTheme.typography.titleMedium)
                Text(statusText, style = MaterialTheme.typography.bodySmall, color = statusColor)
            }
            Button(
                onClick = { if (connected) onStop() else onStart() },
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(if (connected) "Disconnect" else "Connect")
            }
        }
    }
}

@Composable
private fun PerformanceCard(
    activeState: ServerState?,
    onOpenPerformance: () -> Unit
) {
    val live = activeState?.status == ServerStatus.RUNNING || activeState?.status == ServerStatus.STARTING

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onOpenPerformance) {
                    Text("View Details", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatMiniCard(value = if (live) "${activeState?.cpuUsage?.toInt()}%" else "—", label = "CPU")
                StatMiniCard(
                    value = if (live) formatRam(activeState?.memoryUsage ?: 0) else "—",
                    label = "RAM"
                )
                StatMiniCard(
                    value = if (live && activeState?.cpuUsage != null) "%.1f".format(20.0 - (activeState.cpuUsage / 5.0)) else "—",
                    label = "TPS"
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatMiniCard(value = if (live) "${activeState?.playersOnline ?: 0}" else "—", label = "Players")
                StatMiniCard(value = "—", label = "Download")
                StatMiniCard(value = "—", label = "Upload")
            }
        }
    }
}

@Composable
private fun RowScope.StatMiniCard(value: String, label: String) {
    Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConsoleCard(
    consoleLines: List<String>,
    onOpenConsole: () -> Unit,
    onCommand: (String) -> Unit,
    onClearConsole: () -> Unit,
    isOnline: Boolean,
    commandInput: String,
    onCommandInputChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Console", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onClearConsole, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                }
                TextButton(onClick = onOpenConsole) {
                    Text("Open Console", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0D0D0D))
                    .padding(8.dp)
            ) {
                if (consoleLines.isEmpty()) {
                    Text(
                        text = "Console output will appear here...",
                        color = Color(0xFF555555),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                } else {
                    LazyColumn {
                        items(consoleLines.takeLast(6)) { line ->
                            Text(
                                text = line,
                                color = consoleLineColor(line),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }

            if (isOnline) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = onCommandInputChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter command...", fontSize = 13.sp) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = {
                        if (commandInput.isNotBlank()) {
                            onCommand(commandInput)
                            onCommandInputChange("")
                        }
                    }) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityCard(activities: List<ActivityEntry>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {}) {
                    Text("View All", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (activities.isEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("No recent activity", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Spacer(Modifier.height(8.dp))
                activities.take(10).forEach { entry ->
                    ActivityRow(entry)
                    if (entry != activities.take(10).last()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun activityIconAndColor(action: String, defaultTint: Color): Pair<ImageVector, Color> {
    val lower = action.lowercase()
    return when {
        lower.contains("start") || lower.contains("launch") || lower.contains("join") ->
            Icons.Default.CheckCircle to Color(0xFF4CAF50)
        lower.contains("error") || lower.contains("crash") || lower.contains("fail") ->
            Icons.Default.Error to Color(0xFFF44336)
        lower.contains("warn") || lower.contains("warning") ->
            Icons.Default.Warning to Color(0xFFFFC107)
        lower.contains("leave") || lower.contains("quit") || lower.contains("disconnect") ->
            Icons.Default.PersonRemove to Color(0xFFFF9800)
        lower.contains("player") || lower.contains("chat") ->
            Icons.Default.Person to Color(0xFF2196F3)
        else -> Icons.Default.Info to defaultTint
    }
}

@Composable
private fun ActivityRow(entry: ActivityEntry) {
    val (icon, color) = activityIconAndColor(entry.action, MaterialTheme.colorScheme.onSurfaceVariant)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = entry.formattedTime,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.width(8.dp))
        Text("[${entry.serverName}] ${entry.action}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun formatRam(bytes: Long): String = when {
    bytes < 1024 -> "${bytes}B"
    bytes < 1024 * 1024 -> "${bytes / 1024}KB"
    bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
    else -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024 * 1024))}GB"
}

private fun consoleLineColor(line: String): Color {
    return when {
        line.contains(" ERROR ") || line.contains("FATAL") || line.contains("exception", ignoreCase = true) -> Color(0xFFFF5555)
        line.contains(" WARN ") -> Color(0xFFFFAA00)
        line.contains(" INFO ") || line.contains("[User Authenticator #") -> Color(0xFFE0E0E0)
        line.contains("joined the game") -> Color(0xFF55FF55)
        line.contains("left the game") -> Color(0xFFFFFF55)
        line.contains("<") && line.contains(">") -> Color(0xFFAA55FF)
        line.contains("DEBUG") || line.contains("TRACE") -> Color(0xFF888888)
        line.matches(Regex("""^\s*\[\d+:\d+:\d+\]\[.*\].*""")) -> Color(0xFFB0BEC5)
        else -> Color(0xFFCCCCCC)
    }
}
