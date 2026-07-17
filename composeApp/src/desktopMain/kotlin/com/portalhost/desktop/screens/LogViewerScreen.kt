package com.portalhost.desktop.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.filesystem.FileSystem
import org.koin.compose.koinInject
import java.io.File
import java.util.zip.GZIPInputStream

@Composable
fun LogViewerScreen(serverId: String, onBack: () -> Unit = {}) {
    val fileSystem = koinInject<FileSystem>()
    var logFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedLog by remember { mutableStateOf<File?>(null) }
    var logContent by remember { mutableStateOf<List<String>>(emptyList()) }
    var logLevelFilter by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberLazyListState()

    val filteredContent = remember(logContent, logLevelFilter) {
        if (logLevelFilter != null) {
            logContent.filter { it.contains("[${logLevelFilter}]", ignoreCase = true) || it.contains(logLevelFilter!!, ignoreCase = true) }
        } else {
            logContent
        }
    }

    LaunchedEffect(serverId) {
        val logsDir = File(fileSystem.getServersDirBlocking(), "$serverId/logs")
        logFiles = if (logsDir.exists()) {
            logsDir.listFiles()?.filter { it.name.endsWith(".log") || it.name.endsWith(".txt") || it.name.endsWith(".gz") }
                ?.sortedByDescending { it.lastModified() } ?: emptyList()
        } else emptyList()

        val latestLog = File(fileSystem.getServersDirBlocking(), "$serverId/logs/latest.log")
        if (latestLog.exists()) {
            selectedLog = latestLog
            logContent = latestLog.readLines().takeLast(500)
        }
    }

    LaunchedEffect(selectedLog) {
        val file = selectedLog ?: return@LaunchedEffect
        logContent = if (file.name.endsWith(".gz")) {
            GZIPInputStream(file.inputStream()).bufferedReader().use { it.readLines() }
        } else {
            file.readLines().takeLast(500)
        }
    }

    LaunchedEffect(logContent.size) {
        if (logContent.isNotEmpty()) {
            scrollState.animateScrollToItem(logContent.size - 1)
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.width(250.dp).fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text("Log Files", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
                
                // Log level filters
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val logLevels = listOf("INFO", "WARN", "ERROR", "DEBUG", "FATAL")
                    logLevels.forEach { level ->
                        FilterChip(
                            selected = logLevelFilter == level,
                            onClick = { logLevelFilter = if (logLevelFilter == level) null else level },
                            label = { Text(level, fontSize = 10.sp) },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                
                if (logFiles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No log files found", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    LazyColumn {
                        items(logFiles) { file ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp).clickable { selectedLog = file },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (file == selectedLog) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        if (file.isDirectory) Icons.Filled.Folder else Icons.Filled.Description,
                                        contentDescription = null,
                                        modifier = Modifier.width(20.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(file.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1)
                                        Text(formatSize(file), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.weight(1f).fillMaxSize(),
            color = Color(0xFF1E1E1E),
        ) {
            if (logContent.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text("Select a log file to view", color = Color(0xFF888888))
                }
            } else {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    items(filteredContent) { line ->
                        val lineColor = when {
                            line.contains("[ERROR]", ignoreCase = true) || line.contains("FATAL") -> Color(0xFFF44336)
                            line.contains("[WARN]", ignoreCase = true) -> Color(0xFFFF9800)
                            line.contains("[INFO]", ignoreCase = true) -> Color(0xFF4CAF50)
                            else -> Color(0xFFD4D4D4)
                        }
                        Text(line, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = lineColor)
                    }
                }
            }
        }
    }
}

private fun formatSize(file: File): String {
    val bytes = file.length()
    return when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.0f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.0f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
}
