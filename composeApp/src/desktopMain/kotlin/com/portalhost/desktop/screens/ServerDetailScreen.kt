package com.portalhost.desktop.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.db.DatabaseRepository
import com.portalhost.model.ServerConfig
import com.portalhost.model.ServerStatus
import com.portalhost.server.BackupEntry
import com.portalhost.server.BackupManager
import com.portalhost.server.ServerManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File
import java.util.zip.ZipFile

@Composable
fun ServerDetailScreen(
    serverId: String,
    onNavigateToConsole: (String) -> Unit = {},
    onNavigateToFiles: (String) -> Unit = {},
    onNavigateToPlayers: (String) -> Unit = {},
    onNavigateToPerformance: (String) -> Unit = {},
    onNavigateToLogs: (String) -> Unit = {},
    onNavigateToRcon: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val serverManager = koinInject<ServerManager>()
    val servers by serverManager.servers.collectAsState()
    val serverStates by serverManager.serverStates.collectAsState()
    val config = servers[serverId]
    val state = serverStates[serverId]
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }

    val database = koinInject<DatabaseRepository>()
    val backupManager = remember(config) {
        config?.let { BackupManager(File("servers/$serverId"), serverId, database) }
    }
    val backupEntries by backupManager?.backups?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var importFileName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(serverId) {
        backupManager?.refreshBackups()
    }

    if (config == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Server not found", style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Server") },
            text = { Text("Are you sure you want to delete \"${config.name}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { serverManager.deleteServer(serverId) }
                    showDeleteDialog = false
                    onBack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(config.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${config.serverType.name} v${config.version}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StatusBadgeDetail(state?.status ?: ServerStatus.STOPPED)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val isIdle = state?.status == ServerStatus.STOPPED || state?.status == ServerStatus.CRASHED
                    val isRunning = state?.status == ServerStatus.RUNNING
                    Button(onClick = { scope.launch { serverManager.startServer(serverId) } }, enabled = isIdle) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Start")
                    }
                    OutlinedButton(onClick = { scope.launch { serverManager.stopServer(serverId) } }, enabled = isRunning) {
                        Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Stop")
                    }
                    FilledTonalButton(onClick = { scope.launch { serverManager.restartServer(serverId) } }, enabled = isRunning) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Restart")
                    }
                    Spacer(Modifier.weight(1f))
                    ActionIconButton(Icons.Filled.Terminal, "Console") { onNavigateToConsole(serverId) }
                    ActionIconButton(Icons.Filled.Folder, "Files") { onNavigateToFiles(serverId) }
                    ActionIconButton(Icons.Filled.People, "Players") { onNavigateToPlayers(serverId) }
                    ActionIconButton(Icons.Filled.Analytics, "Performance") { onNavigateToPerformance(serverId) }
                    ActionIconButton(Icons.AutoMirrored.Filled.Article, "Logs") { onNavigateToLogs(serverId) }
                    ActionIconButton(Icons.Filled.Public, "RCON") { onNavigateToRcon(serverId) }
                    OutlinedButton(onClick = { showDeleteDialog = true }, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }
        }

        val tabs = listOf("Properties", "Worlds", "Plugins", "Mods", "Datapacks", "Backups")
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, maxLines = 1, fontSize = 13.sp) },
                )
            }
        }

        when (selectedTab) {
            0 -> PropertiesTab(config = config, state = state, serverManager = serverManager, serverId = serverId)
            1 -> WorldsTab(serverId = serverId)
            2 -> PluginsTab(serverId = serverId)
            3 -> ModsTab(serverId = serverId)
            4 -> DatapacksTab(serverId = serverId)
            5 -> BackupsTab(serverId = serverId, backupManager = backupManager)
        }
    }
}

