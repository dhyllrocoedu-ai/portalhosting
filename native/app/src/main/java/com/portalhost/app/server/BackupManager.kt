package com.portalhost.app.server

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(private val serverDir: File) {
    private val TAG = "BackupManager"
    private val backupsDir: File get() = File(serverDir, "backups")

    data class BackupEntry(
        val name: String,
        val file: File,
        val size: Long,
        val timestamp: Long
    )

    fun listBackups(): List<BackupEntry> {
        if (!backupsDir.exists()) return emptyList()
        return backupsDir.listFiles()
            ?.filter { it.name.endsWith(".zip") }
            ?.map { BackupEntry(it.name.removeSuffix(".zip"), it, it.length(), it.lastModified()) }
            ?.sortedByDescending { it.timestamp } ?: emptyList()
    }

    fun createBackup(name: String, worlds: Boolean = true, config: Boolean = true): Result<String> {
        return try {
            backupsDir.mkdirs()
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val safeName = name.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(50)
            val backupName = "${safeName}_$timestamp"
            val zipFile = File(backupsDir, "$backupName.zip")

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                if (worlds) {
                    val worldDir = File(serverDir, "world")
                    if (worldDir.exists()) addDirToZip(zos, worldDir, "world/")

                    val worldsDir = File(serverDir, "worlds")
                    if (worldsDir.exists()) addDirToZip(zos, worldsDir, "worlds/")
                }

                if (config) {
                    val props = File(serverDir, "server.properties")
                    if (props.exists()) addFileToZip(zos, props, "server.properties")

                    val eula = File(serverDir, "eula.txt")
                    if (eula.exists()) addFileToZip(zos, eula, "eula.txt")
                }
            }

            Log.i(TAG, "Backup created: ${zipFile.absolutePath} (${zipFile.length()} bytes)")
            Result.success(backupName)
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun restoreBackup(backupName: String): Result<Unit> {
        return try {
            val zipFile = File(backupsDir, "$backupName.zip")
            if (!zipFile.exists()) return Result.failure(Exception("Backup not found: $backupName"))

            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    val target = File(serverDir, name)

                    if (!entry.isDirectory) {
                        target.parentFile?.mkdirs()
                        FileOutputStream(target).use { out -> zis.copyTo(out) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            Log.i(TAG, "Backup restored: $backupName")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun deleteBackup(backupName: String) {
        val zipFile = File(backupsDir, "$backupName.zip")
        if (zipFile.exists()) zipFile.delete()
    }

    private fun addDirToZip(zos: ZipOutputStream, dir: File, basePath: String) {
        val entries = dir.listFiles() ?: return
        for (file in entries) {
            val entryPath = "$basePath${file.name}"
            if (file.isDirectory) {
                addDirToZip(zos, file, "$entryPath/")
            } else {
                addFileToZip(zos, file, entryPath)
            }
        }
    }

    private fun addFileToZip(zos: ZipOutputStream, file: File, entryName: String) {
        zos.putNextEntry(ZipEntry(entryName))
        FileInputStream(file).use { input -> input.copyTo(zos) }
        zos.closeEntry()
    }
}
