package com.portalhost.desktop.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.filesystem.FileSystem
import com.portalhost.preferences.Preferences
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.awt.FileDialog
import java.awt.Frame
import java.util.concurrent.atomic.AtomicReference

@Composable
fun WelcomeScreen(
    onFinish: () -> Unit
) {
    val preferences = koinInject<Preferences>()
    val fileSystem = koinInject<FileSystem>()

    var currentStep by remember { mutableStateOf(0) }
    var dataDir by remember { mutableStateOf(System.getProperty("user.home") + "/PortalHost") }
    var serverName by remember { mutableStateOf("My First Server") }
    var serverVersion by remember { mutableStateOf("Paper - Latest") }
    var memory by remember { mutableStateOf("2048") }

    var dataDirError by remember { mutableStateOf(false) }
    var serverNameError by remember { mutableStateOf(false) }

    val steps = listOf(
        "Welcome",
        "Data Directory",
        "Create Server",
        "All Set!"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = androidx.compose.material3.MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .width(600.dp)
                    .height(500.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Step indicator
                    StepIndicator(
                        currentStep = currentStep,
                        steps = steps
                    )

                    // Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 48.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        when (currentStep) {
                            0 -> WelcomeContent(
                                title = "Welcome to Portal Host",
                                description = "Manage your Minecraft Java Edition servers with ease. This quick setup will get you started in minutes.",
                                subtitle = "Let's get you set up with your first Minecraft server."
                            )
                            1 -> DataDirectoryStep(
                                dataDir = dataDir,
                                onDataDirChange = { dataDir = it; dataDirError = false },
                                onBrowse = { onBrowseClicked() },
                                error = dataDirError,
                                errorMessage = "Please select a data directory"
                            )
                            2 -> CreateServerStep(
                                serverName = serverName,
                                onServerNameChange = { serverName = it; serverNameError = false },
                                serverVersion = serverVersion,
                                onVersionChange = { serverVersion = it },
                                memory = memory,
                                onMemoryChange = { memory = it },
                                serverNameError = serverNameError,
                                serverNameErrorMessage = "Please enter a server name"
                            )
                            3 -> FinishContent(
                                title = "All Set!",
                                description = "You're ready to go!",
                                subtitle = "Click finish to start managing your server"
                            )
                        }
                    }

                    // Navigation buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (currentStep > 0) {
                            TextButton(onClick = { currentStep--; dataDirError = false; serverNameError = false }) {
                                Text("Back")
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        if (currentStep < 3) {
                            Button(
                                onClick = {
                                    when (currentStep) {
                                        1 -> {
                                            if (dataDir.isBlank()) {
                                                dataDirError = true
                                            } else {
                                                currentStep++
                                            }
                                        }
                                        2 -> {
                                            if (serverName.isBlank()) {
                                                serverNameError = true
                                            } else {
                                                currentStep++
                                            }
                                        }
                                        else -> currentStep++
                                    }
                                }
                            ) {
                                Text("Next")
                                Icon(
                                    imageVector = Icons.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        } else {
                            Button(onClick = {
                                onFinish()
                            }) {
                                Text("Get Started")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(
    currentStep: Int,
    steps: List<String>
) {
    val lastIndex = steps.lastIndex
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, step ->
            if (index > 0) {
                // Connecting line between steps
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(
                            if (index <= currentStep) androidx.compose.material3.MaterialTheme.colorScheme.primary
                            else androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Circle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (index == currentStep) androidx.compose.material3.MaterialTheme.colorScheme.primary
                            else if (index < currentStep) androidx.compose.material3.MaterialTheme.colorScheme.primary
                            else androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    if (index < currentStep) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Completed",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(6.dp)
                                .align(Alignment.Center)
                        )
                    } else {
                        Text(
                            text = (index + 1).toString(),
                            color = if (index == currentStep) androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                            else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxSize()
                                .align(Alignment.Center)
                        )
                    }
                }
                Text(
                    text = step,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = if (index == currentStep) androidx.compose.material3.MaterialTheme.colorScheme.primary
                    else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.width(80.dp)
                )
            }
        }
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
            modifier = Modifier
                .size(120.dp)
                .background(androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(60.dp))
                .align(Alignment.CenterHorizontally)
        ) {
            Icon(
                imageVector = Icons.Filled.Dns,
                contentDescription = null,
                tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
            )
        }
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = description,
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = subtitle,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun DataDirectoryStep(
    dataDir: String,
    onDataDirChange: (String) -> Unit,
    onBrowse: () -> Unit,
    error: Boolean,
    errorMessage: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(60.dp))
                .align(Alignment.CenterHorizontally)
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
            )
        }
        Text(
            text = "Data Directory",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = "Choose where to store your server files. This is where all your server data, worlds, and configurations will be saved.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        OutlinedTextField(
            value = dataDir,
            onValueChange = onDataDirChange,
            label = { Text("Data Directory Path") },
            placeholder = { Text("Select a folder...") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                TextButton(onClick = onBrowse) {
                    Text("Browse")
                }
            },
            singleLine = true,
            isError = error,
            supportingText = { if (error) Text(errorMessage, style = androidx.compose.material3.MaterialTheme.typography.labelSmall) }
        )
        Text(
            text = "Recommended: Use an empty folder on a fast drive (SSD preferred)",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun CreateServerStep(
    serverName: String,
    onServerNameChange: (String) -> Unit,
    serverVersion: String,
    onVersionChange: (String) -> Unit,
    memory: String,
    onMemoryChange: (String) -> Unit,
    serverNameError: Boolean,
    serverNameErrorMessage: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(60.dp))
                .align(Alignment.CenterHorizontally)
        ) {
            Icon(
                imageVector = Icons.Filled.FileDownload,
                contentDescription = null,
                tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
            )
        }
        Text(
            text = "Create Your First Server",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = "Configure your first Minecraft server. You can create and download servers after setup.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = serverName,
                onValueChange = onServerNameChange,
                label = { Text("Server Name") },
                placeholder = { Text("My First Server") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = serverNameError,
                supportingText = { if (serverNameError) Text(serverNameErrorMessage, style = androidx.compose.material3.MaterialTheme.typography.labelSmall) }
            )
            OutlinedTextField(
                value = serverVersion,
                onValueChange = onVersionChange,
                label = { Text("Version") },
                placeholder = { Text("Paper - Latest") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    // TODO: dropdown for versions
                }
            )
            OutlinedTextField(
                value = memory,
                onValueChange = onMemoryChange,
                label = { Text("Memory (MB)") },
                placeholder = { Text("2048") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
private fun FinishContent(
    title: String,
    description: String,
    subtitle: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(60.dp))
                .align(Alignment.CenterHorizontally)
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
            )
        }
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = description,
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = subtitle,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private fun onBrowseClicked() {
    // Use atomic reference to pass result back from EDT
    val result = AtomicReference<String?>(null)
    val frame = Frame()
    frame.isVisible = false
    
    val dialog = FileDialog(frame, "Select Data Directory", FileDialog.LOAD)
    dialog.setDirectory(System.getProperty("user.home"))
    dialog.setVisible(true)
    
    val selectedDir = dialog.directory
    val selectedFile = dialog.file
    if (selectedDir != null && selectedFile != null) {
        val fullPath = java.io.File(selectedDir, selectedFile).absolutePath
        result.set(fullPath)
    }
    frame.dispose()
}