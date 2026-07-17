package com.portalhost.java

import com.portalhost.filesystem.FileSystem
import com.portalhost.model.JavaInstallation
import com.portalhost.model.ServerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipFile

class JdkManager(private val fileSystem: FileSystem = com.portalhost.filesystem.FileSystem()) {
    private val _knownInstallations = MutableStateFlow<List<JavaInstallation>>(emptyList())
    val knownInstallations: StateFlow<List<JavaInstallation>> = _knownInstallations
    
    val isInstalling: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val installProgress: MutableStateFlow<Double> = MutableStateFlow(0.0)
    
    private val knownJdks = mutableMapOf<Int, String>()
    
    init {
        loadKnownJdks()
        kotlinx.coroutines.runBlocking { refresh() }
    }
    
    private fun knownJdksFile(): File {
        return File(fileSystem.getAppDirBlocking(), "jdks/known_jdks.properties")
    }
    
    private fun loadKnownJdks() {
        try {
            val file = knownJdksFile()
            if (file.exists()) {
                file.readLines().forEach { line ->
                    val idx = line.indexOf('=')
                    if (idx > 0) {
                        val key = line.substring(0, idx).trim().toIntOrNull()
                        val value = line.substring(idx + 1).trim()
                        if (key != null && value.isNotBlank()) {
                            knownJdks[key] = value
                        }
                    }
                }
            }
        } catch (_: Exception) { }
    }
    
    private fun saveKnownJdks() {
        try {
            val file = knownJdksFile()
            file.parentFile?.mkdirs()
            file.writeText(knownJdks.entries.joinToString("\n") { "${it.key}=${it.value}" })
        } catch (_: Exception) { }
    }
    
    suspend fun detectInstalled(): List<JavaInstallation> = withContext(Dispatchers.IO) {
        val installations = mutableListOf<JavaInstallation>()
        
        System.getenv("JAVA_HOME")?.let { javaHome ->
            val javaExe = File(javaHome, javaExeName())
            if (javaExe.exists()) {
                val version = getJavaVersion(javaExe)
                if (version > 0) {
                    installations.add(JavaInstallation(
                        version = version,
                        path = javaHome,
                        vendor = getJavaVendor(javaExe),
                        isJre = !File(javaHome, "include").exists()
                    ))
                    knownJdks[version] = javaHome
                }
            }
        }
        
        val userHome = System.getProperty("user.home") ?: "."
        val appJdkDir = File(fileSystem.getAppDirBlocking(), "jdks").absolutePath
        val searchPaths = listOf(
            "/usr/lib/jvm",
            "/Library/Java/JavaVirtualMachines",
            "C:\\Program Files\\Java",
            "C:\\Program Files\\Eclipse Adoptium",
            "C:\\Program Files\\Microsoft",
            "C:\\Program Files\\Amazon Corretto",
            "$userHome/.sdkman/candidates/java",
            appJdkDir,
            "$userHome/.portalhost/jdks",
        )
        
        for (basePath in searchPaths) {
            val dir = File(basePath)
            if (!dir.isDirectory) continue
            val files: Array<File>? = dir.listFiles()
            files?.forEach { file ->
                if (file.isDirectory) {
                    val javaExe = File(file, javaExeName())
                    if (javaExe.exists()) {
                        val version = getJavaVersion(javaExe)
                        if (version > 0 && !knownJdks.containsKey(version)) {
                            val installation = JavaInstallation(
                                version = version,
                                path = file.absolutePath,
                                vendor = getJavaVendor(javaExe),
                                isJre = !File(file, "include").exists()
                            )
                            installations.add(installation)
                            knownJdks[version] = file.absolutePath
                        }
                    }
                }
            }
        }
        
        return@withContext installations.sortedByDescending { it.version }
    }
    
    suspend fun refresh(): List<JavaInstallation> = withContext(Dispatchers.IO) {
        val installations = detectInstalled()
        _knownInstallations.value = installations
        saveKnownJdks()
        installations
    }
    
    fun getJavaPath(version: Int): String? {
        return knownJdks[version]
    }
    
    fun getJavaExecutable(version: Int): File? {
        knownJdks[version]?.let { path ->
            val exe = File(path, javaExeName())
            if (exe.exists()) return exe
        }
        val installed = kotlinx.coroutines.runBlocking { detectInstalled() }
        installed.firstOrNull { it.version >= version }?.let { return File(it.path, javaExeName()) }
        val systemJava = checkSystemJava(version)
        if (systemJava != null) return systemJava
        return null
    }
    
