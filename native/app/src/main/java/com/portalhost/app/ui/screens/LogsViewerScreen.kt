package com.portalhost.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsViewerScreen(
    serverDir: File?,
    onBack: () -> Unit
) {
    var selectedLog by remember { mutableStateOf<File?>(null) }
    var logContent by remember { mutableStateOf("") }

    val logsDir = if (serverDir != null) File(serverDir, "logs") else null
    var logFiles by remember(logsDir) {
        mutableStateOf(logsDir?.listFiles()?.filter { it.name.endsWith(".log") || it.name.endsWith(".txt") || it.name.endsWith(".gz") }?.sortedByDescending { it.lastModified() } ?: emptyList())
    }

    LaunchedEffect(selectedLog) {
        selectedLog?.let { file ->
            logContent = try {
                if (file.name.endsWith(".gz")) {
                    java.util.zip.GZIPInputStream(file.inputStream()).use { it.readBytes().toString(Charsets.UTF_8) }
                } else {
                    file.readText()
                }
            } catch (e: Exception) { "Error reading file: ${e.message}" }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedLog != null) selectedLog!!.name else "Server Logs") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedLog != null) { selectedLog = null } else onBack()
                    }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        if (selectedLog == null) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item { Text("Log Files (${logFiles.size})", style = MaterialTheme.typography.titleSmall); Spacer(Modifier.height(8.dp)) }
                if (logFiles.isEmpty()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Text("No log files found.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                items(logFiles, key = { it.absolutePath }) { file ->
                    Surface(
                        onClick = { selectedLog = file },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Article, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(file.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(formatFileSize(file.length()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
                Text(
                    text = logContent,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
}
