package com.portalhost.desktop.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.db.DatabaseRepository
import com.portalhost.model.ServerConfig
import com.portalhost.model.ServerStatus
import com.portalhost.server.BackupEntry
import com.portalhost.filesystem.FileSystem
import com.portalhost.server.BackupManager
import com.portalhost.server.ServerManager
import com.portalhost.server.getServerIconFile
import com.portalhost.server.loadServerIcon
import com.portalhost.server.saveServerIcon
import com.portalhost.util.pickFile
import com.portalhost.theme.ThemeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.io.File
import java.util.zip.ZipFile
import kotlin.math.roundToInt

private val motdColorMap = mapOf(
    '0' to Color(0xFF000000), '1' to Color(0xFF0000AA), '2' to Color(0xFF00AA00), '3' to Color(0xFF00AAAA),
    '4' to Color(0xFFAA0000), '5' to Color(0xFFAA00AA), '6' to Color(0xFFFFAA00), '7' to Color(0xFFAAAAAA),
    '8' to Color(0xFF555555), '9' to Color(0xFF5555FF), 'a' to Color(0xFF55FF55), 'b' to Color(0xFF55FFFF),
    'c' to Color(0xFFFF5555), 'd' to Color(0xFFFF55FF), 'e' to Color(0xFFFFFF55), 'f' to Color(0xFFFFFFFF)
)

private fun parseMotdPreview(motd: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        var currentColor: Color? = null
        var bold = false
        var italic = false
        var strikethrough = false
        var underline = false
        while (i < motd.length) {
            if (motd[i] == '§' && i + 1 < motd.length) {
                val code = motd[i + 1].lowercaseChar()
                when (code) {
                    in '0'..'9', in 'a'..'f' -> currentColor = motdColorMap[code]
                    'l' -> bold = true
                    'm' -> strikethrough = true
                    'n' -> underline = true
                    'o' -> italic = true
                    'r' -> { currentColor = null; bold = false; italic = false; strikethrough = false; underline = false }
                }
                i += 2
            } else {
                val start = i
                while (i < motd.length && !(motd[i] == '§' && i + 1 < motd.length)) i++
                val segment = motd.substring(start, i)
                if (segment.isNotEmpty()) {
                    withStyle(SpanStyle(
                        color = currentColor ?: Color(0xFFAAAAAA),
                        fontWeight = if (bold) FontWeight.Bold else null,
                        fontStyle = if (italic) FontStyle.Italic else null,
                        textDecoration = when { strikethrough && underline -> TextDecoration.combine(listOf(TextDecoration.LineThrough, TextDecoration.Underline)); strikethrough -> TextDecoration.LineThrough; underline -> TextDecoration.Underline; else -> null }
                    )) { append(segment) }
                }
            }
        }
    }
}

@Composable
fun ServerDetailScreen(
    serverId: String,
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
    val fileSystem = koinInject<FileSystem>()
    val backupManager = remember(config) {
        config?.let {
            val jarFile = serverManager.getServerJar(serverId)
            val serverDir = jarFile.parentFile ?: jarFile.absoluteFile.parentFile ?: File(".")
            BackupManager(File(serverDir, serverId), serverId, database)
        }
    }
    val backupEntries by backupManager?.backups?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var importFileName by remember { mutableStateOf<String?>(null) }

    val serverIcon = remember(config?.id) {
        config?.id?.let { id ->
            val iconFile = getServerIconFile(serverManager.getServerDir(id))
            loadServerIcon(iconFile)
        }
    }

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
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    if (serverIcon != null) {
                        Image(bitmap = serverIcon, contentDescription = "Server Icon", modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
                        Spacer(Modifier.width(16.dp))
                    }
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
                    OutlinedButton(onClick = { showDeleteDialog = true }, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }
        }

        val tabs = listOf("Properties", "Files", "Worlds", "Plugins", "Mods", "Datapacks", "Backups", "Performance", "Logs", "RCON")
        SecondaryScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 4.dp) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, maxLines = 1, fontSize = 13.sp) },
                )
            }
        }

        when (selectedTab) {
            0 -> PropertiesTab(config = config, state = state, serverManager = serverManager, serverId = serverId, onDeleteRequest = { showDeleteDialog = true })
            1 -> ServerFilesScreen(serverId = serverId)
            2 -> WorldsTab(serverId = serverId)
            3 -> PluginsTab(serverId = serverId)
            4 -> ModsTab(serverId = serverId)
            5 -> DatapacksTab(serverId = serverId)
            6 -> BackupsTab(serverId = serverId, backupManager = backupManager)
            7 -> PerformanceScreen(serverId = serverId)
            8 -> LogViewerScreen(serverId = serverId)
            9 -> RconScreen(serverId = serverId)
        }
    }
}