    fun checkSystemJava(requiredVersion: Int): File? {
        return try {
            val javaExe = if (isWindows()) File("java.exe") else File("java")
            val version = getJavaVersion(javaExe)
            if (version >= requiredVersion) javaExe else null
        } catch (_: Exception) { null }
    }
    
    private fun javaExeName(): String {
        return if (isWindows()) "bin/java.exe" else "bin/java"
    }

    private fun isWindows(): Boolean {
        return System.getProperty("os.name").lowercase().contains("win")
    }

    private fun getJavaVersion(javaExe: File): Int {
        return try {
            val process = ProcessBuilder(javaExe.absolutePath, "-version").redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            
            val versionRegex = """version\s+"(\d+)""".toRegex()
            val match = versionRegex.find(output)
            match?.groupValues?.get(1)?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    private fun getJavaVendor(javaExe: File): String {
        return try {
            val process = ProcessBuilder(javaExe.absolutePath, "-version").redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            
            when {
                output.contains("Eclipse Adoptium") || output.contains("AdoptOpenJDK") -> "Eclipse Adoptium"
                output.contains("Oracle") -> "Oracle"
                output.contains("OpenJDK") -> "OpenJDK"
                output.contains("Azul") -> "Azul"
                output.contains("Amazon") -> "Amazon Corretto"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    suspend fun installJdk(version: Int): Result<JavaInstallation> = withContext(Dispatchers.IO) {
        try {
            isInstalling.value = true
            installProgress.value = 0.0
            
            val destinationDir = getJdkInstallDir(version)
            destinationDir.mkdirs()
            val tempDir = File(fileSystem.getAppDirBlocking(), "temp/jdk-install-$version")
            tempDir.mkdirs()
            
            cleanupDirectory(destinationDir)
            
            val downloadUrl = getDownloadUrl(version)
            val archiveExt = if (isWindows()) "zip" else "tar.gz"
            val archiveFile = File(tempDir, "jdk-${version}.$archiveExt")
            
            installProgress.value = 0.05
            downloadFileWithProgress(downloadUrl, archiveFile)
            
            installProgress.value = 0.5
            validateArchive(archiveFile)
            
            extractArchive(archiveFile, destinationDir)
            
            installProgress.value = 0.8
            
            val extractedDir = findJdkDir(destinationDir) ?: throw Exception("JDK extraction failed")
            val javaHome = extractedDir.absolutePath
            val javaExe = File(javaHome, javaExeName())
            
            if (!javaExe.exists()) {
                throw Exception("Java executable not found after extraction")
            }
            
            val versionCheck = getJavaVersion(javaExe)
            if (versionCheck != version) {
                throw Exception("Extracted JDK reports Java $versionCheck, expected $version")
            }
            
            val installation = JavaInstallation(
                version = version,
                path = javaHome,
                vendor = getJavaVendor(javaExe),
                isJre = false
            )
            
            knownJdks[version] = javaHome
            refresh()
            
            archiveFile.delete()
            cleanupDirectory(tempDir)
            
            installProgress.value = 1.0
            isInstalling.value = false
            
            Result.success(installation)
        } catch (e: Exception) {
            isInstalling.value = false
            Result.failure(e)
        }
    }
    
    private fun downloadFileWithProgress(url: String, destination: File) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 60000
        conn.instanceFollowRedirects = true
        
        val responseCode = conn.responseCode
        if (responseCode !in 200..299) {
            throw Exception("Download failed with HTTP $responseCode: $url")
        }
        
        val contentLength = conn.contentLengthLong
        var downloaded = 0L
        
        FileOutputStream(destination).use { output ->
            conn.inputStream.use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    if (contentLength > 0) {
                        installProgress.value = 0.05 + 0.45 * (downloaded.toDouble() / contentLength)
                    }
                }
            }
        }
        
        if (contentLength > 0 && downloaded != contentLength) {
            throw Exception("Download incomplete: expected $contentLength bytes, got $downloaded")
        }
    }
    
    private fun validateArchive(archiveFile: File) {
        if (!archiveFile.exists() || archiveFile.length() == 0L) {
            throw Exception("Downloaded archive is empty")
        }
        val magicBytes = ByteArray(4)
        archiveFile.inputStream().use { it.read(magicBytes) }
        val valid = when {
            archiveFile.name.endsWith(".zip") -> magicBytes[0] == 0x50.toByte() && magicBytes[1] == 0x4B.toByte() && magicBytes[2] == 0x03.toByte() && magicBytes[3] == 0x04.toByte()
            archiveFile.name.endsWith(".tar.gz") -> magicBytes[0] == 0x1F.toByte() && magicBytes[1] == 0x8B.toByte()
            else -> true
        }
        if (!valid) {
            archiveFile.delete()
            throw Exception("Downloaded file is not a valid archive")
        }
    }
    
    private fun findJdkDir(dir: File): File? {
        val javaExeName = javaExeName()
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                if (File(child, javaExeName()).exists()) {
                    return child
                }
                val nested = findJdkDir(child)
                if (nested != null) return nested
            }
        }
        return null
    }
    
