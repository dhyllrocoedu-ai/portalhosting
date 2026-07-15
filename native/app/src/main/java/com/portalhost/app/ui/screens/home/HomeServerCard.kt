package com.portalhost.app.ui.screens.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.app.server.ServerState
import com.portalhost.app.server.ServerStatus
import com.portalhost.app.ui.model.ServerConfig
import com.portalhost.app.ui.model.ServerRepository
import java.io.File

private fun loadServerIcon(serverDir: File?): ImageBitmap? {
    if (serverDir == null) return null
    val iconFile = File(serverDir, "server-icon.png")
    if (!iconFile.exists()) return null
    return try {
        BitmapFactory.decodeFile(iconFile.absolutePath)?.asImageBitmap()
    } catch (e: Exception) { null }
}

@Composable
fun ServerCard(
    activeServer: ServerConfig?,
    serverConfigs: List<ServerConfig>,
    serverState: ServerState,
    statusColor: Color,
    repository: ServerRepository,
    onSelectServer: (String) -> Unit,
    onDeleteServer: (ServerConfig) -> Unit = {},
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val serverDir = activeServer?.let { repository.getServerDir(it.id) }
    val serverIcon = remember(serverDir?.absolutePath) { loadServerIcon(serverDir) }

    val statusLabel = serverState.status.name.lowercase().replaceFirstChar { it.uppercase() }
    val statusColorForBadge = when (serverState.status) {
        ServerStatus.ONLINE -> Color(0xFF4CAF50)
        ServerStatus.STARTING -> Color(0xFFFFC107)
        ServerStatus.STOPPING -> Color(0xFFFF9800)
        ServerStatus.STOPPED -> Color(0xFFA5D6A7)
        ServerStatus.CRASHED -> Color(0xFFF44336)
        ServerStatus.OFFLINE -> Color(0xFF9E9E9E)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (serverIcon != null) {
                    Image(
                        bitmap = serverIcon,
                        contentDescription = "${activeServer?.name} icon",
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = activeServer?.name ?: "No Server",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusColorForBadge.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = statusLabel,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColorForBadge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (activeServer != null) {
                        Text(
                            text = "${formatServerType(activeServer.serverType)} ${activeServer.mcVersion}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (activeServer != null) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("MC ${activeServer.mcVersion.ifBlank { "?" }}", fontSize = 10.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Java 21", fontSize = 10.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                            SuggestionChip(
                                onClick = {},
                                label = { Text("RAM ${activeServer.maxRam}", fontSize = 10.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
                if (serverConfigs.isNotEmpty()) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Switch server"
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val canStart = (serverState.status == ServerStatus.OFFLINE || serverState.status == ServerStatus.STOPPED || serverState.status == ServerStatus.CRASHED) && activeServer != null
                val canStop = serverState.status == ServerStatus.ONLINE
                val canRestart = serverState.status == ServerStatus.ONLINE

                ControlButton(
                    icon = Icons.Default.PlayArrow,
                    label = "Start",
                    onClick = onStart,
                    enabled = canStart,
                    color = Color(0xFF4CAF50)
                )
                ControlButton(
                    icon = Icons.Default.Stop,
                    label = "Stop",
                    onClick = onStop,
                    enabled = canStop,
                    color = Color(0xFFF44336)
                )
                ControlButton(
                    icon = Icons.Default.Refresh,
                    label = "Restart",
                    onClick = onRestart,
                    enabled = canRestart,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                serverConfigs.forEach { config ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable {
                            onSelectServer(config.id)
                            expanded = false
                        },
                        color = if (config.id == activeServer?.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(config.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onDeleteServer(activeServer!!); expanded = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Text("Delete Server", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.ControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    color: Color
) {
    ElevatedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.weight(1f).height(56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = color.copy(alpha = 0.12f),
            contentColor = color,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

private fun formatServerType(type: String): String = when (type.lowercase()) {
    "paper" -> "Paper"
    "vanilla" -> "Vanilla"
    "fabric" -> "Fabric"
    "forge" -> "Forge"
    "neoforge" -> "NeoForge"
    "purpur" -> "Purpur"
    "folia" -> "Folia"
    "custom" -> "Custom"
    else -> type.replaceFirstChar { it.uppercase() }
}
