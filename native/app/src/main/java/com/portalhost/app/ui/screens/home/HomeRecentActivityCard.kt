package com.portalhost.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.portalhost.app.activity.ActivityEntry
import com.portalhost.app.activity.ActivityLog
import com.portalhost.app.activity.ActivityType
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ActivityCard(activityLog: ActivityLog, onViewAll: () -> Unit = {}) {
    val entries = activityLog.entries.takeLast(10)

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
                Text("Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onViewAll) {
                    Text("View All", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (entries.isEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("No recent activity", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Spacer(Modifier.height(8.dp))
                entries.forEach { entry ->
                    ActivityRow(entry)
                    if (entry != entries.last()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityRow(entry: ActivityEntry) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = when (entry.type) {
            ActivityType.SUCCESS, ActivityType.SERVER_STARTED, ActivityType.PLAYER_JOIN -> Icons.Default.CheckCircle
            ActivityType.ERROR, ActivityType.SERVER_CRASH, ActivityType.PLAYER_BAN -> Icons.Default.Error
            ActivityType.WARNING, ActivityType.PLAYER_KICK, ActivityType.SERVER_STARTING, ActivityType.SERVER_STOPPING -> Icons.Default.Warning
            ActivityType.PLAYER_LEAVE -> Icons.Default.PersonRemove
            ActivityType.SERVER_STOPPED -> Icons.Default.CloudOff
            ActivityType.PLAYER_KILL -> Icons.Default.SportsMartialArts
            ActivityType.PLAYER_OP -> Icons.Default.AdminPanelSettings
            ActivityType.PLAYER_DEOP -> Icons.Default.PersonOff
            ActivityType.COMMAND_EXECUTED -> Icons.Default.Terminal
            ActivityType.INFO -> Icons.Default.Info
        }
        val tint = when (entry.type) {
            ActivityType.SUCCESS, ActivityType.SERVER_STARTED, ActivityType.PLAYER_JOIN -> Color(0xFF4ADE80)
            ActivityType.ERROR, ActivityType.SERVER_CRASH, ActivityType.PLAYER_BAN -> Color(0xFFF44336)
            ActivityType.WARNING, ActivityType.PLAYER_KICK, ActivityType.SERVER_STARTING, ActivityType.SERVER_STOPPING -> Color(0xFFFFC107)
            ActivityType.PLAYER_LEAVE -> Color(0xFFFF9800)
            ActivityType.SERVER_STOPPED -> Color(0xFF9CA3AF)
            ActivityType.PLAYER_KILL -> Color(0xFFD500F9)
            ActivityType.PLAYER_OP -> Color(0xFF40C4FF)
            ActivityType.PLAYER_DEOP -> Color(0xFFFB923C)
            ActivityType.COMMAND_EXECUTED -> Color(0xFF7C4DFF)
            ActivityType.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = tint)
        Spacer(Modifier.width(8.dp))
        Text(
            text = timeFormat.format(Date(entry.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.width(8.dp))
        Text(entry.message, style = MaterialTheme.typography.bodySmall)
    }
}
