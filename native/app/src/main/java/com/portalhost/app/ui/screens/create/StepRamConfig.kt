package com.portalhost.app.ui.screens.create

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.portalhost.app.server.RamStatus
import kotlin.math.roundToInt

@Composable
fun StepRamConfig(
    minRam: Float, maxRam: Float,
    maxRamLimit: Float,
    ramStatus: RamStatus,
    onMinChange: (Float) -> Unit, onMaxChange: (Float) -> Unit
) {
    Column {
        Text("Memory (RAM)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Allocate memory for your server. More RAM = better performance.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Text("Minimum RAM: ${"%.1f".format(minRam)} GB", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Slider(
            value = minRam,
            onValueChange = { v -> onMinChange((v / 0.1f).roundToInt() * 0.1f) },
            valueRange = 0.5f..maxRam,
            steps = ((maxRam - 0.5f) / 0.1f).roundToInt(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Text("Maximum RAM: ${"%.1f".format(maxRam)} GB", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Slider(
            value = maxRam,
            onValueChange = { v -> onMaxChange((v / 0.1f).roundToInt() * 0.1f) },
            valueRange = 0.5f..maxRamLimit,
            steps = ((maxRamLimit - 0.5f) / 0.1f).roundToInt(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        RamStatusBadge(ramStatus = ramStatus)
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Allocated: ${"%.1f".format(minRam)} GB – ${"%.1f".format(maxRam)} GB", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun RamStatusBadge(ramStatus: RamStatus) {
    val (label, bgColor, fgColor) = when (ramStatus) {
        RamStatus.RECOMMENDED -> Triple("Recommended", Color(0x1B4CAF50), Color(0xFF4CAF50))
        RamStatus.HIGH -> Triple("High — may cause instability", Color(0x1BFF9800), Color(0xFFFF9800))
        RamStatus.NOT_RECOMMENDED -> Triple("Not Recommended — risk of system issues", Color(0x1BF44336), Color(0xFFF44336))
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = fgColor, fontWeight = FontWeight.SemiBold)
        }
    }
}
