package com.portalhost.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.portalhost.app.server.ProcessStats
import com.portalhost.app.server.ServerState
import com.portalhost.app.server.ServerStatus
import kotlin.math.roundToInt

@Composable
fun PerformanceCard(
    processStats: ProcessStats,
    serverState: ServerState,
    maxPlayers: Int = 0,
    onOpenPerformance: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onOpenPerformance) {
                    Text("View Details", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val live = serverState.status == ServerStatus.ONLINE || serverState.status == ServerStatus.STARTING
                StatMiniCard(value = if (live) "${processStats.cpuPercent.roundToInt()}%" else "—", label = "CPU")
                StatMiniCard(value = if (live) processStats.ramFormatted else "—", label = "RAM")
                StatMiniCard(value = if (live) formatTps(processStats.tps) else "—", label = "TPS")
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val live = serverState.status == ServerStatus.ONLINE
                StatMiniCard(value = if (live) "${serverState.players.size}" else "—", label = "Players")
                StatMiniCard(value = if (live) processStats.rxFormatted else "—", label = "Download")
                StatMiniCard(value = if (live) processStats.txFormatted else "—", label = "Upload")
            }
        }
    }
}

@Composable
private fun RowScope.StatMiniCard(value: String, label: String) {
    Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatProcessRam(mb: Int): String = when {
    mb < 1024 -> "${mb}M"
    mb % 1024 == 0 -> "${mb / 1024}G"
    else -> "${"%.1f".format(mb.toDouble() / 1024)}G"
}

private fun formatTps(tps: Float): String = "%.1f".format(tps)

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
    else -> "${"%.2f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
}