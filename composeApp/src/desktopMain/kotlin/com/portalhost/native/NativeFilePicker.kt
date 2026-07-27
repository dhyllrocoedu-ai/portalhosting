package com.portalhost.native

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.filechooser.FileSystemView

actual class NativeFilePicker {

    actual suspend fun pickFile(config: PickConfig): Result<List<Uri>> = withContext(Dispatchers.IO) {
        runCatching {
            runInEventDispatchThread {
                val chooser = JFileChooser(FileSystemView.getFileSystemView())
                chooser.dialogTitle = config.title
                chooser.fileSelectionMode = JFileChooser.FILES_ONLY
                chooser.isMultiSelectionEnabled = config.multiSelect

                config.startDir?.let { path ->
                    val dir = File(path)
                    if (dir.exists()) chooser.currentDirectory = dir
                }

                if (config.filters.isNotEmpty()) {
                    val first = config.filters.first()
                    chooser.fileFilter = FileNameExtensionFilter(
                        first.name,
                        *first.extensions.map { it.lowercase() }.toTypedArray()
                    )
                    if (config.filters.size > 1) {
                        for (filter in config.filters.drop(1)) {
                            chooser.addChoosableFileFilter(
                                FileNameExtensionFilter(
                                    filter.name,
                                    *filter.extensions.map { it.lowercase() }.toTypedArray()
                                )
                            )
                        }
                    }
                }

                chooser.setAcceptAllFileFilterUsed(true)

                val returnVal = chooser.showOpenDialog(null)
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    val selected = if (config.multiSelect) chooser.selectedFiles else arrayOf(chooser.selectedFile)
                    selected.filterNotNull().map { Uri(it.absolutePath) }
                } else {
                    emptyList()
                }
            }
        }
    }

    actual suspend fun pickDirectory(config: PickConfig): Result<Uri?> = withContext(Dispatchers.IO) {
        runCatching {
            runInEventDispatchThread {
                val chooser = JFileChooser(FileSystemView.getFileSystemView())
                chooser.dialogTitle = config.title
                chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                chooser.isMultiSelectionEnabled = false
                chooser.setAcceptAllFileFilterUsed(false)

                config.startDir?.let { path ->
                    val dir = File(path)
                    if (dir.exists()) chooser.currentDirectory = dir
                }

                val returnVal = chooser.showOpenDialog(null)
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    chooser.selectedFile?.absolutePath?.let { Uri(it) }
                } else {
                    null
                }
            }
        }
    }

    actual suspend fun saveFile(config: SaveConfig): Result<Uri?> = withContext(Dispatchers.IO) {
        runCatching {
            runInEventDispatchThread {
                val chooser = JFileChooser(FileSystemView.getFileSystemView())
                chooser.dialogTitle = config.title
                chooser.fileSelectionMode = JFileChooser.FILES_ONLY

                config.startDir?.let { path ->
                    val dir = File(path)
                    if (dir.exists()) chooser.currentDirectory = dir
                }
                config.defaultName?.let { chooser.selectedFile = File(it) }

                if (config.filters.isNotEmpty()) {
                    val first = config.filters.first()
                    chooser.fileFilter = FileNameExtensionFilter(
                        first.name,
                        *first.extensions.map { it.lowercase() }.toTypedArray()
                    )
                }

                val returnVal = chooser.showSaveDialog(null)
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    chooser.selectedFile?.absolutePath?.let { Uri(it) }
                } else {
                    null
                }
            }
        }
    }

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
}
