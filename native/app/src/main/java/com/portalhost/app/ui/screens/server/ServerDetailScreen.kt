package com.portalhost.app.ui.screens.server

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.app.server.BackupManager
import com.portalhost.app.server.DeviceDetector
import com.portalhost.app.server.RamStatus
import com.portalhost.app.server.ServerState
import com.portalhost.app.server.ServerStatus

import com.portalhost.app.ui.model.ServerConfig
import com.portalhost.app.ui.screens.MotdEditor
import com.portalhost.app.ui.screens.saveServerIcon
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val ALL_TABS = listOf("Properties", "Worlds", "Plugins", "Mods", "Datapacks", "Backups")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerDetailScreen(
    server: ServerConfig,
    serverState: ServerState,
    onBack: () -> Unit,
    onUpdateServer: (ServerConfig) -> Unit = {},
    onDeleteServer: () -> Unit = {},
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
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 4.dp, modifier = Modifier.padding(vertical = 0.dp)) {
                ALL_TABS.forEachIndexed { index, label ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(label, maxLines = 1, fontSize = 13.sp) })
                }
            }

            when (selectedTab) {
                0 -> PropertiesTab(server, serverDir, onUpdateServer, onDeleteServer)
                1 -> WorldsTab(serverDir)
                2 -> PluginsTab(serverDir)
                 3 -> ModsTab(serverDir)
                 4 -> DatapacksTab(serverDir)
                 5 -> BackupsTab(backupManager, serverState)
            }
        }
    }
}



// ─── PROPERTIES ───────────────────────────────────────────────────────────────

private const val MIN_RAM_GB = 0.5f
private const val RAM_STEP_GB = 0.1f

private fun gbToMb(gb: Float): Int = (gb * 1024).toInt()
private fun mbToGb(mb: Int): Float = mb / 1024f

@Composable
private fun RamSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    maxRamGb: Float,
    ramStatus: RamStatus,
    modifier: Modifier = Modifier
) {
    val gbValue = remember(value) { mbToGb(value) }
    var sliderPos by remember(value) { mutableFloatStateOf(gbValue) }

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.weight(1f))
            Text("${"%.1f".format(sliderPos)} GB", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            if (label.contains("Maximum")) {
                Spacer(Modifier.width(8.dp))
                RamStatusBadge(ramStatus = ramStatus)
            }
        }
        Slider(
            value = sliderPos,
            onValueChange = { sliderPos = (it / RAM_STEP_GB).roundToInt() * RAM_STEP_GB },
            onValueChangeFinished = {
                onValueChange(gbToMb(sliderPos.coerceIn(MIN_RAM_GB, maxRamGb)))
            },
            valueRange = MIN_RAM_GB..maxRamGb,
            steps = ((maxRamGb - MIN_RAM_GB) / RAM_STEP_GB).toInt() - 1,
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${MIN_RAM_GB.toInt()} GB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${"%.1f".format(maxRamGb)} GB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatRamMb(mb: Int): String = when {
    mb < 1024 -> "${mb}M"
    mb % 1024 == 0 -> "${mb / 1024}G"
    else -> "${"%.1f".format(mb.toDouble() / 1024)}G"
}

private fun parseRamToMb(ram: String): Int {
    val upper = ram.uppercase().trim()
    return when {
        upper.endsWith("G") -> (upper.removeSuffix("G").toDoubleOrNull()?.times(1024)?.toInt() ?: 2048)
        upper.endsWith("M") -> (upper.removeSuffix("M").toIntOrNull() ?: 512)
        else -> 2048
    }
}

private fun formatRamValue(mb: Int): String = formatRamMb(mb)

@Composable
private fun RamStatusBadge(ramStatus: RamStatus) {
    val (label, bgColor, fgColor) = when (ramStatus) {
        RamStatus.RECOMMENDED -> Triple("Recommended", Color(0x1B4CAF50), Color(0xFF4CAF50))
        RamStatus.HIGH -> Triple("High", Color(0x1BFF9800), Color(0xFFFF9800))
        RamStatus.NOT_RECOMMENDED -> Triple("Not Recommended", Color(0x1BF44336), Color(0xFFF44336))
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = fgColor, fontWeight = FontWeight.SemiBold)
    }
}

private fun String.escapeProperties(): String {
    return this.replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
        .replace("§", "\\u00A7")
}

