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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.portalhost.server.ServerManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File

@Composable
fun WelcomeScreen(
    onFinish: () -> Unit
) {
    val preferences = koinInject<Preferences>()
    val fileSystem = koinInject<FileSystem>()
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf(0) }
    var dataDir by remember { mutableStateOf("") }
    var downloadServer by remember { mutableStateOf(false) }
    var serverName by remember { mutableStateOf("My First Server") }
    var serverVersion by remember { mutableStateOf("Paper - Latest") }
    var memory by remember { mutableStateOf("2048") }

    val steps = listOf(
        "Welcome",
        "Data Directory",
        "Create Server",
        "All Set!"
    )

    val maxStep = steps.lastIndex

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 24.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            steps.forEachIndexed { index, step ->
                                StepIndicator(
                                    number = index + 1,
                                    label = step,
                                    isActive = index == currentStep,
                                    isCompleted = index < currentStep,
                                    isLast = index == steps.lastIndex
                                )
                            }
                        }

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
                                    onDataDirChange = { dataDir = it },
                                    onBrowse = { /* TODO: implement folder picker */ }
                                )
                                2 -> CreateServerStep(
                                    serverName = serverName,
                                    onServerNameChange = { serverName = it },
                                    serverVersion = serverVersion,
                                    onVersionChange = { serverVersion = it },
                                    memory = memory,
                                    onMemoryChange = { memory = it },
                                    onDownloadServer = { downloadServer = true }
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
                                TextButton(onClick = { currentStep-- }) {
                                    Text("Back")
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            if (currentStep < 3) {
                                Button(
                                    onClick = {
                                        if (currentStep == 1 && dataDir.isBlank()) {
                                            // TODO: show error
                                        } else if (currentStep == 2 && serverName.isBlank()) {
                                            // TODO: show error
                                        } else {
                                            currentStep++
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
}

@Composable
private fun StepIndicator(
    number: Int,
    label: String,
    isActive: Boolean,
    isCompleted: Boolean,
    isLast: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (!isLast) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(2.dp)
                    .background(
                        if (isCompleted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    if (isActive) MaterialTheme.colorScheme.primary
                    else if (isCompleted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .clip(RoundedCornerShape(16.dp))
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Completed",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(6.dp)
                        .align(Alignment.Center)
                )
            } else {
                Text(
                    text = number.toString(),
                    color = if (isActive) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.width(80.dp)
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
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(60.dp))
                .align(Alignment.CenterHorizontally)
        ) {
            Icon(
                imageVector = Icons.Filled.Dns,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun DataDirectoryStep(
    dataDir: String,
    onDataDirChange: (String) -> Unit,
    onBrowse: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(60.dp))
                .align(Alignment.CenterHorizontally)
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
            )
        }
        Text(
            text = "Data Directory",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = "Choose where to store your server files. This is where all your server data, worlds, and configurations will be saved.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            singleLine = true
        )
        Text(
            text = "Recommended: Use an empty folder on a fast drive (SSD preferred)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
    onDownloadServer: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(60.dp))
                .align(Alignment.CenterHorizontally)
        ) {
            Icon(
                imageVector = Icons.Filled.FileDownload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
            )
        }
        Text(
            text = "Create Your First Server",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = "Configure your first Minecraft server. We'll download the server JAR for you.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                singleLine = true
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
            Button(
                onClick = onDownloadServer,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Download & Create Server", fontWeight = FontWeight.Medium)
                Icon(
                    imageVector = Icons.Filled.FileDownload,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
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
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(60.dp))
                .align(Alignment.CenterHorizontally)
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

data class WelcomeStep(
    val title: String,
    val description: String,
    val subtitle: String
)