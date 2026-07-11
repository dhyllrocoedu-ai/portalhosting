package com.portalhost.app.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.portalhost.app.ui.components.*
import com.portalhost.app.server.ServerStatus
import com.portalhost.app.ui.model.ServerConfig
import com.portalhost.app.ui.model.ServerRepository
import java.io.File

private data class ServerTypeInfo(val label: String, val color: Color)

private fun serverTypeInfo(type: String): ServerTypeInfo = when (type.lowercase()) {
    "paper" -> ServerTypeInfo("Paper", Color(0xFF4CAF50))
    "fabric" -> ServerTypeInfo("Fabric", Color(0xFF2196F3))
    "forge" -> ServerTypeInfo("Forge", Color(0xFFFF9800))
    "neoforge" -> ServerTypeInfo("NeoForge", Color(0xFF9C27B0))
    "purpur" -> ServerTypeInfo("Purpur", Color(0xFFFFEB3B))
    "folia" -> ServerTypeInfo("Folia", Color(0xFF7B1FA2))
    "vanilla" -> ServerTypeInfo("Vanilla", Color(0xFF9E9E9E))
    else -> ServerTypeInfo(type.uppercase(), Color(0xFF9E9E9E))
}

private val serverTypeIcon: @Composable (String, Modifier, Dp) -> Unit = { type, modifier, size ->
    when (type.lowercase()) {
        "paper" -> PaperIcon(modifier, size)
        "fabric" -> FabricIcon(modifier, size)
        "forge" -> ForgeIcon(modifier, size)
        "neoforge" -> NeoForgeIcon(modifier, size)
        "purpur" -> PurpurIcon(modifier, size)
        "folia" -> FoliaIcon(modifier, size)
        "vanilla" -> VanillaIcon(modifier, size)
        else -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(
    repository: ServerRepository,
    onCreateServer: () -> Unit,
    onServerClick: (ServerConfig) -> Unit,
    onDeleteServer: (ServerConfig) -> Unit,
    onRenameServer: (ServerConfig) -> Unit = {},
    onDuplicateServer: (ServerConfig) -> Unit = {},
    onBackupServer: (ServerConfig) -> Unit = {},
    onExportServer: (ServerConfig) -> Unit = {},
    serverStates: Map<String, ServerStatus> = emptyMap()
) {
    val servers = repository.list()
    var serverToDelete by remember { mutableStateOf<ServerConfig?>(null) }
    var serverToRename by remember { mutableStateOf<ServerConfig?>(null) }
    var renameText by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val filters = listOf("All", "Online", "Offline")

    val filteredServers = servers.filter { server ->
        val matchesSearch = searchQuery.isBlank() || server.name.contains(searchQuery, ignoreCase = true)
        val s = serverStates[server.id] ?: ServerStatus.OFFLINE
        val isOnline = s == ServerStatus.ONLINE || s == ServerStatus.STARTING
        val matchesFilter = when (selectedFilter) {
            "Online" -> isOnline
            "Offline" -> !isOnline
            else -> true
        }
        matchesSearch && matchesFilter
    }

    serverToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { serverToDelete = null },
            title = { Text("Delete Server") },
            text = { Text("Delete \"${target.name}\"? This will remove the server and all its files.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteServer(target)
                    serverToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { serverToDelete = null }) { Text("Cancel") }
            }
        )
    }

    serverToRename?.let { target ->
        AlertDialog(
            onDismissRequest = { serverToRename = null },
            title = { Text("Rename Server") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Server name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameText.isNotBlank()) {
                        val safeName = renameText.replace(Regex("[/\\\\:*?\"<>|\\0]"), "")
                        onRenameServer(target.copy(name = safeName))
                        serverToRename = null
                    }
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { serverToRename = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search Servers...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent
                            )
                        )
                    } else {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Portal Host", fontWeight = FontWeight.Bold)
                            }
                            Text("Manage all your servers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch; if (!showSearch) searchQuery = "" }) {
                        Icon(if (showSearch) Icons.Default.Close else Icons.Default.Search, contentDescription = "Search")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        val contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding(),
            start = 16.dp,
            end = 16.dp,
            bottom = innerPadding.calculateBottomPadding()
        )

        if (servers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("No servers yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap + to create one", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(Modifier.height(4.dp))
                    ScrollableTabRow(
                        selectedTabIndex = filters.indexOf(selectedFilter).coerceAtLeast(0),
                        edgePadding = 0.dp,
                        divider = {}
                    ) {
                        filters.forEach { filter ->
                            Tab(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                text = { Text(filter) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Servers (${filteredServers.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        FilledTonalButton(
                            onClick = onCreateServer,
                            shape = MaterialTheme.shapes.large
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Create Server")
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                items(filteredServers, key = { it.id }) { server ->
                    val serverIcon = remember(server.id) { loadServerIcon(repository.getServerDir(server.id)) }
                    ServerListItem(
                        server = server,
                        serverIcon = serverIcon,
                        onClick = { onServerClick(server) },
                        onDelete = { serverToDelete = server },
                        onRename = { serverToRename = server; renameText = server.name },
                        onDuplicate = { onDuplicateServer(server) },
                        onBackup = { onBackupServer(server) },
                        onExport = { onExportServer(server) }
                    )
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    CreateServerCard(onClick = onCreateServer)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerListItem(
    server: ServerConfig,
    serverIcon: ImageBitmap?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onBackup: () -> Unit,
    onExport: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    val typeInfo = serverTypeInfo(server.serverType)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showContextMenu = true }
            ),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (serverIcon != null) {
                    Image(
                        bitmap = serverIcon,
                        contentDescription = "${server.name} icon",
                        modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.large),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = MaterialTheme.shapes.large,
                        color = typeInfo.color.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            serverTypeIcon(server.serverType, Modifier.size(28.dp), 28.dp)
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = server.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = typeInfo.color.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                serverTypeIcon(server.serverType, Modifier.size(12.dp), 12.dp)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = typeInfo.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = typeInfo.color
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options", modifier = Modifier.size(20.dp))
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                offset = DpOffset(x = (-160).dp, y = 0.dp)
                            ) {
                                DropdownMenuItem(text = { Text("Open Dashboard") }, onClick = { showMenu = false; onClick() },
                                    leadingIcon = { Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(18.dp)) })
                                DropdownMenuItem(text = { Text("Rename") }, onClick = { showMenu = false; onRename() },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) })
                                DropdownMenuItem(text = { Text("Duplicate") }, onClick = { showMenu = false; onDuplicate() },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)) })
                                DropdownMenuItem(text = { Text("Backup") }, onClick = { showMenu = false; onBackup() },
                                    leadingIcon = { Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp)) })
                                DropdownMenuItem(text = { Text("Export") }, onClick = { showMenu = false; onExport() },
                                    leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp)) })
                                HorizontalDivider()
                                DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; onDelete() },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) })
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                InfoChip(Icons.Default.Memory, server.maxRam, Modifier.weight(1f))
                InfoChip(Icons.Default.Storage, "${server.serverType.uppercase()} ${server.mcVersion.ifBlank { "?" }}", Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = server.jarName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = onClick,
                    shape = MaterialTheme.shapes.large,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Open", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    if (showContextMenu) {
        AlertDialog(
            onDismissRequest = { showContextMenu = false },
            title = { Text(server.name) },
            text = {
                Column {
                    ContextMenuItem(Icons.Default.Dashboard, "Open Dashboard") { showContextMenu = false; onClick() }
                    ContextMenuItem(Icons.Default.Edit, "Rename") { showContextMenu = false; onRename() }
                    ContextMenuItem(Icons.Default.ContentCopy, "Duplicate") { showContextMenu = false; onDuplicate() }
                    ContextMenuItem(Icons.Default.Backup, "Backup") { showContextMenu = false; onBackup() }
                    ContextMenuItem(Icons.Default.FileUpload, "Export") { showContextMenu = false; onExport() }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    ContextMenuItem(Icons.Default.Delete, "Delete", Color(0xFFF44336)) { showContextMenu = false; onDelete() }
                }
            },
            confirmButton = { TextButton(onClick = { showContextMenu = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ContextMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
        Spacer(Modifier.width(12.dp))
        Text(label, color = tint, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun CreateServerCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Create New Server", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Paper, Fabric, Forge, NeoForge, Vanilla, etc.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun loadServerIcon(serverDir: File): ImageBitmap? {
    val iconFile = File(serverDir, "server-icon.png")
    if (!iconFile.exists()) return null
    return try {
        BitmapFactory.decodeFile(iconFile.absolutePath)?.asImageBitmap()
    } catch (e: Exception) { null }
}
