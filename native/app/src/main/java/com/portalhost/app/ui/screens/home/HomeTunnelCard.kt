package com.portalhost.app.ui.screens.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.portalhost.app.server.TunnelState
import com.portalhost.app.server.TunnelStatus

@Composable
fun TunnelCard(
    tunnelState: TunnelState? = null,
    onStart: () -> Unit = {},
    onStop: () -> Unit = {},
    onReset: () -> Unit = {}
) {
    val tunStatus = tunnelState?.status
    val isConnected = tunStatus == TunnelStatus.CONNECTED
    val isConnecting = tunStatus == TunnelStatus.CONNECTING || tunStatus == TunnelStatus.DOWNLOADING
    val isClaimRequired = tunStatus == TunnelStatus.CLAIM_REQUIRED
    val address = tunnelState?.tunnels?.firstOrNull()?.publicAddress

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Tunnel", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                when {
                    isConnected -> {
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                            Text("Connected", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                    isConnecting -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                            Text("Connecting...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    isClaimRequired -> {
                        Text("Claim required", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFC107), fontWeight = FontWeight.Medium)
                    }
                    else -> {
                        TextButton(onClick = onStart, contentPadding = PaddingValues(0.dp)) {
                            Text("Start Tunnel", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            if (isConnected && tunnelState?.tunnels?.isNotEmpty() == true) {
                Spacer(Modifier.height(12.dp))
                val javaAddr = tunnelState.tunnels.firstOrNull { it.type.equals("tcp", true) }
                val bedrockAddr = tunnelState.tunnels.firstOrNull { it.type.equals("udp", true) }
                val anyAddr = javaAddr ?: tunnelState.tunnels.firstOrNull()

                if (javaAddr != null || bedrockAddr != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Server Address", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        javaAddr?.let { t ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Java", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(72.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(t.publicAddress, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            }
                        }
                        bedrockAddr?.let { t ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Bedrock", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.width(72.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(t.publicAddress, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            }
                        }
                        FilledTonalButton(
                            onClick = onStop,
                            colors = ButtonDefaults.filledTonalButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CloudOff, contentDescription = "Disconnect", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Disconnect", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            if (isClaimRequired && tunnelState?.claimUrl != null) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFC107).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        val claimCtx = LocalContext.current
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Claim Required", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFFFFC107))
                            FilledTonalButton(
                                onClick = onReset,
                                colors = ButtonDefaults.filledTonalButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset claim", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Reset", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    tunnelState.claimUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tunnelState.claimUrl))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    claimCtx.startActivity(intent)
                                }
                            ) {
                                    Icon(
                                        Icons.Default.OpenInBrowser,
                                        contentDescription = "Open claim URL in browser",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}