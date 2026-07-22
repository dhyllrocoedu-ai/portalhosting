package com.portalhost.util

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

fun pickFile(
    title: String = "Select File",
    extensionFilter: Pair<String, List<String>>? = null,
    directory: File? = null,
    multiSelection: Boolean = false,
): List<File> {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.isMultipleMode = multiSelection
    if (extensionFilter != null) {
        val exts = extensionFilter.second.map { ".$it" }.toTypedArray()
        dialog.filenameFilter = FilenameFilter { _, name ->
            exts.any { name.endsWith(it, ignoreCase = true) }
        }
    }
    directory?.let { dialog.directory = it.absolutePath }
    dialog.isVisible = true
    val files = dialog.files
    return if (files != null && files.isNotEmpty()) files.toList() else emptyList()
}

fun pickSaveFile(
    title: String = "Save File",
    extensionFilter: Pair<String, List<String>>? = null,
    defaultName: String? = null,
    directory: File? = null,
): File? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE)
    if (extensionFilter != null) {
        val exts = extensionFilter.second.map { ".$it" }.toTypedArray()
        dialog.filenameFilter = FilenameFilter { _, name ->
            exts.any { name.endsWith(it, ignoreCase = true) }
        }
    }
    directory?.let { dialog.directory = it.absolutePath }
    defaultName?.let { dialog.file = it }
    dialog.isVisible = true
    val file = dialog.file
    val dir = dialog.directory
    return if (file != null && dir != null) File(dir, file) else null
}

fun pickDirectory(
    title: String = "Select Directory",
    directory: File? = null,
): File? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.filenameFilter = FilenameFilter { _, _ -> true }
    directory?.let { dialog.directory = it.absolutePath }
    dialog.isVisible = true
    val file = dialog.file
    val dir = dialog.directory
    return if (file != null && dir != null) {
        val selected = File(dir, file)
        if (selected.isDirectory) selected else File(dir)
    } else if (dir != null) File(dir) else null
}
