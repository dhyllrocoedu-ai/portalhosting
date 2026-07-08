package com.portalhost.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.app.server.ServerState
import com.portalhost.app.server.ServerStatus
import com.portalhost.app.ui.model.ServerConfig

@Composable
fun QuickActions(
    serverState: ServerState,
    activeServer: ServerConfig?,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val canStart = (serverState.status == ServerStatus.OFFLINE || serverState.status == ServerStatus.STOPPED || serverState.status == ServerStatus.CRASHED) && activeServer != null
            val canStop = serverState.status == ServerStatus.ONLINE
            val canRestart = serverState.status == ServerStatus.ONLINE

            ActionButton(
                icon = Icons.Default.PlayArrow,
                label = "Start",
                onClick = onStart,
                enabled = canStart,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                icon = Icons.Default.Stop,
                label = "Stop",
                onClick = onStop,
                enabled = canStop,
                color = Color(0xFFF44336),
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                icon = Icons.Default.Refresh,
                label = "Restart",
                onClick = onRestart,
                enabled = canRestart,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    ElevatedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = color.copy(alpha = 0.15f),
            contentColor = color,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}
