package com.portalhost.app.ui.screens.create

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.portalhost.app.server.DeviceDetector
import com.portalhost.app.server.JarAnalyzer
import com.portalhost.app.server.RamStatus
import com.portalhost.app.server.ServerDownloader
import com.portalhost.app.ui.model.ServerConfig
import com.portalhost.app.ui.model.ServerRepository
import kotlinx.coroutines.launch
import java.io.File

enum class CreateSource { PICK_FILE, DOWNLOAD_PAPER, DOWNLOAD_VANILLA, DOWNLOAD_FABRIC, DOWNLOAD_FORGE }

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
    var availableVersions by remember { mutableStateOf<List<String>>(emptyList()) }
    var versionsLoading by remember { mutableStateOf(false) }
    var versionsError by remember { mutableStateOf<String?>(null) }
    var minRam by remember { mutableStateOf(1.0f) }
    var maxRam by remember { mutableStateOf(2.0f) }
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

    val context = LocalContext.current
    val deviceSpec = remember {
        DeviceDetector.detect(context)
    }
    val maxRamLimit = remember {
        val cfg = DeviceDetector.generateConfig(deviceSpec)
        DeviceDetector.parseRamMb(cfg.recommendedMaxRam) / 1024f
    }
    fun computeRamStatus(): RamStatus = DeviceDetector.getRamStatus((maxRam * 1024).toInt(), deviceSpec)

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            jarUri = uri
            jarName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIdx >= 0 && cursor.moveToFirst()) cursor.getString(nameIdx) else null
            } ?: try {
                val decoded = java.net.URLDecoder.decode(uri.toString(), "UTF-8")
                val name = decoded.substringAfterLast('/').substringAfterLast(':')
                name.ifBlank { null }
            } catch (_: Exception) { null } ?: "server.jar"
            createSource = CreateSource.PICK_FILE
        }
    }

    LaunchedEffect(jarUri) {
        if (jarUri != null && createSource == CreateSource.PICK_FILE) {
            val detected = JarAnalyzer.detectVersion(context, jarUri!!)
            if (detected.isNotBlank()) mcVersion = detected
        }
    }

    val scope = rememberCoroutineScope()
    val downloader = remember { ServerDownloader() }
    val scrollState = rememberScrollState()

    val step0Complete = createSource != null && downloadError == null && (
        createSource == CreateSource.PICK_FILE || (mcVersion.isNotBlank() && jarTargetPath != null)
    )

    fun startDownload(version: String) {
        val type = createSource ?: return
        downloading = true
        downloadError = null
        scope.launch {
            try {
                val destFile = File(context.cacheDir, "downloads/${type.name.lowercase()}.jar")
                destFile.parentFile?.mkdirs()
                val result = when (type) {
                    CreateSource.DOWNLOAD_PAPER -> {
                        jarName = "paper-$version.jar"
                        downloader.downloadPaper(version, destFile) { read, total ->
                            downloadProgress = read.toFloat() / total.toFloat()
                        }
                    }
                    CreateSource.DOWNLOAD_VANILLA -> {
                        jarName = "vanilla-$version.jar"
                        downloader.downloadVanilla(version, destFile) { read, total ->
                            downloadProgress = read.toFloat() / total.toFloat()
                        }
                    }
                    CreateSource.DOWNLOAD_FABRIC -> {
                        jarName = "fabric-$version-loader.jar"
                        downloader.downloadFabric(version, null, destFile) { read, total ->
                            downloadProgress = read.toFloat() / total.toFloat()
                        }
                    }
                    CreateSource.DOWNLOAD_FORGE -> {
                        val forgeVers = downloader.getForgeVersions()
                        val forgeVer = forgeVers[version] ?: throw Exception("No Forge build for MC $version")
                        jarName = "forge-$version-$forgeVer-server.jar"
                        downloader.downloadForge(version, forgeVer, destFile) { read, total ->
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
                    availableVersions = availableVersions,
                    versionsLoading = versionsLoading,
                    versionsError = versionsError,
                    onVersionChange = { mcVersion = it },
                    onSelectPickFile = { filePickerLauncher.launch(arrayOf("application/java-archive", "application/octet-stream", "*/*")) },
                    onSelectDownload = { type ->
                        createSource = type
                        mcVersion = ""
                        jarName = ""
                        jarTargetPath = null
                        downloadError = null
                        versionsLoading = true
                        versionsError = null
                        availableVersions = emptyList()
                        scope.launch {
                            val (vers, err) = try {
                                val result = when (type) {
                                    CreateSource.DOWNLOAD_PAPER -> downloader.getPaperVersions()
                                    CreateSource.DOWNLOAD_VANILLA -> downloader.getVanillaVersions()
                                    CreateSource.DOWNLOAD_FABRIC -> downloader.getVanillaVersions()
                                    CreateSource.DOWNLOAD_FORGE -> downloader.getForgeVersions().keys.toList()
                                    else -> emptyList()
                                }
                                Pair(result, null)
                            } catch (e: Exception) {
                                Pair(emptyList(), e.message ?: "Failed to fetch versions")
                            }
                            availableVersions = vers
                            versionsError = err
                            versionsLoading = false
                        }
                    }
                )

                1 -> StepServerName(name = serverName, onNameChange = { serverName = it })
                2 -> StepRamConfig(minRam = minRam, maxRam = maxRam, maxRamLimit = maxRamLimit, ramStatus = computeRamStatus(), onMinChange = { minRam = it.coerceAtMost(maxRam) }, onMaxChange = { maxRam = it.coerceAtLeast(minRam) })
                3 -> StepProperties(port = port, gamemode = gamemode, difficulty = difficulty, motd = motd, gamemodes = gamemodes, difficulties = difficulties, onPortChange = { port = it }, onGamemodeChange = { gamemode = it }, onDifficultyChange = { difficulty = it }, onMotdChange = { motd = it })
                4 -> StepStorageCheck(availableBytes = availableStorage, requiredBytes = requiredStorage, maxRam = maxRam, onCheck = {
                    val stat = android.os.StatFs(context.filesDir.absolutePath)
                    availableStorage = stat.availableBlocksLong * stat.blockSizeLong
                    requiredStorage = (maxRam * 1024 * 1024 * 1024).toLong() + 500_000_000L
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
                        onClick = {
                            if (currentStep == 0 && createSource != null && createSource != CreateSource.PICK_FILE && jarTargetPath == null && mcVersion.isNotBlank()) {
                                startDownload(mcVersion)
                            } else {
                                currentStep++
                            }
                        },
                        enabled = when (currentStep) {
                            0 -> step0Complete && !downloading
                            1 -> serverName.isNotBlank()
                            4 -> availableStorage > 0 && requiredStorage > 0 && availableStorage >= requiredStorage
                            else -> true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (currentStep == 0 && createSource != null && createSource != CreateSource.PICK_FILE && jarTargetPath == null && mcVersion.isNotBlank())
                                "Download ${mcVersion} & Next"
                            else if (currentStep == 0 && createSource != null && createSource != CreateSource.PICK_FILE && jarTargetPath != null)
                                "Ready - Next"
                            else
                                "Next"
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            creating = true
                            val serverTypeLabel = when (createSource) {
                                CreateSource.DOWNLOAD_PAPER -> "paper"
                                CreateSource.DOWNLOAD_VANILLA -> "vanilla"
                                CreateSource.DOWNLOAD_FABRIC -> "fabric"
                                CreateSource.DOWNLOAD_FORGE -> "forge"
                                else -> "custom"
                            }
                            val config = ServerConfig(
                                name = serverName.ifBlank { "My Server" },
                                jarPath = "",
                                jarName = jarName.ifBlank { "server.jar" },
                                serverType = serverTypeLabel,
                                mcVersion = mcVersion,
                                minRam = "${(minRam * 1024).toInt()}M",
                                maxRam = "${(maxRam * 1024).toInt()}M",
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