@Composable
private fun PropertiesTab(server: ServerConfig, serverDir: File, onUpdateServer: (ServerConfig) -> Unit = {}, onDeleteServer: () -> Unit = {}) {
    val context = LocalContext.current
    val deviceSpec = remember { DeviceDetector.detect(context) }
    val maxRamGb = remember {
        val cfg = DeviceDetector.generateConfig(deviceSpec)
        DeviceDetector.parseRamMb(cfg.recommendedMaxRam) / 1024f
    }
    var name by remember(server) { mutableStateOf(server.name) }
    var portText by remember(server) { mutableStateOf(server.port.toString()) }
    var gamemode by remember(server) { mutableStateOf(server.gamemode) }
    var difficulty by remember(server) { mutableStateOf(server.difficulty) }
    var motd by remember(server) { mutableStateOf(server.motd) }
    var minRamMb by remember(server) { mutableStateOf(parseRamToMb(server.minRam)) }
    var maxRamMb by remember(server) { mutableStateOf(parseRamToMb(server.maxRam)) }
    var maxPlayersText by remember(server) { mutableStateOf(readProp(serverDir, "max-players", "20")) }
    var onlineMode by remember(server) { mutableStateOf(readProp(serverDir, "online-mode", "true") == "true") }
    var pvpEnabled by remember(server) { mutableStateOf(readProp(serverDir, "pvp", "true") == "true") }
    var spawnProtectionText by remember(server) { mutableStateOf(readProp(serverDir, "spawn-protection", "16")) }
    var whiteListEnabled by remember(server) { mutableStateOf(readProp(serverDir, "white-list", "false") == "true") }
    var showRcon by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var iconPreview by remember(serverDir) { mutableStateOf(loadServerIconFile(serverDir)) }
    fun computeRamStatus(): RamStatus = DeviceDetector.getRamStatus(maxRamMb, deviceSpec)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val iconFile = File(serverDir, "server-icon.png")
            try {
                if (saveServerIcon(context.contentResolver, uri, iconFile)) {
                    iconPreview = loadServerIconFile(serverDir)
                    scope.launch {
                        snackbarHostState.showSnackbar("Icon saved. Restart server for it to appear in the Minecraft server list.", duration = SnackbarDuration.Short)
                    }
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("Failed to save icon: image could not be decoded", duration = SnackbarDuration.Long)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ServerDetail", "Failed to save icon", e)
                scope.launch {
                    snackbarHostState.showSnackbar("Failed to save icon: ${e.message}", duration = SnackbarDuration.Long)
                }
            }
        }
    }

    val gamemodes = listOf("survival", "creative", "adventure", "spectator")
    val difficulties = listOf("peaceful", "easy", "normal", "hard")

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Server Icon picker
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (iconPreview != null) {
                        Image(
                            bitmap = iconPreview!!,
                            contentDescription = "Server Icon",
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Server Icon", style = MaterialTheme.typography.titleSmall)
                        Text(if (iconPreview != null) "Tap to change" else "Select a picture", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = { iconPickerLauncher.launch("image/*") }) {
                        Text(if (iconPreview != null) "Change" else "Select")
                    }
                }
                Text("Recommended: 64x64 PNG — restart server to apply", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
            }

            Text("Server Properties", style = MaterialTheme.typography.titleSmall)

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = portText, onValueChange = { portText = it.filter { c -> c.isDigit() }.take(5) }, label = { Text("Port") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = maxPlayersText, onValueChange = { maxPlayersText = it.filter { c -> c.isDigit() }.take(4) }, label = { Text("Max Players") }, singleLine = true, modifier = Modifier.fillMaxWidth())
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
            MotdEditor(motd = motd, onMotdChange = { motd = it })
            OutlinedTextField(value = spawnProtectionText, onValueChange = { spawnProtectionText = it.filter { c -> c.isDigit() }.take(4) }, label = { Text("Spawn Protection (radius)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("PvP", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                Switch(checked = pvpEnabled, onCheckedChange = { pvpEnabled = it })
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Online Mode", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                Switch(checked = onlineMode, onCheckedChange = { onlineMode = it })
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Whitelist", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                Switch(checked = whiteListEnabled, onCheckedChange = { whiteListEnabled = it })
            }

            RamSlider("Minimum RAM", value = minRamMb, onValueChange = { minRamMb = it.coerceAtMost(maxRamMb) }, maxRamGb = maxRamGb, ramStatus = computeRamStatus())
            RamSlider("Maximum RAM", value = maxRamMb, onValueChange = { maxRamMb = it.coerceAtLeast(minRamMb) }, maxRamGb = maxRamGb, ramStatus = computeRamStatus())

            Button(
                onClick = {
                    try {
                        val newPort = portText.toIntOrNull() ?: server.port
                        val newMaxPlayers = maxPlayersText.toIntOrNull() ?: 20
                        val newSpawnProtection = spawnProtectionText.toIntOrNull() ?: 16
                        val propsFile = File(serverDir, "server.properties")
                        if (!propsFile.exists()) {
                            val defaultProps = """#Minecraft server properties
server-port=$newPort
gamemode=$gamemode
difficulty=$difficulty
motd=${motd.escapeProperties()}
max-players=$newMaxPlayers
online-mode=$onlineMode
pvp=$pvpEnabled
spawn-protection=$newSpawnProtection
white-list=$whiteListEnabled
"""
                            propsFile.writeText(defaultProps)
                        } else {
                            var content = propsFile.readText()
                            val replacements = mapOf(
                                "server-port" to newPort.toString(),
                                "gamemode" to gamemode,
                                "difficulty" to difficulty,
                                "motd" to motd.escapeProperties(),
                                "max-players" to newMaxPlayers.toString(),
                                "online-mode" to onlineMode.toString(),
                                "pvp" to pvpEnabled.toString(),
                                "spawn-protection" to newSpawnProtection.toString(),
                                "white-list" to whiteListEnabled.toString()
                            )
                            for ((key, value) in replacements) {
                                val regex = Regex("(?m)^$key=.*")
                                content = if (regex.containsMatchIn(content)) {
                                    content.replace(regex, "$key=$value")
                                } else {
                                    content + "\n$key=$value"
                                }
                            }
                            propsFile.writeText(content)
                        }
                        val updated = server.copy(name = name, port = newPort, gamemode = gamemode, difficulty = difficulty, motd = motd, minRam = formatRamMb(minRamMb), maxRam = formatRamMb(maxRamMb))
                        onUpdateServer(updated)
                        scope.launch {
                            snackbarHostState.showSnackbar("Configuration saved. Restart server to apply changes.", duration = SnackbarDuration.Short)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ServerDetailScreen", "Failed to save properties", e)
                        scope.launch {
                            snackbarHostState.showSnackbar("Failed to save: ${e.message}", duration = SnackbarDuration.Long)
                        }
                    }
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

            OutlinedButton(onClick = { showRcon = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("RCON Console")
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Delete Server")
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
        )

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Server") },
                text = { Text("Delete \"${server.name}\"? This will remove the server and all its files. This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        onDeleteServer()
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                }
            )
        }

        if (showRcon) {
            val rconHost = readProp(serverDir, "rcon.host", "localhost")
            val rconPort = readProp(serverDir, "rcon.port", "25575").toIntOrNull() ?: 25575
            val rconPassword = readProp(serverDir, "rcon.password", "")
            if (rconPassword.isBlank()) {
                AlertDialog(
                    onDismissRequest = { showRcon = false },
                    title = { Text("RCON Not Configured") },
                    text = { Text("Set rcon.password, rcon.port, and enable-rcon=true in server.properties, then restart the server.") },
                    confirmButton = { TextButton(onClick = { showRcon = false }) { Text("OK") } }
                )
            } else {
                RconDialog(host = rconHost, port = rconPort, password = rconPassword, onDismiss = { showRcon = false })
            }
        }
    }
}

private fun readProp(serverDir: File, key: String, default: String): String {
    val propsFile = File(serverDir, "server.properties")
    if (!propsFile.exists()) return default
    return try {
        val props = java.util.Properties()
        propsFile.inputStream().use { props.load(it) }
        props.getProperty(key)?.trim() ?: default
    } catch (_: Exception) { default }
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
    val context = LocalContext.current
    var worlds by remember { mutableStateOf(listOf<File>()) }
    var renameTarget by remember { mutableStateOf<File?>(null) }
    var renameText by remember { mutableStateOf("") }
    var worldToExport by remember { mutableStateOf<File?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { worlds = listWorlds(serverDir) }

    val refresh: () -> Unit = { worlds = listWorlds(serverDir) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        for (uri in uris) importWorld(context, uri, serverDir)
        if (uris.isNotEmpty()) refresh()
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null && worldToExport != null) {
            val world = worldToExport!!
            worldToExport = null
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        java.util.zip.ZipOutputStream(out).use { zos ->
                            world.walkTopDown().forEach { file ->
                                if (file == world) return@forEach
                                val entryName = file.relativeTo(world).path.replace('\\', '/')
                                if (file.isDirectory) {
                                    zos.putNextEntry(java.util.zip.ZipEntry("$entryName/"))
                                    zos.closeEntry()
                                } else {
                                    zos.putNextEntry(java.util.zip.ZipEntry(entryName))
                                    file.inputStream().use { it.copyTo(zos) }
                                    zos.closeEntry()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ServerDetail", "Export world failed: ${e.message}")
                }
            }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Worlds", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = { importLauncher.launch(arrayOf("application/zip", "*/*")) }) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Import World")
                }
            }
        }

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
                        IconButton(onClick = { worldToExport = world; exportLauncher.launch("${world.name}.zip") }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Download", modifier = Modifier.size(20.dp))
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

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        for (uri in uris) importJar(context, uri, pluginsDir)
        if (uris.isNotEmpty()) plugins = listJars(pluginsDir)
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

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        for (uri in uris) importJar(context, uri, modsDir)
        if (uris.isNotEmpty()) mods = listJars(modsDir)
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

// ─── DATAPACKS ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatapacksTab(serverDir: File) {
    val context = LocalContext.current
    val datapacksDir = remember { File(serverDir, "world/datapacks") }
    var datapacks by remember { mutableStateOf(listOf<File>()) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        for (uri in uris) importDatapack(context, uri, datapacksDir)
        if (uris.isNotEmpty()) datapacks = listDatapacks(datapacksDir)
    }

    LaunchedEffect(Unit) { datapacks = listDatapacks(datapacksDir) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Datapacks (${datapacks.size})", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            SmallFloatingActionButton(onClick = { importLauncher.launch(arrayOf("application/zip", "*/*")) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Datapack")
            }
        }

        if (datapacks.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text("No datapacks installed", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(datapacks, key = { it.absolutePath }) { dp ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(dp.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                if (dp.isDirectory) "Directory" else formatSize(dp.length()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            if (dp.isDirectory) dp.deleteRecursively() else dp.delete()
                            datapacks = listDatapacks(datapacksDir)
                        }) {
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
                            enabled = serverState.status == ServerStatus.OFFLINE || serverState.status == ServerStatus.STOPPED,
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

private fun listDatapacks(dir: File): List<File> {
    if (!dir.exists()) return emptyList()
    return dir.listFiles()?.filter { it.name.endsWith(".zip") || it.isDirectory }?.sortedBy { it.name } ?: emptyList()
}

private fun getFileName(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        }
    } catch (_: Exception) { null }
}

private fun resolveDestFile(parent: File, name: String): File {
    val dest = File(parent, name)
    if (!dest.exists()) return dest
    val base = name.substringBeforeLast('.')
    val ext = name.substringAfterLast('.', "")
    var count = 1
    while (true) {
        val candidate = if (ext.isEmpty()) "${base}_$count" else "${base}_$count.$ext"
        val file = File(parent, candidate)
        if (!file.exists()) return file
        count++
    }
}

private fun importJar(context: android.content.Context, uri: android.net.Uri, destDir: File) {
    try {
        destDir.mkdirs()
        context.contentResolver.openInputStream(uri)?.use { input ->
            val name = getFileName(context, uri) ?: uri.lastPathSegment?.substringAfterLast('/') ?: "plugin.jar"
            val cleanName = if (name.endsWith(".jar")) name else "$name.jar"
            val dest = resolveDestFile(destDir, cleanName)
            dest.outputStream().use { output -> input.copyTo(output) }
        }
    } catch (e: Exception) {
        android.util.Log.e("ServerDetail", "Import failed: ${e.message}")
    }
}

private fun importDatapack(context: android.content.Context, uri: android.net.Uri, destDir: File) {
    try {
        destDir.mkdirs()
        context.contentResolver.openInputStream(uri)?.use { input ->
            val name = getFileName(context, uri) ?: uri.lastPathSegment?.substringAfterLast('/') ?: "datapack.zip"
            val cleanName = if (name.endsWith(".zip")) name else "$name.zip"
            val dest = resolveDestFile(destDir, cleanName)
            dest.outputStream().use { output -> input.copyTo(output) }
        }
    } catch (e: Exception) {
        android.util.Log.e("ServerDetail", "Datapack import failed: ${e.message}")
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
    else -> "${"%.2f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
}

private fun loadServerIconFile(serverDir: File): ImageBitmap? {
    val iconFile = File(serverDir, "server-icon.png")
    if (!iconFile.exists()) return null
    return try {
        BitmapFactory.decodeFile(iconFile.absolutePath)?.asImageBitmap()
    } catch (e: Exception) { null }
}

private fun formatTimestamp(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.US)
    return sdf.format(java.util.Date(millis))
}

private fun importWorld(context: android.content.Context, uri: android.net.Uri, serverDir: File) {
    try {
        val worldsDir = File(serverDir, "worlds")
        worldsDir.mkdirs()
        val zipName = getFileName(context, uri) ?: uri.lastPathSegment?.substringAfterLast('/') ?: "world.zip"
        val worldName = zipName.removeSuffix(".zip").removeSuffix(".ZIP")
        val destDir = resolveDestFile(worldsDir, worldName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            val tmpZip = File(context.cacheDir, "world_import_${System.nanoTime()}.zip")
            tmpZip.outputStream().use { input.copyTo(it) }
            java.util.zip.ZipInputStream(tmpZip.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val target = File(destDir, entry.name)
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { zis.copyTo(it) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            tmpZip.delete()
        }
    } catch (e: Exception) {
        android.util.Log.e("ServerDetail", "Import world failed: ${e.message}")
    }
}


