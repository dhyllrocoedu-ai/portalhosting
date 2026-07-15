package com.portalhost.app.ui.screens.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.portalhost.app.server.providers.BuildInfo
import kotlin.math.ceil

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
    selectedBuildId: String,
    availableBuilds: List<BuildInfo>,
    buildsLoading: Boolean,
    buildsError: String?,
    showBuildPicker: Boolean,
    onVersionChange: (String) -> Unit,
    onBuildChange: (String) -> Unit,
    onSelectPickFile: () -> Unit,
    onSelectDownload: (CreateSource) -> Unit
) {
    Column {
        Text("Server Software", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Choose a server jar source — download the latest or pick your own file", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

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

        // Version and build pickers for download types
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(versionsError, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { onSelectDownload(createSource) }) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Retry")
                }
            } else if (availableVersions.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text("No versions available. The server may not have published any releases yet.", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { onSelectDownload(createSource) }) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Refresh")
                }
            } else if (!downloading && jarName.isBlank()) {
                VersionGridPicker(
                    selectedVersion = mcVersion,
                    availableVersions = availableVersions,
                    onVersionSelected = onVersionChange
                )

                if (mcVersion.isNotBlank()) {
                    Text("Selected: $mcVersion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }

                // Build picker (for Paper, Fabric, Forge)
                if (showBuildPicker && mcVersion.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    if (buildsLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Loading builds...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else if (buildsError != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(buildsError, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    } else if (availableBuilds.isNotEmpty()) {
                        var buildExpanded by remember { mutableStateOf(false) }
                        val selectedLabel = availableBuilds.find { it.id == selectedBuildId }?.let { b ->
                            b.label
                        } ?: "Latest"

                        Box {
                            OutlinedTextField(
                                value = selectedLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Build") },
                                trailingIcon = {
                                    IconButton(onClick = { buildExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select build")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            DropdownMenu(expanded = buildExpanded, onDismissRequest = { buildExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("Latest") },
                                    onClick = { onBuildChange(""); buildExpanded = false }
                                )
                                availableBuilds.take(100).forEach { b ->
                                    DropdownMenuItem(
                                        text = { Text(b.label) },
                                        onClick = { onBuildChange(b.id); buildExpanded = false }
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

        // Download error with retry
        downloadError?.let { error ->
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(error, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Show selection
        if (createSource != null && !downloading && downloadError == null && jarName.isNotBlank()) {
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

@Composable
fun VersionGridPicker(
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

    // Reset page when search changes
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
                    // Search field
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
                        keyboardActions = KeyboardActions(onSearch = { /* no-op */ }),
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
                        // 5-column grid using FlowRow
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            maxItemsInEachRow = 5
                        ) {
                            pagedVersions.forEach { version ->
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

                    // Pagination
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

                    // Close button
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
