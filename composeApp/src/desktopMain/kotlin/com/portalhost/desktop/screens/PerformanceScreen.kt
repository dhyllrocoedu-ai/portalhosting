package com.portalhost.desktop.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.portalhost.server.ProcessMonitor
import com.portalhost.server.ProcessStats
import com.portalhost.server.ServerManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun PerformanceScreen(serverId: String, onBack: () -> Unit = {}) {
    val serverManager = koinInject<ServerManager>()
    val serverStates by serverManager.serverStates.collectAsState()
    val state = serverStates[serverId]
    val processMonitor = remember { ProcessMonitor() }
    var stats by remember { mutableStateOf<ProcessStats?>(null) }
    val cpuData = remember { mutableStateListOf<Float>() }
    val ramData = remember { mutableStateListOf<Long>() }
    val scope = rememberCoroutineScope()

    // Poll stats every 2 seconds when server is running
    LaunchedEffect(state?.status == com.portalhost.model.ServerStatus.RUNNING) {
        if (state?.status == com.portalhost.model.ServerStatus.RUNNING) {
            while (state?.status == com.portalhost.model.ServerStatus.RUNNING) {
                val process = serverManager.getProcessForServer(serverId)
                val newStats = processMonitor.getStats(process, state?.maxPlayers?.let { it * 128 } ?: 2048)
                stats = newStats

                // Update chart data
                cpuData.add(newStats.cpuPercent)
                if (cpuData.size > 60) cpuData.removeAt(0)

                ramData.add(newStats.ramBytes)
                if (ramData.size > 60) ramData.removeAt(0)

                delay(2000)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Text("Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        val cpuValue = stats?.cpuPercent?.let { "%.1f".format(it) } ?: "0"
        val ramValue = stats?.ramFormatted ?: "0 B"
        val tpsValue = stats?.tps?.let { "%.1f".format(it) } ?: "20.0"
        val playersValue = "${state?.playersOnline ?: 0}/${state?.maxPlayers ?: 20}"

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) { StatValueCard("CPU", "$cpuValue%", Icons.Filled.Speed, Color(0xFF5C6BC0)) }
            Box(modifier = Modifier.weight(1f)) { StatValueCard("RAM", ramValue, Icons.Filled.Memory, Color(0xFF4CAF50)) }
            Box(modifier = Modifier.weight(1f)) { StatValueCard("TPS", tpsValue, Icons.Filled.Speed, Color(0xFFFF9800)) }
            Box(modifier = Modifier.weight(1f)) { StatValueCard("Players", playersValue, Icons.Filled.People, Color(0xFF42A5F5)) }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(modifier = Modifier.padding(12.dp).fillMaxSize()) {
                Text("CPU Usage", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                LineChart(
                    data = cpuData,
                    maxValue = 100f,
                    color = Color(0xFF5C6BC0),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(modifier = Modifier.padding(12.dp).fillMaxSize()) {
                Text("RAM Usage", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                LineChart(
                    data = ramData.map { it.toFloat() },
                    maxValue = (stats?.maxRamBytes?.coerceAtLeast(1) ?: 1).toFloat(),
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }

        if (stats != null) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Network", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(modifier = Modifier.weight(1f)) { StatMini("RX", stats!!.rxFormatted, Icons.Filled.Speed, Color(0xFF42A5F5)) }
                        Box(modifier = Modifier.weight(1f)) { StatMini("TX", stats!!.txFormatted, Icons.Filled.Speed, Color(0xFFFF9800)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatValueCard(label: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun StatMini(label: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun LineChart(
    data: List<Float>,
    maxValue: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Collecting data...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    Canvas(modifier = modifier) {
        val padding = 4f
        val chartWidth = size.width - padding * 2
        val chartHeight = size.height - padding * 2

        if (chartWidth <= 0 || chartHeight <= 0 || maxValue <= 0) return@Canvas

        val stepX = chartWidth / (data.size - 1).coerceAtLeast(1)

        val path = Path()
        data.forEachIndexed { index, value ->
            val x = padding + index * stepX
            val y = padding + chartHeight - (value / maxValue * chartHeight)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path, color = color, style = Stroke(width = 2f))

        if (data.size > 1) {
            val lastX = padding + (data.size - 1) * stepX
            val lastY = padding + chartHeight - ((data.last() / maxValue) * chartHeight)
            drawCircle(color = color, radius = 3f, center = Offset(lastX, lastY))
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576L -> "%.0f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024L -> "%.0f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
}