@Composable
private fun StatusBadgeDetail(status: ServerStatus) {
    val color = ThemeColors.serverStatusColor(status)
    val label = when (status) {
        ServerStatus.RUNNING -> "Online"
        ServerStatus.STARTING -> "Starting"
        ServerStatus.STOPPING -> "Stopping"
        ServerStatus.CRASHED -> "Crashed"
        ServerStatus.RESTARTING -> "Restarting"
        ServerStatus.STOPPED -> "Stopped"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PropertiesTab(config: ServerConfig, state: com.portalhost.model.ServerState?, serverManager: ServerManager, serverId: String, onDeleteRequest: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val database = koinInject<DatabaseRepository>()
    val fileSystem = koinInject<FileSystem>()
    var name by remember(config) { mutableStateOf<String>(config.name) }
    var port by remember(config) { mutableStateOf<String>(config.port.toString()) }
    var memoryMinGb by remember(config) { mutableFloatStateOf((config.memoryMin / 1024f).coerceIn(0.5f, 16f)) }
    var memoryMaxGb by remember(config) { mutableFloatStateOf((config.memoryMax / 1024f).coerceIn(0.5f, 16f)) }
    var gamemode by remember(config) { mutableStateOf<String>(config.properties["gamemode"] ?: "survival") }
    var difficulty by remember(config) { mutableStateOf<String>(config.properties["difficulty"] ?: "easy") }
    var motd by remember(config) { mutableStateOf(TextFieldValue(config.properties["motd"] ?: "A Minecraft Server")) }
    var pvp by remember(config) { mutableStateOf<Boolean>(config.properties["pvp"]?.toBooleanStrictOrNull() ?: true) }
    var onlineMode by remember(config) { mutableStateOf<Boolean>(config.properties["online-mode"]?.toBooleanStrictOrNull() ?: true) }
    var whitelist by remember(config) { mutableStateOf<Boolean>(config.properties["white-list"]?.toBooleanStrictOrNull() ?: false) }
    var spawnProtection by remember(config) { mutableStateOf<String>(config.properties["spawn-protection"] ?: "16") }
    var rconEnabled by remember(config) { mutableStateOf<Boolean>(config.rconEnabled) }
    var rconPort by remember(config) { mutableStateOf<String>(config.rconPort.toString()) }
    var autoRestart by remember(config) { mutableStateOf<Boolean>(config.autoRestart) }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    var iconPreview by remember(config) { mutableStateOf<ImageBitmap?>(loadServerIcon(getServerIconFile(serverManager.getServerDir(config.id)))) }

    val gamemodes = listOf("survival", "creative", "adventure", "spectator")
    val difficulties = listOf("peaceful", "easy", "normal", "hard")

    fun writeServerPropertiesFile(props: Map<String, String>) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val propsFile = java.io.File(serverManager.getServerDir(serverId), "server.properties")
                if (propsFile.exists()) {
                    val lines = propsFile.readLines().toMutableList()
                    for ((key, value) in props) {
                        val idx = lines.indexOfFirst { it.startsWith("$key=") }
                        if (idx >= 0) lines[idx] = "$key=$value"
                        else lines.add("$key=$value")
                    }
                    propsFile.writeText(lines.joinToString("\n") + "\n")
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
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
                Spacer(Modifier.height(12.dp))
                Text("Min Memory: ${"%.1f".format(memoryMinGb)} GB", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Slider(
                    value = memoryMinGb,
                    onValueChange = { v -> memoryMinGb = (v / 0.5f).roundToInt() * 0.5f },
                    valueRange = 0.5f..memoryMaxGb,
                    steps = ((memoryMaxGb - 0.5f) / 0.5f).roundToInt() - 1,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text("Max Memory: ${"%.1f".format(memoryMaxGb)} GB", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Slider(
                    value = memoryMaxGb,
                    onValueChange = { v -> memoryMaxGb = (v / 0.5f).roundToInt() * 0.5f },
                    valueRange = 0.5f..16f,
                    steps = 30,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = motd, onValueChange = { motd = it }, label = { Text("MOTD") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                var showMotdColors by remember { mutableStateOf(false) }
                TextButton(onClick = { showMotdColors = !showMotdColors }) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (showMotdColors) "Hide Codes" else "Color Codes", style = MaterialTheme.typography.labelSmall)
                }
                if (showMotdColors) {
                    val insertCode: (String) -> Unit = { code ->
                        val cursor = motd.selection.start
                        val text = motd.text
                        motd = TextFieldValue(
                            text = text.substring(0, cursor) + code + text.substring(cursor),
                            selection = TextRange(cursor + code.length)
                        )
                    }
                    val mcColors = listOf(
                        '0' to Color(0xFF000000), '1' to Color(0xFF0000AA), '2' to Color(0xFF00AA00), '3' to Color(0xFF00AAAA),
                        '4' to Color(0xFFAA0000), '5' to Color(0xFFAA00AA), '6' to Color(0xFFFFAA00), '7' to Color(0xFFAAAAAA),
                        '8' to Color(0xFF555555), '9' to Color(0xFF5555FF), 'a' to Color(0xFF55FF55), 'b' to Color(0xFF55FFFF),
                        'c' to Color(0xFFFF5555), 'd' to Color(0xFFFF55FF), 'e' to Color(0xFFFFFF55), 'f' to Color(0xFFFFFFFF),
                    )
                    Text("Colors", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        mcColors.forEach { (code, color) ->
                            val borderColor = if (code == 'f') Color(0xFF888888) else Color.Transparent
                            Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(12.dp)).background(color).border(0.5.dp, borderColor, RoundedCornerShape(12.dp)).clickable { insertCode("§$code") }, contentAlignment = Alignment.Center) {
                                Text(code.toString(), fontSize = 10.sp, color = if (code in listOf('0', '8', '4')) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Formatting", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val formatCodes = listOf("Bold" to "§l", "Italic" to "§o", "Underline" to "§n", "Strike" to "§m", "Obfuscated" to "§k", "Reset" to "§r")
                        formatCodes.forEach { (label, code) ->
                            SuggestionChip(onClick = { insertCode(code) }, label = { Text(label, fontSize = 11.sp) })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Preview", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B2B))) {
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                            val parsed = remember(motd.text) { parseMotdPreview(motd.text) }
                            Text(text = if (parsed.text.isEmpty()) buildAnnotatedString { withStyle(SpanStyle(color = Color(0xFFAAAAAA))) { append("MOTD preview will appear here") } } else parsed, fontSize = 15.sp, lineHeight = 22.sp)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = {
                    scope.launch {
                        val files = pickFile("Select Server Icon", "Images" to listOf("png", "jpg", "jpeg"))
                        if (files.isNotEmpty()) {
                            val iconDest = getServerIconFile(serverManager.getServerDir(serverId))
                            withContext(Dispatchers.IO) {
                                saveServerIcon(java.io.File(files[0].absolutePath), iconDest)
                            }
                            iconPreview = loadServerIcon(iconDest)
                        }
                    }
                }) {
                    Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (iconPreview != null) "Change Icon" else "Set Server Icon", style = MaterialTheme.typography.labelSmall)
                }
                if (iconPreview != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("Icon selected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        var expandedGm by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expandedGm, onExpandedChange = { expandedGm = it }) {
                            OutlinedTextField(
                                value = gamemode.replaceFirstChar { it.uppercase() },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Gamemode") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGm) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                            )
                            ExposedDropdownMenu(expanded = expandedGm, onDismissRequest = { expandedGm = false }) {
                                gamemodes.forEach { gm ->
                                    DropdownMenuItem(
                                        text = { Text(gm.replaceFirstChar { it.uppercase() }) },
                                        onClick = { gamemode = gm; expandedGm = false },
                                    )
                                }
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        var expandedDiff by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expandedDiff, onExpandedChange = { expandedDiff = it }) {
                            OutlinedTextField(
                                value = difficulty.replaceFirstChar { it.uppercase() },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Difficulty") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDiff) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                            )
                            ExposedDropdownMenu(expanded = expandedDiff, onDismissRequest = { expandedDiff = false }) {
                                difficulties.forEach { diff ->
                                    DropdownMenuItem(
                                        text = { Text(diff.replaceFirstChar { it.uppercase() }) },
                                        onClick = { difficulty = diff; expandedDiff = false },
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = spawnProtection, onValueChange = { spawnProtection = it.filter { c -> c.isDigit() } }, label = { Text("Spawn Protection") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("PvP", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = pvp, onCheckedChange = {
                        pvp = it
                        writeServerPropertiesFile(mapOf("pvp" to it.toString()))
                    })
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Online Mode", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = onlineMode, onCheckedChange = {
                        onlineMode = it
                        writeServerPropertiesFile(mapOf("online-mode" to it.toString()))
                    })
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Whitelist", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = whitelist, onCheckedChange = {
                        whitelist = it
                        writeServerPropertiesFile(mapOf("white-list" to it.toString()))
                    })
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
                                memoryMin = (memoryMinGb * 1024).roundToInt(),
                                memoryMax = (memoryMaxGb * 1024).roundToInt(),
                                properties = config.properties + mapOf(
                                    "gamemode" to gamemode,
                                    "difficulty" to difficulty,
                                    "motd" to motd.text,
                                    "pvp" to pvp.toString(),
                                    "online-mode" to onlineMode.toString(),
                                    "white-list" to whitelist.toString(),
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
                            memoryMinGb = (updatedConfig.memoryMin / 1024f).coerceIn(0.5f, 16f)
                            memoryMaxGb = (updatedConfig.memoryMax / 1024f).coerceIn(0.5f, 16f)
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
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onDeleteRequest,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete This Server")
                }
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
    val fileSystem = koinInject<FileSystem>()
    val servers by serverManager.servers.collectAsState()
    val config = servers[serverId]
    var worlds by remember(serverId) { mutableStateOf<List<File>>(emptyList()) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importFilePath by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(serverId) {
        val dir = serverManager.getServerDir(serverId)
        worlds = dir.listFiles()?.filter {
            it.isDirectory && (it.name == "world" || it.name.startsWith("world_"))
        } ?: emptyList()
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Worlds (${worlds.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = {
                    val dir = serverManager.getServerDir(serverId)
                    worlds = dir.listFiles()?.filter {
                        it.isDirectory && (it.name == "world" || it.name.startsWith("world_"))
                    } ?: emptyList()
                }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", modifier = Modifier.size(18.dp))
                }
                Button(onClick = { showImportDialog = true }) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Import ZIP")
                }
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
                            Icon(Icons.Filled.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            scope.launch {
                                val files = pickFile("Select World ZIP", "ZIP files" to listOf("zip"))
                                if (files.isNotEmpty()) {
                                    importFilePath = files[0].absolutePath
                                }
                            }
                        }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Browse")
                        }
                    }
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
                        importWorldZip(serverManager.getServerDir(serverId), importFilePath)
                        showImportDialog = false
                        importFilePath = ""
                    }
                }) { Text("Import") }
            },
            dismissButton = { TextButton(onClick = { showImportDialog = false }) { Text("Cancel") } }
        )
    }
}

private fun importWorldZip(serverDir: File, zipPath: String) {
    val zipFile = File(zipPath)
    val targetDir = serverDir
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
    val serverManager = koinInject<ServerManager>()
    val scope = rememberCoroutineScope()
    var plugins by remember(serverId) { mutableStateOf<List<File>>(emptyList()) }

    fun refreshPlugins() {
        val dir = java.io.File(serverManager.getServerDir(serverId), "plugins")
        plugins = if (dir.exists()) dir.listFiles()?.filter { it.name.endsWith(".jar") }?.sortedBy { it.name } ?: emptyList() else emptyList()
    }

    LaunchedEffect(serverId) { refreshPlugins() }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Plugins (${plugins.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { refreshPlugins() }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", modifier = Modifier.size(18.dp))
                }
                Button(onClick = {
                    scope.launch {
                        val files = pickFile("Select JAR file", "JAR files" to listOf("jar"), multiSelection = true)
                        if (files.isNotEmpty()) {
                            withContext(Dispatchers.IO) {
                                val dir = java.io.File(serverManager.getServerDir(serverId), "plugins")
                                dir.mkdirs()
                                files.forEach { f ->
                                    File(f.absolutePath).copyTo(File(dir, f.name), overwrite = true)
                                }
                            }
                            refreshPlugins()
                        }
                    }
                }) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Import JAR")
                }
            }
        }
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
                            IconButton(onClick = { plugin.delete() }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
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
    val serverManager = koinInject<ServerManager>()
    val scope = rememberCoroutineScope()
    var mods by remember(serverId) { mutableStateOf<List<File>>(emptyList()) }

    fun refreshMods() {
        val dir = java.io.File(serverManager.getServerDir(serverId), "mods")
        mods = if (dir.exists()) dir.listFiles()?.filter { it.name.endsWith(".jar") }?.sortedBy { it.name } ?: emptyList() else emptyList()
    }

    LaunchedEffect(serverId) { refreshMods() }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Mods (${mods.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { refreshMods() }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", modifier = Modifier.size(18.dp))
                }
                Button(onClick = {
                    scope.launch {
                        val files = pickFile("Select JAR file", "JAR files" to listOf("jar"), multiSelection = true)
                        if (files.isNotEmpty()) {
                            withContext(Dispatchers.IO) {
                                val dir = java.io.File(serverManager.getServerDir(serverId), "mods")
                                dir.mkdirs()
                                files.forEach { f ->
                                    File(f.absolutePath).copyTo(File(dir, f.name), overwrite = true)
                                }
                            }
                            refreshMods()
                        }
                    }
                }) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Import JAR")
                }
            }
        }
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
                            Icon(Icons.Filled.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mod.name.removeSuffix(".jar"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(formatSize(mod), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { mod.delete() }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
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
    val serverManager = koinInject<ServerManager>()
    val scope = rememberCoroutineScope()
    var datapacks by remember(serverId) { mutableStateOf<List<File>>(emptyList()) }

    fun refreshDatapacks() {
        val dir = java.io.File(serverManager.getServerDir(serverId), "world/datapacks")
        datapacks = if (dir.exists()) dir.listFiles()?.filter { it.isDirectory || it.name.endsWith(".zip") }?.sortedBy { it.name } ?: emptyList() else emptyList()
    }

    LaunchedEffect(serverId) { refreshDatapacks() }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Datapacks (${datapacks.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { refreshDatapacks() }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", modifier = Modifier.size(18.dp))
                }
                Button(onClick = {
                    scope.launch {
                        val files = pickFile("Select datapack file", "Datapack files" to listOf("zip"), multiSelection = true)
                        if (files.isNotEmpty()) {
                            withContext(Dispatchers.IO) {
                                val dir = java.io.File(serverManager.getServerDir(serverId), "world/datapacks")
                                dir.mkdirs()
                                files.forEach { f ->
                                    File(f.absolutePath).copyTo(File(dir, f.name), overwrite = true)
                                }
                            }
                            refreshDatapacks()
                        }
                    }
                }) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Import ZIP")
                }
            }
        }
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
                            Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(dp.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(formatSize(dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { dp.deleteRecursively() }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
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

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Backups (${backups.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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