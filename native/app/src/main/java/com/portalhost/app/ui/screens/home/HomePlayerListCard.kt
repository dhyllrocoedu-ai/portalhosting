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
import androidx.compose.ui.unit.sp
import com.portalhost.app.server.SkinService
import com.portalhost.app.ui.components.MinecraftHeadIcon
import com.portalhost.app.ui.components.SkinHeadIcon

@Composable
fun PlayerListCard(
    players: List<String>,
    isOnline: Boolean,
    onCommand: (String) -> Unit,
    onOpenPlayers: () -> Unit,
    skinService: SkinService? = null,
    maxPlayers: Int = 20
) {
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
                Text("Online Players (${players.size}/$maxPlayers)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onOpenPlayers) {
                    Text("Player Management →")
                }
            }

            if (players.isEmpty()) {
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text("0 online", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Spacer(Modifier.height(8.dp))
                players.take(5).forEach { player ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (skinService != null) {
                            SkinHeadIcon(player = player, skinService = skinService, size = 18.dp)
                        } else {
                            MinecraftHeadIcon(player = player, size = 18.dp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(player, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (players.size > 5) {
                    TextButton(onClick = onOpenPlayers, modifier = Modifier.fillMaxWidth()) {
                        Text("Show all (${players.size})")
                    }
                }
            }
        }
    }
}

@Composable
fun ActionChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
