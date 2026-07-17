package com.portalhost.java

import com.portalhost.filesystem.resolveAppDataDir
import com.portalhost.model.JavaInstallation
import com.portalhost.model.ServerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipFile

class JdkManager {
    private val _knownInstallations = MutableStateFlow<List<JavaInstallation>>(emptyList())
    val knownInstallations: StateFlow<List<JavaInstallation>> = _knownInstallations
    
    val isInstalling: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val installProgress: MutableStateFlow<Double> = MutableStateFlow(0.0)
    
    private val knownJdks = mutableMapOf<Int, String>()
    
    init {
        kotlinx.coroutines.runBlocking { refresh() }
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
        
        val searchPaths = listOf(
            "/usr/lib/jvm",
            "/Library/Java/JavaVirtualMachines",
            "C:\\Program Files\\Java",
            "C:\\Program Files\\Eclipse Adoptium",
            System.getProperty("user.home") + "/.sdkman/candidates/java",
        )
        
        for (basePath in searchPaths) {
            val dir = File(basePath)
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
        installations
    }
    
    fun getJavaPath(version: Int): String? {
        return knownJdks[version]
    }
    
    fun getJavaExecutable(version: Int): File? {
        return knownJdks[version]?.let { path ->
            File(path, javaExeName()).takeIf { it.exists() }
        } ?: run {
            val installed = kotlinx.coroutines.runBlocking { detectInstalled() }
            installed.firstOrNull { it.version == version }?.let { File(it.path, javaExeName()) }
        }
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
            
            val downloadUrl = getDownloadUrl(version)
            val destinationDir = getJdkInstallDir(version)
            destinationDir.mkdirs()
            
            val archiveExt = if (isWindows()) "zip" else "tar.gz"
            val archiveFile = File(destinationDir, "jdk-${version}.$archiveExt")
            val downloadUrlObj = URL(downloadUrl)
            
            downloadUrlObj.openStream().use { input ->
                FileOutputStream(archiveFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            installProgress.value = 0.5
            
            extractArchive(archiveFile, destinationDir)
            
            val extractedDir: File? = destinationDir.listFiles()?.firstOrNull { it.isDirectory }
            val extractedDirNonNull = extractedDir ?: throw Exception("JDK extraction failed")
            
            val javaHome = extractedDirNonNull.absolutePath
            val javaExe = File(javaHome, javaExeName())
            
            if (!javaExe.exists()) {
                throw Exception("Java executable not found after extraction")
            }
            
            val installation = JavaInstallation(
                version = version,
                path = javaHome,
                vendor = getJavaVendor(javaExe),
                isJre = false
            )
            
            knownJdks[version] = javaHome
            refresh()
            
            installProgress.value = 1.0
            isInstalling.value = false
            
            Result.success(installation)
        } catch (e: Exception) {
            isInstalling.value = false
            Result.failure(e)
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
        return File(resolveAppDataDir(), "jdks/jdk-$version")
    }
    
    private fun extractArchive(archiveFile: File, destinationDir: File) {
        if (archiveFile.name.endsWith(".tar.gz")) {
            val exitCode = ProcessBuilder("tar", "-xzf", archiveFile.absolutePath, "-C", destinationDir.absolutePath)
                .directory(destinationDir)
                .start()
                .waitFor()
            if (exitCode != 0) throw Exception("tar extraction failed with exit code $exitCode")
        } else if (archiveFile.extension == "zip") {
            if (isWindows()) {
                val psCommand = "Expand-Archive -Path '${archiveFile.absolutePath}' -DestinationPath '${destinationDir.absolutePath}' -Force"
                val psProcess = ProcessBuilder("powershell", "-Command", psCommand)
                    .directory(destinationDir)
                    .start()
                val exitCode = psProcess.waitFor()
                if (exitCode != 0) throw Exception("Expand-Archive failed with exit code $exitCode")
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
        archiveFile.delete()
    }
}
