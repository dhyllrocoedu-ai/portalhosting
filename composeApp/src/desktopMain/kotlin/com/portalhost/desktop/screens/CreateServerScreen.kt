package com.portalhost.desktop.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.portalhost.java.JdkManager
import java.io.File
import com.portalhost.model.ServerConfig
import com.portalhost.model.ServerType
import com.portalhost.model.ServerVersion
import com.portalhost.server.ServerDownloader
import com.portalhost.server.ServerManager
import com.portalhost.server.providers.ServerProviderRegistry
import com.portalhost.uinotify.ToastManager
import com.portalhost.util.pickFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateServerScreen() {
    val serverManager = koinInject<ServerManager>()
    val serverDownloader = koinInject<ServerDownloader>()
    val providerRegistry = koinInject<ServerProviderRegistry>()
    val jdkManager = koinInject<JdkManager>()
    val scope = rememberCoroutineScope()
    val toastManager = koinInject<ToastManager>()

    val downloadProgress by serverDownloader.downloadProgress.collectAsState()
    val downloadStatus by serverDownloader.currentStatus.collectAsState()
    
    val jdkInstallations by jdkManager.knownInstallations.collectAsState()
    val isJdkInstalling by jdkManager.isInstalling.collectAsState()
    val jdkInstallProgress by jdkManager.installProgress.collectAsState()

    var serverName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ServerType.PAPER) }
    var selectedVersion by remember { mutableStateOf<String?>(null) }
    var selectedVersionBuild by remember { mutableStateOf<String?>(null) }
    var selectedJavaVersion by remember { mutableStateOf(21) }
    var memoryMin by remember { mutableStateOf(1024f) }
    var memoryMax by remember { mutableStateOf(4096f) }
    var port by remember { mutableStateOf("25565") }
    var autoRestart by remember { mutableStateOf(false) }
    var rconEnabled by remember { mutableStateOf(false) }
    var rconPort by remember { mutableStateOf("25575") }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    var importJarPath by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }
    var versionExpanded by remember { mutableStateOf(false) }
    var javaVersionExpanded by remember { mutableStateOf(false) }

    val versions = remember { mutableStateOf<List<ServerVersion>>(emptyList()) }
    val isLoadingVersions = remember { mutableStateOf(false) }

    LaunchedEffect(selectedType) {
        isLoadingVersions.value = true
        selectedVersion = null
        selectedVersionBuild = null
        versions.value = emptyList()
        val providers = providerRegistry.getProvidersForType(selectedType)
        if (providers.isNotEmpty()) {
            val result = providers.first().fetchVersions()
            result.onSuccess { fetched ->
                versions.value = fetched
                if (fetched.isNotEmpty()) {
                    selectedVersion = fetched.first().version
                }
            }
            result.onFailure {
                errorMessage = "Failed to fetch versions: ${it.message}"
            }
        }
        isLoadingVersions.value = false
    }

    // Set recommended Java version based on server type
    LaunchedEffect(selectedType) {
        selectedJavaVersion = jdkManager.getRecommendedJavaVersion(selectedType)
    }

    // Refresh JDK list on screen enter
    LaunchedEffect(Unit) {
        jdkManager.refresh()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Create New Server", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                OutlinedTextField(
                    value = serverName,
                    onValueChange = { serverName = it },
                    label = { Text("Server Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("My Awesome Server") },
                )

                Spacer(Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Server Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        ServerType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    selectedType = type
                                    typeExpanded = false
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (isLoadingVersions.value) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Loading versions...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (versions.value.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = versionExpanded,
                        onExpandedChange = { versionExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedVersion ?: "Select version",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Version") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = versionExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                        )
                        ExposedDropdownMenu(expanded = versionExpanded, onDismissRequest = { versionExpanded = false }) {
                            versions.value.forEach { ver ->
                                DropdownMenuItem(
                                    text = { Text("${ver.version} ${if (!ver.stable) "(snapshot)" else ""}") },
                                    onClick = {
                                        selectedVersion = ver.version
                                        versionExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Import JAR option
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text("Or import a local server JAR", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = importJarPath?.let { File(it).name } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Selected JAR") },
                        placeholder = { Text("No file selected") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Button(onClick = { showImportDialog = true }) {
                        Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Browse")
                    }
                    if (importJarPath != null) {
                        TextButton(onClick = { importJarPath = null }) {
                            Text("Clear")
                        }
                    }
                }
                if (importJarPath != null) {
                    Text("A local JAR will be used instead of downloading.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // JDK Version Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExposedDropdownMenuBox(
                        expanded = javaVersionExpanded,
                        onExpandedChange = { javaVersionExpanded = it },
                        modifier = Modifier.weight(1f),
                    ) {
                        OutlinedTextField(
                            value = "Java ${selectedJavaVersion}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Java Version") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = javaVersionExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                        )
                        ExposedDropdownMenu(expanded = javaVersionExpanded, onDismissRequest = { javaVersionExpanded = false }) {
                            listOf(8, 11, 17, 21).forEach { version ->
                                val isInstalled = jdkInstallations.any { it.version == version }
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("Java $version ${if (version == 21) "(LTS)" else if (version == 17) "(LTS)" else ""}")
                                            if (isInstalled) {
                                                Text("✓ Installed", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedJavaVersion = version
                                        javaVersionExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    // Install JDK Button
                    if (jdkInstallations.none { it.version == selectedJavaVersion }) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val result = jdkManager.installJdk(selectedJavaVersion)
                                    result.onFailure {
                                        errorMessage = "Failed to install JDK $selectedJavaVersion: ${it.message}"
                                    }
                                }
                            },
                            enabled = !isJdkInstalling,
                        ) {
                            if (isJdkInstalling) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(Modifier.width(4.dp))
                            } else {
                                Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(if (isJdkInstalling) "Installing..." else "Install")
                        }
                    }
                }

                if (isJdkInstalling) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { jdkInstallProgress.toFloat() }, modifier = Modifier.fillMaxWidth())
                    Text("Downloading and installing Java ${selectedJavaVersion}... ${(jdkInstallProgress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Text("Memory", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("Minimum: ${memoryMin.toInt()} MB", style = MaterialTheme.typography.bodySmall)
                Slider(value = memoryMin, onValueChange = { memoryMin = it }, valueRange = 256f..16384f, steps = 63, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Text("Maximum: ${memoryMax.toInt()} MB", style = MaterialTheme.typography.bodySmall)
                Slider(value = memoryMax, onValueChange = { memoryMax = it }, valueRange = 256f..16384f, steps = 63, modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { c -> c.isDigit() } },
                    label = { Text("Port") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().widthIn(max = 200.dp),
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Auto-restart on crash")
                    Switch(checked = autoRestart, onCheckedChange = { autoRestart = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Enable RCON")
                    Switch(checked = rconEnabled, onCheckedChange = { rconEnabled = it })
                }

                if (rconEnabled) {
                    OutlinedTextField(
                        value = rconPort,
                        onValueChange = { rconPort = it.filter { c -> c.isDigit() } },
                        label = { Text("RCON Port") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().widthIn(max = 200.dp),
                    )
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                if (downloadProgress > 0 && downloadProgress < 1.0) {
                    LinearProgressIndicator(progress = { downloadProgress.toFloat() }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    Text(downloadStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                }

                if (isJdkInstalling) {
                    LinearProgressIndicator(progress = { jdkInstallProgress.toFloat() }, modifier = Modifier.fillMaxWidth())
                    Text("Installing Java ${selectedJavaVersion}... ${(jdkInstallProgress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                }

                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { errorMessage = null }) { Text("Dismiss") }
                    Spacer(Modifier.height(8.dp))
                }

                if (successMessage != null) {
                    Text(successMessage!!, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { successMessage = null }) { Text("Dismiss") }
                    Spacer(Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        if (serverName.isBlank()) {
                            errorMessage = "Server name is required"
                            return@Button
                        }
                        if (selectedVersion == null) {
                            errorMessage = "Please select a version"
                            return@Button
                        }
                        isCreating = true
                        errorMessage = null
                        successMessage = null
                        scope.launch {
                            val localJar = importJarPath?.let { File(it) }
                            if (localJar != null && !localJar.exists()) {
                                errorMessage = "Selected JAR file not found: ${localJar.name}"
                                isCreating = false
                                return@launch
                            }

                            val config = ServerConfig(
                                name = serverName.trim(),
                                version = localJar?.name ?: selectedVersion!!,
                                serverType = selectedType,
                                source = when (selectedType) {
                                    ServerType.VANILLA -> com.portalhost.model.ServerSource.OFFICIAL
                                    ServerType.PAPER -> com.portalhost.model.ServerSource.PAPERMC
                                    ServerType.FABRIC -> com.portalhost.model.ServerSource.FABRICMC
                                    ServerType.FORGE -> com.portalhost.model.ServerSource.FORGE
                                    ServerType.NEOFORGE -> com.portalhost.model.ServerSource.NEOFORGE
                                    ServerType.PURPUR -> com.portalhost.model.ServerSource.PURPUR
                                    ServerType.FOLIA -> com.portalhost.model.ServerSource.FOLIA
                                },
                                javaVersion = selectedJavaVersion,
                                memoryMin = memoryMin.toInt(),
                                memoryMax = memoryMax.toInt(),
                                port = port.toIntOrNull() ?: 25565,
                                autoRestart = autoRestart,
                                rconEnabled = rconEnabled,
                                rconPort = rconPort.toIntOrNull() ?: 25575,
                            )

                            if (localJar != null) {
                                val targetDir = java.io.File(System.getProperty("user.dir"), "servers")
                                targetDir.mkdirs()
                                val targetFile = java.io.File(targetDir, "${config.id}.jar")
                                withContext(Dispatchers.IO) {
                                    localJar.copyTo(targetFile, overwrite = true)
                                }
                                serverManager.registerImportedServer(config, targetFile)
                                successMessage = "Server \"${config.name}\" imported successfully!"
                                importJarPath = null
                                serverName = ""
                                isCreating = false
                            } else {
                                val result = serverManager.createServer(config)
                                result.onSuccess {
                                    successMessage = "Server \"${config.name}\" created successfully!"
                                    serverName = ""
                                    isCreating = false
                                }
                                result.onFailure {
                                    errorMessage = "Failed to create server: ${it.message}"
                                    isCreating = false
                                }
                            }
                        }
                    },
                    enabled = !isCreating && serverName.isNotBlank() && selectedVersion != null && !isJdkInstalling,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Create Server")
                }
            }
        }
    }

    // Import JAR dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Server JAR") },
            text = { Text("Select a Minecraft server JAR file (.jar) from your local filesystem.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val files = pickFile(
                            title = "Select Server JAR",
                            extensionFilter = "JAR files" to listOf("jar"),
                        )
                        if (files.isNotEmpty()) {
                            importJarPath = files.first().absolutePath
                            toastManager.success("Selected: ${files.first().name}")
                        }
                        showImportDialog = false
                    }
                }) { Text("Browse") }
            },
            dismissButton = { TextButton(onClick = { showImportDialog = false }) { Text("Cancel") } },
        )
    }
}