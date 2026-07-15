package com.portalhost.server

import com.portalhost.db.DatabaseRepository
import com.portalhost.model.BackupEntry as ModelBackupEntry
import com.portalhost.model.BackupType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

data class BackupEntry(
    val name: String,
    val file: File,
    val size: Long,
    val timestamp: Long
)

class BackupManager(
    private val serverDir: File,
    private val serverId: String,
    private val database: DatabaseRepository,
) {
    private val backupsDir: File get() = File(serverDir, "backups")
    var maxBackups: Int = 10
    private var autoBackupJob: Job? = null

    private val _backups = MutableStateFlow<List<BackupEntry>>(emptyList())
    val backups: StateFlow<List<BackupEntry>> = _backups

    fun startAutoBackup(scope: CoroutineScope, intervalHours: Int = 6) {
        autoBackupJob?.cancel()
        autoBackupJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(intervalHours * 60 * 60 * 1000L)
                createBackup("auto", worlds = true, config = false)
            }
        }
    }

    fun stopAutoBackup() {
        autoBackupJob?.cancel()
        autoBackupJob = null
    }

    suspend fun refreshBackups() = withContext(Dispatchers.IO) {
        val dbBackups = database.getBackups(serverId)
        val list = if (dbBackups.isNotEmpty()) {
            dbBackups.map { db ->
                BackupEntry(db.id, File(db.path), db.size, db.createdAt)
            }
        } else {
            listBackups()
        }
        _backups.value = list
    }

    private fun enforceMaxBackups() {
        val backups = listBackups()
        if (backups.size > maxBackups) {
            backups.drop(maxBackups).forEach { it.file.delete() }
            // Also delete from database
            val dbBackups = database.getBackups(serverId)
            if (dbBackups.size > maxBackups) {
                dbBackups.drop(maxBackups).forEach { database.deleteBackup(it.id) }
            }
        }
    }

    fun listBackups(): List<BackupEntry> {
        if (!backupsDir.exists()) return emptyList()
        return backupsDir.listFiles()
            ?.filter { it.name.endsWith(".zip") }
            ?.map { BackupEntry(it.name.removeSuffix(".zip"), it, it.length(), it.lastModified()) }
            ?.sortedByDescending { it.timestamp } ?: emptyList()
    }

    suspend fun createBackup(name: String, worlds: Boolean = true, config: Boolean = true): Result<String> = withContext(Dispatchers.IO) {
        logger.info { "Creating backup '$name' for server $serverId" }
        try {
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

            // Persist to database
            val modelBackup = ModelBackupEntry(
                id = backupName,
                serverId = serverId,
                path = zipFile.absolutePath,
                size = zipFile.length(),
                createdAt = System.currentTimeMillis(),
                type = when (name) {
                    "auto" -> BackupType.AUTO
                    else -> BackupType.MANUAL
                }
            )
            database.insertBackup(modelBackup)

            enforceMaxBackups()
            refreshBackups()
            logger.info { "Backup created: $backupName" }
            Result.success(backupName)
        } catch (e: Exception) {
            logger.error(e) { "Failed to create backup '$name' for server $serverId" }
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(backupName: String): Result<Unit> = withContext(Dispatchers.IO) {
        logger.info { "Restoring backup '$backupName' for server $serverId" }
        try {
            val zipFile = File(backupsDir, "$backupName.zip")
            if (!zipFile.exists()) {
                logger.warn { "Backup file not found: $backupName" }
                return@withContext Result.failure(Exception("Backup not found: $backupName"))
            }

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
            logger.info { "Backup restored: $backupName" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Failed to restore backup '$backupName'" }
            Result.failure(e)
        }
    }

    fun deleteBackup(backupName: String) {
        logger.info { "Deleting backup '$backupName' for server $serverId" }
        val zipFile = File(backupsDir, "$backupName.zip")
        if (zipFile.exists()) zipFile.delete()
        database.deleteBackup(backupName)
        _backups.value = listBackups()
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