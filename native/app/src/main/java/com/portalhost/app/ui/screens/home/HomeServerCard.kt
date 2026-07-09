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
import androidx.compose.ui.unit.dp
import com.portalhost.app.network.NetworkInfo
import com.portalhost.app.server.ServerState
import com.portalhost.app.server.ServerStatus
import com.portalhost.app.server.TunnelState
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
    onDeleteServer: (ServerConfig) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    val serverDir = activeServer?.let { repository.getServerDir(it.id) }
    val serverIcon = remember(serverDir?.absolutePath) {
        loadServerIcon(serverDir)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(statusColor)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (serverIcon != null) {
                        Image(
                            bitmap = serverIcon,
                            contentDescription = "${activeServer?.name} icon",
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeServer?.name ?: "No Server",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (activeServer != null) {
                            Text(
                                text = "${activeServer.jarName} · ${activeServer.mcVersion.ifBlank { serverTypeLabel(activeServer.serverType) }} · ${serverState.status.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        if (serverState.status == ServerStatus.ONLINE) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lan, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (networkInfo.localIp != "Unknown") "${networkInfo.localIp}:${activeServer?.port}" else "Local IP unknown",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lan, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(4.dp))
                                Text(text = "Server not running", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (tunnelUrl.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(4.dp))
                                Text(text = tunnelUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        if (tunnelState?.tunnels?.isNotEmpty() == true) {
                            for (tunnel in tunnelState.tunnels) {
                                Spacer(Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(4.dp))
                                    Text(text = "${tunnel.type.uppercase()}: ${tunnel.publicAddress}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        if (networkInfo.isCellular && serverState.status == ServerStatus.ONLINE) {
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFFC107))
                                Spacer(Modifier.width(4.dp))
                                Text(text = "Mobile data — port forwarding may not work", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFC107))
                            }
                        }
                    }

                    if (serverConfigs.isNotEmpty()) {
                        Icon(
                            if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Switch server"
                        )
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
                            else MaterialTheme.colorScheme.surface
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
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(12.dp))
                            Text("Delete Server", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}