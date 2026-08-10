package com.portalhost.desktop.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.layout.BoxWithConstraints
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
import com.portalhost.server.ActivityType
import com.portalhost.server.ServerManager
import com.portalhost.server.TunnelManager
import com.portalhost.server.TunnelInfo
import com.portalhost.server.TunnelStatus
import com.portalhost.network.NetworkInfo
import com.portalhost.network.NetworkManager
import com.portalhost.server.ProcessMonitor
import com.portalhost.server.ProcessStats
import com.portalhost.server.classifyLogLevel
import com.portalhost.server.consoleLineColor
import com.portalhost.server.getServerIconFile
import com.portalhost.server.loadServerIcon
import com.portalhost.server.LogLevel
import com.portalhost.server.ALL_LOG_LEVELS
import com.portalhost.theme.ThemeColors
import com.portalhost.desktop.util.UpdateChecker
import com.portalhost.desktop.util.UpdateInfo
import com.portalhost.desktop.util.rememberResourcePainter
import com.portalhost.preferences.Preferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File
import androidx.compose.ui.platform.LocalClipboardManager

private val TunnelGreen = Color(0xFF4ADE80)

@Composable
fun DashboardScreen(
    onNavigateToConsole: (String) -> Unit = {},
    onNavigateToServer: (String) -> Unit = {},
    onNavigateToCreate: () -> Unit = {},
    onNavigateToPlayers: (String) -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
) {
    val serverManager = koinInject<ServerManager>()
    val fileSystem = koinInject<com.portalhost.filesystem.FileSystem>()
    val jdkManager = koinInject<JdkManager>()
    val activityLog = koinInject<ActivityLog>()
    val tunnelManager = koinInject<TunnelManager>()
    val preferences = koinInject<Preferences>()
    val servers by serverManager.servers.collectAsState()
    val serverStates by serverManager.serverStates.collectAsState()
    val consoleOutputs by serverManager.consoleOutputs.collectAsState()
    val tunnelState by tunnelManager.state.collectAsState()
    val activities by activityLog.activities.collectAsState()
    val jdkInstallations by jdkManager.knownInstallations.collectAsState()
    val jdkProgress by jdkManager.progress.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedServerId by remember { mutableStateOf<String?>(null) }
    val previousServerCount by remember { mutableStateOf(servers.size) }
    var expanded by remember { mutableStateOf(false) }
    var commandInput by remember { mutableStateOf("") }
    val processMonitor = remember { ProcessMonitor() }
    var processStats by remember { mutableStateOf(ProcessStats()) }
    val networkManager = remember { NetworkManager() }
    val localIpInfo = remember { mutableStateOf(networkManager.getLocalIpAddress()) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateBanner by remember { mutableStateOf(true) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (preferences.autoCheckUpdates.value) {
            when (val result = UpdateChecker.checkForUpdate(preferences.githubToken.value)) {
                is com.portalhost.desktop.util.UpdateResult.UpdateAvailable -> {
                    updateInfo = result.info
                }
                else -> {}
            }
        }
    }

    LaunchedEffect(selectedServerId) {
        processMonitor.resetNetworkStats()
    }

    LaunchedEffect(selectedServerId) {
        while (isActive) {
            val pid = selectedServerId?.let { serverManager.getProcessForServer(it) }
            val stats = processMonitor.getStats(pid)
            val parsedTps = selectedServerId?.let { sid ->
                serverManager.processStats.value[sid]?.tps
            }
            processStats = if (parsedTps != null && parsedTps > 0f) stats.copy(tps = parsedTps) else stats
            delay(2000)
        }
    }

    LaunchedEffect(servers, serverStates) {
        // Auto-select the first RUNNING server, or fall back to the first server
        val runningServer = servers.keys.firstOrNull { id ->
            serverStates[id]?.status == ServerStatus.RUNNING
        }
        val targetId = runningServer ?: servers.keys.firstOrNull()
        if (targetId != null && selectedServerId != targetId) {
            selectedServerId = targetId
        }
    }

    val activeServer = selectedServerId?.let { servers[it] }
    val activeState = selectedServerId?.let { serverStates[it] }
    val activeConsole = selectedServerId?.let { consoleOutputs[it] } ?: emptyList()
    val serverIcon = remember(activeServer?.id) {
        activeServer?.id?.let { id ->
            val iconFile = getServerIconFile(serverManager.getServerDir(id))
            loadServerIcon(iconFile)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (updateInfo != null && showUpdateBanner) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Update available: v${updateInfo!!.latestVersion}", style = MaterialTheme.typography.titleSmall)
                        Text(updateInfo!!.releaseNotes, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = { showUpdateDialog = true }) { Text("Download") }
                    IconButton(onClick = { showUpdateBanner = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        if (servers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(400.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("No servers yet", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("Go to the Servers tab to create one", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
                val statusColor = ThemeColors.serverStatusColor(activeState?.status ?: ServerStatus.STOPPED)

                ServerCard(
                    serverIcon = serverIcon,
                    activeServer = activeServer,
                    serverConfigs = servers.values.toList(),
                    activeState = activeState,
                    statusColor = statusColor,
                    localIp = localIpInfo.value.localIp,
                    tunnels = tunnelState.tunnels,
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

                if (jdkProgress.phase == JdkManager.InstallPhase.DOWNLOADING ||
                    jdkProgress.phase == JdkManager.InstallPhase.EXTRACTING ||
                    jdkProgress.phase == JdkManager.InstallPhase.VERIFYING ||
                    jdkProgress.phase == JdkManager.InstallPhase.VALIDATING ||
                    jdkProgress.phase == JdkManager.InstallPhase.CONNECTING) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Installing Java runtime...", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        formatPhase(jdkProgress.phase),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(progress = { jdkProgress.percentage.toFloat() / 100f }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    buildProgressText(jdkProgress),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    formatPhase(jdkProgress.phase),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (jdkProgress.phase == JdkManager.InstallPhase.EXTRACTING && jdkProgress.totalEntries > 0) {
                                Text(
                                    "Extracting: ${jdkProgress.extractedEntries} / ${jdkProgress.totalEntries} files",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else if (jdkProgress.phase == JdkManager.InstallPhase.ERROR) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(12.dp))
                            Text("Java installation failed: ${jdkProgress.errorMessage ?: "Unknown error"}", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else if (jdkProgress.phase == JdkManager.InstallPhase.COMPLETE) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(12.dp))
                            Text("Java installation complete!", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodySmall)
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
                    localIp = localIpInfo.value.localIp,
                    onStart = { scope.launch { tunnelManager.start(activeServer?.port ?: 25565) } },
                    onStop = { scope.launch { tunnelManager.stop() } },
                    onClaim = { scope.launch {
                        if (tunnelState.status == TunnelStatus.CLAIM_REQUIRED && tunnelState.claimUrl == null) {
                            tunnelManager.forceStop()
                        } else {
                            tunnelManager.startClaimFlow()
                        }
                    } },
                    onOpenClaimUrl = {
                        tunnelState.claimUrl?.let { url ->
                            try { java.awt.Desktop.getDesktop().browse(java.net.URI(url)) } catch (_: Exception) {}
                        }
                    },
                    onReset = { scope.launch { tunnelManager.resetKey() } }
                )

                PerformanceCard(
                    activeState = activeState,
                    processStats = processStats,
                    onOpenPerformance = { selectedServerId?.let { onNavigateToServer(it) } },
                    onOpenPlayers = { selectedServerId?.let { onNavigateToPlayers(it) } }
                )

                StorageCard(
                    serverId = selectedServerId,
                    fileSystem = fileSystem,
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

BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val useTwoColumnLayout = maxWidth >= 900.dp

                        if (useTwoColumnLayout) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                PlayerListCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 300.dp),
                                    players = activeState?.players ?: emptyList(),
                                    onlineCount = activeState?.playersOnline ?: 0,
                                    maxPlayers = activeState?.maxPlayers ?: 20,
                                    onOpenPlayers = { selectedServerId?.let { onNavigateToPlayers(it) } }
                                )

                                ActivityCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 300.dp),
                                    activities = activities,
                                    onViewAll = onNavigateToActivity
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                PlayerListCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 250.dp),
                                    players = activeState?.players ?: emptyList(),
                                    onlineCount = activeState?.playersOnline ?: 0,
                                    maxPlayers = activeState?.maxPlayers ?: 20,
                                    onOpenPlayers = { selectedServerId?.let { onNavigateToPlayers(it) } }
                                )

                                ActivityCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 250.dp),
                                    activities = activities,
                                    onViewAll = onNavigateToActivity
                                )
                            }
                        }
                    }
}
    }

    if (showUpdateDialog && updateInfo != null) {
        UpdateDialog(
            updateInfo = updateInfo!!,
            onDismiss = { showUpdateDialog = false },
            onNoLongerNeeded = { updateInfo = null; showUpdateBanner = false }
        )
    }
}

