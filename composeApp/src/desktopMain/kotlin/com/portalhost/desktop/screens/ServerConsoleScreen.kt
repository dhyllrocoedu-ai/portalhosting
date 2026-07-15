package com.portalhost.desktop.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.server.ServerManager
import org.koin.compose.koinInject
import java.io.File

@Composable
fun ServerConsoleScreen(serverId: String, onBack: () -> Unit = {}) {
    val serverManager = koinInject<ServerManager>()
    val consoleOutputs by serverManager.consoleOutputs.collectAsState()
    val allLines = consoleOutputs[serverId] ?: emptyList()
    val consoleListState = rememberLazyListState()
    var commandInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var logLevelFilter by remember { mutableStateOf<String?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    val commandHistory = remember { mutableStateListOf<String>() }
    var historyIndex by remember { mutableStateOf(-1) }

    val logLevels = listOf("INFO", "WARN", "ERROR", "DEBUG", "FATAL")
    val filteredLines = if (logLevelFilter != null) {
        allLines.filter { it.contains("[${logLevelFilter}]", ignoreCase = true) || it.contains(logLevelFilter!!, ignoreCase = true) }
    } else {
        allLines
    }
    val displayLines = if (searchQuery.isNotBlank()) {
        filteredLines.filter { it.contains(searchQuery, ignoreCase = true) }
    } else {
        filteredLines
    }

    LaunchedEffect(displayLines.size) {
        if (displayLines.isNotEmpty()) {
            consoleListState.animateScrollToItem(displayLines.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            Column(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Server Console", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row {
                        IconButton(onClick = { showSearch = !showSearch }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { logLevelFilter = logLevelFilter?.let { null } ?: "INFO" }) {
                            Icon(Icons.Filled.FilterAlt, contentDescription = "Filter", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { showHistory = !showHistory }) {
                            Icon(Icons.Filled.History, contentDescription = "History", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = {
                            val lines = displayLines.joinToString("\n")
                            val tempFile = File.createTempFile("console_${serverId}_", ".log")
                            tempFile.writeText(lines)
                        }) {
                            Icon(Icons.Filled.SaveAlt, contentDescription = "Save", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = {
                            serverManager.consoleOutputs.value.toMutableMap()[serverId]?.let {
                                val tempFile = File.createTempFile("console_${serverId}_", ".log")
                                tempFile.writeText(it.joinToString("\n"))
                            }
                        }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = {
                            val map = serverManager.consoleOutputs.value.toMutableMap()
                            map[serverId] = emptyList()
                        }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            if (showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search console...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                )
                Spacer(Modifier.height(4.dp))
            }
            if (logLevelFilter != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    logLevels.forEach { level ->
                        FilterChip(
                            selected = logLevelFilter == level,
                            onClick = { logLevelFilter = if (logLevelFilter == level) null else level },
                            label = { Text(level, fontSize = 11.sp) },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            if (showHistory && commandHistory.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Command History", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { commandHistory.clear() }) { Text("Clear") }
                        }
                        Spacer(Modifier.height(4.dp))
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            items(commandHistory.withIndex().reversed()) { (index, cmd) ->
                                Text(
                                    cmd,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)
                                        .clickable { commandInput = cmd; showHistory = false },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(0.dp),
    ) {
        if (displayLines.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    if (searchQuery.isNotBlank()) "No matching lines found"
                    else "Console output will appear here when the server is running...",
                    color = Color(0xFF888888),
                    fontSize = 13.sp,
                )
            }
        } else {
            LazyColumn(
                state = consoleListState,
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                items(displayLines) { line ->
                    val lineColor = when {
                        line.contains("[ERROR]", ignoreCase = true) || line.contains("[FATAL]", ignoreCase = true) -> Color(0xFFF44336)
                        line.contains("[WARN]", ignoreCase = true) -> Color(0xFFFF9800)
                        line.contains("[INFO]", ignoreCase = true) -> Color(0xFF4CAF50)
                        else -> Color(0xFFD4D4D4)
                    }
                    Text(
                        line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = lineColor,
                    )
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF252526),
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(">", color = Color(0xFF4CAF50), fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = commandInput,
                onValueChange = {
                    commandInput = it
                    historyIndex = -1
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter command...", color = Color(0xFF888888), fontSize = 13.sp) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = Color(0xFFD4D4D4),
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (commandInput.isNotBlank()) {
                            commandHistory.add(0, commandInput)
                            commandInput = ""
                            historyIndex = -1
                        }
                    }
                ),
            )
        }
    }
}