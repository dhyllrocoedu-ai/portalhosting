package com.portalhost.desktop.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.server.ActivityLog
import com.portalhost.server.ServerManager
import com.portalhost.server.classifyLogLevel
import com.portalhost.server.consoleLineColor
import com.portalhost.server.LogLevel
import com.portalhost.server.ALL_LOG_LEVELS
import com.portalhost.desktop.util.rememberResourcePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.portalhost.util.pickSaveFile

@Suppress("DEPRECATION")
@Composable
fun ServerConsoleScreen(serverId: String, onBack: () -> Unit = {}) {
    val serverManager = koinInject<ServerManager>()
    val activityLog = koinInject<ActivityLog>()
    val consoleOutputs by serverManager.consoleOutputs.collectAsState()
    val allLines = consoleOutputs[serverId] ?: emptyList()
    val serverName = serverManager.getServerName(serverId) ?: serverId

    val listState = rememberLazyListState()
    var commandInput by remember { mutableStateOf("") }
    var commandHistory by remember { mutableStateOf(listOf<String>()) }
    var historyIndex by remember { mutableIntStateOf(-1) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(listOf<Int>()) }
    var currentSearchIdx by remember { mutableIntStateOf(0) }
    var activeLevel by remember { mutableStateOf(LogLevel.ALL) }
    var wrapLines by remember { mutableStateOf(false) }
    var showTimestamps by remember { mutableStateOf(true) }

    val arrowUpIcon = rememberResourcePainter("/icons/arrow_up_highlighted.png")
    val arrowDownIcon = rememberResourcePainter("/icons/arrow_down_highlighted.png")
    val isUserScrolling = remember { mutableStateOf(false) }
    var isAtBottom by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    val sendCommand: () -> Unit = {
        if (commandInput.isNotBlank()) {
            val process = serverManager.getProcessForServer(serverId)
            if (process != null) {
                try {
                    val writer = process.outputStream.bufferedWriter()
                    writer.write("$commandInput\n")
                    writer.flush()
                } catch (_: Exception) {}
            }
            activityLog.logCommand(serverId, serverName, "console", commandInput)
            commandHistory = (commandHistory + commandInput).take(100)
            commandInput = ""
            historyIndex = -1
        }
    }

    val levelFiltered = if (activeLevel == LogLevel.ALL) allLines
        else allLines.filter { classifyLogLevel(it) == activeLevel }

    val displayLines = if (searchQuery.isNotBlank()) {
        levelFiltered.filterIndexed { idx, _ ->
            val originalIdx = if (activeLevel == LogLevel.ALL) idx
                else allLines.indexOf(levelFiltered[idx])
            originalIdx in searchResults
        }
    } else levelFiltered

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            searchResults = allLines.mapIndexedNotNull { idx, line ->
                if (line.contains(searchQuery, ignoreCase = true)) idx else null
            }
            currentSearchIdx = 0
        } else {
            searchResults = emptyList()
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= info.totalItemsCount - 2
        }
            .distinctUntilChanged()
            .collect { near ->
                isAtBottom = near
                if (near) isUserScrolling.value = false
                if (!near && !isUserScrolling.value) {
                    isUserScrolling.value = true
                }
            }
    }

    LaunchedEffect(displayLines.size) {
        if (!isUserScrolling.value && displayLines.isNotEmpty()) {
            try { listState.scrollToItem(displayLines.size - 1) } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onKeyEvent { keyEvent ->
                if (keyEvent.isCtrlPressed && keyEvent.key == Key.L) {
                    serverManager.clearConsole(serverId)
                    true
                } else {
                    false
                }
            }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            Column {
                Column(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row {
                            if (showSearch && searchResults.isNotEmpty()) {
                                Text(
                                    "${currentSearchIdx + 1}/${searchResults.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            }
                            IconButton(onClick = { showSearch = !showSearch }) {
                                Icon(Icons.Filled.Search, contentDescription = "Search", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = {
                                activeLevel = if (activeLevel == LogLevel.ALL) LogLevel.ERROR else LogLevel.ALL
                            }) {
                                Icon(Icons.Filled.FilterAlt, contentDescription = "Filter", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = {
                                val lines = displayLines.joinToString("\n")
                                scope.launch { clipboard.setText(AnnotatedString(lines)) }
                            }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { wrapLines = !wrapLines }) {
                                Icon(Icons.AutoMirrored.Filled.WrapText, contentDescription = if (wrapLines) "Disable line wrap" else "Enable line wrap", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { showTimestamps = !showTimestamps }) {
                                Icon(Icons.Filled.AccessTime, contentDescription = if (showTimestamps) "Hide timestamps" else "Show timestamps", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = {
                                val lines = displayLines.joinToString("\n")
                                scope.launch {
                                    pickSaveFile(
                                        title = "Save Console Log",
                                        extensionFilter = "Log file" to listOf("log", "txt"),
                                        defaultName = "console_${serverId}_${System.currentTimeMillis()}.log"
                                    )?.writeText(lines)
                                }
                            }) {
                                Icon(Icons.Filled.SaveAlt, contentDescription = "Save", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { serverManager.clearConsole(serverId) }) {
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
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    )
                    if (searchResults.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Match ${currentSearchIdx + 1}: ${allLines.getOrNull(searchResults.getOrNull(currentSearchIdx) ?: -1)?.take(80) ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                            )
                            Row {
                                IconButton(onClick = {
                                    currentSearchIdx = (currentSearchIdx - 1).coerceAtLeast(0)
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(painter = arrowUpIcon, contentDescription = "Previous", modifier = Modifier.size(16.dp), tint = Color.Unspecified)
                                }
                                IconButton(onClick = {
                                    currentSearchIdx = (currentSearchIdx + 1).coerceAtMost(searchResults.size - 1)
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(painter = arrowDownIcon, contentDescription = "Next", modifier = Modifier.size(16.dp), tint = Color.Unspecified)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ALL_LOG_LEVELS.forEach { level ->
                        FilterChip(
                            selected = activeLevel == level,
                            onClick = { activeLevel = level },
                            label = { Text(level.name, fontSize = 11.sp) },
                            leadingIcon = if (activeLevel == level) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF0D0D0D))
                .padding(8.dp),
        ) {
            if (displayLines.isEmpty()) {
                Text(
                    if (searchQuery.isNotBlank()) "No matching lines found"
                    else "Console output will appear here when the server is running...",
                    color = Color(0xFF555555),
                    fontSize = 13.sp,
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    items(count = displayLines.size, key = { index -> "console_$index" }) { index ->
                        val line = displayLines[index]
                        Text(
                            line,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = consoleLineColor(line),
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth()
                .onKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Enter && !keyEvent.isCtrlPressed) {
                        sendCommand()
                        true
                    } else false
                },
            color = Color(0xFF1E1E1E),
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
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
                    keyboardActions = KeyboardActions(onSend = { sendCommand() }),
                )
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        if (commandHistory.isNotEmpty()) {
                            val newIdx = if (historyIndex == -1) commandHistory.size - 1
                                else (historyIndex - 1).coerceAtLeast(0)
                            historyIndex = newIdx
                            commandInput = commandHistory[newIdx]
                        }
                    },
                    modifier = Modifier.size(36.dp),
                ) { Icon(painter = arrowUpIcon, contentDescription = "Previous", modifier = Modifier.size(20.dp), tint = Color.Unspecified) }
                IconButton(
                    onClick = {
                        if (historyIndex >= 0) {
                            val newIdx = historyIndex + 1
                            if (newIdx >= commandHistory.size) {
                                historyIndex = -1
                                commandInput = ""
                            } else {
                                historyIndex = newIdx
                                commandInput = commandHistory[newIdx]
                            }
                        }
                    },
                    modifier = Modifier.size(36.dp),
                ) { Icon(painter = arrowDownIcon, contentDescription = "Next", modifier = Modifier.size(20.dp), tint = Color.Unspecified) }
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        if (isAtBottom) {
                            sendCommand()
                        } else {
                            isUserScrolling.value = false
                            scope.launch {
                                try { listState.animateScrollToItem(displayLines.size - 1) } catch (_: Exception) {}
                            }
                        }
                    },
                    modifier = Modifier.size(36.dp).background(
                        color = if (isAtBottom && commandInput.isNotBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            else if (!isAtBottom) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                            else Color.Transparent,
                        shape = CircleShape
                    ),
                    enabled = if (isAtBottom) commandInput.isNotBlank() else true,
                ) {
                    if (isAtBottom) {
                        Icon(
                            Icons.Filled.Send,
                            contentDescription = "Send command",
                            modifier = Modifier.size(18.dp),
                            tint = if (commandInput.isNotBlank()) Color.White else Color(0xFF555555)
                        )
                    } else {
                        Icon(
                            painter = arrowDownIcon,
                            contentDescription = "Scroll to bottom",
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
