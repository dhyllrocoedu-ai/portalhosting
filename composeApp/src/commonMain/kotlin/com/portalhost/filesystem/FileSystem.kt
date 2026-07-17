package com.portalhost.filesystem

import com.portalhost.preferences.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

fun resolveAppDataDir(): File {
    val custom = System.getProperty("portalhost.data.dir")?.takeIf { it.isNotBlank() }
    if (custom != null) {
        return File(custom).also { it.mkdirs() }
    }
    val home = System.getProperty("user.home") ?: "."
    return File(home, ".portalhost").also { it.mkdirs() }
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
    
    suspend fun readStream(input: java.io.InputStream): String = withContext(Dispatchers.IO) {
        input.bufferedReader().use { it.readText() }
    }
    
    suspend fun writeStream(output: FileOutputStream, input: InputStream): Long = withContext(Dispatchers.IO) {
        input.copyTo(output)
    }
}
