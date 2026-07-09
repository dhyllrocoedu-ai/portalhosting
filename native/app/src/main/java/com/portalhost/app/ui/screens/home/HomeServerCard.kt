package com.portalhost.app.ui.screens.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.portalhost.app.network.NetworkInfo
import com.portalhost.app.server.ServerState
import com.portalhost.app.server.ServerStatus
import com.portalhost.app.server.TunnelState
import com.portalhost.app.ui.components.GrassIcon
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerCard(
    activeServer: ServerConfig?,
    serverConfigs: List<ServerConfig>,
    serverState: ServerState,
    statusColor: Color,
    networkInfo: NetworkInfo,
    tunnelUrl: String = "",
    tunnelState: TunnelState? = null,
    repository: ServerRepository,
    onSelectServer: (String) -> Unit,
    onCreateServer: () -> Unit,
    onDeleteServer: (ServerConfig) -> Unit = {},
    onStart: () -> Unit = {},
    onStop: () -> Unit = {},
    onRestart: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    val serverDir = activeServer?.let { repository.getServerDir(it.id) }
    val serverIcon = remember(serverDir?.absolutePath) {
        loadServerIcon(serverDir)
    }

    val canStart = (serverState.status == ServerStatus.OFFLINE || serverState.status == ServerStatus.STOPPED || serverState.status == ServerStatus.CRASHED) && activeServer != null
    val canStop = serverState.status == ServerStatus.ONLINE
    val canRestart = serverState.status == ServerStatus.ONLINE

    val tunnelAddress = tunnelState?.tunnels?.firstOrNull()?.publicAddress
    val displayAddress = tunnelAddress?.takeIf { it.isNotBlank() } ?: tunnelUrl.takeIf { it.isNotBlank() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(statusColor)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (serverIcon != null) {
                    Image(
                        bitmap = serverIcon,
                        contentDescription = "${activeServer?.name} icon",
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    GrassIcon(size = 64.dp)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = activeServer?.name ?: "No Server",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = serverState.status.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (activeServer != null) {
                        Text(
                            text = "${activeServer.jarName} ${activeServer.mcVersion}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatChip(label = "MC", value = activeServer?.mcVersion?.ifBlank { "Latest" } ?: "—")
                        StatChip(label = "RAM", value = activeServer?.maxRam ?: "—")
                        StatChip(label = "Port", value = activeServer?.port?.toString() ?: "—")
                    }
                    Spacer(Modifier.height(6.dp))
                    if (serverState.status == ServerStatus.ONLINE) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lan, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${networkInfo.localIp}:${activeServer?.port}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (displayAddress != null) {
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = displayAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (networkInfo.isCellular && serverState.status == ServerStatus.ONLINE) {
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFFC107))
                            Spacer(Modifier.width(6.dp))
                            Text("Mobile data — port forwarding may not work", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFC107))
                        }
                    }
                }
                if (serverConfigs.size > 1) {
                    Icon(
                        if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Switch server",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ElevatedButton(
                    onClick = onStart,
                    enabled = canStart,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f),
                        contentColor = Color(0xFF4CAF50),
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Start", fontWeight = FontWeight.Medium)
                }
                ElevatedButton(
                    onClick = onStop,
                    enabled = canStop,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFFF44336).copy(alpha = 0.15f),
                        contentColor = Color(0xFFF44336),
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Stop", fontWeight = FontWeight.Medium)
                }
                ElevatedButton(
                    onClick = onRestart,
                    enabled = canRestart,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Restart", fontWeight = FontWeight.Medium)
                }
            }

            if (expanded) {
                HorizontalDivider()
                serverConfigs.forEach { config ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectServer(config.id)
                                expanded = false
                            },
                        color = if (config.id == activeServer?.id)
                            MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(config.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDeleteServer(activeServer!!)
                            expanded = false
                        },
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Text("Delete Server", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}