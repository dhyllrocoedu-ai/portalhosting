package com.portalhost.app.ui.screens.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepChooseSource(
    createSource: CreateSource?,
    jarName: String,
    downloading: Boolean,
    downloadProgress: Float,
    downloadError: String?,
    mcVersion: String,
    availableVersions: List<String>,
    versionsLoading: Boolean,
    versionsError: String?,
    onVersionChange: (String) -> Unit,
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
        Spacer(Modifier.height(8.dp))
        DownloadOptionCard(
            icon = Icons.Default.Build,
            title = "Forge",
            subtitle = "Popular mod loader with extensive mod support",
            selected = createSource == CreateSource.DOWNLOAD_FORGE,
            onClick = { if (!downloading) onSelectDownload(CreateSource.DOWNLOAD_FORGE) }
        )

        // Version picker for download types
        if (createSource != null && createSource != CreateSource.PICK_FILE) {
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
                        Text(versionsError, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else if (availableVersions.isEmpty()) {
                Text("No versions available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            } else if (!downloading && jarName.isBlank()) {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedTextField(
                        value = if (mcVersion.isNotBlank()) mcVersion else "Select version",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Minecraft Version") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                        singleLine = true
                    )
                    // Invisible spacer to capture clicks on the whole field
                    Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        availableVersions.forEach { v ->
                            DropdownMenuItem(
                                text = { Text(v) },
                                onClick = { onVersionChange(v); expanded = false }
                            )
                        }
                    }
                }
                if (mcVersion.isNotBlank()) {
                    Text("Selected: $mcVersion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

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
fun DownloadOptionCard(
    icon: ImageVector,
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
