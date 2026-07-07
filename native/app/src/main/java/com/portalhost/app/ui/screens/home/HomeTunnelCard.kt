package com.portalhost.app.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.portalhost.app.server.TunnelState
import com.portalhost.app.server.TunnelStatus

@Composable
fun TunnelCard(
    tunnelState: TunnelState? = null,
    onStart: () -> Unit = {},
    onStop: () -> Unit = {},
    onReset: () -> Unit = {},
    onSaveSecretKey: (String) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var secretKeyInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Cloud, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Connect to Tunnel", style = MaterialTheme.typography.titleSmall)
                    val tunStatus = tunnelState?.status
                    val statusText = when (tunStatus) {
                        TunnelStatus.IDLE -> "Not connected"
                        TunnelStatus.DOWNLOADING -> "Downloading binary..."
                        TunnelStatus.CLAIM_REQUIRED -> "Claim required"
                        TunnelStatus.CONNECTING -> "Connecting..."
                        TunnelStatus.CONNECTED -> "Connected"
                        TunnelStatus.ERROR -> "Error"
                        null -> "Not initialized"
                    }
                    Text(statusText, style = MaterialTheme.typography.bodySmall,
                        color = when (tunStatus) {
                            TunnelStatus.CONNECTED -> MaterialTheme.colorScheme.primary
                            TunnelStatus.ERROR -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        })
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))

                if (tunnelState?.claimUrl != null) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Claim Required", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tunnelState.claimUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.Link, contentDescription = "Open", modifier = Modifier.size(20.dp).clickable {
                                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(tunnelState.claimUrl)))
                                }, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("After claiming, paste your secret key below or tap Start again.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (tunnelState?.tunnels?.isNotEmpty() == true) {
                    tunnelState.tunnels.forEach { tunnel ->
                        Text("${tunnel.type.uppercase()}: ${tunnel.publicAddress}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (tunnelState?.error != null) {
                    Text(tunnelState.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(4.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isRunning = tunnelState?.status == TunnelStatus.CONNECTING || tunnelState?.status == TunnelStatus.CONNECTED
                    Button(onClick = onStart, enabled = !isRunning && tunnelState?.status != TunnelStatus.DOWNLOADING) { Text("Start") }
                    OutlinedButton(onClick = onStop, enabled = isRunning) { Text("Stop") }
                    OutlinedButton(onClick = onReset, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Reset") }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = secretKeyInput, onValueChange = { secretKeyInput = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Paste your secret key here") }, singleLine = true, label = { Text("Secret Key") })
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onSaveSecretKey(secretKeyInput); secretKeyInput = "" }, enabled = secretKeyInput.isNotBlank()) { Text("Save Secret Key") }
            }
        }
    }
}
