package com.portalhost.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleScreen(
    consoleLines: List<String>,
    onCommand: (String) -> Unit,
    isOnline: Boolean,
    serverDir: File? = null
) {
    var commandInput by remember { mutableStateOf("") }
    var commandHistory by remember { mutableStateOf(listOf<String>()) }
    var historyIndex by remember { mutableIntStateOf(-1) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(listOf<Int>()) }
    var currentSearchIdx by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Filtered lines for search
    val displayLines = if (searchQuery.isNotBlank()) {
        consoleLines.filterIndexed { idx, _ -> idx in searchResults }
    } else consoleLines

    // Search logic
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            searchResults = consoleLines.mapIndexedNotNull { idx, line ->
                if (line.contains(searchQuery, ignoreCase = true)) idx else null
            }
            currentSearchIdx = 0
        } else {
            searchResults = emptyList()
        }
    }

    // Auto-scroll (only when not in search mode and at bottom)
    LaunchedEffect(consoleLines.size) {
        if (showSearch || searchQuery.isNotBlank()) return@LaunchedEffect
        if (consoleLines.isEmpty()) return@LaunchedEffect
        val lastIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        val totalItems = listState.layoutInfo.totalItemsCount
        if (lastIndex == null || lastIndex >= totalItems - 2) {
            scope.launch { listState.animateScrollToItem(consoleLines.size - 1) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { if (showSearch) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search logs...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { /* trigger handled by LaunchedEffect */ }),
                        trailingIcon = {
                            if (searchQuery.isNotBlank() && searchResults.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${currentSearchIdx + 1}/${searchResults.size}", style = MaterialTheme.typography.labelSmall)
                                    IconButton(onClick = { currentSearchIdx = (currentSearchIdx + 1).coerceAtMost(searchResults.size - 1) }) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next")
                                    }
                                    IconButton(onClick = { currentSearchIdx = (currentSearchIdx - 1).coerceAtLeast(0) }) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Prev")
                                    }
                                }
                            }
                            IconButton(onClick = { showSearch = false; searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Close search")
                            }
                        }
                    )
                } else Text("Console") },
                actions = {
                    if (!showSearch) {
                        IconButton(onClick = { showSearch = true }) { Icon(Icons.Default.Search, contentDescription = "Search") }
                        IconButton(onClick = { copyLogs(context, consoleLines) }) { Icon(Icons.Default.ContentCopy, contentDescription = "Copy") }
                        IconButton(onClick = { saveLogs(context, consoleLines, serverDir) }) { Icon(Icons.Default.Save, contentDescription = "Save") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // Search result highlight
            if (searchResults.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    val currentLine = searchResults.getOrNull(currentSearchIdx)?.let { consoleLines.getOrNull(it) } ?: ""
                    Text(
                        "Match ${currentSearchIdx + 1}: $currentLine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0D0D0D))
                    .padding(8.dp)
            ) {
                LazyColumn(state = listState) {
                    items(displayLines) { line ->
                        Text(
                            text = line,
                            color = Color(0xFF00FF41),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Command input
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = {
                            commandInput = it
                            historyIndex = -1
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter command...") },
                        singleLine = true,
                        enabled = isOnline,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (commandInput.isNotBlank() && isOnline) {
                                    onCommand(commandInput)
                                    commandHistory = (commandHistory + commandInput).take(100)
                                    commandInput = ""
                                    historyIndex = -1
                                }
                            }
                        )
                    )
                    Spacer(Modifier.width(4.dp))
                    // Up arrow (command history previous)
                    IconButton(
                        onClick = {
                            if (commandHistory.isNotEmpty()) {
                                val newIdx = if (historyIndex == -1) commandHistory.size - 1
                                    else (historyIndex - 1).coerceAtLeast(0)
                                historyIndex = newIdx
                                commandInput = commandHistory[newIdx]
                            }
                        },
                        enabled = isOnline && commandHistory.isNotEmpty(),
                        modifier = Modifier.size(36.dp)
                    ) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous", modifier = Modifier.size(20.dp)) }
                    // Down arrow (command history next)
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
                        enabled = isOnline && historyIndex >= 0,
                        modifier = Modifier.size(36.dp)
                    ) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next", modifier = Modifier.size(20.dp)) }
                    IconButton(
                        onClick = {
                            if (commandInput.isNotBlank() && isOnline) {
                                onCommand(commandInput)
                                commandHistory = (commandHistory + commandInput).take(100)
                                commandInput = ""
                                historyIndex = -1
                            }
                        },
                        enabled = isOnline
                    ) { Icon(Icons.Default.Send, contentDescription = "Send") }
                }
            }
        }
    }
}

private fun copyLogs(context: Context, lines: List<String>) {
    val text = lines.joinToString("\n").takeLast(50000)
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Console Logs", text))
}

private fun saveLogs(context: Context, lines: List<String>, serverDir: File?) {
    val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
    val text = lines.joinToString("\n")

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && serverDir != null) {
        // Save to server's logs directory
        val logsDir = File(serverDir, "logs")
        logsDir.mkdirs()
        val logFile = File(logsDir, "console_$timestamp.log")
        logFile.writeText(text)
    } else {
        // Fallback: save to Downloads via MediaStore
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "console_$timestamp.log")
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out ->
                out.write(text.toByteArray())
            }
        }
    }
}
