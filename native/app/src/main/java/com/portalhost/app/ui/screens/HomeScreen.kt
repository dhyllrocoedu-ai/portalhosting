package com.portalhost.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.app.activity.ActivityEntry
import com.portalhost.app.activity.ActivityLog
import com.portalhost.app.activity.ActivityType
import com.portalhost.app.network.NetworkInfo
import com.portalhost.app.network.NetworkManager
import com.portalhost.app.server.ProcessStats
import com.portalhost.app.server.ServerState
import com.portalhost.app.server.ServerStatus
import com.portalhost.app.storage.StorageStats
import com.portalhost.app.ui.components.GrassIcon
import com.portalhost.app.ui.components.MinecraftHeadIcon
import com.portalhost.app.ui.components.PlayerIcon
import com.portalhost.app.ui.model.ServerConfig
import java.text.SimpleDateFormat
import java.util.*

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
    jdkInstalled: Boolean,
    jdkInstalling: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onCommand: (String) -> Unit,
    onOpenConsole: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenPlayers: () -> Unit,
    onSelectServer: (String) -> Unit,
    onCreateServer: () -> Unit,
    onDeleteServer: (ServerConfig) -> Unit,
    publicIp: String = "",
    tunnelUrl: String = ""
) {
    val activeServer = serverConfigs.find { it.id == activeServerId }
    val clipboardManager = LocalClipboardManager.current

    val statusColor by animateColorAsState(
        targetValue = when (serverState.status) {
            ServerStatus.ONLINE -> Color(0xFF4CAF50)
            ServerStatus.STARTING -> Color(0xFFFFC107)
            ServerStatus.STOPPING -> Color(0xFFFF9800)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Section 1 — Server Card
            item {
                ServerCard(
                    activeServer = activeServer,
                    serverConfigs = serverConfigs,
                    serverState = serverState,
                    statusColor = statusColor,
                    networkInfo = networkInfo,
                    publicIp = publicIp,
                    tunnelUrl = tunnelUrl,
                    onSelectServer = onSelectServer,
                    onCreateServer = onCreateServer,
                    onDeleteServer = onDeleteServer
                )
            }

            // JDK install status
            if (jdkInstalling) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("Installing Java runtime...")
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

            // Error display
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

            // Section 2 — Quick Actions
            item {
                QuickActions(
                    serverState = serverState,
                    activeServer = activeServer,
                    onStart = onStart,
                    onStop = onStop,
                    onRestart = onRestart
                )
            }

            // Section 3 — Live Stats
            item {
                LiveStatsGrid(
                    processStats = processStats,
                    serverState = serverState
                )
            }

            // Section 4 — Console Preview
            item {
                ConsolePreview(
                    consoleLines = consoleLines,
                    onOpenConsole = onOpenConsole,
                    onCommand = onCommand,
                    isOnline = serverState.status == ServerStatus.ONLINE
                )
            }

            // Section 5 — Player List
            item {
                PlayerListCard(
                    players = serverState.players,
                    isOnline = serverState.status == ServerStatus.ONLINE,
                    onCommand = onCommand,
                    onOpenPlayers = onOpenPlayers
                )
            }

            // Section 6 — Recent Activity
            item {
                RecentActivityCard(activityLog = activityLog)
            }

            // Section 7 — Storage
            item {
                StorageCard(storageStats = storageStats)
            }

            // Section 8 — Shortcuts
            item {
                ShortcutGrid(
                    onFiles = onOpenFiles
                )
            }
        }
    }
}

