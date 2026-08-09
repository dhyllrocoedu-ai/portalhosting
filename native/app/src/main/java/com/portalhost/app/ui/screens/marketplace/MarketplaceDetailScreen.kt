package com.portalhost.app.ui.screens.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.portalhost.app.notifications.AppNotifier
import com.portalhost.app.server.ServerDownloader
import com.portalhost.app.ui.model.ServerConfig
import com.portalhost.app.ui.screens.create.NativeVersionPickerSheet
import com.portalhost.marketplace.MarketplaceRepository
import com.portalhost.marketplace.formatDownloads
import com.portalhost.marketplace.getSuggestedFolder
import com.portalhost.marketplace.isProjectCompatible
import com.portalhost.marketplace.isVersionCompatible
import com.portalhost.model.ModrinthProject
import com.portalhost.model.ModrinthVersion
import com.portalhost.model.ServerInstallTarget
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceDetailScreen(
    projectId: String,
    servers: List<ServerConfig>,
    getServerDir: (String) -> File,
    notifier: AppNotifier,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val repository = remember { MarketplaceRepository() }
    val downloader = remember { ServerDownloader() }
    val snackbarHostState = remember { SnackbarHostState() }

    var project by remember { mutableStateOf<ModrinthProject?>(null) }
    var versions by remember { mutableStateOf<List<ModrinthVersion>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedVersion by remember { mutableStateOf<ModrinthVersion?>(null) }
    var showInstallDialog by remember { mutableStateOf(false) }
    var installingServer by remember { mutableStateOf<ServerConfig?>(null) }
    var downloadProgress by remember { mutableStateOf(0f) }

    // Version filters
    var filterGameVersion by remember { mutableStateOf<String?>(null) }
    var filterLoader by remember { mutableStateOf<String?>(null) }
    var gameVersionSheetOpen by remember { mutableStateOf(false) }
    var loaderMenuOpen by remember { mutableStateOf(false) }

    val allGameVersions = remember(versions) {
        versions.flatMap { it.gameVersions }.distinct().sortedDescending()
    }
    val allLoaders = remember(versions) {
        versions.flatMap { it.loaders }.distinct().sorted()
    }

    val filteredVersions = remember(versions, filterGameVersion, filterLoader) {
        versions.filter { v ->
            (filterGameVersion == null || filterGameVersion in v.gameVersions) &&
                (filterLoader == null || filterLoader in v.loaders)
        }
    }

    LaunchedEffect(projectId) {
        isLoading = true
        repository.getProject(projectId)
            .onSuccess { p ->
                project = p
                repository.getProjectVersions(projectId)
                    .onSuccess { vs -> versions = vs }
                    .onFailure { e -> error = e.message }
            }
            .onFailure { e -> error = e.message }
        isLoading = false
    }

    val installTargets: List<ServerInstallTarget> = remember(servers, project, selectedVersion) {
        val p = project ?: return@remember emptyList()
        servers.map { s ->
            ServerInstallTarget(
                serverId = s.id,
                serverName = s.name,
                serverVersion = s.mcVersion,
                serverType = s.serverType.lowercase().replaceFirstChar { it.uppercase() },
                compatible = selectedVersion?.let { isVersionCompatible(it, s.mcVersion, s.serverType) }
                    ?: isProjectCompatible(p, s.mcVersion, s.serverType),
                folderHint = getSuggestedFolder(p, s.serverType)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        project?.title ?: "Add-on",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            project?.let { p ->
                val selected = selectedVersion
                if (selected != null) {
                    InstallBar(
                        fileName = selected.files.firstOrNull { it.primary }?.filename
                            ?: selected.files.firstOrNull()?.filename ?: selected.name,
                        enabled = servers.isNotEmpty(),
                        serverCount = servers.size,
                        onClick = { showInstallDialog = true }
                    )
                } else if (versions.isNotEmpty()) {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        Text(
                            "Select a version to install",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Failed to load", color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Text(error.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            project != null -> {
                val p = project!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        ProjectHeader(p)
                    }

                    item {
                        HorizontalDivider()
                    }

                    if (versions.isNotEmpty()) {
                        item {
                            Text(
                                "Versions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = filterGameVersion != null,
                                    onClick = { gameVersionSheetOpen = true },
                                    label = {
                                        Text(
                                            filterGameVersion ?: "Game version",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                                Box {
                                    FilterChip(
                                        selected = filterLoader != null,
                                        onClick = { loaderMenuOpen = true },
                                        label = {
                                            Text(
                                                filterLoader ?: if (p.projectType.equals("datapack", ignoreCase = true)) "datapack" else "Loader",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    )
                                    DropdownMenu(expanded = loaderMenuOpen, onDismissRequest = { loaderMenuOpen = false }) {
                                        DropdownMenuItem(
                                            text = { Text("All loaders") },
                                            onClick = {
                                                filterLoader = null
                                                loaderMenuOpen = false
                                            }
                                        )
                                        allLoaders.forEach { l ->
                                            DropdownMenuItem(
                                                text = { Text(l) },
                                                onClick = {
                                                    filterLoader = l
                                                    loaderMenuOpen = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (filteredVersions.isEmpty()) {
                            item {
                                Text(
                                    "No versions match the current filters",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        items(filteredVersions, key = { it.id }) { v ->
                            VersionRow(
                                version = v,
                                selected = selectedVersion?.id == v.id,
                                onClick = { selectedVersion = v }
                            )
                        }
                    }
                }
            }
        }
    }

    if (gameVersionSheetOpen && versions.isNotEmpty()) {
        NativeVersionPickerSheet(
            selectedVersion = filterGameVersion ?: "",
            availableVersions = allGameVersions,
            onDismiss = { gameVersionSheetOpen = false },
            onVersionSelected = { v ->
                filterGameVersion = v.ifBlank { null }
                gameVersionSheetOpen = false
            }
        )
    }

    if (showInstallDialog && project != null) {
        InstallTargetDialog(
            targets = installTargets,
            projectTitle = project!!.title,
            installingServer = installingServer,
            downloadProgress = downloadProgress,
            onSelect = { target ->
                val version = selectedVersion ?: return@InstallTargetDialog
                val file = version.files.firstOrNull { it.primary } ?: version.files.firstOrNull() ?: return@InstallTargetDialog
                val server = servers.firstOrNull { it.id == target.serverId } ?: return@InstallTargetDialog
                scope.launch {
                    installingServer = server
                    downloadProgress = 0f
                    val destDir = File(getServerDir(target.serverId), target.folderHint)
                    destDir.mkdirs()
                    val destFile = File(destDir, file.filename)
                    downloader.download(file.url, destFile, onProgress = { done, total ->
                        if (total > 0) downloadProgress = done.toFloat() / total.toFloat()
                    }).onSuccess {
                        installingServer = null
                        showInstallDialog = false
                        snackbarHostState.showSnackbar("Installed ${file.filename} to ${target.serverName}")
                        notifier.notify(
                            message = "Installed ${project!!.title} to ${target.serverName}.",
                            success = true,
                            title = "Add-on installed",
                            systemOnly = true
                        )
                    }.onFailure { e ->
                        installingServer = null
                        snackbarHostState.showSnackbar("Failed to install: ${e.message}")
                        notifier.notify(
                            message = "Failed to install ${project!!.title}: ${e.message}",
                            success = false,
                            title = "Add-on install failed",
                            systemOnly = true
                        )
                    }
                }
            },
            onDismiss = {
                if (installingServer == null) showInstallDialog = false
            }
        )
    }
}

@Composable
private fun ProjectHeader(p: ModrinthProject) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MarketplaceAsyncImage(
            url = p.iconUrl,
            contentDescription = p.title,
            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                p.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            p.author?.let {
                Text(
                    "by $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "${formatDownloads(p.downloads)} downloads",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${formatDownloads(p.followers)} followers",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Text(
        p.description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun VersionRow(
    version: ModrinthVersion,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        version.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        version.versionType.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (version.versionType) {
                            "release" -> MaterialTheme.colorScheme.primary
                            "beta" -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    version.gameVersions.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${version.loaders.joinToString(", ")} • ${formatDownloads(version.downloads)} downloads",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun InstallBar(
    fileName: String,
    enabled: Boolean,
    serverCount: Int,
    onClick: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                fileName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onClick, enabled = enabled) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (serverCount > 0) "Install" else "Install")
            }
        }
    }
}

@Composable
private fun InstallTargetDialog(
    targets: List<ServerInstallTarget>,
    projectTitle: String,
    installingServer: ServerConfig?,
    downloadProgress: Float,
    onSelect: (ServerInstallTarget) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Install to server",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    projectTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))

                if (targets.isEmpty()) {
                    Text(
                        "No servers found. Create a server first to install add-ons.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onDismiss) { Text("Close") }
                } else {
                    targets.forEach { target ->
                        TargetRow(
                            target = target,
                            installing = installingServer?.id == target.serverId,
                            progress = downloadProgress,
                            onSelect = { onSelect(target) }
                        )
                    }
                    if (installingServer == null) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetRow(
    target: ServerInstallTarget,
    installing: Boolean,
    progress: Float,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(enabled = !installing) { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (target.compatible)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (target.compatible) Icons.Default.Check else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (target.compatible)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        target.serverName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "${target.serverType} • ${target.serverVersion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!target.compatible) {
                        Text(
                            "May not be compatible",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                if (installing) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
            if (installing) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
