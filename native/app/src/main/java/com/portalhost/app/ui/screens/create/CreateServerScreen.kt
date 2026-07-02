package com.portalhost.app.ui.screens.create

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.portalhost.app.server.JarAnalyzer
import com.portalhost.app.server.ServerDownloader
import com.portalhost.app.ui.model.ServerConfig
import com.portalhost.app.ui.model.ServerRepository
import kotlinx.coroutines.launch
import java.io.File

enum class CreateSource { PICK_FILE, DOWNLOAD_PAPER, DOWNLOAD_VANILLA, DOWNLOAD_FABRIC }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateServerScreen(
    repository: ServerRepository,
    onCreated: (ServerConfig) -> Unit,
    onBack: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 6

    var createSource by remember { mutableStateOf<CreateSource?>(null) }
    var jarUri by remember { mutableStateOf<Uri?>(null) }
    var jarName by remember { mutableStateOf("") }
    var jarTargetPath by remember { mutableStateOf<String?>(null) }
    var serverName by remember { mutableStateOf("") }
    var mcVersion by remember { mutableStateOf("") }
    var minRam by remember { mutableStateOf("512") }
    var maxRam by remember { mutableStateOf("2048") }
    var port by remember { mutableStateOf("25565") }
    var gamemode by remember { mutableStateOf("survival") }
    var difficulty by remember { mutableStateOf("easy") }
    var motd by remember { mutableStateOf("A Minecraft Server") }
    var eulaAccepted by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var availableStorage by remember { mutableStateOf(0L) }
    var requiredStorage by remember { mutableStateOf(0L) }

    val gamemodes = listOf("survival", "creative", "adventure", "spectator")
    val difficulties = listOf("peaceful", "easy", "normal", "hard")
    val ramOptions = listOf("512", "1024", "2048", "4096", "6144", "8192")
    val ramLabels = listOf("512 MB", "1 GB", "2 GB", "4 GB", "6 GB", "8 GB")

    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            jarUri = uri
            // Query ContentResolver for the actual display name
            jarName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIdx >= 0 && cursor.moveToFirst()) cursor.getString(nameIdx) else null
            } ?: try {
                // Fallback: decode URI path and extract filename
                val decoded = java.net.URLDecoder.decode(uri.toString(), "UTF-8")
                val name = decoded.substringAfterLast('/').substringAfterLast(':')
                name.ifBlank { null }
            } catch (_: Exception) { null } ?: "server.jar"
            createSource = CreateSource.PICK_FILE
        }
    }

    // Auto-detect MC version from picked JAR
    LaunchedEffect(jarUri) {
        if (jarUri != null && createSource == CreateSource.PICK_FILE) {
            val detected = JarAnalyzer.detectVersion(context, jarUri!!)
            if (detected.isNotBlank()) mcVersion = detected
        }
    }

    val scope = rememberCoroutineScope()
    val downloader = remember { ServerDownloader() }
    val scrollState = rememberScrollState()

    // Track whether step 0 selection changes are valid
    val step0Complete = createSource != null && downloadError == null && (createSource == CreateSource.PICK_FILE || jarName.isNotBlank())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Server") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 0) currentStep-- else onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(scrollState)
        ) {
            // Step indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 0 until totalSteps) {
                    Surface(
                        modifier = Modifier.weight(1f).height(4.dp),
                        color = if (i <= currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {}
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Step ${currentStep + 1} of $totalSteps",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            when (currentStep) {
                0 -> StepChooseSource(
                    createSource = createSource,
                    jarName = jarName,
                    downloading = downloading,
                    downloadProgress = downloadProgress,
                    downloadError = downloadError,
                    mcVersion = mcVersion,
                    onSelectPickFile = { filePickerLauncher.launch(arrayOf("application/java-archive", "application/octet-stream", "*/*")) },
                    onSelectDownload = { type ->
                        createSource = type
                        downloading = true
                        downloadError = null
                        scope.launch {
                            try {
                                val destFile = File(context.cacheDir, "downloads/${type.name.lowercase()}.jar")
                                destFile.parentFile?.mkdirs()
                                val result = when (type) {
                                    CreateSource.DOWNLOAD_PAPER -> {
                                        val versions = downloader.getPaperVersions()
                                        if (versions.isEmpty()) throw Exception("No Paper versions found")
                                        val v = mcVersion.ifBlank { versions.first() }
                                        mcVersion = v
                                        jarName = "paper-$v.jar"
                                        downloader.downloadPaper(v, destFile) { read, total ->
                                            downloadProgress = read.toFloat() / total.toFloat()
                                        }
                                    }
                                    CreateSource.DOWNLOAD_VANILLA -> {
                                        val versions = downloader.getVanillaVersions()
                                        if (versions.isEmpty()) throw Exception("No Vanilla versions found")
                                        val v = mcVersion.ifBlank { versions.first() }
                                        mcVersion = v
                                        jarName = "vanilla-$v.jar"
                                        downloader.downloadVanilla(v, destFile) { read, total ->
                                            downloadProgress = read.toFloat() / total.toFloat()
                                        }
                                    }
                                    CreateSource.DOWNLOAD_FABRIC -> {
                                        val versions = downloader.getVanillaVersions()
                                        if (versions.isEmpty()) throw Exception("No Minecraft versions found")
                                        val v = mcVersion.ifBlank { versions.first() }
                                        mcVersion = v
                                        jarName = "fabric-$v-loader.jar"
                                        downloader.downloadFabric(v, null, destFile) { read, total ->
                                            downloadProgress = read.toFloat() / total.toFloat()
                                        }
                                    }
                                    else -> throw Exception("Invalid download type")
                                }
                                if (result.isSuccess) {
                                    jarTargetPath = destFile.absolutePath
                                } else {
                                    downloadError = result.exceptionOrNull()?.message ?: "Download failed"
                                }
                            } catch (e: Exception) {
                                downloadError = e.message ?: "Download failed"
                            } finally {
                                downloading = false
                            }
                        }
                    }
                )

                1 -> StepServerName(name = serverName, onNameChange = { serverName = it })
                2 -> StepRamConfig(minRam = minRam, maxRam = maxRam, ramOptions = ramOptions, ramLabels = ramLabels, onMinChange = { minRam = it }, onMaxChange = { maxRam = it })
                3 -> StepProperties(port = port, gamemode = gamemode, difficulty = difficulty, motd = motd, gamemodes = gamemodes, difficulties = difficulties, onPortChange = { port = it }, onGamemodeChange = { gamemode = it }, onDifficultyChange = { difficulty = it }, onMotdChange = { motd = it })
                4 -> StepStorageCheck(availableBytes = availableStorage, requiredBytes = requiredStorage, maxRam = maxRam, onCheck = {
                    val stat = android.os.StatFs(context.filesDir.absolutePath)
                    availableStorage = stat.availableBlocksLong * stat.blockSizeLong
                    requiredStorage = maxRam.toLongOrNull()?.let { it * 1024 * 1024 + 500_000_000 } ?: 3_000_000_000L
                })
                5 -> StepEula(eulaAccepted = eulaAccepted, onEulaChange = { eulaAccepted = it })
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }
                }

                if (currentStep < totalSteps - 1) {
                    Button(
                        onClick = { currentStep++ },
                        enabled = when (currentStep) {
                            0 -> step0Complete && !downloading
                            1 -> serverName.isNotBlank()
                            4 -> availableStorage > 0 && requiredStorage > 0 && availableStorage >= requiredStorage
                            else -> true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Next")
                    }
                } else {
                    Button(
                        onClick = {
                            creating = true
                            val serverTypeLabel = when (createSource) {
                                CreateSource.DOWNLOAD_PAPER -> "paper"
                                CreateSource.DOWNLOAD_VANILLA -> "vanilla"
                                CreateSource.DOWNLOAD_FABRIC -> "fabric"
                                else -> "custom"
                            }
                            val config = ServerConfig(
                                name = serverName.ifBlank { "My Server" },
                                jarPath = "",
                                jarName = jarName.ifBlank { "server.jar" },
                                serverType = serverTypeLabel,
                                mcVersion = mcVersion,
                                minRam = "${minRam}M",
                                maxRam = "${maxRam}M",
                                port = port.toIntOrNull() ?: 25565,
                                gamemode = gamemode,
                                difficulty = difficulty,
                                motd = motd,
                                eulaAccepted = eulaAccepted
                            )
                            val created = repository.add(config)
                            val serverDir = repository.getServerDir(created.id)
                            serverDir.mkdirs()
                            val targetFile = File(serverDir, "server.jar")
                            try {
                                if (jarTargetPath != null) {
                                    val downloaded = File(jarTargetPath!!)
                                    if (downloaded.exists()) {
                                        downloaded.copyTo(targetFile, overwrite = true)
                                    }
                                } else if (jarUri != null) {
                                    context.contentResolver.openInputStream(jarUri!!)?.use { input ->
                                        targetFile.outputStream().use { output -> input.copyTo(output) }
                                    }
                                }
                                val updated = created.copy(jarPath = targetFile.absolutePath)
                                repository.update(updated)
                                onCreated(updated)
                            } catch (e: Exception) {
                                creating = false
                                onBack()
                            }
                        },
                        enabled = eulaAccepted && !creating && !downloading,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (creating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Create Server")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepChooseSource(
    createSource: CreateSource?,
    jarName: String,
    downloading: Boolean,
    downloadProgress: Float,
    downloadError: String?,
    mcVersion: String,
    onSelectPickFile: () -> Unit,
    onSelectDownload: (CreateSource) -> Unit
) {
    Column {
        Text("Server Software", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Choose a server jar source — download the latest or pick your own file", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        // Download options
        DownloadOptionCard(
            icon = Icons.Default.Description,
            title = "Paper",
            subtitle = "High-performance server software, recommended",
            selected = createSource == CreateSource.DOWNLOAD_PAPER,
            onClick = { if (!downloading) onSelectDownload(CreateSource.DOWNLOAD_PAPER) }
        )
        Spacer(Modifier.height(8.dp))
        DownloadOptionCard(
            icon = Icons.Default.Description,
            title = "Vanilla",
            subtitle = "Official Mojang server jar",
            selected = createSource == CreateSource.DOWNLOAD_VANILLA,
            onClick = { if (!downloading) onSelectDownload(CreateSource.DOWNLOAD_VANILLA) }
        )
        Spacer(Modifier.height(8.dp))
        DownloadOptionCard(
            icon = Icons.Default.Extension,
            title = "Fabric",
            subtitle = "Lightweight mod loader",
            selected = createSource == CreateSource.DOWNLOAD_FABRIC,
            onClick = { if (!downloading) onSelectDownload(CreateSource.DOWNLOAD_FABRIC) }
        )

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        // Pick local file
        Card(
            onClick = { if (!downloading) onSelectPickFile() },
            modifier = Modifier.fillMaxWidth(),
            colors = if (createSource == CreateSource.PICK_FILE)
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            else CardDefaults.cardColors()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Archive, contentDescription = null, tint = if (createSource == CreateSource.PICK_FILE) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pick a JAR file", style = MaterialTheme.typography.titleMedium)
                    Text(if (jarName.isNotBlank() && createSource == CreateSource.PICK_FILE) jarName else "Browse device storage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.FolderOpen, contentDescription = null)
            }
        }

        // Download progress
        if (downloading) {
            Spacer(Modifier.height(16.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Downloading...", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (downloadProgress > 0f) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { downloadProgress }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        // Download error
        downloadError?.let { error ->
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(error, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Show selection
        if (createSource != null && !downloading && downloadError == null) {
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text("Selected: ${createSource.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }} — $jarName", color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

@Composable
private fun DownloadOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = if (selected)
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            } else {
                Icon(Icons.Default.CloudDownload, contentDescription = null)
            }
        }
    }
}

@Composable
private fun StepServerName(name: String, onNameChange: (String) -> Unit) {
    Column {
        Text("Server Name", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Give your server a memorable name", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Server Name") },
            placeholder = { Text("My Survival Server") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
        )
    }
}

@Composable
private fun StepRamConfig(
    minRam: String, maxRam: String,
    ramOptions: List<String>, ramLabels: List<String>,
    onMinChange: (String) -> Unit, onMaxChange: (String) -> Unit
) {
    Column {
        Text("Memory (RAM)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Allocate memory for your server. More RAM = better performance.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Text("Minimum RAM", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        SingleChoiceRamSelector(selected = minRam, options = ramOptions, labels = ramLabels, onSelect = { onMinChange(it); if (ramOptions.indexOf(it) > ramOptions.indexOf(maxRam)) onMaxChange(it) })
        Spacer(Modifier.height(16.dp))
        Text("Maximum RAM", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        SingleChoiceRamSelector(selected = maxRam, options = ramOptions, labels = ramLabels, onSelect = { onMaxChange(it); if (ramOptions.indexOf(it) < ramOptions.indexOf(minRam)) onMinChange(it) })
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Allocated: ${ramLabels[ramOptions.indexOf(minRam)]} – ${ramLabels[ramOptions.indexOf(maxRam)]}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SingleChoiceRamSelector(selected: String, options: List<String>, labels: List<String>, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { index, option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(labels[index], style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}

@Composable
private fun StepProperties(
    port: String, gamemode: String, difficulty: String, motd: String,
    gamemodes: List<String>, difficulties: List<String>,
    onPortChange: (String) -> Unit, onGamemodeChange: (String) -> Unit,
    onDifficultyChange: (String) -> Unit, onMotdChange: (String) -> Unit
) {
    Column {
        Text("Server Properties", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Configure basic server settings", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = port,
            onValueChange = { onPortChange(it.filter { c -> c.isDigit() }.take(5)) },
            label = { Text("Port") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = { Icon(Icons.Default.Lan, contentDescription = null) }
        )
        Spacer(Modifier.height(12.dp))
        Text("Gamemode", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            gamemodes.forEach { gm ->
                FilterChip(
                    selected = gamemode == gm,
                    onClick = { onGamemodeChange(gm) },
                    label = { Text(gm.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Difficulty", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            difficulties.forEach { diff ->
                FilterChip(
                    selected = difficulty == diff,
                    onClick = { onDifficultyChange(diff) },
                    label = { Text(diff.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = motd,
            onValueChange = onMotdChange,
            label = { Text("MOTD") },
            placeholder = { Text("A Minecraft Server") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null) }
        )
    }
}

@Composable
private fun StepStorageCheck(
    availableBytes: Long,
    requiredBytes: Long,
    maxRam: String,
    onCheck: () -> Unit
) {
    LaunchedEffect(Unit) { onCheck() }
    Column {
        Text("Storage Check", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Ensure enough space is available before creating the server", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        if (availableBytes == 0L) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Checking storage...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            val sufficient = availableBytes >= requiredBytes
            val availableFormatted = com.portalhost.app.storage.StorageInfo.formatBytes(availableBytes)
            val requiredFormatted = com.portalhost.app.storage.StorageInfo.formatBytes(requiredBytes)

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
                    StorageRow("Available", availableFormatted)
                    StorageRow("Required (JAR + world + swap)", requiredFormatted)
                }
            }

            if (!sufficient) {
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Free up space or reduce the RAM allocation. The server needs at least 500 MB plus the allocated RAM size.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("RAM: ${com.portalhost.app.storage.StorageInfo.formatBytes((maxRam.toLongOrNull() ?: 2048) * 1024 * 1024)}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(16.dp))
                Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("JAR + overhead: 500 MB", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StorageRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StepEula(eulaAccepted: Boolean, onEulaChange: (Boolean) -> Unit) {
    Column {
        Text("EULA Agreement", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Minecraft Server Software End User License Agreement", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "By checking the box below, you agree to the Minecraft End User License Agreement (EULA). " +
                    "This means:\n\n" +
                    "• You may run the server for personal or private use\n" +
                    "• You may not distribute or sell the server software\n" +
                    "• You must comply with Mojang's EULA at https://aka.ms/MinecraftEULA\n\n" +
                    "The eula.txt file will be created with eula=true.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = eulaAccepted, onCheckedChange = onEulaChange)
            Spacer(Modifier.width(8.dp))
            Text("I agree to the Minecraft EULA", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
