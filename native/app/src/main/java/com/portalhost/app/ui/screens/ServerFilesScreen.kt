@file:Suppress("DEPRECATION")

package com.portalhost.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class FileEntry(
    val file: File,
    val isDirectory: Boolean = file.isDirectory,
    val size: Long = if (file.isFile) file.length() else 0L,
    val lastModified: Long = file.lastModified()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerFilesScreen(
    serverName: String,
    serverDir: File,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentDir by remember { mutableStateOf(serverDir) }
    var entries by remember { mutableStateOf(listOf<FileEntry>()) }
    var sortBy by remember { mutableStateOf("name") }
    var sortAsc by remember { mutableStateOf(true) }

    // Dialogs
    var renameTarget by remember { mutableStateOf<File?>(null) }
    var renameText by remember { mutableStateOf("") }
    var compressTarget by remember { mutableStateOf<File?>(null) }
    var compressResult by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<File?>(null) }
    var undoFile by remember { mutableStateOf<File?>(null) }
    var undoFiles by remember { mutableStateOf<List<File>?>(null) }
    var editingFile by remember { mutableStateOf<File?>(null) }
    var editorValue by remember { mutableStateOf(TextFieldValue("")) }
    var showSnackbar by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFiles by remember { mutableStateOf(setOf<String>()) }
    var batchDeleteTargets by remember { mutableStateOf<List<File>?>(null) }
    val selectionMode = selectedFiles.isNotEmpty()

    // Export launcher (for a single file)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            val exportFile = compressTarget
            if (exportFile != null) {
                copyToUri(context, exportFile, uri)
                showSnackbar = "Exported ${exportFile.name}"
                compressTarget = null
            }
        }
    }

    // Breadcrumb path
    val breadcrumbs = remember(currentDir) {
        buildList {
            var dir = currentDir
            while (dir != serverDir.parentFile) {
                add(dir)
                dir = dir.parentFile ?: break
            }
            add(serverDir)
            reverse()
        }
    }

    // Import launcher (multi-select)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        for (uri in uris) {
            importFile(context, uri, currentDir)
        }
        if (uris.isNotEmpty()) {
            refreshDir(currentDir, sortBy, sortAsc) { entries = it }
        }
    }

    // Refresh on directory change
    LaunchedEffect(currentDir, sortBy, sortAsc) {
        val result = withContext(Dispatchers.IO) {
            val files = currentDir.listFiles()?.toList() ?: emptyList()
            val mapped = files.map { FileEntry(it) }
            val sorted = when (sortBy) {
                "name" -> if (sortAsc) mapped.sortedBy { it.file.name.lowercase() } else mapped.sortedByDescending { it.file.name.lowercase() }
                "date" -> if (sortAsc) mapped.sortedBy { it.lastModified } else mapped.sortedByDescending { it.lastModified }
                "size" -> if (sortAsc) mapped.sortedBy { it.size } else mapped.sortedByDescending { it.size }
                else -> mapped
            }
            sorted.partition { it.isDirectory }.let { (dirs, files) -> dirs + files }
        }
        entries = result
    }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(showSnackbar) {
        if (showSnackbar.isNotBlank()) {
            val hasUndo = undoFile != null || undoFiles != null
            val result = snackbarHostState.showSnackbar(
                message = showSnackbar,
                actionLabel = if (hasUndo) "Undo" else null
            )
            if (result == SnackbarResult.ActionPerformed) {
                if (undoFile != null) {
                    val uf = undoFile!!
                    val original = File(uf.parentFile?.parentFile, uf.name)
                    uf.renameTo(original)
                    undoFile = null
                } else if (undoFiles != null) {
                    undoFiles!!.forEach { trashed ->
                        val original = File(trashed.parentFile?.parentFile, trashed.name)
                        trashed.renameTo(original)
                    }
                    undoFiles = null
                }
                refreshDir(currentDir, sortBy, sortAsc) { entries = it }
            } else if (undoFile != null) {
                // Dismissed without Undo — permanently delete trash
                val uf = undoFile!!
                uf.deleteRecursively()
                undoFile = null
            } else if (undoFiles != null) {
                undoFiles!!.forEach { it.deleteRecursively() }
                undoFiles = null
            }
            showSnackbar = ""
        }
    }

    val filteredEntries = remember(entries, searchQuery) {
        if (searchQuery.isBlank()) entries
        else entries.filter { it.file.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = { Text("${selectedFiles.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { selectedFiles = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            selectedFiles = if (selectedFiles.size == filteredEntries.size) emptySet()
                            else filteredEntries.map { it.file.absolutePath }.toSet()
                        }) {
                            Text(if (selectedFiles.size == filteredEntries.size) "Deselect all" else "Select all")
                        }
                        if (selectedFiles.isNotEmpty()) {
                            IconButton(onClick = {
                                batchDeleteTargets = selectedFiles.map { File(it) }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            } else {
                TopAppBar(
                    title = { Text(serverName) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        },
        floatingActionButton = {
            SmallFloatingActionButton(onClick = {
                importLauncher.launch(arrayOf("*/*"))
            }) {
                Icon(Icons.Default.FileDownload, contentDescription = "Import")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Breadcrumbs
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    breadcrumbs.forEachIndexed { index, dir ->
                        if (index > 0) {
                            Text(" > ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = if (index == 0) serverName else dir.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (index == breadcrumbs.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable {
                                currentDir = dir
                            }
                        )
                    }
                }
            }

            // Sort controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sort:", style = MaterialTheme.typography.labelSmall)
                FilterChip(
                    selected = sortBy == "name",
                    onClick = {
                        if (sortBy == "name") sortAsc = !sortAsc else { sortBy = "name"; sortAsc = true }
                    },
                    label = { Text("Name ${if (sortBy == "name") if (sortAsc) "↑" else "↓" else ""}", style = MaterialTheme.typography.labelSmall) }
                )
                FilterChip(
                    selected = sortBy == "date",
                    onClick = {
                        if (sortBy == "date") sortAsc = !sortAsc else { sortBy = "date"; sortAsc = false }
                    },
                    label = { Text("Date ${if (sortBy == "date") if (sortAsc) "↑" else "↓" else ""}", style = MaterialTheme.typography.labelSmall) }
                )
                FilterChip(
                    selected = sortBy == "size",
                    onClick = {
                        if (sortBy == "size") sortAsc = !sortAsc else { sortBy = "size"; sortAsc = false }
                    },
                    label = { Text("Size ${if (sortBy == "size") if (sortAsc) "↑" else "↓" else ""}", style = MaterialTheme.typography.labelSmall) }
                )
            }

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                placeholder = { Text("Search files...") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {{
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }} else null
            )

            // File list
            if (filteredEntries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (searchQuery.isNotBlank()) "No matching files" else "Empty directory", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    // Parent directory
                    if (currentDir != serverDir) {
                        item {
                            ListItem(
                                headlineContent = { Text("..") },
                                leadingContent = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                                modifier = Modifier.clickable {
                                    if (selectionMode) selectedFiles = emptySet()
                                    currentDir = currentDir.parentFile ?: serverDir
                                }
                            )
                        }
                    }

                    items(filteredEntries, key = { it.file.absolutePath }) { entry ->
                        FileRow(
                            entry = entry,
                            isSelected = entry.file.absolutePath in selectedFiles,
                            selectionMode = selectionMode,
                            onToggleSelect = {
                                val path = entry.file.absolutePath
                                selectedFiles = if (path in selectedFiles) selectedFiles - path
                                else selectedFiles + path
                            },
                            onClick = {
                                if (selectionMode) {
                                    val path = entry.file.absolutePath
                                    selectedFiles = if (path in selectedFiles) selectedFiles - path
                                    else selectedFiles + path
                                } else if (entry.isDirectory) {
                                    currentDir = entry.file
                                } else if (isEditableFile(entry.file)) {
                                    if (entry.file.length() > 10 * 1024 * 1024) {
                                        showSnackbar = "File too large to edit (>10MB)"
                                    } else {
                                        editingFile = entry.file
                                        val text = entry.file.readText()
                                        editorValue = TextFieldValue(text, selection = TextRange(text.length))
                                    }
                                } else {
                                    showSnackbar = "Cannot edit binary file"
                                }
                            },
                            onRename = { renameTarget = it; renameText = it.name },
                            onShare = { shareFile(context, it) },
                            onEdit = {
                                if (isEditableFile(it)) {
                                    if (it.length() > 10 * 1024 * 1024) {
                                        showSnackbar = "File too large to edit (>10MB)"
                                    } else {
                                        editingFile = it
                                        val text = it.readText()
                                        editorValue = TextFieldValue(text, selection = TextRange(text.length))
                                    }
                                } else {
                                    showSnackbar = "Cannot edit binary file"
                                }
                            },
                            onCompress = { compressTarget = it; compress(context, it) {
                                compressResult = it; showSnackbar = it; refreshDir(currentDir, sortBy, sortAsc) { entries = it }
                            } },
                            onExport = { compressTarget = it; exportLauncher.launch("${it.name}.zip") },
                            onDelete = { deleteTarget = it }
                        )
                    }
                }
            }
        }
    }

    // Rename dialog
    renameTarget?.let { file ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("New name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameText.isNotBlank() && renameText != file.name) {
                        val newFile = File(file.parent, renameText)
                        if (file.renameTo(newFile)) {
                            showSnackbar = "Renamed to ${newFile.name}"
                        } else {
                            showSnackbar = "Rename failed"
                        }
                    }
                    renameTarget = null
                    refreshDir(currentDir, sortBy, sortAsc) { entries = it }
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            }
        )
    }

    // File editor dialog
    editingFile?.let { file ->
        Dialog(
            onDismissRequest = { editingFile = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Editor top bar
                    TopAppBar(
                        title = { Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        navigationIcon = {
                            IconButton(onClick = { editingFile = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        },
                        actions = {
                            TextButton(onClick = {
                                try {
                                    file.writeText(editorValue.text)
                                    showSnackbar = "Saved ${file.name}"
                                    editingFile = null
                                    refreshDir(currentDir, sortBy, sortAsc) { entries = it }
                                } catch (e: Exception) {
                                    showSnackbar = "Save failed: ${e.message}"
                                }
                            }) { Text("Save") }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                    // Editor content
                    OutlinedTextField(
                        value = editorValue,
                        onValueChange = { editorValue = it },
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = Color(0xFFE0E0E0)
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E1E1E),
                            unfocusedContainerColor = Color(0xFF1E1E1E),
                            focusedBorderColor = Color(0xFF555555),
                            unfocusedBorderColor = Color(0xFF333333),
                            cursorColor = Color(0xFFE0E0E0)
                        )
                    )
                }
            }
        }
    }

    // Delete dialog
    deleteTarget?.let { file ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete") },
            text = { Text("Delete \"${file.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    val trashDir = File(serverDir, ".trash")
                    trashDir.mkdirs()
                    val trashed = File(trashDir, file.name)
                    var dest = trashed
                    var counter = 1
                    while (dest.exists()) {
                        dest = File(trashDir, "${file.name}_$counter")
                        counter++
                    }
                    file.renameTo(dest)
                    deleteTarget = null
                    undoFile = dest
                    showSnackbar = "Deleted ${file.name}"
                    refreshDir(currentDir, sortBy, sortAsc) { entries = it }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Batch delete dialog
    batchDeleteTargets?.let { files ->
        AlertDialog(
            onDismissRequest = { batchDeleteTargets = null },
            title = { Text("Delete ${files.size} file${if (files.size != 1) "s" else ""}") },
            text = { Text("Delete ${files.size} selected file${if (files.size != 1) "s" else ""}?") },
            confirmButton = {
                TextButton(onClick = {
                    val trashDir = File(serverDir, ".trash")
                    trashDir.mkdirs()
                    val trashedFiles = files.map { file ->
                        val trashed = File(trashDir, file.name)
                        var dest = trashed
                        var counter = 1
                        while (dest.exists()) {
                            dest = File(trashDir, "${file.name}_$counter")
                            counter++
                        }
                        file.renameTo(dest)
                        dest
                    }
                    batchDeleteTargets = null
                    selectedFiles = emptySet()
                    undoFiles = trashedFiles
                    showSnackbar = "Deleted ${files.size} file${if (files.size != 1) "s" else ""}"
                    refreshDir(currentDir, sortBy, sortAsc) { entries = it }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { batchDeleteTargets = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FileRow(
    entry: FileEntry,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onClick: () -> Unit,
    onRename: (File) -> Unit,
    onShare: (File) -> Unit,
    onEdit: (File) -> Unit,
    onCompress: (File) -> Unit,
    onExport: (File) -> Unit,
    onDelete: (File) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(entry.file.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            if (!entry.isDirectory) {
                Text(formatFileSize(entry.size), style = MaterialTheme.typography.bodySmall)
            }
        },
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectionMode) {
                    Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() })
                    Spacer(Modifier.width(8.dp))
                }
                val icon = if (entry.isDirectory) Icons.Default.Folder else fileIcon(entry.file.name)
                Icon(icon, contentDescription = null, tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        trailingContent = {
            Row {
                if (!selectionMode && entry.isDirectory) {
                    IconButton(onClick = onClick) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Open", modifier = Modifier.size(20.dp))
                    }
                }
                if (!selectionMode) {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            offset = DpOffset(0.dp, 0.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = { menuExpanded = false; onEdit(entry.file) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = { menuExpanded = false; onRename(entry.file) },
                                leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) }
                            )
                            if (!entry.isDirectory) {
                                DropdownMenuItem(
                                    text = { Text("Share") },
                                    onClick = { menuExpanded = false; onShare(entry.file) },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Compress") },
                                onClick = { menuExpanded = false; onCompress(entry.file) },
                                leadingIcon = { Icon(Icons.Default.Compress, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Export") },
                                onClick = { menuExpanded = false; onExport(entry.file) },
                                leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = { menuExpanded = false; onDelete(entry.file) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }
        },
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = { if (!selectionMode) onToggleSelect() }
        )
    )
}

private fun refreshDir(
    dir: File,
    sortBy: String,
    sortAsc: Boolean,
    onResult: (List<FileEntry>) -> Unit
) {
    val files = dir.listFiles()?.toList() ?: emptyList()
    val mapped = files.map { FileEntry(it) }
    val sorted = when (sortBy) {
        "name" -> if (sortAsc) mapped.sortedBy { it.file.name.lowercase() } else mapped.sortedByDescending { it.file.name.lowercase() }
        "date" -> if (sortAsc) mapped.sortedBy { it.lastModified } else mapped.sortedByDescending { it.lastModified }
        "size" -> if (sortAsc) mapped.sortedBy { it.size } else mapped.sortedByDescending { it.size }
        else -> mapped
    }
    val result = sorted.partition { it.isDirectory }.let { (dirs, files) -> dirs + files }
    onResult(result)
}

private val EDITABLE_EXTENSIONS = setOf(
    "properties", "yml", "yaml", "xml", "json", "txt", "log",
    "sh", "bat", "toml", "cfg", "conf", "ini", "md", "env",
    "gitignore", "dockerignore", "svg", "css", "html", "js", "ts",
    "kts", "gradle", "java", "kt", "py", "lua", "mcfunction"
)

private fun isEditableFile(file: File): Boolean {
    if (file.isDirectory || !file.exists()) return false
    val ext = file.extension.lowercase()
    return ext in EDITABLE_EXTENSIONS || file.name == "eula.txt" || file.name == "server.properties"
}

private fun fileIcon(name: String) = when {
    name.endsWith(".jar") -> Icons.Default.Archive
    name.endsWith(".properties") || name.endsWith(".yml") || name.endsWith(".xml") || name.endsWith(".json") -> Icons.Default.Code
    name.endsWith(".txt") || name.endsWith(".log") -> Icons.Default.Description
    name.endsWith(".zip") || name.endsWith(".tar.gz") || name.endsWith(".gz") -> Icons.Default.Compress
    name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif") -> Icons.Default.Image
    name.endsWith(".sh") || name.endsWith(".bat") -> Icons.Default.Terminal
    else -> Icons.Default.InsertDriveFile
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
    else -> "${"%.2f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
}

private fun resolveFileName(context: Context, uri: Uri): String {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                val name = cursor.getString(nameIndex)
                if (!name.isNullOrBlank()) name else "imported_file"
            } else "imported_file"
        } ?: "imported_file"
    } catch (_: Exception) {
        uri.lastPathSegment?.substringAfterLast('/') ?: "imported_file"
    }
}

private fun resolveDestFile(destDir: File, name: String): File {
    val dest = File(destDir, name)
    if (!dest.exists()) return dest
    val base = name.substringBeforeLast('.')
    val ext = name.substringAfterLast('.', "")
    var count = 1
    while (true) {
        val candidate = if (ext.isEmpty()) "${base}_$count" else "${base}_$count.$ext"
        val file = File(destDir, candidate)
        if (!file.exists()) return file
        count++
    }
}

private fun importFile(context: Context, uri: Uri, destDir: File) {
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val name = resolveFileName(context, uri)
            val dest = resolveDestFile(destDir, name)
            dest.outputStream().use { output -> input.copyTo(output) }
        }
    } catch (e: Exception) {
        android.util.Log.e("ServerFiles", "Import failed: ${e.message}")
    }
}

private fun shareFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${file.name}"))
    } catch (e: Exception) {
        android.util.Log.e("ServerFiles", "Share failed: ${e.message}")
    }
}

private fun compress(context: Context, file: File, onResult: (String) -> Unit) {
    try {
        val zipFile = File(file.parentFile, "${file.name}.zip")
        var count = 1
        while (zipFile.exists()) {
            val baseName = file.name.removeSuffix(".zip")
            val zipFile2 = File(file.parentFile, "${baseName}_$count.zip")
            if (!zipFile2.exists()) break
            count++
        }
        val target = if (!zipFile.exists()) zipFile
            else File(file.parentFile, "${file.name.removeSuffix(".zip")}_$count.zip")

        ZipOutputStream(FileOutputStream(target)).use { zos ->
            if (file.isDirectory) {
                addDirToZip(zos, file, file.name)
            } else {
                addFileToZip(zos, file, file.name)
            }
        }
        onResult("Compressed to ${target.name}")
    } catch (e: Exception) {
        android.util.Log.e("ServerFiles", "Compress failed: ${e.message}")
        onResult("Compress failed: ${e.message}")
    }
}

private fun addDirToZip(zos: ZipOutputStream, dir: File, basePath: String) {
    val entries = dir.listFiles() ?: return
    for (file in entries) {
        if (file.isDirectory) {
            addDirToZip(zos, file, "$basePath/${file.name}")
        } else {
            addFileToZip(zos, file, "$basePath/${file.name}")
        }
    }
}

private fun addFileToZip(zos: ZipOutputStream, file: File, entryName: String) {
    zos.putNextEntry(ZipEntry(entryName))
    FileInputStream(file).use { input -> input.copyTo(zos) }
    zos.closeEntry()
}

private fun copyToUri(context: Context, file: File, uri: Uri) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            if (file.isDirectory) {
                val zipFile = File(context.cacheDir, "${file.name}.zip")
                ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                    addDirToZip(zos, file, file.name)
                }
                zipFile.inputStream().use { input -> input.copyTo(output) }
                zipFile.delete()
            } else {
                file.inputStream().use { input -> input.copyTo(output) }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("ServerFiles", "Export failed: ${e.message}")
    }
}