@Composable
private fun ActionIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusBadgeDetail(status: ServerStatus) {
    val (color, label) = when (status) {
        ServerStatus.RUNNING -> Color(0xFF4CAF50) to "Running"
        ServerStatus.STARTING -> Color(0xFFFFC107) to "Starting"
        ServerStatus.STOPPING -> Color(0xFFFF9800) to "Stopping"
        ServerStatus.CRASHED -> Color(0xFFF44336) to "Crashed"
        ServerStatus.RESTARTING -> Color(0xFFFFC107) to "Restarting"
        ServerStatus.STOPPED -> Color(0xFF9E9E9E) to "Stopped"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PropertiesTab(config: ServerConfig, state: com.portalhost.model.ServerState?, serverManager: ServerManager, serverId: String) {
    val scope = rememberCoroutineScope()
    val database = koinInject<DatabaseRepository>()
    var name by remember(config) { mutableStateOf(config.name) }
    var port by remember(config) { mutableStateOf(config.port.toString()) }
    var memoryMin by remember(config) { mutableStateOf(config.memoryMin.toString()) }
    var memoryMax by remember(config) { mutableStateOf(config.memoryMax.toString()) }
    var gamemode by remember(config) { mutableStateOf(config.properties["gamemode"] ?: "survival") }
    var difficulty by remember(config) { mutableStateOf(config.properties["difficulty"] ?: "easy") }
    var motd by remember(config) { mutableStateOf(config.properties["motd"] ?: "A Minecraft Server") }
    var pvp by remember(config) { mutableStateOf(config.properties["pvp"] ?: "true") }
    var onlineMode by remember(config) { mutableStateOf(config.properties["online-mode"] ?: "true") }
    var whitelist by remember(config) { mutableStateOf(config.properties["white-list"] ?: "false") }
    var spawnProtection by remember(config) { mutableStateOf(config.properties["spawn-protection"] ?: "16") }
    var rconEnabled by remember(config) { mutableStateOf(config.rconEnabled) }
    var rconPort by remember(config) { mutableStateOf(config.rconPort.toString()) }
    var autoRestart by remember(config) { mutableStateOf(config.autoRestart) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Server Properties", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Server Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = port, onValueChange = { port = it.filter { c -> c.isDigit() } }, label = { Text("Port") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = memoryMin, onValueChange = { memoryMin = it.filter { c -> c.isDigit() } }, label = { Text("Min Memory (MB)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = memoryMax, onValueChange = { memoryMax = it.filter { c -> c.isDigit() } }, label = { Text("Max Memory (MB)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = motd, onValueChange = { motd = it }, label = { Text("MOTD") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = gamemode, onValueChange = { gamemode = it }, label = { Text("Gamemode") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = difficulty, onValueChange = { difficulty = it }, label = { Text("Difficulty") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = pvp, onValueChange = { pvp = it }, label = { Text("PvP") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = onlineMode, onValueChange = { onlineMode = it }, label = { Text("Online Mode") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = whitelist, onValueChange = { whitelist = it }, label = { Text("Whitelist") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = spawnProtection, onValueChange = { spawnProtection = it.filter { c -> c.isDigit() } }, label = { Text("Spawn Protection") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("RCON Enabled")
                    Button(onClick = { rconEnabled = !rconEnabled }) {
                        Text(if (rconEnabled) "Disable" else "Enable")
                    }
                }
                if (rconEnabled) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = rconPort, onValueChange = { rconPort = it.filter { c -> c.isDigit() } }, label = { Text("RCON Port") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(8.dp))
                Spacer(Modifier.height(12.dp))

                if (savedMessage != null) {
                    Text(savedMessage!!, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        scope.launch {
                            val updatedConfig = config.copy(
                                name = name.trim(),
                                port = port.toIntOrNull() ?: config.port,
                                memoryMin = memoryMin.toIntOrNull() ?: config.memoryMin,
                                memoryMax = memoryMax.toIntOrNull() ?: config.memoryMax,
                                properties = config.properties + mapOf(
                                    "gamemode" to gamemode,
                                    "difficulty" to difficulty,
                                    "motd" to motd,
                                    "pvp" to pvp,
                                    "online-mode" to onlineMode,
                                    "white-list" to whitelist,
                                    "spawn-protection" to spawnProtection,
                                ),
                                rconEnabled = rconEnabled,
                                rconPort = rconPort.toIntOrNull() ?: config.rconPort,
                                autoRestart = autoRestart,
                            )
                            database.insertServer(updatedConfig)
                            database.updateServerState(serverId, com.portalhost.model.ServerState(
                                id = serverId,
                                status = state?.status ?: ServerStatus.STOPPED,
                            ))
                            name = updatedConfig.name
                            port = updatedConfig.port.toString()
                            memoryMin = updatedConfig.memoryMin.toString()
                            memoryMax = updatedConfig.memoryMax.toString()
                            savedMessage = "Properties saved to database"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save Properties")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Danger Zone", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(Modifier.height(8.dp))
                Text("These actions are irreversible.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }

        if (state != null && state.lastError != null) {
            Spacer(Modifier.height(8.dp))
            Text("Last error: ${state.lastError}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun WorldsTab(serverId: String) {
    val serverManager = koinInject<ServerManager>()
    val servers by serverManager.servers.collectAsState()
    val config = servers[serverId]
    var worlds by remember(serverId) { mutableStateOf<List<File>>(emptyList()) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importFilePath by remember { mutableStateOf("") }

    LaunchedEffect(serverId) {
        val dir = File("servers/$serverId")
        worlds = dir.listFiles()?.filter {
            it.isDirectory && (it.name == "world" || it.name.startsWith("world_"))
        } ?: emptyList()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Worlds", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Button(onClick = { showImportDialog = true }) {
                Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Import ZIP")
            }
        }
        Spacer(Modifier.height(12.dp))
        if (worlds.isEmpty()) {
            Text("No worlds found", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        } else {
            LazyColumn {
                items(worlds) { world ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Games, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(world.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(formatSize(world), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import World from ZIP") },
            text = {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Enter the path to a ZIP file containing a world folder:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importFilePath,
                        onValueChange = { importFilePath = it },
                        label = { Text("ZIP file path") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (importFilePath.isNotBlank()) {
                        importWorldZip(serverId, importFilePath)
                        showImportDialog = false
                        importFilePath = ""
                    }
                }) { Text("Import") }
            },
            dismissButton = { TextButton(onClick = { showImportDialog = false }) { Text("Cancel") } }
        )
    }
}

private fun importWorldZip(serverId: String, zipPath: String) {
    val zipFile = File(zipPath)
    val targetDir = File("servers/$serverId")
    targetDir.mkdirs()
    try {
        ZipFile(zipFile).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val outFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        outFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        // Error handling - could show a toast
    }
}

@Composable
private fun PluginsTab(serverId: String) {
    var plugins by remember(serverId) { mutableStateOf<List<File>>(emptyList()) }

    LaunchedEffect(serverId) {
        val dir = File("servers/$serverId/plugins")
        plugins = if (dir.exists()) dir.listFiles()?.filter { it.name.endsWith(".jar") }?.sortedBy { it.name } ?: emptyList() else emptyList()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Plugins", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        if (plugins.isEmpty()) {
            Text("No plugins installed", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        } else {
            LazyColumn {
                items(plugins) { plugin ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(plugin.name.removeSuffix(".jar"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(formatSize(plugin), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModsTab(serverId: String) {
    var mods by remember(serverId) { mutableStateOf<List<File>>(emptyList()) }

    LaunchedEffect(serverId) {
        val dir = File("servers/$serverId/mods")
        mods = if (dir.exists()) dir.listFiles()?.filter { it.name.endsWith(".jar") }?.sortedBy { it.name } ?: emptyList() else emptyList()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Mods", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        if (mods.isEmpty()) {
            Text("No mods installed", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        } else {
            LazyColumn {
                items(mods) { mod ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.GridView, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mod.name.removeSuffix(".jar"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(formatSize(mod), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DatapacksTab(serverId: String) {
    var datapacks by remember(serverId) { mutableStateOf<List<File>>(emptyList()) }

    LaunchedEffect(serverId) {
        val dir = File("servers/$serverId/world/datapacks")
        datapacks = if (dir.exists()) dir.listFiles()?.filter { it.isDirectory || it.name.endsWith(".zip") }?.sortedBy { it.name } ?: emptyList() else emptyList()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Datapacks", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        if (datapacks.isEmpty()) {
            Text("No datapacks installed", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        } else {
            LazyColumn {
                items(datapacks) { dp ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(dp.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(formatSize(dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupsTab(serverId: String, backupManager: BackupManager?) {
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }
    var backupName by remember { mutableStateOf("") }
    var restoring by remember { mutableStateOf<String?>(null) }
    val backups by backupManager?.backups?.collectAsState() ?: remember { mutableStateOf(emptyList()) }

    if (restoring != null) {
        AlertDialog(
            onDismissRequest = { restoring = null },
            title = { Text("Restore Backup") },
            text = { Text("Restore \"${restoring}\"? Current worlds/config will be overwritten.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        backupManager?.restoreBackup(restoring!!)
                        restoring = null
                    }
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { restoring = null }) { Text("Cancel") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Backups", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = backupName, onValueChange = { backupName = it }, placeholder = { Text("Backup name") }, singleLine = true, modifier = Modifier.width(200.dp))
                Button(
                    onClick = {
                        val name = backupName.ifBlank { "manual" }
                        creating = true
                        scope.launch {
                            backupManager?.createBackup(name)
                            creating = false
                            backupName = ""
                        }
                    },
                    enabled = !creating,
                ) {
                    Text(if (creating) "Creating..." else "Create")
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (backups.isEmpty()) {
            Text("No backups yet", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        } else {
            LazyColumn {
                items(backups) { backup ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(backup.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(formatSize(backup.file), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { restoring = backup.name }) { Text("Restore") }
                            TextButton(onClick = { backupManager?.deleteBackup(backup.name) }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }
}

private fun formatSize(file: File): String {
    val bytes = if (file.isDirectory) file.walkTopDown().filter { it.isFile }.sumOf { it.length() } else file.length()
    return when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.0f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.0f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
}