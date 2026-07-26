package com.portalhost.desktop.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import com.portalhost.filesystem.FileSystem
import com.portalhost.java.JdkManager
import com.portalhost.model.ServerConfig
import com.portalhost.model.ServerSource
import com.portalhost.model.ServerType
import com.portalhost.model.ServerVersion
import com.portalhost.server.ServerDownloader
import com.portalhost.server.ServerManager
import com.portalhost.server.getServerIconFile
import com.portalhost.server.loadServerIcon
import com.portalhost.server.saveServerIcon
import com.portalhost.server.providers.ServerProviderRegistry
import com.portalhost.uinotify.ToastManager
import com.portalhost.util.pickDirectory
import com.portalhost.util.pickFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.io.File
import java.util.Properties
import kotlin.math.roundToInt

enum class CreateSource {
    PICK_FILE, IMPORT_FOLDER, DOWNLOAD_PAPER, DOWNLOAD_VANILLA, DOWNLOAD_FABRIC, DOWNLOAD_FORGE,
    DOWNLOAD_NEOFORGE, DOWNLOAD_FOLIA, DOWNLOAD_PURPUR
}

fun CreateSource.toServerType(): ServerType? = when (this) {
    CreateSource.DOWNLOAD_PAPER -> ServerType.PAPER
    CreateSource.DOWNLOAD_VANILLA -> ServerType.VANILLA
    CreateSource.DOWNLOAD_FABRIC -> ServerType.FABRIC
    CreateSource.DOWNLOAD_FORGE -> ServerType.FORGE
    CreateSource.DOWNLOAD_NEOFORGE -> ServerType.NEOFORGE
    CreateSource.DOWNLOAD_FOLIA -> ServerType.FOLIA
    CreateSource.DOWNLOAD_PURPUR -> ServerType.PURPUR
    CreateSource.PICK_FILE, CreateSource.IMPORT_FOLDER -> null
}

