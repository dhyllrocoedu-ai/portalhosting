package com.portalhost.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.app.activity.ActivityEntry
import com.portalhost.app.activity.ActivityLog
import com.portalhost.app.activity.ActivityType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentActivityScreen(
    activityLog: ActivityLog,
    onBack: () -> Unit
) {
    val entries = remember(activityLog) { activityLog.entries }
    val sdf = remember { SimpleDateFormat("MMM d, h:mm a", Locale.US) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recent Activity") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (entries.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "No activity yet",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(entries.reversed(), key = { it.timestamp }) { entry ->
                    ActivityRow(entry, sdf)
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(entry: ActivityEntry, sdf: SimpleDateFormat) {
    val (icon, color) = when (entry.type) {
        ActivityType.SUCCESS, ActivityType.SERVER_ONLINE, ActivityType.PLAYER_JOIN -> Pair(Icons.Default.CheckCircle, Color(0xFF4ADE80))
        ActivityType.ERROR, ActivityType.SERVER_CRASH, ActivityType.PLAYER_BAN -> Pair(Icons.Default.Error, Color(0xFFF44336))
        ActivityType.WARNING, ActivityType.PLAYER_KICK -> Pair(Icons.Default.Warning, Color(0xFFFFC107))
        ActivityType.PLAYER_LEAVE -> Pair(Icons.Default.PersonRemove, Color(0xFFFF9800))
        ActivityType.SERVER_OFFLINE -> Pair(Icons.Default.CloudOff, Color(0xFF9CA3AF))
        ActivityType.PLAYER_KILL -> Pair(Icons.Default.SportsMartialArts, Color(0xFFD500F9))
        ActivityType.PLAYER_OP -> Pair(Icons.Default.AdminPanelSettings, Color(0xFF40C4FF))
        ActivityType.PLAYER_DEOP -> Pair(Icons.Default.PersonOff, Color(0xFFFB923C))
        ActivityType.COMMAND_EXECUTED -> Pair(Icons.Default.Terminal, Color(0xFF7C4DFF))
        ActivityType.INFO -> Pair(Icons.Default.Info, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = color
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.message,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = sdf.format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}