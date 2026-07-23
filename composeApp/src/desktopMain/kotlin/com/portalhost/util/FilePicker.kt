package com.portalhost.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

suspend fun pickFile(
    title: String = "Select File",
    extensionFilter: Pair<String, List<String>>? = null,
    directory: File? = null,
    multiSelection: Boolean = false,
): List<File> = withContext(Dispatchers.IO) {
    System.setProperty("awt.fileDialog.useNativeLF", "true")

    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.isMultipleMode = multiSelection
    if (extensionFilter != null) {
        val exts = extensionFilter.second.map { ".${it.lowercase()}" }.toTypedArray()
        dialog.filenameFilter = FilenameFilter { _, name ->
            exts.any { name.lowercase().endsWith(it) }
        }
    }
    directory?.let { dialog.directory = it.absolutePath }

    dialog.isVisible = true
    val files = dialog.files
    if (files != null && files.isNotEmpty()) files.toList() else emptyList()
}

suspend fun pickSaveFile(
    title: String = "Save File",
    extensionFilter: Pair<String, List<String>>? = null,
    defaultName: String? = null,
    directory: File? = null,
): File? = withContext(Dispatchers.IO) {
    System.setProperty("awt.fileDialog.useNativeLF", "true")

    val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE)
    if (extensionFilter != null) {
        val exts = extensionFilter.second.map { ".${it.lowercase()}" }.toTypedArray()
        dialog.filenameFilter = FilenameFilter { _, name ->
            exts.any { name.lowercase().endsWith(it) }
        }
    }
    directory?.let { dialog.directory = it.absolutePath }
    defaultName?.let { dialog.file = it }

    dialog.isVisible = true
    val f = dialog.file
    val dir = dialog.directory
    if (f != null && dir != null) File(dir, f) else null
}

suspend fun pickDirectory(
    title: String = "Select Directory",
    directory: File? = null,
): File? = withContext(Dispatchers.IO) {
    System.setProperty("awt.fileDialog.useNativeLF", "true")

    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.filenameFilter = FilenameFilter { _, _ -> true }
    directory?.let { dialog.directory = it.absolutePath }

    dialog.isVisible = true
    val f = dialog.file
    val dir = dialog.directory
    if (f != null && dir != null) {
        val selected = File(dir, f)
        if (selected.isDirectory) selected else File(dir)
    } else if (dir != null) {
        File(dir)
    } else null
}