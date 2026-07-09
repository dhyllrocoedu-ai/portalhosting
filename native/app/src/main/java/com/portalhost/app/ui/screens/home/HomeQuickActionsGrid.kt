package com.portalhost.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun QuickActionsGrid(
    onFiles: () -> Unit,
    onLogs: () -> Unit,
    onPlayers: () -> Unit,
    onPerformance: () -> Unit,
    onTunnel: () -> Unit = {},
    onPlugins: () -> Unit = {},
    onBackups: () -> Unit = {},
    onWorlds: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Quick Actions", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                QACard(Icons.Default.Folder, "Files", onFiles, Modifier.weight(1f))
                QACard(Icons.Default.Article, "Logs", onLogs, Modifier.weight(1f))
                QACard(Icons.Default.People, "Players", onPlayers, Modifier.weight(1f))
                QACard(Icons.Default.TrendingUp, "Performance", onPerformance, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                QACard(Icons.Default.Cloud, "Tunnel", onTunnel, Modifier.weight(1f))
                QACard(Icons.Default.Extension, "Plugins", onPlugins, Modifier.weight(1f))
                QACard(Icons.Default.Backup, "Backups", onBackups, Modifier.weight(1f))
                QACard(Icons.Default.Public, "Worlds", onWorlds, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QACard(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}