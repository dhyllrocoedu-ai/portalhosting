package com.portalhost.app.ui.screens.create

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepProperties(
    port: String, gamemode: String, difficulty: String, motd: String,
    gamemodes: List<String>, difficulties: List<String>,
    onPortChange: (String) -> Unit, onGamemodeChange: (String) -> Unit,
    onDifficultyChange: (String) -> Unit, onMotdChange: (String) -> Unit
) {
    Column {
        Text("Server Properties", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Configure basic server settings", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = port,
            onValueChange = { onPortChange(it.filter { c -> c.isDigit() }.take(5)) },
            label = { Text("Port") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = { Icon(Icons.Default.Lan, contentDescription = null) }
        )
        Spacer(Modifier.height(12.dp))
        Text("Gamemode", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            gamemodes.forEach { gm ->
                FilterChip(
                    selected = gamemode == gm,
                    onClick = { onGamemodeChange(gm) },
                    label = { Text(gm.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Difficulty", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            difficulties.forEach { diff ->
                FilterChip(
                    selected = difficulty == diff,
                    onClick = { onDifficultyChange(diff) },
                    label = { Text(diff.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = motd,
            onValueChange = onMotdChange,
            label = { Text("MOTD") },
            placeholder = { Text("A Minecraft Server") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null) }
        )
    }
}
