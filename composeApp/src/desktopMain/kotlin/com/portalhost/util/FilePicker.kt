package com.portalhost.util

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter
import javax.swing.SwingUtilities

fun pickFile(
    title: String = "Select File",
    extensionFilter: Pair<String, List<String>>? = null,
    directory: File? = null,
    multiSelection: Boolean = false,
): List<File> {
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

    var result: List<File> = emptyList()
    val lock = Object()
    var completed = false

    SwingUtilities.invokeLater {
        dialog.isVisible = true
        val files = dialog.files
        if (files != null && files.isNotEmpty()) {
            result = files.toList()
        }
        synchronized(lock) {
            completed = true
            lock.notifyAll()
        }
    }

    synchronized(lock) {
        while (!completed) {
            try {
                lock.wait()
            } catch (e: InterruptedException) {
                return emptyList()
            }
        }
    }

    return result
}

fun pickSaveFile(
    title: String = "Save File",
    extensionFilter: Pair<String, List<String>>? = null,
    defaultName: String? = null,
    directory: File? = null,
): File? {
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

    var result: File? = null
    val lock = Object()
    var completed = false

    SwingUtilities.invokeLater {
        dialog.isVisible = true
        val f = dialog.file
        val dir = dialog.directory
        if (f != null && dir != null) {
            result = File(dir, f)
        }
        synchronized(lock) {
            completed = true
            lock.notifyAll()
        }
    }

    synchronized(lock) {
        while (!completed) {
            try {
                lock.wait()
            } catch (e: InterruptedException) {
                return null
            }
        }
    }

    return result
}

fun pickDirectory(
    title: String = "Select Directory",
    directory: File? = null,
): File? {
    System.setProperty("awt.fileDialog.useNativeLF", "true")

    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.filenameFilter = FilenameFilter { _, _ -> true }
    directory?.let { dialog.directory = it.absolutePath }

    var result: File? = null
    val lock = Object()
    var completed = false

    SwingUtilities.invokeLater {
        dialog.isVisible = true
        val f = dialog.file
        val dir = dialog.directory
        if (f != null && dir != null) {
            val selected = File(dir, f)
            result = if (selected.isDirectory) selected else File(dir)
        } else if (dir != null) {
            result = File(dir)
        }
        synchronized(lock) {
            completed = true
            lock.notifyAll()
        }
    }

    synchronized(lock) {
        while (!completed) {
            try {
                lock.wait()
            } catch (e: InterruptedException) {
                return null
            }
        }
    }

    return result
}