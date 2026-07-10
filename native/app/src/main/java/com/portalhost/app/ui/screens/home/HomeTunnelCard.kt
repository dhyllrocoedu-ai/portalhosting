package com.portalhost.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.portalhost.app.server.TunnelState
import com.portalhost.app.server.TunnelStatus

@Composable
fun TunnelCard(
    tunnelState: TunnelState? = null,
    onStart: () -> Unit = {},
    onStop: () -> Unit = {}
) {
    val tunStatus = tunnelState?.status
    val connected = tunStatus == TunnelStatus.CONNECTED
    val statusText = when (tunStatus) {
        TunnelStatus.IDLE -> "Not Connected"
        TunnelStatus.DOWNLOADING -> "Downloading..."
        TunnelStatus.CLAIM_REQUIRED -> "Claim Required"
        TunnelStatus.CONNECTING -> "Connecting..."
        TunnelStatus.CONNECTED -> "Connected"
        TunnelStatus.ERROR -> "Error"
        null -> "Not initialized"
    }
    val statusColor = when (tunStatus) {
        TunnelStatus.CONNECTED -> MaterialTheme.colorScheme.primary
        TunnelStatus.ERROR -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val enabled = tunStatus != TunnelStatus.DOWNLOADING && tunStatus != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Tunnel", style = MaterialTheme.typography.titleMedium)
                Text(statusText, style = MaterialTheme.typography.bodySmall, color = statusColor)
            }
            Button(
                onClick = { if (connected) onStop() else onStart() },
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(if (connected) "Disconnect" else "Connect")
            }
        }
    }
}