// ── Section 1 — Server Card ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerCard(
    activeServer: ServerConfig?,
    serverConfigs: List<ServerConfig>,
    serverState: ServerState,
    statusColor: Color,
    networkInfo: NetworkInfo,
    publicIp: String = "",
    tunnelUrl: String = "",
    onSelectServer: (String) -> Unit,
    onCreateServer: () -> Unit,
    onDeleteServer: (ServerConfig) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var serverToDelete by remember { mutableStateOf<ServerConfig?>(null) }

    // Delete confirmation dialog
    serverToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { serverToDelete = null },
            title = { Text("Delete Server") },
            text = { Text("Delete \"${target.name}\"? This will remove the server and all its files.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteServer(target)
                    serverToDelete = null
                    expanded = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { serverToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(statusColor)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activeServer?.name ?: "No Server",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (activeServer != null) {
                    Text(
                        text = "${activeServer.jarName} · ${activeServer.mcVersion.ifBlank { serverTypeLabel(activeServer.serverType) }} · ${serverState.status.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (serverState.uptimeSeconds > 0) {
                        Text(
                            text = "Started ${formatRelativeTime(serverState.uptimeSeconds)} ago",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Connection info inline
                    Spacer(Modifier.height(4.dp))
                    if (serverState.status == ServerStatus.ONLINE) {
                        if (publicIp.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(4.dp))
                                Text(text = "$publicIp:${activeServer.port}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(Modifier.height(2.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lan, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (networkInfo.localIp != "Unknown") "${networkInfo.localIp}:${activeServer.port}" else "Local IP unknown",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lan, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(4.dp))
                            Text(text = "Server not running", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                        }
                    }
                    // Tunnel URL (e.g. playit.gg)
                    if (tunnelUrl.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Cloud,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = tunnelUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    // Cellular warning
                    if (networkInfo.isCellular && serverState.status == ServerStatus.ONLINE) {
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFFFC107)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Mobile data — port forwarding may not work",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFC107)
                            )
                        }
                    }
                }
            }
            if (serverConfigs.isNotEmpty()) {
                Icon(
                    if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = "Switch server"
                )
            }
        }

        // Dropdown server list
        if (expanded) {
            HorizontalDivider()
            serverConfigs.forEach { config ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelectServer(config.id)
                            expanded = false
                        },
                    color = if (config.id == activeServer?.id)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(config.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { serverToDelete = config },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete server", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCreateServer() },
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Create new server", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// ── Section 2 — Quick Actions ──

@Composable
private fun QuickActions(
    serverState: ServerState,
    activeServer: ServerConfig?,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val canStart = (serverState.status == ServerStatus.OFFLINE || serverState.status == ServerStatus.CRASHED) && activeServer != null
            val canStop = serverState.status == ServerStatus.ONLINE
            val canRestart = serverState.status == ServerStatus.ONLINE

            ActionButton(
                icon = Icons.Default.PlayArrow,
                label = "Start",
                onClick = onStart,
                enabled = canStart,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                icon = Icons.Default.Stop,
                label = "Stop",
                onClick = onStop,
                enabled = canStop,
                color = Color(0xFFF44336),
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                icon = Icons.Default.Refresh,
                label = "Restart",
                onClick = onRestart,
                enabled = canRestart,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    ElevatedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = color.copy(alpha = 0.15f),
            contentColor = color,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Section 3 — Live Stats ──

@Composable
private fun LiveStatsGrid(
    processStats: ProcessStats,
    serverState: ServerState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Performance", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallStatCard("CPU", "${processStats.cpuPercent.roundToInt()}%", Modifier.weight(1f))
                SmallStatCard("RAM", "${processStats.ramFormatted} / ${processStats.maxRamFormatted}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallStatCard("TPS", String.format("%.1f", processStats.tps), Modifier.weight(1f))
                SmallStatCard("Players", "${serverState.players.size} / 20", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallStatCard("↓ Down", processStats.rxFormatted, Modifier.weight(1f))
                SmallStatCard("↑ Up", processStats.txFormatted, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SmallStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Section 4 — Connection ──

@Composable
private fun ConnectionCard(
    networkInfo: NetworkInfo,
    port: Int,
    isRunning: Boolean,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lan, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Connection", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))

            val address = if (isRunning && networkInfo.localIp != "Unknown")
                "${networkInfo.localIp}:$port"
            else
                "Server not running"

            Text(
                text = address,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (isRunning && networkInfo.localIp != "Unknown") {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { clipboardManager.setText(AnnotatedString(address)) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Copy")
                    }
                }
            }
        }
    }
}

// ── Section 5 — Console Preview ──

@Composable
private fun ConsolePreview(
    consoleLines: List<String>,
    onOpenConsole: () -> Unit,
    onCommand: (String) -> Unit,
    isOnline: Boolean
) {
    var commandInput by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Console", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onOpenConsole) {
                    Text("Open Console →")
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0D0D0D))
                    .padding(8.dp)
            ) {
                LazyColumn {
                    items(consoleLines.takeLast(5)) { line ->
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

            if (isOnline) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter command...", fontSize = 13.sp) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (commandInput.isNotBlank()) {
                                onCommand(commandInput)
                                commandInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

// ── Section 6 — Player List ──

@Composable
private fun PlayerListCard(
    players: List<String>,
    isOnline: Boolean,
    onCommand: (String) -> Unit,
    onOpenPlayers: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Online Players (${players.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onOpenPlayers) {
                    Text("Player Management →")
                }
            }

            if (players.isEmpty()) {
                Text(
                    "No players online",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Spacer(Modifier.height(8.dp))
                players.take(5).forEach { player ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MinecraftHeadIcon(player = player, size = 18.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(player, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (players.size > 5) {
                    TextButton(onClick = onOpenPlayers, modifier = Modifier.fillMaxWidth()) {
                        Text("Show all (${players.size})")
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Section 7 — Recent Activity ──

@Composable
private fun RecentActivityCard(activityLog: ActivityLog) {
    val entries = activityLog.entries.takeLast(10)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Recent Activity", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (entries.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("No recent activity", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Spacer(Modifier.height(8.dp))
                entries.forEach { entry ->
                    ActivityRow(entry)
                    if (entry != entries.last()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(entry: ActivityEntry) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = when (entry.type) {
            ActivityType.SUCCESS -> Icons.Default.CheckCircle
            ActivityType.ERROR -> Icons.Default.Error
            ActivityType.WARNING -> Icons.Default.Warning
            ActivityType.PLAYER_JOIN -> Icons.Default.PersonAdd
            ActivityType.PLAYER_LEAVE -> Icons.Default.PersonRemove
            ActivityType.INFO -> Icons.Default.Info
        }
        val tint = when (entry.type) {
            ActivityType.SUCCESS -> Color(0xFF4CAF50)
            ActivityType.ERROR -> Color(0xFFF44336)
            ActivityType.WARNING -> Color(0xFFFFC107)
            ActivityType.PLAYER_JOIN -> Color(0xFF4CAF50)
            ActivityType.PLAYER_LEAVE -> Color(0xFFFF9800)
            ActivityType.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = tint)
        Spacer(Modifier.width(8.dp))
        Text(
            text = timeFormat.format(Date(entry.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.width(8.dp))
        Text(entry.message, style = MaterialTheme.typography.bodySmall)
    }
}

// ── Section 8 — Storage ──

@Composable
private fun StorageCard(storageStats: StorageStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Storage", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StorageMiniCard("World", storageStats.worldFormatted, Icons.Default.Public, Modifier.weight(1f))
                StorageMiniCard("Logs", storageStats.logsFormatted, Icons.Default.Article, Modifier.weight(1f))
                StorageMiniCard("Backups", storageStats.backupsFormatted, Icons.Default.Backup, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(storageStats.availableFormatted, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(4.dp))
                Text("/ ${storageStats.totalFormatted}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StorageMiniCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Section 9 — Shortcuts ──

@Composable
private fun ShortcutGrid(
    onFiles: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Quick Access", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ShortcutCard(Icons.Default.Folder, "File Manager", onFiles, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ShortcutCard(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

// ── Helpers ──

private fun serverTypeLabel(type: String): String = when (type) {
    "paper" -> "Paper"
    "vanilla" -> "Vanilla"
    "fabric" -> "Fabric"
    else -> ""
}

private fun formatRelativeTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m"
        else -> "${seconds}s"
    }
}

private fun Float.roundToInt(): Int = (this + 0.5f).toInt()
