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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.server.ServerManager
import com.portalhost.uinotify.ToastManager
import com.portalhost.util.pickFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.io.File
import java.util.LinkedHashMap

data class FileEntry(val file: File, val isDirectory: Boolean)

@Composable
fun ServerFilesScreen(serverId: String, onBack: () -> Unit = {}) {
    val serverManager = koinInject<ServerManager>()
    val rootDir = remember(serverId) { serverManager.getServerDir(serverId) }
    var currentDir by remember(serverId) { mutableStateOf(rootDir) }
    var entries by remember { mutableStateOf<List<FileEntry>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var sortByName by remember { mutableStateOf(true) }
    var deleteTarget by remember { mutableStateOf<File?>(null) }
    var renameTarget by remember { mutableStateOf<File?>(null) }
    var renameText by remember { mutableStateOf("") }
    var editTarget by remember { mutableStateOf<File?>(null) }
    var editContent by remember { mutableStateOf("") }
    var showTrash by remember { mutableStateOf(false) }
    var selectedFiles by remember { mutableStateOf<Set<File>>(emptySet()) }
    val scope = rememberCoroutineScope()
    val toastManager = koinInject<ToastManager>()

    // Trash for undo delete
    val trash = remember { LinkedHashMap<File, File>() }

    fun refresh(dir: File) {
        val files = dir.listFiles()?.toList() ?: emptyList()
        val mapped = files.map { FileEntry(it, it.isDirectory) }
        val sorted = if (sortByName) {
            mapped.sortedWith(compareBy<FileEntry> { !it.isDirectory }.thenBy { it.file.name.lowercase() })
        } else {
            mapped.sortedBy { it.file.length() }
        }
        entries = if (searchQuery.isNotBlank()) {
            sorted.filter { it.file.name.contains(searchQuery, ignoreCase = true) }
        } else {
            sorted
        }
    }

    LaunchedEffect(currentDir, sortByName, searchQuery) {
        refresh(currentDir)
    }

    // Breadcrumbs
    val breadcrumbs = buildList {
        var dir: File? = currentDir
        while (dir != null) {
            add(dir)
            dir = dir.parentFile
        }
    }.reversed()

    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 1.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = {
                        if (currentDir != rootDir) {
                            currentDir = currentDir.parentFile ?: rootDir
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Files", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(8.dp))
                        Text(rootDir.name, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(currentDir.name, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    Row {
                        IconButton(onClick = { refresh(currentDir) }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { showSearch = !showSearch }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { sortByName = !sortByName }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = {
                            scope.launch {
                                val files = pickFile(
                                    title = "Select files to upload",
                                    extensionFilter = "Server files" to listOf("jar", "zip", "json", "yml", "yaml", "properties", "txt", "conf", "config", "toml", "dat", "db", "log"),
                                    multiSelection = true,
                                )
                                if (files.isNotEmpty()) {
                                    withContext(Dispatchers.IO) {
                                        files.forEach { file ->
                                            val target = File(currentDir, file.name)
                                            file.copyTo(target, overwrite = true)
                                        }
                                    }
                                    toastManager.success("Uploaded ${files.size} file(s)")
                                    refresh(currentDir)
                                }
                            }
                        }) {
                            Icon(Icons.Filled.UploadFile, contentDescription = "Upload", modifier = Modifier.size(20.dp))
                        }
                        if (trash.isNotEmpty()) {
                            IconButton(onClick = { showTrash = true }) {
                                Icon(Icons.Filled.Restore, contentDescription = "Trash", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                if (showSearch) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search files...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { refresh(currentDir) }),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear")
                                }
                            }
                        },
                    )
                }
            }
        }

        // Breadcrumbs
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                breadcrumbs.forEach { dir ->
                    Text(
                        text = dir.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )
                    if (dir != currentDir) {
                        Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        HorizontalDivider()

        // File list
        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No files found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                items(entries) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedFiles.contains(entry.file)) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        onClick = {
                            if (entry.isDirectory) {
                                val canonicalRoot = rootDir.canonicalFile
                                val canonicalTarget = entry.file.canonicalFile
                                if (canonicalTarget.path.startsWith(canonicalRoot.path)) {
                                    currentDir = entry.file
                                }
                            }
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (entry.isDirectory) Icons.Filled.Folder else Icons.Filled.Description,
                                contentDescription = null,
                                tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.file.name, style = MaterialTheme.typography.bodyMedium, fontWeight = if (entry.isDirectory) FontWeight.Medium else FontWeight.Normal)
                                Text(
                                    if (entry.isDirectory) "Directory" else formatSize(entry.file),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (!entry.isDirectory) {
                                IconButton(onClick = {
                                    editTarget = entry.file
                                    editContent = entry.file.readText()
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = {
                                    renameTarget = entry.file
                                    renameText = entry.file.name
                                }) {
                                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Rename", modifier = Modifier.size(16.dp))
                                }
                            }
                            IconButton(onClick = { deleteTarget = entry.file }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
}
        }
    }
}

// Dialogs - all at the top level of the composable
// Delete confirmation
if (deleteTarget != null) {
    AlertDialog(
        onDismissRequest = { deleteTarget = null },
        title = { Text("Delete") },
        text = { Text("Delete \"${deleteTarget?.name}\"? This action cannot be undone.") },
        confirmButton = {
            TextButton(onClick = {
                deleteTarget?.let { f ->
                    trash[f] = f.parentFile ?: rootDir
                    f.deleteRecursively()
                }
                deleteTarget = null
                refresh(currentDir)
            }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
    )
}

// Rename dialog
if (renameTarget != null) {
    AlertDialog(
        onDismissRequest = { renameTarget = null; renameText = "" },
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = renameText,
                onValueChange = { renameText = it },
                label = { Text("New name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (renameText.isNotBlank()) {
                    renameTarget?.let { target ->
                        val parent = target.parentFile ?: currentDir
                        target.renameTo(File(parent, renameText))
                    }
                    renameTarget = null
                    renameText = ""
                    refresh(currentDir)
                }
            }) { Text("Rename") }
        },
        dismissButton = { TextButton(onClick = { renameTarget = null; renameText = "" }) { Text("Cancel") } }
    )
}

// Edit file dialog
if (editTarget != null) {
    AlertDialog(
        onDismissRequest = { editTarget = null; editContent = "" },
        title = { Text("Edit: ${editTarget?.name}") },
        text = {
            OutlinedTextField(
                value = editContent,
                onValueChange = { editContent = it },
                modifier = Modifier.fillMaxWidth().height(300.dp),
                singleLine = false,
                maxLines = 50,
            )
        },
        confirmButton = {
            TextButton(onClick = {
                editTarget?.writeText(editContent)
                editTarget = null
                editContent = ""
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = { editTarget = null; editContent = "" }) { Text("Cancel") } }
    )
}

// Trash dialog
if (showTrash && trash.isNotEmpty()) {
    AlertDialog(
        onDismissRequest = { showTrash = false },
        title = { Text("Trash (${trash.size} items)") },
        text = {
            Column {
                trash.keys.forEach { f ->
                    Text("• ${f.name} (from ${trash[f]?.name})")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                trash.keys.forEach { f ->
                    val restoreDir = trash[f] ?: rootDir
                    f.copyTo(File(restoreDir, f.name), overwrite = true)
                }
                trash.clear()
                showTrash = false
                refresh(currentDir)
            }) { Text("Restore All") }
        },
        dismissButton = { TextButton(onClick = { showTrash = false }) { Text("Close") } }
    )
}
}
}

private fun formatSize(file: File): String {
    val bytes = if (file.isDirectory) file.walkTopDown().filter { it.isFile }.sumOf { it.length() } else file.length()
    return when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.0f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.0f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
}