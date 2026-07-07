package com.portalhost.app.ui.screens.create

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StepEula(eulaAccepted: Boolean, onEulaChange: (Boolean) -> Unit) {
    Column {
        Text("EULA Agreement", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Minecraft Server Software End User License Agreement", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "By checking the box below, you agree to the Minecraft End User License Agreement (EULA). " +
                    "This means:\n\n" +
                    "• You may run the server for personal or private use\n" +
                    "• You may not distribute or sell the server software\n" +
                    "• You must comply with Mojang's EULA at https://aka.ms/MinecraftEULA\n\n" +
                    "The eula.txt file will be created with eula=true.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = eulaAccepted, onCheckedChange = onEulaChange)
            Spacer(Modifier.width(8.dp))
            Text("I agree to the Minecraft EULA", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