fun CreateSource.supportsBuilds(): Boolean = toServerType()?.let {
    it == ServerType.PAPER || it == ServerType.FABRIC || it == ServerType.FORGE ||
    it == ServerType.NEOFORGE
} ?: false

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

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("EXPERIMENTAL_API_USAGE")
@Composable
fun CreateServerScreen(
    onServerCreated: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val serverManager = koinInject<ServerManager>()
    val serverDownloader = koinInject<ServerDownloader>()
    val providerRegistry = koinInject<ServerProviderRegistry>()
    val jdkManager = koinInject<JdkManager>()
    val fileSystem = koinInject<FileSystem>()
    val scope = rememberCoroutineScope()
    val toastManager = koinInject<ToastManager>()

    val downloadProgress by serverDownloader.downloadProgress.collectAsState()
    val downloadStatus by serverDownloader.currentStatus.collectAsState()
    val jdkInstallations by jdkManager.knownInstallations.collectAsState()
    val isJdkInstalling by jdkManager.isInstalling.collectAsState()
    val jdkInstallProgress by jdkManager.installProgress.collectAsState()

    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 6

    var createSource by remember { mutableStateOf<CreateSource?>(null) }
    var jarPath by remember { mutableStateOf<String?>(null) }
    var jarName by remember { mutableStateOf("") }
    var serverName by remember { mutableStateOf("") }
    var mcVersion by remember { mutableStateOf("") }
    var availableVersions by remember { mutableStateOf<List<String>>(emptyList()) }
    var versionsLoading by remember { mutableStateOf(false) }
    var versionsError by remember { mutableStateOf<String?>(null) }
    var selectedBuildId by remember { mutableStateOf("") }
    var availableBuilds by remember { mutableStateOf<List<com.portalhost.model.ServerBuild>>(emptyList()) }
    var buildsLoading by remember { mutableStateOf(false) }
    var buildsError by remember { mutableStateOf<String?>(null) }
    var minRam by remember { mutableFloatStateOf(1.0f) }
    var maxRam by remember { mutableFloatStateOf(4.0f) }
    var port by remember { mutableStateOf("25565") }
    var gamemode by remember { mutableStateOf("survival") }
    var difficulty by remember { mutableStateOf("easy") }
    var motd by remember { mutableStateOf(TextFieldValue("A Minecraft Server")) }
    var iconPath by remember { mutableStateOf<String?>(null) }
    var eulaAccepted by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedJavaVersion by remember { mutableStateOf(21) }
    var retryTrigger by remember { mutableIntStateOf(0) }
    var importFolderPath by remember { mutableStateOf<String?>(null) }
    var importedServerProps by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val importReadyToCreate by remember(importFolderPath, jarPath, serverName) {
        derivedStateOf {
            importFolderPath?.isNotBlank() == true && jarPath?.isNotBlank() == true && serverName.isNotBlank()
        }
    }
    val serversDir = fileSystem.getServersDirBlocking()
    val availableBytes = serversDir.parentFile?.let { it.usableSpace } ?: 0L
    val requiredBytes = (maxRam * 1024 * 1024 * 1024).toLong() + 500_000_000L

    val gamemodes = listOf("survival", "creative", "adventure", "spectator")
    val difficulties = listOf("peaceful", "easy", "normal", "hard")

    val provider: com.portalhost.server.providers.ServerProvider? = createSource?.toServerType()?.let {
        providerRegistry.getProvidersForType(it).firstOrNull()
    }

    val downloadOptions = listOf(
        Triple("Paper", "High-performance server software, recommended", Icons.Default.Description),
        Triple("Vanilla", "Official Mojang server jar", Icons.Default.Cloud),
        Triple("Fabric", "Lightweight mod loader", Icons.Default.Extension),
        Triple("Forge", "Popular mod loader with extensive mod support", Icons.Default.Build),
        Triple("NeoForge", "Next-generation Forge fork", Icons.Default.Build),
        Triple("Folia", "Region-based multithreaded Paper fork", Icons.Default.Description),
        Triple("Purpur", "Feature-rich Paper fork", Icons.Default.Description),
    )

    LaunchedEffect(createSource, retryTrigger) {
        if (createSource != null && createSource != CreateSource.PICK_FILE && createSource != CreateSource.IMPORT_FOLDER) {
            mcVersion = ""
            selectedBuildId = ""
            jarName = ""
            jarPath = null
            versionsLoading = true
            versionsError = null
            availableVersions = emptyList()
            val prov = provider ?: return@LaunchedEffect
            try {
                val versions = prov.fetchVersions().getOrThrow()
                availableVersions = versions.map { it.version }
            } catch (e: Exception) {
                versionsError = e.message ?: "Failed to fetch versions"
            }
            versionsLoading = false
        }
    }

    LaunchedEffect(mcVersion, createSource) {
        if (mcVersion.isNotBlank() && (createSource?.supportsBuilds() == true) && provider != null) {
            buildsLoading = true
            buildsError = null
            try {
                val builds = provider.fetchBuilds(mcVersion).getOrThrow()
                availableBuilds = builds
            } catch (e: Exception) {
                buildsError = e.message ?: "Failed to load builds"
                availableBuilds = emptyList()
            }
            buildsLoading = false
        }
    }

    LaunchedEffect(importFolderPath) {
        if (importFolderPath.isNullOrBlank()) {
            return@LaunchedEffect
        }
        val path = importFolderPath!!
        withContext(Dispatchers.IO) {
            val folder = File(path)
            if (!folder.exists() || !folder.isDirectory) {
                return@withContext
            }
            val propsFile = File(folder, "server.properties")
            val propsResult = mutableMapOf<String, String>()
            if (propsFile.exists()) {
                try {
                    val props = java.util.Properties()
                    props.load(propsFile.inputStream())
                    propsResult.putAll(props.entries.associate { (it.key as String) to (it.value as String) })
                } catch (_: Exception) { }
            }
            val jarFiles = folder.listFiles { _, name -> name.endsWith(".jar") } ?: emptyArray()
            val jarResult = jarFiles.firstOrNull()
            withContext(Dispatchers.Main) {
                if (propsResult.isNotEmpty()) {
                    importedServerProps = propsResult
                    propsResult["server-name"]?.let { serverName = it }
                    propsResult["server-port"]?.let { port = it }
                    propsResult["gamemode"]?.let { gamemode = it }
                    propsResult["difficulty"]?.let { difficulty = it }
                    propsResult["motd"]?.let { motd = TextFieldValue(it) }
                }
                if (jarResult != null) {
                    jarPath = jarResult.absolutePath
                    jarName = jarResult.name
                }
                if (serverName.isBlank()) {
                    serverName = path.substringAfterLast(java.io.File.separator)
                }
            }
        }
    }

    @Composable
    fun StepIndicator(current: Int, total: Int) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 0 until total) {
                Surface(
                    modifier = Modifier.weight(1f).height(4.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = if (i <= current) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ) {}
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Step ${current + 1} of $total",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    @Composable
    fun StepChooseSource() {
        Text("Server Software", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Choose a server jar source", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        downloadOptions.forEach { (title, subtitle, icon) ->
            val enumVal = CreateSource.valueOf("DOWNLOAD_${title.uppercase().replace(" ", "_")}")
            val selected = createSource == enumVal
            Card(
                onClick = { createSource = enumVal },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Text(subtitle, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (selected) Icon(Icons.Default.CheckCircle, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        if (createSource != null && createSource != CreateSource.PICK_FILE && createSource != CreateSource.IMPORT_FOLDER) {
            Spacer(Modifier.height(12.dp))
            if (versionsLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Loading versions...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (versionsError != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(versionsError!!, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { retryTrigger++ }) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Retry")
                }
            } else if (availableVersions.isNotEmpty()) {
                DesktopVersionGrid(
                    selectedVersion = mcVersion,
                    availableVersions = availableVersions,
                    onVersionSelected = { mcVersion = it }
                )

                if (createSource?.supportsBuilds() == true && mcVersion.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    if (buildsLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Loading builds...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else if (buildsError != null) {
                        Text(buildsError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    } else if (availableBuilds.isNotEmpty()) {
                        var buildExpanded by remember { mutableStateOf(false) }
                        val selectedLabel = availableBuilds.find { it.id == selectedBuildId }?.let { b ->
                            b.label
                        } ?: "Latest"
                        ExposedDropdownMenuBox(expanded = buildExpanded, onExpandedChange = { buildExpanded = it }) {
                            OutlinedTextField(
                                value = selectedLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Build") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = buildExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                                singleLine = true
                            )
                            ExposedDropdownMenu(expanded = buildExpanded, onDismissRequest = { buildExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("Latest") },
                                    onClick = { selectedBuildId = ""; buildExpanded = false }
                                )
                                availableBuilds.take(100).forEach { b ->
                                    DropdownMenuItem(
                                        text = { Text(b.label) },
                                        onClick = { selectedBuildId = b.id; buildExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        Card(
            onClick = {
                createSource = CreateSource.PICK_FILE
                scope.launch {
                    val files = pickFile("Select Server JAR", "JAR files" to listOf("jar"))
                    if (files.isNotEmpty()) {
                        jarPath = files.first().absolutePath
                        jarName = files.first().name
                        toastManager.success("Selected: ${files.first().name}")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (createSource == CreateSource.PICK_FILE)
                    MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            )
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Archive, contentDescription = null,
                    tint = if (createSource == CreateSource.PICK_FILE) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pick a JAR file", style = MaterialTheme.typography.titleMedium)
                    Text(if (jarName.isNotBlank() && createSource == CreateSource.PICK_FILE) jarName else "Browse your computer",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.FolderOpen, contentDescription = null)
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            onClick = {
                createSource = CreateSource.IMPORT_FOLDER
                scope.launch {
                    val folder = pickDirectory("Select Server Folder")
                    if (folder != null) {
                        importFolderPath = folder.absolutePath
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (createSource == CreateSource.IMPORT_FOLDER)
                    MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            )
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FolderOpen, contentDescription = null,
                    tint = if (createSource == CreateSource.IMPORT_FOLDER) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Import Server Folder", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (importFolderPath?.isNotBlank() == true && createSource == CreateSource.IMPORT_FOLDER)
                            importFolderPath!!
                        else
                            "Import an existing server directory",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(Icons.Default.FolderOpen, contentDescription = null)
            }
        }

        val cs = createSource
        if (cs != null && cs != CreateSource.PICK_FILE && cs != CreateSource.IMPORT_FOLDER && mcVersion.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text("Selected: ${cs.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }} — $mcVersion",
                color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }

        if (createSource == CreateSource.IMPORT_FOLDER && importFolderPath?.isNotBlank() == true) {
            Spacer(Modifier.height(12.dp))
            if (importReadyToCreate) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Import Ready", style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("Server folder \"$serverName\" detected with JAR and properties. Ready to create.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Will skip Version, Build, RAM, Properties, Storage steps. Go directly to EULA.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null,
                                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Import Incomplete", style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Selected folder must contain a server JAR file. No JAR found in folder.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }

    @Composable
    fun StepServerName() {
        Text("Server Name", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Give your server a memorable name", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = serverName,
            onValueChange = { serverName = it },
            label = { Text("Server Name") },
            placeholder = { Text("My Survival Server") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
        )
    }

    @Composable
    fun StepRamConfig() {
        Text("Memory (RAM)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Allocate memory for your server", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        val javaVersion = selectedJavaVersion.toString()
        val hasJdk = jdkInstallations.any { it.version == selectedJavaVersion }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Java $javaVersion", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            if (hasJdk) {
                Text("✓ Installed", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Not installed", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (!hasJdk && !isJdkInstalling) {
            Spacer(Modifier.height(4.dp))
            Button(onClick = {
                scope.launch {
                    jdkManager.installJdk(selectedJavaVersion).onFailure {
                        errorMessage = "Failed to install JDK: ${it.message}"
                    }
                }
            }) { Text("Install Java $javaVersion") }
        }
        if (isJdkInstalling) {
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(progress = { jdkInstallProgress.toFloat() }, modifier = Modifier.fillMaxWidth())
            Text("Installing Java ${selectedJavaVersion}... ${(jdkInstallProgress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(24.dp))
        Text("Minimum RAM: ${"%.1f".format(minRam)} GB", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Slider(
            value = minRam,
            onValueChange = { v -> minRam = (v / 0.1f).roundToInt() * 0.1f },
            valueRange = 0.5f..maxRam,
            steps = ((maxRam - 0.5f) / 0.1f).roundToInt(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Text("Maximum RAM: ${"%.1f".format(maxRam)} GB", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Slider(
            value = maxRam,
            onValueChange = { v -> maxRam = (v / 0.1f).roundToInt() * 0.1f },
            valueRange = 0.5f..16f,
            steps = 154,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Allocated: ${"%.1f".format(minRam)} GB – ${"%.1f".format(maxRam)} GB",
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    @Composable
    fun StepProperties() {
        Text("Server Properties", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Configure basic server settings", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = port,
            onValueChange = { port = it.filter { c -> c.isDigit() }.take(5) },
            label = { Text("Port") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = { Icon(Icons.Default.Lan, contentDescription = null) }
        )

        Spacer(Modifier.height(12.dp))
        Text("Gamemode", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        var gamemodeExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = gamemodeExpanded, onExpandedChange = { gamemodeExpanded = it }) {
            OutlinedTextField(
                value = gamemode.replaceFirstChar { it.uppercase() },
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gamemodeExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
            )
            ExposedDropdownMenu(expanded = gamemodeExpanded, onDismissRequest = { gamemodeExpanded = false }) {
                gamemodes.forEach { gm ->
                    DropdownMenuItem(
                        text = { Text(gm.replaceFirstChar { it.uppercase() }) },
                        onClick = { gamemode = gm; gamemodeExpanded = false }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Difficulty", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        var difficultyExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = difficultyExpanded, onExpandedChange = { difficultyExpanded = it }) {
            OutlinedTextField(
                value = difficulty.replaceFirstChar { it.uppercase() },
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = difficultyExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
            )
            ExposedDropdownMenu(expanded = difficultyExpanded, onDismissRequest = { difficultyExpanded = false }) {
                difficulties.forEach { diff ->
                    DropdownMenuItem(
                        text = { Text(diff.replaceFirstChar { it.uppercase() }) },
                        onClick = { difficulty = diff; difficultyExpanded = false }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        var showMotdColors by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = motd,
            onValueChange = { motd = if (it.text.length <= 60) it else it.copy(text = it.text.take(60)) },
            label = { Text("MOTD (Message of the Day)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
        )
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
                if (files.isNotEmpty()) iconPath = files[0].absolutePath
            }
        }) {
            Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (iconPath != null) "Change Icon" else "Set Server Icon", style = MaterialTheme.typography.labelSmall)
        }
        if (iconPath != null) {
            Spacer(Modifier.height(4.dp))
            Text("Icon: $iconPath", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }

    @Composable
    fun StepStorageCheck() {
        Text("Storage Check", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Ensure enough space is available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        val sufficient = availableBytes >= requiredBytes
        val availableFormatted = formatBytes(availableBytes)
        val requiredFormatted = formatBytes(requiredBytes)

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (sufficient) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (sufficient) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (sufficient) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (sufficient) "Sufficient storage" else "Insufficient storage",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (sufficient) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text("Available", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(availableFormatted, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text("Required (JAR + world + swap)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(requiredFormatted, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("RAM: ${formatBytes((maxRam * 1024 * 1024 * 1024).toLong())}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(16.dp))
                Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("JAR + overhead: 500 MB", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    @Composable
    fun StepEula() {
        Text("EULA Agreement", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Minecraft End User License Agreement", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "By checking the box below, you agree to the Minecraft End User License Agreement (EULA).\n\n" +
                    "• You may run the server for personal or private use\n" +
                    "• You may not distribute or sell the server software\n" +
                    "• You must comply with Mojang's EULA at https://aka.ms/MinecraftEULA\n\n" +
                    "The eula.txt file will be created with eula=true.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = eulaAccepted, onCheckedChange = { eulaAccepted = it })
            Spacer(Modifier.width(8.dp))
            Text("I agree to the Minecraft EULA", style = MaterialTheme.typography.bodyMedium)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                StepIndicator(current = currentStep, total = totalSteps)
                Spacer(Modifier.height(16.dp))

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (currentStep) {
                        0 -> StepChooseSource()
                        1 -> StepServerName()
                        2 -> StepRamConfig()
                        3 -> StepProperties()
                        4 -> StepStorageCheck()
                        5 -> StepEula()
                    }

                    if (creating) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = downloadStatus.ifBlank { "Preparing..." },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${(downloadProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { downloadProgress.toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    errorMessage?.let { msg ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(8.dp))
                                Text(msg, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep--; errorMessage = null },
                            modifier = Modifier.weight(1f)
                        ) { Text("Back") }
                    }

                    if (currentStep < totalSteps - 1) {
                        Button(
                            onClick = {
                                when (currentStep) {
                                    0 -> {
                                        if (createSource == CreateSource.PICK_FILE && jarPath == null) {
                                            scope.launch {
                                                val files = pickFile("Select Server JAR", "JAR files" to listOf("jar"))
                                                if (files.isNotEmpty()) {
                                                    jarPath = files.first().absolutePath
                                                    jarName = files.first().name
                                                    toastManager.success("Selected: ${files.first().name}")
                                                }
                                            }
                                            return@Button
                                        }
                                        if (createSource == null) {
                                            errorMessage = "Please select a server source"
                                            return@Button
                                        }
                                        if (createSource != CreateSource.PICK_FILE && createSource != CreateSource.IMPORT_FOLDER && mcVersion.isBlank()) {
                                            errorMessage = "Please select a Minecraft version"
                                            return@Button
                                        }
                                        
                                        // Auto-skip to EULA if importing folder with valid server
                                        if (importReadyToCreate) {
                                            currentStep = 5  // Jump to EULA step (step 5)
                                        } else {
                                            currentStep++
                                        }
                                    }
                                    else -> currentStep++
                                }
                                errorMessage = null
                            },
                            enabled = when (currentStep) {
                                1 -> serverName.isNotBlank()
                                4 -> availableBytes >= requiredBytes
                                else -> true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                when {
                                    currentStep == 0 && createSource == CreateSource.PICK_FILE -> "Browse File"
                                    currentStep == 0 && createSource != null -> "Next"
                                    else -> "Next"
                                }
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                if (!eulaAccepted) {
                                    errorMessage = "You must accept the EULA to create a server"
                                    return@Button
                                }
                                creating = true
                                errorMessage = null
                                scope.launch {
                                    val serverTypeLabel = when (createSource) {
                                        CreateSource.DOWNLOAD_PAPER -> "paper"
                                        CreateSource.DOWNLOAD_VANILLA -> "vanilla"
                                        CreateSource.DOWNLOAD_FABRIC -> "fabric"
                                        CreateSource.DOWNLOAD_FORGE -> "forge"
                                        CreateSource.DOWNLOAD_NEOFORGE -> "neoforge"
                                        CreateSource.DOWNLOAD_FOLIA -> "folia"
                                        CreateSource.DOWNLOAD_PURPUR -> "purpur"
                                        else -> "custom"
                                    }
                                    
                                    // Use imported properties when available (for IMPORT_FOLDER)
                                    val importedGamemode = importedServerProps["gamemode"] ?: "survival"
                                    val importedDifficulty = importedServerProps["difficulty"] ?: "easy"
                                    val importedMotd = importedServerProps["motd"] ?: "A Minecraft Server"
                                    val importedPort = importedServerProps["server-port"]?.toIntOrNull() ?: 25565
                                    val importedPvp = importedServerProps["pvp"]?.toBooleanStrictOrNull() ?: true
                                    val importedOnlineMode = importedServerProps["online-mode"]?.toBooleanStrictOrNull() ?: true
                                    val importedWhitelist = importedServerProps["white-list"]?.toBooleanStrictOrNull() ?: false
                                    val importedSpawnProtection = importedServerProps["spawn-protection"] ?: "0"

                                    val config = ServerConfig(
                                        name = serverName.ifBlank { "My Server" },
                                        version = mcVersion.ifBlank { "latest" },
                                        buildId = selectedBuildId,
                                        serverType = createSource?.toServerType() ?: ServerType.PAPER,
                                        source = when (createSource) {
                                            CreateSource.DOWNLOAD_PAPER -> ServerSource.PAPERMC
                                            CreateSource.DOWNLOAD_VANILLA -> ServerSource.OFFICIAL
                                            CreateSource.DOWNLOAD_FABRIC -> ServerSource.FABRICMC
                                            CreateSource.DOWNLOAD_FORGE -> ServerSource.FORGE
                                            CreateSource.DOWNLOAD_NEOFORGE -> ServerSource.NEOFORGE
                                            CreateSource.DOWNLOAD_FOLIA -> ServerSource.FOLIA
                                            CreateSource.DOWNLOAD_PURPUR -> ServerSource.PURPUR
                                            else -> ServerSource.PAPERMC
                                        },
                                        javaVersion = selectedJavaVersion,
                                        memoryMin = (minRam * 1024).toInt(),
                                        memoryMax = (maxRam * 1024).toInt(),
                                        port = if (importReadyToCreate) importedPort else port.toIntOrNull() ?: 25565,
                                        autoRestart = true,
                                        rconEnabled = false,
                                        rconPort = 25575,
                                        properties = mapOf(
                                            "gamemode" to (if (importReadyToCreate) importedGamemode else gamemode),
                                            "difficulty" to (if (importReadyToCreate) importedDifficulty else difficulty),
                                            "motd" to (if (importReadyToCreate) importedMotd else motd.text),
                                            "pvp" to (if (importReadyToCreate) importedPvp.toString() else "true"),
                                            "online-mode" to (if (importReadyToCreate) importedOnlineMode.toString() else "true"),
                                            "white-list" to (if (importReadyToCreate) importedWhitelist.toString() else "false"),
                                            "spawn-protection" to (if (importReadyToCreate) importedSpawnProtection else "0"),
                                        ),
                                    )

                                    if (jarPath != null) {
                                        val sourceDir = if (createSource == CreateSource.IMPORT_FOLDER) File(jarPath!!).parentFile!! else File(jarPath!!)
                                        serverManager.registerImportedServer(config, sourceDir)
                                        toastManager.success("Server \"${config.name}\" imported!")
                                        onServerCreated(config.id)
                                    } else {
                                        serverManager.createServer(config).onFailure {
                                            errorMessage = it.message ?: "Failed to create server"
                                            creating = false
                                            return@launch
                                        }
                                        if (iconPath != null) {
                                            val iconDest = getServerIconFile(java.io.File(fileSystem.getServersDirBlocking(), config.id))
                                            withContext(Dispatchers.IO) {
                                                saveServerIcon(java.io.File(iconPath!!), iconDest)
                                            }
                                        }
                                        toastManager.success("Server \"${config.name}\" created!")
                                        onServerCreated(config.id)
                                    }
                                    withContext(Dispatchers.IO) {
                                        val propsFile = java.io.File(fileSystem.getServersDirBlocking(), "${config.id}/server.properties")
                                        val propsToWrite = mapOf(
                                            "gamemode" to gamemode,
                                            "difficulty" to difficulty,
                                            "motd" to motd.text,
                                            "pvp" to "true",
                                            "online-mode" to "true",
                                            "white-list" to "false",
                                            "spawn-protection" to "0",
                                        )
                                        if (propsFile.exists()) {
                                            val lines = propsFile.readLines().toMutableList()
                                            for ((key, value) in propsToWrite) {
                                                val idx = lines.indexOfFirst { it.startsWith("$key=") }
                                                if (idx >= 0) lines[idx] = "$key=$value"
                                                else lines.add("$key=$value")
                                            }
                                            propsFile.writeText(lines.joinToString("\n") + "\n")
                                        } else {
                                            propsFile.parentFile.mkdirs()
                                            propsFile.writeText(propsToWrite.entries.joinToString("\n") { "${it.key}=${it.value}" } + "\n")
                                        }
                                    }
                                    creating = false
                                    serverName = ""
                                    createSource = null
                                    currentStep = 0
                                }
                            },
                            enabled = eulaAccepted && !creating && !isJdkInstalling,
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            if (creating) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Create Server")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopVersionGrid(
    selectedVersion: String,
    availableVersions: List<String>,
    onVersionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var currentPage by remember { mutableIntStateOf(0) }

    val filteredVersions = remember(availableVersions, searchQuery) {
        if (searchQuery.isBlank()) availableVersions
        else availableVersions.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    val pageSize = 10
    val totalPages = remember(filteredVersions.size) { ceil(filteredVersions.size.toFloat() / pageSize).toInt().coerceAtLeast(1) }
    val pagedVersions = remember(filteredVersions, currentPage) {
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, filteredVersions.size)
        if (start < filteredVersions.size) filteredVersions.subList(start, end) else emptyList()
    }

    LaunchedEffect(searchQuery) { currentPage = 0 }

    Column {
        OutlinedTextField(
            value = if (selectedVersion.isNotBlank()) selectedVersion else "Select version",
            onValueChange = {},
            readOnly = true,
            label = { Text("Minecraft Version") },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp).fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it; currentPage = 0 },
                        placeholder = { Text("Search versions...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { }),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    if (pagedVersions.isEmpty()) {
                        Text(
                            if (searchQuery.isNotBlank()) "No versions match \"$searchQuery\""
                            else "No versions available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth()
                        )
                    } else {
                        val rows = pagedVersions.chunked(5)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            rows.forEach { rowItems ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowItems.forEach { version ->
                                        val isSelected = version == selectedVersion
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                onVersionSelected(version)
                                                expanded = false
                                                searchQuery = ""
                                            },
                                            label = {
                                                Text(
                                                    version,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            modifier = Modifier.width(IntrinsicSize.Min),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (filteredVersions.size > pageSize) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { if (currentPage > 0) currentPage-- },
                                enabled = currentPage > 0
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(2.dp))
                                Text("Prev")
                            }

                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Page ${currentPage + 1} of $totalPages",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))

                            TextButton(
                                onClick = { if (currentPage < totalPages - 1) currentPage++ },
                                enabled = currentPage < totalPages - 1
                            ) {
                                Text("Next")
                                Spacer(Modifier.width(2.dp))
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { expanded = false; searchQuery = "" }) {
                            Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.0f MB".format(bytes / 1_048_576.0)
        bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}
