package com.portalhost.app.ui.screens.server

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.app.server.BackupManager
import com.portalhost.app.server.ServerState
import com.portalhost.app.server.ServerStatus
import com.portalhost.app.ui.model.ServerConfig
import java.io.File

private val ALL_TABS = listOf("Properties", "Worlds", "Plugins", "Mods", "Backups")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerDetailScreen(
    server: ServerConfig,
    serverState: ServerState,
    onBack: () -> Unit,
    onUpdateServer: (ServerConfig) -> Unit = {},
    serverDir: File
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val backupManager = remember(serverDir) { BackupManager(serverDir) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(server.name) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 4.dp) {
                ALL_TABS.forEachIndexed { index, label ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(label, maxLines = 1, fontSize = 13.sp) })
                }
            }

            when (selectedTab) {
                0 -> PropertiesTab(server, serverDir, onUpdateServer)
                1 -> WorldsTab(serverDir)
                2 -> PluginsTab(serverDir)
                3 -> ModsTab(serverDir)
                4 -> BackupsTab(backupManager, serverState)
            }
        }
    }
}



// ─── PROPERTIES ───────────────────────────────────────────────────────────────

private val RAM_OPTIONS = listOf("512M", "1G", "2G", "3G", "4G", "6G", "8G", "12G", "16G")

