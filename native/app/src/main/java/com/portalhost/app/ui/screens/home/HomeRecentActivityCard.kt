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
fun ActivityCard(activityLog: ActivityLog) {
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
                TextButton(onClick = {}) {
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
            ActivityType.SUCCESS -> Icons.Default.CheckCircle
            ActivityType.ERROR -> Icons.Default.Error
            ActivityType.WARNING -> Icons.Default.Warning
            ActivityType.PLAYER_JOIN -> Icons.Default.PersonAdd
            ActivityType.PLAYER_LEAVE -> Icons.Default.PersonRemove
            ActivityType.INFO -> Icons.Default.Info
        }
        val tint = when (entry.type) {
            ActivityType.SUCCESS -> Color(0xFF4CAF50)
            ActivityType.ERROR -> Color(0xFFF44336)
            ActivityType.WARNING -> Color(0xFFFFC107)
            ActivityType.PLAYER_JOIN -> Color(0xFF4CAF50)
            ActivityType.PLAYER_LEAVE -> Color(0xFFFF9800)
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
