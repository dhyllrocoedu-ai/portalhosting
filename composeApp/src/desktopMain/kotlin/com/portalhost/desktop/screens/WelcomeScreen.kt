package com.portalhost.desktop.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.portalhost.db.defaultDataDir
import com.portalhost.java.JdkManager
import com.portalhost.native.NativeFilePicker
import com.portalhost.native.PickConfig
import com.portalhost.preferences.Preferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File

@Composable
fun WelcomeScreen(
    onFinish: () -> Unit
) {
    val preferences = koinInject<Preferences>()
    val jdkManager = koinInject<JdkManager>()
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 4

    var jdkInstalling by remember { mutableStateOf(false) }
    var jdkProgress by remember { mutableStateOf(0.0) }
    var jdkError by remember { mutableStateOf<String?>(null) }
    var jdkInstalled by remember { mutableStateOf(false) }
    var jdkSkipped by remember { mutableStateOf(false) }

    var dataFolderCreated by remember { mutableStateOf(false) }
    var selectedDataDir by remember { mutableStateOf("") }
    var prefsConfigured by remember { mutableStateOf(false) }
    var showSkipDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        selectedDataDir = preferences.dataDirectory.value.ifBlank {
            defaultDataDir().absolutePath
        }
    }

    LaunchedEffect(jdkInstalling) {
        if (jdkInstalling) {
            jdkManager.installProgress.collect { progress ->
                jdkProgress = progress
            }
        }
    }

    LaunchedEffect(currentStep) {
        when (currentStep) {
            1 -> {
            }
            2 -> {
                if (!jdkInstalled && !jdkSkipped && !jdkInstalling) {
                    scope.launch {
                        jdkInstalling = true
                        jdkProgress = 0.0
                        jdkError = null
                        jdkManager.installJdk(21).onFailure { e ->
                            jdkError = e.message ?: "Failed to install JDK"
                            jdkInstalling = false
                        }.onSuccess {
                            jdkInstalled = true
                            jdkProgress = 1.0
                            jdkInstalling = false
                        }
                    }
                }
            }
            3 -> {
                delay(300)
                prefsConfigured = true
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.widthIn(max = 560.dp).fillMaxHeight().padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                StepIndicator(current = currentStep, total = totalSteps)
                Spacer(Modifier.height(32.dp))

                when (currentStep) {
                    0 -> WelcomeContent(
                        title = "Welcome to Portal Host",
                        description = "Manage your Minecraft Java Edition servers with ease. This quick setup will configure your data folder, install the Java runtime, and get you started in minutes.",
                        subtitle = "Click Next to begin the setup process."
                    )
                    1 -> DataFolderStep(
                        currentPath = selectedDataDir,
                        onPathChange = { selectedDataDir = it },
                        onConfirm = {
                            preferences.dataDirectory.value = selectedDataDir
                            dataFolderCreated = true
                        }
                    )
                    2 -> JdkInstallStep(
                        isInstalling = jdkInstalling,
                        progress = jdkProgress,
                        error = jdkError,
                        jdkInstalled = jdkInstalled,
                        onInstall = {
                            scope.launch {
                                jdkInstalling = true
                                jdkProgress = 0.0
                                jdkError = null
                                jdkManager.installJdk(21).onFailure { e ->
                                    jdkError = e.message ?: "Failed to install JDK"
                                    jdkInstalling = false
                                }.onSuccess {
                                    jdkInstalled = true
                                    jdkProgress = 1.0
                                    jdkInstalling = false
                                }
                            }
                        },
                        onSkip = { showSkipDialog = true }
                    )
                    3 -> SetupStepContent(
                        icon = Icons.Default.Settings,
                        title = "Configuration",
                        description = "Default preferences have been configured. You can customize everything later in Settings.",
                        isComplete = prefsConfigured,
                        statusText = if (prefsConfigured) "Configuration ready" else "Configuring preferences..."
                    )
                }

                jdkError?.let { msg ->
                    Spacer(Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(msg, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                if (currentStep == 2 && jdkSkipped) {
                    Spacer(Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("JDK not installed — server management may be limited. You can install Java later from Settings.", color = MaterialTheme.colorScheme.onTertiaryContainer, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = {
                                currentStep--
                                jdkError = null
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Back") }
                    }

                    if (currentStep < totalSteps - 1) {
                        Button(
                            onClick = {
                                if (currentStep == 0 || currentStep == 1) {
                                    currentStep++
                                } else if (currentStep == 2 && (jdkInstalled || jdkSkipped)) {
                                    currentStep++
                                }
                                jdkError = null
                            },
                            enabled = when (currentStep) {
                                0 -> true
                                1 -> dataFolderCreated
                                2 -> jdkInstalled || jdkSkipped
                                else -> true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                when {
                                    currentStep == 2 && jdkInstalling -> "Installing..."
                                    else -> "Next"
                                }
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.padding(start = 8.dp).size(18.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = { onFinish() },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Get Started")
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.padding(start = 8.dp).size(18.dp)
                            )
                        }
                    }
                }

                if (currentStep < totalSteps - 1) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { onFinish() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Skip for now", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showSkipDialog) {
        AlertDialog(
            onDismissRequest = { showSkipDialog = false },
            title = { Text("Skip JDK Installation?") },
            text = { Text("Without Java 21, you won't be able to start Minecraft servers. You can install it later from Settings.") },
            confirmButton = {
                TextButton(onClick = {
                    jdkSkipped = true
                    jdkInstalling = false
                    jdkError = null
                    showSkipDialog = false
                }) { Text("Skip") }
            },
            dismissButton = {
                TextButton(onClick = { showSkipDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StepIndicator(current: Int, total: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 0 until total) {
                Box(
                    modifier = Modifier.weight(1f).height(4.dp).background(
                        if (i <= current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(2.dp)
                    )
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Step ${current + 1} of $total",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WelcomeContent(
    title: String,
    description: String,
    subtitle: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(120.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(60.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Dns,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(64.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SetupStepContent(
    icon: ImageVector,
    title: String,
    description: String,
    isComplete: Boolean,
    statusText: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(120.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(60.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(64.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isComplete) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            } else {
                LinearProgressIndicator(modifier = Modifier.width(100.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(statusText, style = MaterialTheme.typography.bodyMedium, color = if (isComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun JdkInstallStep(
    isInstalling: Boolean,
    progress: Double,
    error: String?,
    jdkInstalled: Boolean,
    onInstall: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(120.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(60.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(64.dp)
            )
        }
        Text(
            text = "Install Java 21",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Minecraft servers require Java 21 to run. Portal Host will download and install it automatically.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (isInstalling) {
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress.toFloat() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Downloading and installing Java 21... ${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        } else if (!isInstalling && progress == 0.0 && !jdkInstalled) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onInstall,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            ) {
                Text("Install Java 21")
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp).size(18.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            ) {
                Text("Skip for now")
            }
        }

        if (jdkInstalled) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Java 21 installed successfully!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DataFolderStep(
    currentPath: String,
    onPathChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    val filePicker = remember { NativeFilePicker() }
    val scope = rememberCoroutineScope()
    var isConfirming by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(120.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(60.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(64.dp)
            )
        }
        Text(
            text = "Data Folder",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Choose where Portal Host will store server data, JDKs, and configurations.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Current location:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentPath,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val result = filePicker.pickDirectory(PickConfig(title = "Select Data Folder"))
                                result.getOrNull()?.let { uri ->
                                    onPathChange(uri.path)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Browse")
                    }
                }
            }
        }

        Button(
            onClick = {
                isConfirming = true
                onConfirm()
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        ) {
            Text("Use This Location")
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp).size(18.dp)
            )
        }
    }
}