    private fun cleanupDirectory(dir: File) {
        if (!dir.exists()) return
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                cleanupDirectory(child)
            }
            child.delete()
        }
    }
    
    suspend fun verifyInstallation(installation: JavaInstallation): Boolean = withContext(Dispatchers.IO) {
        val javaExe = File(installation.path, javaExeName())
        return@withContext try {
            val process = ProcessBuilder(javaExe.absolutePath, "-version").redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            process.exitValue() == 0 && output.contains("version")
        } catch (e: Exception) {
            false
        }
    }
    
    fun getRequiredJavaVersion(serverType: ServerType): Int {
        return when (serverType) {
            ServerType.VANILLA -> 21
            ServerType.PAPER -> 21
            ServerType.FOLIA -> 21
            ServerType.PURPUR -> 21
            ServerType.FABRIC -> 21
            ServerType.FORGE -> 17
            ServerType.NEOFORGE -> 21
            else -> 21
        }
    }
    
    fun getRecommendedJavaVersion(serverType: ServerType): Int {
        return when (serverType) {
            ServerType.VANILLA -> 21
            ServerType.PAPER -> 21
            ServerType.FOLIA -> 21
            ServerType.PURPUR -> 21
            ServerType.FABRIC -> 21
            ServerType.FORGE -> 21
            ServerType.NEOFORGE -> 21
            else -> 21
        }
    }
    
    private fun getDownloadUrl(version: Int): String {
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()

        val osStr = when {
            os.contains("win") -> "windows"
            os.contains("mac") -> "mac"
            else -> "linux"
        }

        val archStr = when (arch) {
            "x86_64", "amd64" -> "x64"
            "aarch64", "arm64" -> "aarch64"
            else -> "x64"
        }

        val versionStr = when (version) {
            8 -> "8"
            11 -> "11"
            17 -> "17"
            21 -> "21"
            else -> throw IllegalArgumentException("Unsupported Java version: $version")
        }

        return "https://api.adoptium.net/v3/binary/latest/$versionStr/ga/$osStr/$archStr/jdk/hotspot/normal/eclipse"
    }
    
    private fun getJdkInstallDir(version: Int): File {
        return File(fileSystem.getAppDirBlocking(), "jdks/jdk-$version")
    }
    
    private fun extractArchive(archiveFile: File, destinationDir: File) {
        if (archiveFile.name.endsWith(".tar.gz")) {
            val process = ProcessBuilder("tar", "-xzf", archiveFile.absolutePath, "-C", destinationDir.absolutePath)
                .directory(destinationDir)
                .redirectErrorStream(true)
                .start()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val stderr = try { process.inputStream.bufferedReader().readText() } catch (_: Exception) { "" }
                throw Exception("tar extraction failed with exit code $exitCode: $stderr")
            }
        } else if (archiveFile.extension == "zip") {
            if (isWindows()) {
                val psCommand = "Expand-Archive -Path '${archiveFile.absolutePath}' -DestinationPath '${destinationDir.absolutePath}' -Force"
                val psProcess = ProcessBuilder("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", psCommand)
                    .directory(destinationDir)
                    .redirectErrorStream(true)
                    .start()
                val exitCode = psProcess.waitFor()
                if (exitCode != 0) {
                    val stderr = try { psProcess.inputStream.bufferedReader().readText() } catch (_: Exception) { "" }
                    val retryCode = extractZipFallback(archiveFile, destinationDir)
                    if (retryCode != 0) {
                        throw Exception("Expand-Archive failed with exit code $exitCode: $stderr")
                    }
                }
            } else {
                ZipFile(archiveFile).use { zipFile ->
                    val entries = zipFile.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val outFile = File(destinationDir, entry.name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            zipFile.getInputStream(entry).copyTo(FileOutputStream(outFile))
                        }
                    }
                }
            }
        }
    }
    
    private fun extractZipFallback(archiveFile: File, destinationDir: File): Int {
        return try {
            ZipFile(archiveFile).use { zipFile ->
                val entries = zipFile.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val outFile = File(destinationDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        zipFile.getInputStream(entry).copyTo(FileOutputStream(outFile))
                    }
                }
            }
            0
        } catch (_: Exception) { 1 }
    }
}
