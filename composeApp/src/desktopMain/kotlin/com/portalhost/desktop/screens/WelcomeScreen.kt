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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.portalhost.java.JdkManager
import com.portalhost.preferences.Preferences
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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
                // Step indicator at top
                StepIndicator(current = currentStep, total = totalSteps)
                Spacer(Modifier.height(32.dp))

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

                Spacer(Modifier.height(32.dp))

                // Navigation buttons
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
                                if (currentStep == 0) {
                                    currentStep++
                                } else if (currentStep == 1 && jdkInstalled) {
                                    currentStep++
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
                            Text(
                                if (currentStep == 1 && jdkInstalling) "Installing..."
                                else if (currentStep == 1 && jdkInstalled) "Next"
                                else "Next"
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
            }
        }
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
                    modifier = Modifier.padding(start = 8.dp).size(18.dp)
                )
            }
        }

        if (progress >= 1.0 && !isInstalling) {
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
            modifier = Modifier.size(120.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(60.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
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
