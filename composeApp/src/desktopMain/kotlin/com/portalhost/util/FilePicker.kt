package com.portalhost.util

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

fun pickFile(
    title: String = "Select File",
    extensionFilter: Pair<String, List<String>>? = null,
    directory: File? = null,
    multiSelection: Boolean = false,
): List<File> {
    val chooser = JFileChooser()
    chooser.dialogTitle = title
    chooser.isMultiSelectionEnabled = multiSelection
    if (extensionFilter != null) {
        chooser.fileFilter = FileNameExtensionFilter(extensionFilter.first, *extensionFilter.second.toTypedArray())
    }
    directory?.let { chooser.currentDirectory = it }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        if (multiSelection) chooser.selectedFiles.toList() else listOf(chooser.selectedFile)
    } else {
        emptyList()
    }
}

fun pickSaveFile(
    title: String = "Save File",
    extensionFilter: Pair<String, List<String>>? = null,
    defaultName: String? = null,
    directory: File? = null,
): File? {
    val chooser = JFileChooser()
    chooser.dialogTitle = title
    if (extensionFilter != null) {
        chooser.fileFilter = FileNameExtensionFilter(extensionFilter.first, *extensionFilter.second.toTypedArray())
    }
    directory?.let { chooser.currentDirectory = it }
    defaultName?.let { chooser.selectedFile = File(it) }
    return if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile
    } else {
        null
    }
}

fun pickDirectory(
    title: String = "Select Directory",
    directory: File? = null,
): File? {
    val chooser = JFileChooser()
    chooser.dialogTitle = title
    chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    directory?.let { chooser.currentDirectory = it }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile
    } else {
        null
    }
}
