package com.portalhost.desktop.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.model.ServerConfig
import com.portalhost.model.ServerState
import com.portalhost.model.ServerStatus
import com.portalhost.server.ServerManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun ServersScreen(onNavigateToDetail: (String) -> Unit = {}) {
    val serverManager = koinInject<ServerManager>()
    val servers by serverManager.servers.collectAsState()
    val serverStates by serverManager.serverStates.collectAsState()
    var selectedServerId by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    val deleteScope = rememberCoroutineScope()

    val scope = rememberCoroutineScope()

    if (showDeleteDialog != null) {
        val id = showDeleteDialog!!
        val config = servers[id]
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Server") },
            text = { Text("Are you sure you want to delete \"${config?.name ?: id}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { serverManager.deleteServer(id) }
                    if (selectedServerId == id) selectedServerId = null
                    showDeleteDialog = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "Servers",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp),
            )
            if (servers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Dns, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Spacer(Modifier.height(8.dp))
                        Text("No servers yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Go to Create tab to add one", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(servers.entries.toList(), key = { it.key }) { (id, config) ->
                        ServerListItem(
                            config = config,
                            state = serverStates[id],
                            isSelected = selectedServerId == id,
                            onClick = {
                                selectedServerId = id
                                onNavigateToDetail(id)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerListItem(config: ServerConfig, state: ServerState?, isSelected: Boolean, onClick: () -> Unit) {
    val status = state?.status ?: ServerStatus.STOPPED
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(config.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text("${config.serverType.name} ${config.version}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            StatusBadge(status)
        }
    }
}

@Composable
private fun StatusBadge(status: ServerStatus) {
    val (color, label) = when (status) {
        ServerStatus.RUNNING -> Color(0xFF4CAF50) to "Running"
        ServerStatus.STARTING -> Color(0xFFFFC107) to "Starting"
        ServerStatus.STOPPING -> Color(0xFFFF9800) to "Stopping"
        ServerStatus.CRASHED -> Color(0xFFF44336) to "Crashed"
        ServerStatus.RESTARTING -> Color(0xFFFFC107) to "Restarting"
        ServerStatus.STOPPED -> Color(0xFF9E9E9E) to "Stopped"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = color)
    }
}
