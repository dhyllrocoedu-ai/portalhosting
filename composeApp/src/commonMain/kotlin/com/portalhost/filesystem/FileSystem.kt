package com.portalhost.filesystem

import com.portalhost.preferences.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

fun defaultDataDir(): File {
    val os = System.getProperty("os.name", "").lowercase()
    val home = System.getProperty("user.home") ?: "."
    return when {
        os.contains("win") -> {
            val localAppData = System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() }
            File(localAppData ?: File(home, "AppData/Local").absolutePath, "PortalHost")
        }
        os.contains("mac") -> File(home, "Library/Application Support/PortalHost")
        else -> {
            val xdgDataHome = System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }
            File(xdgDataHome ?: File(home, ".local/share").absolutePath, "portalhost")
        }
    }
}

fun resolveAppDataDir(): File {
    val custom = System.getProperty("portalhost.data.dir")?.takeIf { it.isNotBlank() }
    if (custom != null) {
        return File(custom).also { it.mkdirs() }
    }
    return defaultDataDir().also { it.mkdirs() }
}

class FileSystem(private val preferences: Preferences? = null) {
    private fun resolveDataDir(): File {
        val custom = preferences?.dataDirectory?.value?.takeIf { it.isNotBlank() }
        if (custom != null) {
            return File(custom).also { it.mkdirs() }
        }
        return resolveAppDataDir()
    }

    suspend fun getAppDir(): File {
        return resolveDataDir()
    }

    fun getAppDirBlocking(): File {
        return resolveDataDir()
    }

    suspend fun getServersDir(): File {
        return File(resolveDataDir(), "servers").also { it.mkdirs() }
    }

    fun getServersDirBlocking(): File {
        return File(resolveDataDir(), "servers").also { it.mkdirs() }
    }

    suspend fun getBackupsDir(): File {
        return File(resolveDataDir(), "backups").also { it.mkdirs() }
    }

    suspend fun getTempDir(): File {
        return File(resolveDataDir(), "temp").also { it.mkdirs() }
    }

    suspend fun readFile(file: File): String? = withContext(Dispatchers.IO) {
        file.readText().takeIf { it.isNotBlank() }
    }
    
    suspend fun writeFile(file: File, content: String): Boolean = withContext(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        file.writeText(content)
        true
    }
    
    suspend fun copyFile(source: File, destination: File): Boolean = withContext(Dispatchers.IO) {
        destination.parentFile?.mkdirs()
        Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        true
    }
    
    suspend fun deleteFile(file: File): Boolean = withContext(Dispatchers.IO) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteFile(it) }
        }
        file.delete()
    }
    
    suspend fun listFiles(dir: File): List<File> = withContext(Dispatchers.IO) {
        dir.listFiles()?.toList() ?: emptyList()
    }
    
    suspend fun createDirectories(dir: File): Boolean = withContext(Dispatchers.IO) {
        dir.mkdirs()
    }
    
    suspend fun getFileSize(file: File): Long = withContext(Dispatchers.IO) {
        if (file.isDirectory) {
            file.walk().filter { it.isFile }.sumOf { it.length() }
        } else {
            file.length()
        }
    }
    
    suspend fun fileExists(file: File): Boolean = withContext(Dispatchers.IO) {
        file.exists()
    }
    
    suspend fun isDirectory(file: File): Boolean = withContext(Dispatchers.IO) {
        file.isDirectory
    }

    data class StorageBreakdown(
        val worldBytes: Long = 0,
        val pluginsBytes: Long = 0,
        val modsBytes: Long = 0,
        val datapacksBytes: Long = 0,
        val resourcepacksBytes: Long = 0,
        val otherBytes: Long = 0,
    ) {
        val totalBytes: Long get() = worldBytes + pluginsBytes + modsBytes + datapacksBytes + resourcepacksBytes + otherBytes
    }

    suspend fun getServerStorageStats(serverDir: File): StorageBreakdown = withContext(Dispatchers.IO) {
        if (!serverDir.exists()) return@withContext StorageBreakdown()

        fun dirSize(dir: File): Long =
            if (dir.isDirectory) dir.walk().filter { it.isFile }.sumOf { it.length() } else 0L

        val worldDir = File(serverDir, "world")
        val pluginsDir = File(serverDir, "plugins")
        val modsDir = File(serverDir, "mods")
        val datapacksDir = File(serverDir, "datapacks")
        val resourcepacksDir = File(serverDir, "resourcepacks")

        val knownDirs = setOf(worldDir, pluginsDir, modsDir, datapacksDir, resourcepacksDir)
        val knownSize = knownDirs.sumOf { dirSize(it) }

        StorageBreakdown(
            worldBytes = dirSize(worldDir),
            pluginsBytes = dirSize(pluginsDir),
            modsBytes = dirSize(modsDir),
            datapacksBytes = dirSize(datapacksDir),
            resourcepacksBytes = dirSize(resourcepacksDir),
            otherBytes = (dirSize(serverDir) - knownSize).coerceAtLeast(0),
        )
    }
    
    suspend fun readStream(input: java.io.InputStream): String = withContext(Dispatchers.IO) {
        input.bufferedReader().use { it.readText() }
    }
    
    suspend fun writeStream(output: FileOutputStream, input: InputStream): Long = withContext(Dispatchers.IO) {
        input.copyTo(output)
    }
}
