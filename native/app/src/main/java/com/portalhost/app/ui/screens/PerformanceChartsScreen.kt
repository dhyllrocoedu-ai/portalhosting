package com.portalhost.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.app.server.ProcessStats
import kotlin.math.roundToInt

private const val MAX_POINTS = 60

data class StatsSnapshot(
    val timestamp: Long,
    val cpuPercent: Float,
    val ramMb: Float,
    val tps: Float,
    val rxBytesPerSec: Long,
    val txBytesPerSec: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceChartsScreen(
    currentStats: ProcessStats,
    history: List<StatsSnapshot>,
    onBack: () -> Unit
) {
    val allPoints = remember(history, currentStats) {
        val snapshot = StatsSnapshot(
            timestamp = System.currentTimeMillis(),
            cpuPercent = currentStats.cpuPercent,
            ramMb = currentStats.ramBytes / (1024f * 1024f),
            tps = currentStats.tps,
            rxBytesPerSec = currentStats.rxBytesPerSec,
            txBytesPerSec = currentStats.txBytesPerSec
        )
        (history + snapshot).takeLast(MAX_POINTS)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance History") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Last ${allPoints.size} samples (~${allPoints.size * 3}s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (allPoints.size < 2) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Text("Collecting data... start your server to see performance charts.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall)
                }
            } else {
                MiniLineChart("CPU %", allPoints.map { it.cpuPercent.toDouble() }, Color(0xFF4CAF50), maxValue = 100.0)
                MiniLineChart("RAM (MB)", allPoints.map { it.ramMb.toDouble() }, Color(0xFF2196F3))
                MiniLineChart("TPS", allPoints.map { it.tps.toDouble() }, Color(0xFFFF9800), minValue = 0.0, maxValue = 20.0)
            }
        }
    }
}

@Composable
private fun MiniLineChart(
    label: String,
    data: List<Double>,
    lineColor: Color,
    minValue: Double? = null,
    maxValue: Double? = null
) {
    val textMeasurer = rememberTextMeasurer()
    val actualMin = minValue ?: (data.minOrNull() ?: 0.0)
    val actualMax = maxValue ?: (data.maxOrNull() ?: 100.0)
    val range = (actualMax - actualMin).coerceAtLeast(1.0)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            val current = data.lastOrNull() ?: 0.0
            Text("${"%.1f".format(current)} ${if (label == "CPU %") "" else if (label == "TPS") "" else ""}",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                val w = size.width
                val h = size.height
                val padding = 4.dp.toPx()
                val chartW = w - padding * 2
                val chartH = h - padding * 2
                val stepX = if (data.size > 1) chartW / (data.size - 1) else chartW

                val path = Path()
                data.forEachIndexed { i, value ->
                    val x = padding + i * stepX
                    val y = padding + chartH - ((value - actualMin) / range * chartH).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(path, color = lineColor, style = Stroke(width = 2.dp.toPx()))
                // Draw dots
                data.forEachIndexed { i, value ->
                    val x = padding + i * stepX
                    val y = padding + chartH - ((value - actualMin) / range * chartH).toFloat()
                    drawCircle(lineColor, radius = 2.dp.toPx() / 2, center = Offset(x, y))
                }
            }
        }
    }
}