@Composable
private fun ServerCard(
    serverIcon: ImageBitmap?,
    activeServer: ServerConfig?,
    serverConfigs: List<ServerConfig>,
    activeState: ServerState?,
    statusColor: Color,
    localIp: String = "Unknown",
    tunnels: List<TunnelInfo> = emptyList(),
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectServer: (String) -> Unit,
    onCreateServer: () -> Unit,
    onDeleteServer: (ServerConfig) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val mcIcon = rememberResourcePainter("/icons/grass_block.png")
    val javaIcon = rememberResourcePainter("/icons/java_icon.png")
    val bedrockIcon = rememberResourcePainter("/icons/bedrock.png")
    val status = activeState?.status ?: ServerStatus.STOPPED
    val statusLabel = if (status == ServerStatus.RUNNING) "Online" else status.name.lowercase().replaceFirstChar { it.uppercase() }
    val statusColorForBadge = ThemeColors.serverStatusColor(status)

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
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(painter = mcIcon, contentDescription = "Minecraft version", modifier = Modifier.size(14.dp), tint = Color.Unspecified)
                                Text(text = "MC ${activeServer.version.ifBlank { "?" }}", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(painter = javaIcon, contentDescription = "Java version", modifier = Modifier.size(14.dp), tint = Color.Unspecified)
                                Text(text = "Java ${activeServer.javaVersion}", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Settings, contentDescription = "RAM", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(text = "${activeServer.memoryMax}M", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.width(16.dp))
                if (status == ServerStatus.RUNNING && activeServer != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$localIp:${activeServer.port}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            IconButton(onClick = { scope.launch { clipboardManager.setText(AnnotatedString("$localIp:${activeServer.port}")) } }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy local IP", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        val domains = tunnels.groupBy { it.publicAddress.substringBefore(":") }
                        domains.forEach { (domain, tunnelList) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val isBedrock = tunnelList.all { it.type == "udp" }
                                Icon(
                                    painter = if (isBedrock) bedrockIcon else mcIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isBedrock) Color.Unspecified else TunnelGreen
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = domain,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = TunnelGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isBedrock) {
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "UDP",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                IconButton(onClick = { scope.launch { clipboardManager.setText(AnnotatedString(tunnelList.first().publicAddress)) } }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy domain", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
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
                    color = ThemeColors.StatusSuccess
                )
                ControlButton(
                    icon = Icons.Default.Stop,
                    label = "Stop",
                    onClick = onStop,
                    enabled = canStop,
                    color = ThemeColors.StatusError
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

@Suppress("DEPRECATION")
@Composable
private fun TunnelCard(
    tunnelState: com.portalhost.server.TunnelState,
    localIp: String = "0.0.0.0",
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClaim: () -> Unit = {},
    onOpenClaimUrl: () -> Unit = {},
    onReset: () -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val tunnelMcIcon = rememberResourcePainter("/icons/grass_block.png")
    val tunnelBedrockIcon = rememberResourcePainter("/icons/bedrock.png")
    val status = tunnelState.status
    val connected = status == TunnelStatus.CONNECTED
    val claimRequired = status == TunnelStatus.CLAIM_REQUIRED
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
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tunnel", style = MaterialTheme.typography.titleMedium)
                    Text(statusText, style = MaterialTheme.typography.bodySmall, color = statusColor)
                }
                when (status) {
                    TunnelStatus.IDLE -> {
                        Button(
                            onClick = onStart,
                            enabled = enabled,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) { Text("Connect") }
                    }
                    TunnelStatus.CONNECTED -> {
                        Button(
                            onClick = onStop,
                            enabled = enabled,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) { Text("Disconnect") }
                    }
                    TunnelStatus.CLAIM_REQUIRED -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onOpenClaimUrl,
                                enabled = enabled,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) { Text("Open Claim URL") }
                            OutlinedButton(
                                onClick = onStop,
                                enabled = enabled,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) { Text("Stop") }
                        }
                    }
                    TunnelStatus.ERROR -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onClaim,
                                enabled = enabled,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) { Text("Retry") }
                            OutlinedButton(
                                onClick = onStop,
                                enabled = enabled,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) { Text("Stop") }
                        }
                    }
                    TunnelStatus.CONNECTING, TunnelStatus.DOWNLOADING -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            OutlinedButton(
                                onClick = onStop,
                                enabled = enabled,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) { Text("Stop") }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (claimRequired && tunnelState.claimUrl != null) {
                    val claimUrl = tunnelState.claimUrl!!
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Claim URL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = claimUrl,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { scope.launch { clipboardManager.setText(AnnotatedString(claimUrl)) } }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy claim URL", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = onOpenClaimUrl) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open claim URL in browser", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onClaim) { Text("Re-claim") }
                        TextButton(onClick = onReset) { Text("Reset") }
                    }
                } else if (claimRequired) {
                    // Waiting for daemon to output claim URL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                        Text("Waiting for claim URL...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onClaim) { Text("Cancel") }
                        TextButton(onClick = onReset) { Text("Reset") }
                    }
                } else if (connected && tunnelState.tunnels.isNotEmpty()) {
                    val domains = tunnelState.tunnels.groupBy { it.publicAddress.substringBefore(":") }
                    domains.forEach { (domain, tunnels) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(painter = if (tunnels.any { it.type == "udp" }) tunnelBedrockIcon else tunnelMcIcon, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (tunnels.any { it.type == "udp" }) Color.Unspecified else TunnelGreen)
                                    Text(
                                        text = domain,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = FontFamily.Monospace,
                                        color = TunnelGreen,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (tunnels.any { it.type == "udp" }) {
                                        Text(
                                            text = "UDP",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Text(
                                    text = tunnels.joinToString(", ") {
                                        val port = it.publicAddress.substringAfter(":")
                                        "${if (it.type == "udp") "UDP " else ""}:$port"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                tunnels.forEach { tunnel ->
                                    IconButton(onClick = { scope.launch { clipboardManager.setText(AnnotatedString(tunnel.publicAddress)) } }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy address", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                } else if (status == TunnelStatus.ERROR) {
                    if (tunnelState.error != null) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = tunnelState.error!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (tunnelState.lastOutput.isNotBlank()) {
                        Text(
                            text = tunnelState.lastOutput,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onClaim) { Text("Retry") }
                        TextButton(onClick = onReset) { Text("Reset") }
                    }
} else if (status == TunnelStatus.CONNECTING || status == TunnelStatus.DOWNLOADING) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            modifier = Modifier.weight(1f),
                            progress = { if (status == TunnelStatus.DOWNLOADING) 0.5f else 0f },
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(statusText, style = MaterialTheme.typography.bodySmall, color = statusColor)
                    }
                } else {
                    Text("Not connected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun PerformanceCard(
    activeState: ServerState?,
    processStats: ProcessStats = ProcessStats(),
    onOpenPerformance: () -> Unit,
    onOpenPlayers: () -> Unit
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
                StatMiniCard(value = if (live) "${processStats.cpuPercent.toInt()}%" else "—", label = "CPU")
                StatMiniCard(
                    value = if (live) processStats.ramFormatted else "—",
                    label = "RAM"
                )
                StatMiniCard(
                    value = if (live) "%.1f".format(processStats.tps) else "—",
                    label = "TPS"
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (live) "${activeState?.playersOnline ?: 0}" else "—",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(text = "Players", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                StatMiniCard(value = if (live) processStats.rxFormatted else "—", label = "Download")
                StatMiniCard(value = if (live) processStats.txFormatted else "—", label = "Upload")
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
    var commandInput by remember { mutableStateOf(commandInput) }
    var commandHistory by remember { mutableStateOf(listOf<String>()) }
    var historyIndex by remember { mutableIntStateOf(-1) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(listOf<Int>()) }
    var currentSearchIdx by remember { mutableIntStateOf(0) }
    var activeLevel by remember { mutableStateOf(LogLevel.ALL) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var isUserScrolling by remember { mutableStateOf(false) }
    var isNearBottom by remember { mutableStateOf(true) }

    val sendCommand: () -> Unit = {
        if (commandInput.isNotBlank()) {
            onCommand(commandInput)
            commandInput = ""
            onCommandInputChange("")
        }
    }

    // Apply log level filter
    val levelFiltered = if (activeLevel == LogLevel.ALL) consoleLines
        else consoleLines.filter { classifyLogLevel(it) == activeLevel }

    // Apply search on top of level filter
    val displayLines = if (searchQuery.isNotBlank()) {
        levelFiltered.filterIndexed { idx, _ ->
            val originalIdx = if (activeLevel == LogLevel.ALL) idx
                else consoleLines.indexOf(levelFiltered[idx])
            originalIdx in searchResults
        }
    } else levelFiltered

    // Search logic (indexes into original consoleLines)
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            searchResults = consoleLines.mapIndexedNotNull { idx, line ->
                if (line.contains(searchQuery, ignoreCase = true)) idx else null
            }
            currentSearchIdx = 0
        } else {
            searchResults = emptyList()
        }
    }

    // Auto-scroll: always scroll to latest line unless user scrolls up manually
    val isScrolling = remember { mutableStateOf(false) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.distinctUntilChanged().collect { scrolling ->
            if (!scrolling && isScrolling.value) {
                isScrolling.value = false
            }
        }
    }
    LaunchedEffect(displayLines.size) {
        if (!isScrolling.value && displayLines.isNotEmpty()) {
            try { listState.scrollToItem(displayLines.size - 1) } catch (_: Exception) {}
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header with search, filter, clear, open console
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Console", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                
                // Search toggle
                IconButton(onClick = { showSearch = !showSearch }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Search, contentDescription = if (showSearch) "Hide search" else "Search", modifier = Modifier.size(20.dp))
                }
                // Filter toggle
                IconButton(onClick = { activeLevel = if (activeLevel == LogLevel.ALL) LogLevel.ERROR else LogLevel.ALL }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.FilterAlt, contentDescription = if (activeLevel == LogLevel.ALL) "Filter: ERROR only" else "Filter: ALL", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onClearConsole, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                }
                TextButton(onClick = onOpenConsole) {
                    Text("Open Console", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Search bar
            if (showSearch) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search console...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )
                if (searchResults.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Match ${currentSearchIdx + 1}: ${consoleLines.getOrNull(searchResults.getOrNull(currentSearchIdx) ?: -1)?.take(80) ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                        )
                        Row {
                            IconButton(onClick = {
                                currentSearchIdx = (currentSearchIdx - 1).coerceAtLeast(0)
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous", modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = {
                                currentSearchIdx = (currentSearchIdx + 1).coerceAtMost(searchResults.size - 1)
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Log level filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LogLevel.entries.forEach { level ->
                    FilterChip(
                        selected = activeLevel == level,
                        onClick = { activeLevel = level },
                        label = { Text(level.name, fontSize = 11.sp) },
                        leadingIcon = if (activeLevel == level) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                    )
                }
            }

            // Console output
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0D0D0D))
                    .padding(8.dp),
            ) {
                if (displayLines.isEmpty()) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No matching lines found"
                        else "Console output will appear here when the server is running...",
                        color = Color(0xFF555555),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        items(count = displayLines.size, key = { index -> "console_$index" }) { index ->
                            val line = displayLines[index]
                            Text(
                                line,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = consoleLineColor(line),
                            )
                        }
                    }
                }
            }

            // Scroll to bottom FAB
            if (isScrolling.value && displayLines.size > 20) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.material3.SmallFloatingActionButton(
                        onClick = {
                            isScrolling.value = false
                            scope.launch {
                                try { listState.animateScrollToItem(displayLines.size - 1) } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll to bottom")
                    }
                }
            }

            // Command input
            if (isOnline) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = { newInput ->
                            commandInput = newInput
                            historyIndex = -1
                            onCommandInputChange(newInput)
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter command...", fontSize = 13.sp) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { sendCommand() })
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = {
                        if (commandInput.isNotBlank()) {
                            onCommand(commandInput)
                            commandInput = ""
                            onCommandInputChange("")
                        }
                    }) {
                        Icon(painter = rememberResourcePainter("/icons/arrow_right_curved_highlighted.png"), contentDescription = "Send", tint = Color.Unspecified)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityCard(
    modifier: Modifier = Modifier,
    activities: List<ActivityEntry>,
    onViewAll: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                TextButton(onClick = onViewAll) {
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
internal fun activityIconAndColor(type: ActivityType, action: String, defaultTint: Color): Pair<ImageVector, Color> {
    return when (type) {
        ActivityType.SERVER_START, ActivityType.SERVER_ONLINE, ActivityType.PLAYER_JOIN ->
            Icons.Default.CheckCircle to ThemeColors.ActivityLogColors.Start
        ActivityType.SERVER_STOP, ActivityType.SERVER_OFFLINE ->
            Icons.Default.Stop to ThemeColors.ActivityLogColors.Stop
        ActivityType.SERVER_CRASH ->
            Icons.Default.Error to ThemeColors.ActivityLogColors.Error
        ActivityType.PLAYER_LEAVE ->
            Icons.Default.PersonRemove to ThemeColors.ActivityLogColors.Leave
        ActivityType.PLAYER_KICK ->
            Icons.Default.PersonRemove to ThemeColors.ActivityLogColors.Warning
        ActivityType.PLAYER_BAN ->
            Icons.Default.Delete to ThemeColors.ActivityLogColors.Error
        ActivityType.PLAYER_OP, ActivityType.PLAYER_DEOP ->
            Icons.Default.Person to ThemeColors.ActivityLogColors.Player
        ActivityType.PLAYER_KILL ->
            Icons.Default.Warning to ThemeColors.ActivityLogColors.Kill
        ActivityType.COMMAND_EXECUTED ->
            Icons.AutoMirrored.Filled.Send to ThemeColors.ActivityLogColors.Command
        ActivityType.INFO -> {
            val lower = action.lowercase()
            when {
                lower.contains("start") || lower.contains("launch") || lower.contains("join") ->
                    Icons.Default.CheckCircle to ThemeColors.ActivityLogColors.Start
                lower.contains("error") || lower.contains("crash") || lower.contains("fail") ->
                    Icons.Default.Error to ThemeColors.ActivityLogColors.Error
                lower.contains("leave") || lower.contains("quit") ->
                    Icons.Default.PersonRemove to ThemeColors.ActivityLogColors.Leave
                else -> Icons.Default.Info to defaultTint
            }
        }
    }
}

@Composable
internal fun ActivityRow(entry: ActivityEntry) {
    val (icon, color) = activityIconAndColor(entry.type, entry.action, MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
private fun PlayerListCard(
    modifier: Modifier = Modifier,
    players: List<String> = emptyList(),
    onlineCount: Int = players.size,
    maxPlayers: Int = 20,
    onOpenPlayers: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Online Players ($onlineCount/$maxPlayers)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onOpenPlayers) {
                    Text("Player Management →")
                }
            }

            if (onlineCount == 0 && players.isEmpty()) {
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text("No online players", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Spacer(Modifier.height(8.dp))
                players.forEach { player ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MinecraftHeadIcon(player = player, size = 18.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(player, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private fun formatRam(bytes: Long): String = when {
    bytes < 1024 -> "${bytes}B"
    bytes < 1024 * 1024 -> "${bytes / 1024}KB"
    bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
    else -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024 * 1024))}GB"
}

@Composable
private fun StorageCard(
    serverId: String?,
    fileSystem: com.portalhost.filesystem.FileSystem,
) {
    val serverManager = koinInject<ServerManager>()
    var storage by remember(serverId) { mutableStateOf<com.portalhost.filesystem.FileSystem.StorageBreakdown?>(null) }
    LaunchedEffect(serverId) {
        if (serverId != null) {
            storage = fileSystem.getServerStorageStats(serverManager.getServerDir(serverId))
        } else {
            storage = null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            if (storage == null || serverId == null) {
                Text("Select a server to view storage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val s = storage!!
                val total = s.totalBytes.coerceAtLeast(1)
                val segments = listOf(
                    "World" to s.worldBytes,
                    "Plugins" to s.pluginsBytes,
                    "Mods" to s.modsBytes,
                    "Datapacks" to s.datapacksBytes,
                    "Resourcepacks" to s.resourcepacksBytes,
                    "Other" to s.otherBytes,
                ).filter { it.second > 0 }

                if (segments.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        segments.forEach { (label, bytes) ->
                            val fraction = bytes.toFloat() / total
                            val color = when (label) {
                                "World" -> ThemeColors.StorageColors.World
                                "Plugins" -> ThemeColors.StorageColors.Plugins
                                "Mods" -> ThemeColors.StorageColors.Mods
                                "Datapacks" -> ThemeColors.StorageColors.Datapacks
                                "Resourcepacks" -> ThemeColors.StorageColors.Resourcepacks
                                else -> ThemeColors.StorageColors.Other
                            }
                            Box(
                                modifier = Modifier
                                    .weight(fraction.coerceAtLeast(0.01f))
                                    .fillMaxHeight()
                                    .background(color, RoundedCornerShape(4.dp))
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    segments.forEach { (label, bytes) ->
                        val color = when (label) {
                            "World" -> ThemeColors.StorageColors.World
                            "Plugins" -> ThemeColors.StorageColors.Plugins
                            "Mods" -> ThemeColors.StorageColors.Mods
                            "Datapacks" -> ThemeColors.StorageColors.Datapacks
                            "Resourcepacks" -> ThemeColors.StorageColors.Resourcepacks
                            else -> ThemeColors.StorageColors.Other
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(formatRam(bytes), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        }
                    }
                } else {
                    Text("Empty", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

