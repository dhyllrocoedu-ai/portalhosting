package com.portalhost.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class LogLevel { ALL, ERROR, WARN, INFO, PLAYER, CHAT, OTHER }

private val ALL_LEVELS = LogLevel.entries.toList()

fun classifyLogLevel(line: String): LogLevel {
    val upper = line.uppercase()
    return when {
        upper.contains("[SEVERE]") || upper.contains("[ERROR]") || upper.contains("[FATAL]") -> LogLevel.ERROR
        upper.contains("[WARN]") || upper.contains("[WARNING]") -> LogLevel.WARN
        upper.contains("JOINED THE GAME") || upper.contains("LEFT THE GAME") -> LogLevel.PLAYER
        line.contains("<") && line.contains(">") -> LogLevel.CHAT
        upper.contains("[DEBUG]") || upper.contains("[FINE]") || upper.contains("[FINER]") ||
            upper.contains("[FINEST]") || upper.contains("[TRACE]") || upper.contains("[VERBOSE]") ||
            upper.contains("[INFO]") || upper.contains("[NOTICE]") || upper.contains("[CONFIG]") ||
            upper.contains("]: ") -> LogLevel.INFO
        else -> LogLevel.OTHER
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleScreen(
    consoleLines: List<String>,
    onCommand: (String) -> Unit,
    isOnline: Boolean,
    serverDir: File? = null,
    onBack: (() -> Unit)? = null,
    isFullScreen: Boolean = false
) {
    var commandInput by remember { mutableStateOf("") }
    var commandHistory by remember { mutableStateOf(listOf<String>()) }
    var historyIndex by remember { mutableIntStateOf(-1) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(listOf<Int>()) }
    var currentSearchIdx by remember { mutableIntStateOf(0) }
    var activeLevel by remember { mutableStateOf(LogLevel.ALL) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var isNearBottom by remember { mutableStateOf(true) }

    // Apply log level filter
    val levelFiltered = if (activeLevel == LogLevel.ALL) consoleLines
        else consoleLines.filter { classifyLogLevel(it) == activeLevel }

    // Apply search on top of level filter
    val displayLines = if (searchQuery.isNotBlank()) {
        levelFiltered.filterIndexed { idx, _ ->
            val originalIdx = if (activeLevel == LogLevel.ALL) idx
                else consoleLines.indexOf(levelFiltered[idx])
            originalIdx in searchResults
        }
    } else levelFiltered

    // Search logic (indexes into original consoleLines)
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

    // Track scroll position
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= info.totalItemsCount - 3
        }
            .distinctUntilChanged()
            .collect { near -> isNearBottom = near }
    }

    // Auto-scroll when new lines arrive AND user is near bottom
    LaunchedEffect(consoleLines.size) {
        if (showSearch || searchQuery.isNotBlank()) return@LaunchedEffect
        if (consoleLines.isEmpty()) return@LaunchedEffect
        if (!isNearBottom) return@LaunchedEffect
        try {
            listState.scrollToItem(displayLines.size - 1)
        } catch (_: Exception) {}
    }

    // Timestamp-based log rotation: auto-save every 500 new lines
    var lastSavedCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(consoleLines.size) {
        val serverDirVal = serverDir ?: return@LaunchedEffect
        val count = consoleLines.size
        if (count - lastSavedCount >= 500) {
            lastSavedCount = count
            val logsDir = File(serverDirVal, "logs")
            logsDir.mkdirs()
            // Rotate if current log > 2 MB
            val existing = logsDir.listFiles()?.filter { it.name.startsWith("console_") && it.name.endsWith(".log") }
            existing?.forEach { file ->
                if (file.length() > 2 * 1024 * 1024) {
                    val ts = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date(file.lastModified()))
                    file.renameTo(File(logsDir, "console_$ts.log"))
                }
            }
            val ts = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val logFile = File(logsDir, "console_$ts.log")
            try {
                logFile.writeText(consoleLines.joinToString("\n"))
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
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
                            keyboardActions = KeyboardActions(onSearch = { }),
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
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (onBack != null) {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                            Text("Console")
                        }
                    }
                },
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
            // ── Log level filter chips ──
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ALL_LEVELS.forEach { level ->
                    FilterChip(
                        selected = activeLevel == level,
                        onClick = { activeLevel = level },
                        label = { Text(level.name, fontSize = 11.sp) },
                        leadingIcon = if (activeLevel == level) {{ Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }} else null
                    )
                }
            }

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
                            color = consoleLineColor(line),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Scroll-to-bottom button when scrolled up
                if (!isNearBottom && !showSearch) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable {
                                scope.launch {
                                    listState.animateScrollToItem(displayLines.size - 1)
                                }
                            },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        tonalElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Scroll to bottom",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
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

/** Color-code a console line based on its content. */
fun consoleLineColor(line: String): Color {
    val upper = line.uppercase()
    return when {
        upper.contains("[SEVERE]") || upper.contains("[ERROR]") || upper.contains("[FATAL]") ||
            upper.startsWith("[ERROR]") -> Color(0xFFFF4444)
        upper.contains("[WARN]") || upper.contains("[WARNING]") -> Color(0xFFFFAA00)
        upper.contains("JOINED THE GAME") -> Color(0xFF55FFFF)
        upper.contains("LEFT THE GAME") -> Color(0xFFFFFF55)
        line.contains("<") && line.contains(">") -> Color(0xFFAA55FF)
        upper.contains("[DEBUG]") || upper.contains("[FINE]") || upper.contains("[FINER]") ||
            upper.contains("[FINEST]") || upper.contains("[TRACE]") || upper.contains("[VERBOSE]") -> Color(0xFF666666)
        upper.contains("[INFO]") || upper.contains("[NOTICE]") || upper.contains("[CONFIG]") ||
            upper.contains("]: ") -> Color(0xFFAAAAAA)
        else -> Color(0xFF00FF41)
    }
}

private fun saveLogs(context: Context, lines: List<String>, serverDir: File?) {
    val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
    val text = lines.joinToString("\n")

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && serverDir != null) {
        val logsDir = File(serverDir, "logs")
        logsDir.mkdirs()
        val logFile = File(logsDir, "console_$timestamp.log")
        logFile.writeText(text)
    } else {
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
