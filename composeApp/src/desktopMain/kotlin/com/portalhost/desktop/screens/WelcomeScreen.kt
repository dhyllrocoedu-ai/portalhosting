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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.java.JdkManager
import com.portalhost.preferences.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("EXPERIMENTAL_API_USAGE")
@Composable
fun WelcomeScreen(
    onFinish: () -> Unit
) {
    val preferences = koinInject<Preferences>()
    val jdkManager = koinInject<JdkManager>()
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 3

    var jdkInstalling by remember { mutableStateOf(false) }
    var jdkProgress by remember { mutableStateOf(0.0) }
    var jdkError by remember { mutableStateOf<String?>(null) }
    var jdkInstalled by remember { mutableStateOf(false) }

    LaunchedEffect(jdkInstalling) {
        if (jdkInstalling) {
            jdkManager.installProgress.collect { progress ->
                jdkProgress = progress
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Welcome to Portal Host", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))

                // Step indicator
                StepIndicator(current = currentStep, total = totalSteps)
                Spacer(Modifier.height(16.dp))

                // Step content
                when (currentStep) {
                    0 -> WelcomeContent(
                        title = "Welcome to Portal Host",
                        description = "Manage your Minecraft Java Edition servers with ease. This quick setup will install the required Java runtime and get you started in minutes.",
                        subtitle = "Let's begin by installing Java 21, which is required to run Minecraft servers."
                    )
                    1 -> JdkInstallStep(
                        isInstalling = jdkInstalling,
                        progress = jdkProgress,
                        error = jdkError,
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
                        }
                    )
                    2 -> FinishContent(
                        title = "All Set!",
                        description = "Java 21 is installed and ready. You can now create and manage your Minecraft servers.",
                        subtitle = "Click Get Started to open the dashboard"
                    )
                }

                // Error message
                jdkError?.let { msg ->
                    Spacer(Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = null,
                                tint = androidx.compose.material3.MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(msg, color = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(16.dp))

                // Navigation buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep--; jdkError = null },
                            modifier = Modifier.weight(1f)
                        ) { Text("Back") }
                    }

                    if (currentStep < totalSteps - 1) {
                        Button(
                            onClick = {
                                when (currentStep) {
                                    0 -> currentStep++
                                    1 -> {
                                        if (!jdkInstalling && !jdkInstalled) {
                                            // Trigger install
                                        }
                                    }
                                }
                                jdkError = null
                            },
                            enabled = when (currentStep) {
                                0 -> true
                                1 -> jdkInstalled
                                else -> true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (currentStep == 1 && jdkInstalling) "Installing..." else "Next")
                            Icon(
                                imageVector = Icons.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = { onFinish() },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Get Started")
                        }
                    }
                }
            }
        }
    }

    // Auto-advance when JDK install completes
    LaunchedEffect(jdkInstalled) {
        if (jdkInstalled) {
            currentStep = 2
        }
    }

    // Trigger JDK install when entering step 1
    LaunchedEffect(currentStep) {
        if (currentStep == 1 && !jdkInstalling && !jdkInstalled) {
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
}

@Composable
private fun StepIndicator(current: Int, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 0 until total) {
            Surface(
                modifier = Modifier.weight(1f).height(4.dp),
                shape = RoundedCornerShape(2.dp),
                color = if (i <= current) androidx.compose.material3.MaterialTheme.colorScheme.primary
                else androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
            ) {}
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "Step ${current + 1} of $total",
        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
    )
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
private fun JdkInstallStep(
    isInstalling: Boolean,
    progress: Double,
    error: String?,
    onInstall: () -> Unit
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
                imageVector = Icons.Filled.Download,
                contentDescription = null,
                tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
            )
        }
        Text(
            text = "Install Java 21",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = "Minecraft servers require Java 21 to run. Portal Host will download and install it automatically.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        if (isInstalling) {
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = progress.toFloat(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Downloading and installing Java 21... ${(progress * 100).toInt()}%",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        } else if (!isInstalling && progress == 0.0) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onInstall,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            ) {
                Text("Install Java 21")
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        if (progress >= 1.0) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Java 21 installed successfully!",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary
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