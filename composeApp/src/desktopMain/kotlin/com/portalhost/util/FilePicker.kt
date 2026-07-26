package com.portalhost.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

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
    val result = java.util.concurrent.CountDownLatch(1)
    var selectedDir: File? = null

    try {
        SwingUtilities.invokeLater {
            try {
                val chooser = JFileChooser()
                chooser.dialogTitle = title
                chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                chooser.isMultiSelectionEnabled = false
                directory?.let { chooser.currentDirectory = it }

                val returnVal = chooser.showOpenDialog(null)
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    selectedDir = chooser.selectedFile
                }
            } catch (_: Exception) {
            } finally {
                result.countDown()
            }
        }
    } catch (_: Exception) {
        result.countDown()
    }

    result.await()
    selectedDir
}