@Composable
private fun PropertiesTab(server: ServerConfig, serverDir: File, onUpdateServer: (ServerConfig) -> Unit = {}) {
    var name by remember(server) { mutableStateOf(server.name) }
    var portText by remember(server) { mutableStateOf(server.port.toString()) }
    var gamemode by remember(server) { mutableStateOf(server.gamemode) }
    var difficulty by remember(server) { mutableStateOf(server.difficulty) }
    var motd by remember(server) { mutableStateOf(server.motd) }
    var minRam by remember(server) { mutableStateOf(server.minRam) }
    var maxRam by remember(server) { mutableStateOf(server.maxRam) }
    var saved by remember { mutableStateOf(false) }

    val gamemodes = listOf("survival", "creative", "adventure", "spectator")
    val difficulties = listOf("peaceful", "easy", "normal", "hard")

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Server Properties", style = MaterialTheme.typography.titleSmall)

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = portText, onValueChange = { portText = it.filter { c -> c.isDigit() }.take(5) }, label = { Text("Port") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Gamemode", style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            gamemodes.forEach { gm ->
                FilterChip(selected = gamemode == gm, onClick = { gamemode = gm }, label = { Text(gm.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) })
            }
        }
        Text("Difficulty", style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            difficulties.forEach { diff ->
                FilterChip(selected = difficulty == diff, onClick = { difficulty = diff }, label = { Text(diff.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) })
            }
        }
        OutlinedTextField(value = motd, onValueChange = { motd = it }, label = { Text("MOTD") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Text("Minimum RAM", style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            RAM_OPTIONS.forEach { ram ->
                FilterChip(selected = minRam == ram, onClick = { minRam = ram }, label = { Text(ram, style = MaterialTheme.typography.labelSmall) })
            }
        }
        Text("Maximum RAM", style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            RAM_OPTIONS.forEach { ram ->
                FilterChip(selected = maxRam == ram, onClick = { maxRam = ram }, label = { Text(ram, style = MaterialTheme.typography.labelSmall) })
            }
        }

        Button(
            onClick = {
                val newPort = portText.toIntOrNull() ?: server.port
                val propsFile = File(serverDir, "server.properties")
                if (propsFile.exists()) {
                    var content = propsFile.readText()
                    content = content.replace(Regex("(?m)^server-port=\\d+"), "server-port=$newPort")
                    content = content.replace(Regex("(?m)^gamemode=\\w+"), "gamemode=$gamemode")
                    content = content.replace(Regex("(?m)^difficulty=\\w+"), "difficulty=$difficulty")
                    content = content.replace(Regex("(?m)^motd=.*"), "motd=$motd")
                    propsFile.writeText(content)
                }
                val updated = server.copy(name = name, port = newPort, gamemode = gamemode, difficulty = difficulty, motd = motd, minRam = minRam, maxRam = maxRam)
                onUpdateServer(updated)
                saved = true
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save Properties") }

        Text("Read-only info", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PropertyRowReadOnly("JAR", server.jarName)
                PropertyRowReadOnly("Server Type", server.serverType.replaceFirstChar { it.uppercase() })
                PropertyRowReadOnly("MC Version", server.mcVersion.ifBlank { "—" })
            }
        }

        if (saved) {
            Text("Properties updated. Restart server to apply changes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PropertyRowReadOnly(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

// ─── WORLDS ───────────────────────────────────────────────────────────────────

@Composable
private fun WorldsTab(serverDir: File) {
    var worlds by remember { mutableStateOf(listOf<File>()) }
    var renameTarget by remember { mutableStateOf<File?>(null) }
    var renameText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { worlds = listWorlds(serverDir) }

    val refresh: () -> Unit = { worlds = listWorlds(serverDir) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Worlds", style = MaterialTheme.typography.titleSmall) }

        if (worlds.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text("No worlds found", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        worlds.forEach { world ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(world.name, style = MaterialTheme.typography.bodyMedium)
                            Text(formatWorldSize(world), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { renameTarget = world; renameText = world.name }) {
                            Icon(Icons.Default.DriveFileRenameOutline, contentDescription = "Rename", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { world.deleteRecursively(); refresh() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    renameTarget?.let { world ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename World") },
            text = { OutlinedTextField(value = renameText, onValueChange = { renameText = it }, label = { Text("New name") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    if (renameText.isNotBlank() && renameText != world.name) {
                        val newDir = File(world.parentFile, renameText)
                        world.renameTo(newDir)
                        refresh()
                    }
                    renameTarget = null
                }) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancel") } }
        )
    }
}

private fun listWorlds(serverDir: File): List<File> {
    val worlds = mutableListOf<File>()
    val world = File(serverDir, "world")
    if (world.exists()) worlds.add(world)
    val worldsDir = File(serverDir, "worlds")
    if (worldsDir.exists()) {
        worldsDir.listFiles()?.filter { it.isDirectory }?.let { worlds.addAll(it) }
    }
    return worlds
}

private fun formatWorldSize(dir: File): String {
    val bytes = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
        else -> "${"%.2f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
    }
}

// ─── PLUGINS ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PluginsTab(serverDir: File) {
    val context = LocalContext.current
    val pluginsDir = remember { File(serverDir, "plugins") }
    var plugins by remember { mutableStateOf(listOf<File>()) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importJar(context, uri, pluginsDir)
            plugins = listJars(pluginsDir)
        }
    }

    LaunchedEffect(Unit) { plugins = listJars(pluginsDir) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Plugins (${plugins.size})", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            SmallFloatingActionButton(onClick = { importLauncher.launch(arrayOf("application/java-archive", "*/*")) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Plugin")
            }
        }

        if (plugins.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text("No plugins installed", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(plugins, key = { it.absolutePath }) { plugin ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Extension, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(plugin.name.removeSuffix(".jar"), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(formatSize(plugin.length()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { plugin.delete(); plugins = listJars(pluginsDir) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

// ─── MODS ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModsTab(serverDir: File) {
    val context = LocalContext.current
    val modsDir = remember { File(serverDir, "mods") }
    var mods by remember { mutableStateOf(listOf<File>()) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importJar(context, uri, modsDir)
            mods = listJars(modsDir)
        }
    }

    LaunchedEffect(Unit) { mods = listJars(modsDir) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mods (${mods.size})", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            SmallFloatingActionButton(onClick = { importLauncher.launch(arrayOf("application/java-archive", "*/*")) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Mod")
            }
        }

        if (mods.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text("No mods installed", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(mods, key = { it.absolutePath }) { mod ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mod.name.removeSuffix(".jar"), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(formatSize(mod.length()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { mod.delete(); mods = listJars(modsDir) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

// ─── BACKUPS ──────────────────────────────────────────────────────────────────

@Composable
private fun BackupsTab(backupManager: BackupManager, serverState: ServerState) {
    var backups by remember { mutableStateOf(backupManager.listBackups()) }
    var creatingBackup by remember { mutableStateOf(false) }
    var restoreTarget by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var backupName by remember { mutableStateOf("") }
    var backupWorlds by remember { mutableStateOf(true) }
    var backupConfig by remember { mutableStateOf(true) }

    val refresh = { backups = backupManager.listBackups() }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Backups (${backups.size})", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                SmallFloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Create Backup")
                }
            }
        }

        if (backups.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text("No backups yet. Create one to protect your world.", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        backups.forEach { backup ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(backup.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row {
                                Text(formatSize(backup.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("  •  ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatTimestamp(backup.timestamp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = { backupManager.deleteBackup(backup.name); refresh() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    // Restore button row
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(
                            onClick = { restoreTarget = backup.name },
                            enabled = serverState.status == ServerStatus.OFFLINE,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Restore", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Create backup dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = backupName, onValueChange = { backupName = it }, label = { Text("Backup name") }, singleLine = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = backupWorlds, onCheckedChange = { backupWorlds = it })
                        Text("Include worlds")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = backupConfig, onCheckedChange = { backupConfig = it })
                        Text("Include config (server.properties)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = backupName.ifBlank { "manual" }
                    creatingBackup = true
                    backupManager.createBackup(name, worlds = backupWorlds, config = backupConfig)
                    creatingBackup = false
                    showCreateDialog = false
                    backupName = ""
                    refresh()
                }) { Text("Backup") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
        )
    }

    // Restore confirmation
    restoreTarget?.let { name ->
        AlertDialog(
            onDismissRequest = { restoreTarget = null },
            title = { Text("Restore Backup") },
            text = { Text("Restore \"$name\"? This will overwrite current world files. The server must be offline.") },
            confirmButton = {
                TextButton(onClick = {
                    backupManager.restoreBackup(name)
                    restoreTarget = null
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { restoreTarget = null }) { Text("Cancel") } }
        )
    }
}

// ─── HELPERS ──────────────────────────────────────────────────────────────────

private fun listJars(dir: File): List<File> {
    if (!dir.exists()) return emptyList()
    return dir.listFiles()?.filter { it.name.endsWith(".jar") }?.sortedBy { it.name } ?: emptyList()
}

private fun importJar(context: android.content.Context, uri: Uri, destDir: File) {
    try {
        destDir.mkdirs()
        context.contentResolver.openInputStream(uri)?.use { input ->
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "plugin.jar"
            val dest = File(destDir, name)
            dest.outputStream().use { output -> input.copyTo(output) }
        }
    } catch (e: Exception) {
        android.util.Log.e("ServerDetail", "Import failed: ${e.message}")
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
    else -> "${"%.2f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
}

private fun formatTimestamp(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.US)
    return sdf.format(java.util.Date(millis))
}
