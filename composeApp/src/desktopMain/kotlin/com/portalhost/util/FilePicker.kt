package com.portalhost.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

private fun <T> runInEventDispatchThread(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) {
        return block()
    }
    val result = java.util.concurrent.atomic.AtomicReference<T?>()
    val latch = java.util.concurrent.CountDownLatch(1)
    SwingUtilities.invokeLater {
        try {
            result.set(block())
        } catch (_: Exception) {
            result.set(null)
        } finally {
            latch.countDown()
        }
    }
    latch.await()
    @Suppress("UNCHECKED_CAST")
    return result.get() as T
}

suspend fun pickFile(
    title: String = "Select File",
    extensionFilter: Pair<String, List<String>>? = null,
    directory: File? = null,
    multiSelection: Boolean = false,
): List<File> = withContext(Dispatchers.IO) {
    runInEventDispatchThread {
        val chooser = JFileChooser()
        chooser.dialogTitle = title
        chooser.fileSelectionMode = JFileChooser.FILES_ONLY
        chooser.isMultiSelectionEnabled = multiSelection
        directory?.let { chooser.currentDirectory = it }

        if (extensionFilter != null) {
            chooser.fileFilter = FileNameExtensionFilter(
                extensionFilter.first,
                *extensionFilter.second.map { it.lowercase() }.toTypedArray()
            )
        }

        val returnVal = chooser.showOpenDialog(null)
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            val selected = if (multiSelection) chooser.selectedFiles else arrayOf(chooser.selectedFile)
            selected.filterNotNull().toList()
        } else {
            emptyList()
        }
    }
}

suspend fun pickSaveFile(
    title: String = "Save File",
    extensionFilter: Pair<String, List<String>>? = null,
    defaultName: String? = null,
    directory: File? = null,
): File? = withContext(Dispatchers.IO) {
    runInEventDispatchThread {
        val chooser = JFileChooser()
        chooser.dialogTitle = title
        chooser.fileSelectionMode = JFileChooser.FILES_ONLY
        directory?.let { chooser.currentDirectory = it }
        defaultName?.let { chooser.selectedFile = File(it) }

        if (extensionFilter != null) {
            chooser.fileFilter = FileNameExtensionFilter(
                extensionFilter.first,
                *extensionFilter.second.map { it.lowercase() }.toTypedArray()
            )
        }

        val returnVal = chooser.showSaveDialog(null)
        if (returnVal == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
    }
}

suspend fun pickDirectory(
    title: String = "Select Directory",
    directory: File? = null,
): File? = withContext(Dispatchers.IO) {
    runInEventDispatchThread {
        val chooser = JFileChooser()
        chooser.dialogTitle = title
        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        chooser.isMultiSelectionEnabled = false
        directory?.let { chooser.currentDirectory = it }

        val returnVal = chooser.showOpenDialog(null)
        if (returnVal == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
    }
}