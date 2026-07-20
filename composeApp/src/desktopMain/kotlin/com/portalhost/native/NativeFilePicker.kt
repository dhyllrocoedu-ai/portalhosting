package com.portalhost.native

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class NativeFilePicker {

    actual suspend fun pickFile(config: PickConfig): Result<List<Uri>> = withContext(Dispatchers.IO) {
        try {
            val dialog = FileDialog(null as Frame?, config.title, FileDialog.LOAD)
            dialog.isMultipleMode = config.multiSelect
            dialog.isVisible = true
            val files = dialog.files
            if (files != null && files.isNotEmpty()) {
                Result.success(files.map { Uri(it.absolutePath) })
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun pickDirectory(config: PickConfig): Result<Uri?> = withContext(Dispatchers.IO) {
        try {
            val dialog = FileDialog(null as Frame?, config.title, FileDialog.LOAD)
            dialog.isMultipleMode = false
            System.setProperty("apple.awt.fileDialogForDirectories", "true")
            try {
                dialog.isVisible = true
                val file = dialog.file
                val directory = dialog.directory
                if (file != null && directory != null) {
                    Result.success(Uri(File(directory, file).absolutePath))
                } else if (directory != null) {
                    Result.success(Uri(directory))
                } else {
                    Result.success(null)
                }
            } finally {
                System.setProperty("apple.awt.fileDialogForDirectories", "false")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun saveFile(config: SaveConfig): Result<Uri?> = withContext(Dispatchers.IO) {
        try {
            val dialog = FileDialog(null as Frame?, config.title, FileDialog.SAVE)
            if (!config.defaultName.isNullOrBlank()) {
                dialog.file = config.defaultName
            }
            dialog.isVisible = true
            val file = dialog.file
            val directory = dialog.directory
            if (file != null && directory != null) {
                Result.success(Uri(File(directory, file).absolutePath))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
