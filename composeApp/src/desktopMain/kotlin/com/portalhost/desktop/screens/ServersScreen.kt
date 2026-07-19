package com.portalhost.desktop.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.filesystem.FileSystem
import com.portalhost.model.ServerConfig
import com.portalhost.model.ServerState
import com.portalhost.model.ServerStatus
import com.portalhost.server.ServerManager
import com.portalhost.server.getServerIconFile
import com.portalhost.server.loadServerIcon
import com.portalhost.theme.ThemeColors
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private data class ServerTypeInfo(val label: String, val color: Color)

private fun serverTypeInfo(type: com.portalhost.model.ServerType): ServerTypeInfo = when (type) {
    com.portalhost.model.ServerType.PAPER -> ServerTypeInfo("Paper", ThemeColors.serverTypeColor(type))
    com.portalhost.model.ServerType.FABRIC -> ServerTypeInfo("Fabric", ThemeColors.serverTypeColor(type))
    com.portalhost.model.ServerType.FORGE -> ServerTypeInfo("Forge", ThemeColors.serverTypeColor(type))
    com.portalhost.model.ServerType.NEOFORGE -> ServerTypeInfo("NeoForge", ThemeColors.serverTypeColor(type))
    com.portalhost.model.ServerType.PURPUR -> ServerTypeInfo("Purpur", ThemeColors.serverTypeColor(type))
    com.portalhost.model.ServerType.FOLIA -> ServerTypeInfo("Folia", ThemeColors.serverTypeColor(type))
    com.portalhost.model.ServerType.VANILLA -> ServerTypeInfo("Vanilla", ThemeColors.serverTypeColor(type))
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(onNavigateToDetail: (String) -> Unit = {}) {
    val serverManager = koinInject<ServerManager>()
    val servers by serverManager.servers.collectAsState()
    val serverStates by serverManager.serverStates.collectAsState()
    var serverToDelete by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val filters = listOf("All", "Online", "Offline")

    val filteredServers = servers.entries.filter { (id, config) ->
        val matchesStatus = when (selectedFilter) {
            "Online" -> serverStates[id]?.status == ServerStatus.RUNNING || serverStates[id]?.status == ServerStatus.STARTING
            "Offline" -> serverStates[id]?.status == null || serverStates[id]?.status == ServerStatus.STOPPED || serverStates[id]?.status == ServerStatus.CRASHED || serverStates[id]?.status == ServerStatus.STOPPING
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() || config.name.contains(searchQuery, ignoreCase = true)
        matchesStatus && matchesSearch
    }

    serverToDelete?.let { id ->
        val config = servers[id]
        AlertDialog(
            onDismissRequest = { serverToDelete = null },
            title = { Text("Delete Server") },
            text = { Text("Delete \"${config?.name ?: id}\"? This will remove the server and all its files.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { serverManager.deleteServer(id) }
                    serverToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { serverToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (servers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("No servers yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("Go to Servers tab or click + to add one", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Servers (${servers.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row {
                        IconButton(onClick = { showSearch = !showSearch }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        FilledTonalButton(
                            onClick = { onNavigateToDetail("") },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Create Server")
                        }
                    }
                }

                if (showSearch) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search servers...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    filters.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter, fontSize = 12.sp) },
                        )
                    }
                }

                if (filteredServers.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            if (searchQuery.isNotBlank()) "No servers matching \"$searchQuery\""
                            else "No ${selectedFilter.lowercase()} servers",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredServers, key = { (id, _) -> id }) { (id, config) ->
                            ServerListItem(
                                server = config,
                                state = serverStates[id],
                                onClick = { onNavigateToDetail(id) },
                                onDelete = { serverToDelete = id },
                            )
                        }

                        item {
                            Spacer(Modifier.height(4.dp))
                            CreateServerCard(onClick = { onNavigateToDetail("") })
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ServerListItem(
    server: ServerConfig,
    state: ServerState?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val typeInfo = serverTypeInfo(server.serverType)
    val status = state?.status ?: ServerStatus.STOPPED
    val fileSystem = koinInject<FileSystem>()
    val serverIcon = remember(server.id) {
        val iconFile = getServerIconFile(java.io.File(fileSystem.getServersDirBlocking(), server.id))
        loadServerIcon(iconFile)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = typeInfo.color.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (serverIcon != null) {
                            Image(bitmap = serverIcon, contentDescription = "Server Icon", modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)))
                        } else {
                            Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(24.dp), tint = typeInfo.color)
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
                            shape = RoundedCornerShape(6.dp),
                            color = typeInfo.color.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = typeInfo.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = typeInfo.color,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
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
                                DropdownMenuItem(
                                    text = { Text("Open Dashboard") },
                                    onClick = { showMenu = false; onClick() },
                                    leadingIcon = { Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    onClick = { showMenu = false },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Duplicate") },
                                    onClick = { showMenu = false },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                    onClick = { showMenu = false; onDelete() },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) }
                                )
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
                InfoChip(Icons.Default.Person, if (status == ServerStatus.RUNNING) "${state?.playersOnline ?: 0}" else "—", Modifier.weight(1f))
                InfoChip(Icons.Default.Memory, "${server.memoryMax}M", Modifier.weight(1f))
                InfoChip(Icons.Default.Storage, "—", Modifier.weight(1f))
                InfoChip(
                    Icons.Default.Cloud,
                    when (status) {
                        ServerStatus.RUNNING -> "Online"
                        ServerStatus.STARTING -> "Starting"
                        ServerStatus.STOPPING -> "Stopping"
                        ServerStatus.CRASHED -> "Crashed"
                        else -> "Offline"
                    },
                    Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = ThemeColors.serverStatusColor(status)
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${server.version.ifBlank { "Latest" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                FilledTonalButton(
                    onClick = onClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Open", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(10.dp),
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
