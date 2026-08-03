package com.portalhost.java

import com.portalhost.filesystem.FileSystem
import com.portalhost.model.JavaInstallation
import com.portalhost.model.ServerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.ZipFile

class JdkManager(private val fileSystem: FileSystem = com.portalhost.filesystem.FileSystem()) {
    // --- Progress Reporting ---
    
    enum class InstallPhase {
        IDLE,
        CONNECTING,
        DOWNLOADING,
        VALIDATING,
        EXTRACTING,
        VERIFYING,
        COMPLETE,
        ERROR
    }
    
    data class DownloadProgress(
        val phase: InstallPhase = InstallPhase.IDLE,
        val totalBytes: Long = -1,
        val downloadedBytes: Long = 0,
        val speedBytesPerSec: Long = 0,
        val etaMillis: Long = -1,
        val errorMessage: String? = null,
        val currentChunk: Int = 0,
        val totalChunks: Int = 1,
        val extractedEntries: Int = 0,
        val totalEntries: Int = 0
    ) {
        val downloadedMB: Double = downloadedBytes / (1024.0 * 1024.0)
        val totalMB: Double = if (totalBytes > 0) totalBytes / (1024.0 * 1024.0) else -1.0
        val speedMBps: Double = speedBytesPerSec / (1024.0 * 1024.0)
        val etaSeconds: Long = etaMillis / 1000
        val percentage: Double = if (totalBytes > 0) (downloadedBytes.toDouble() / totalBytes * 100).coerceAtMost(100.0) else 0.0
    }
    
    private val _progress = MutableStateFlow(DownloadProgress())
    val progress: StateFlow<DownloadProgress> = _progress
    
    // --- Known Installations ---
    
    private val _knownInstallations = MutableStateFlow<List<JavaInstallation>>(emptyList())
    val knownInstallations: StateFlow<List<JavaInstallation>> = _knownInstallations
    private val refreshScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
    
    private val knownJdks = mutableMapOf<Int, String>()
    
    init {
        loadKnownJdks()
        refreshScope.launch { refresh() }
    }
    
    // --- Persistence ---
    
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
    
    // --- Installation Flow ---
    
    suspend fun installJdk(version: Int): Result<JavaInstallation> = withContext(Dispatchers.IO) {
        try {
            _progress.value = DownloadProgress(phase = InstallPhase.CONNECTING)
            
            val destinationDir = getJdkInstallDir(version)
            destinationDir.mkdirs()
            val tempDir = File(fileSystem.getAppDirBlocking(), "temp/jdk-install-$version")
            tempDir.mkdirs()
            
            cleanupDirectory(destinationDir)
            
            val downloadUrl = getDownloadUrl(version)
            val archiveExt = if (isWindows()) "zip" else "tar.gz"
            val archiveFile = File(tempDir, "jdk-${version}.$archiveExt")
            
            _progress.value = DownloadProgress(phase = InstallPhase.DOWNLOADING)
            
            // Download with parallel chunks
            downloadFileParallel(downloadUrl, archiveFile, chunks = 4)
            
            _progress.value = _progress.value.copy(phase = InstallPhase.VALIDATING)
            validateArchive(archiveFile)
            
            _progress.value = _progress.value.copy(phase = InstallPhase.EXTRACTING)
            extractArchiveParallel(archiveFile, destinationDir)
            
            // Verify extraction produced expected structure
            val extractedDir = findJdkDir(destinationDir) ?: throw Exception("JDK extraction failed - no jdk directory found in $destinationDir")
            val javaHome = extractedDir.absolutePath
            val javaExe = File(javaHome, javaExeName())
            
            if (!javaExe.exists()) {
                throw Exception("Java executable not found after extraction at $javaExe")
            }
            
            val versionCheck = getJavaVersion(javaExe)
            if (versionCheck != version) {
                throw Exception("Extracted JDK reports Java $versionCheck, expected $version (javaExe: $javaExe)")
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
            
            _progress.value = DownloadProgress(phase = InstallPhase.COMPLETE)
            Result.success(installation)
        } catch (e: Exception) {
            _progress.value = DownloadProgress(phase = InstallPhase.ERROR, errorMessage = e.message)
            Result.failure(e)
        }
    }
    
    // --- Parallel Download with Resume ---
    
    private suspend fun downloadFileParallel(url: String, destination: File, chunks: Int = 4) {
        var currentUrl = url
        var redirectCount = 0
        val maxRedirects = 5
        
        // Resolve final URL with redirects
        while (true) {
            val conn = URL(currentUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 60000
            conn.instanceFollowRedirects = false
            
            when (conn.responseCode) {
                in 200..299 -> break
                in 300..399 -> {
                    if (++redirectCount > 5) throw Exception("Too many redirects for $url")
                    val location = conn.getHeaderField("Location")
                        ?: throw Exception("Redirect with no Location header")
                    currentUrl = URL(URL(currentUrl), location).toExternalForm()
                }
                else -> throw Exception("HTTP ${conn.responseCode} for $url")
            }
        }
        
        // Get content length and check Range support
        val headConn = URL(currentUrl).openConnection() as HttpURLConnection
        headConn.requestMethod = "HEAD"
        headConn.connectTimeout = 15000
        headConn.readTimeout = 15000
        headConn.connect()
        
        val totalSize = headConn.contentLengthLong
        val acceptRanges = headConn.getHeaderField("Accept-Ranges") == "bytes"
        
        if (totalSize <= 0 || !acceptRanges) {
            // Fallback to single-threaded download
            downloadFileSingleThread(currentUrl, destination)
            return
        }
        
        // Check for existing partial download
        val existingSize = destination.length().coerceIn(0, totalSize)
        val isResume = existingSize > 0 && existingSize < totalSize
        
        // Pre-size the destination so each chunk writes directly at its exact
        // offset. This preserves an existing partial download (no truncating
        // combine step on resume).
        destination.parentFile.mkdirs()
        RandomAccessFile(destination, "rw").use { it.setLength(totalSize) }
        
        val chunkSize = totalSize / chunks
        val chunkRanges = mutableListOf<Pair<Long, Long>>()
        
        for (i in 0 until chunks) {
            val start = if (isResume && i == 0) existingSize else i * chunkSize
            val end = if (i == chunks - 1) totalSize - 1 else (i + 1) * chunkSize - 1
            chunkRanges.add(start to end)
        }
        
        // A chunk whose range is already fully covered (start > end) needs no
        // download, e.g. chunk 0 when the partial download already covered it.
        val activeRanges = chunkRanges.filter { it.first <= it.second }
        
        val completedChunks = AtomicLong(0)
        val totalDownloaded = AtomicLong(if (isResume) existingSize else 0L)
        val startTime = System.currentTimeMillis()

        coroutineScope {
            val progressJob = launch {
                while (completedChunks.get() < chunks) {
                    delay(200)
                    val now = System.currentTimeMillis()
                    val elapsedSec = (now - startTime) / 1000.0
                    val speed = if (elapsedSec > 0) (totalDownloaded.get() / elapsedSec).toLong() else 0L
                    val remaining = totalSize - totalDownloaded.get()
                    val eta = if (speed > 0) (remaining * 1000 / speed) else -1L
                    _progress.value = DownloadProgress(
                        phase = InstallPhase.DOWNLOADING,
                        totalBytes = totalSize,
                        downloadedBytes = totalDownloaded.get(),
                        speedBytesPerSec = speed,
                        etaMillis = eta,
                        currentChunk = completedChunks.get().toInt() + 1,
                        totalChunks = chunks
                    )
                }
                val now = System.currentTimeMillis()
                val speed = if (now > startTime) (totalDownloaded.get() * 1000 / (now - startTime)) else 0L
                _progress.value = DownloadProgress(
                    phase = InstallPhase.DOWNLOADING,
                    totalBytes = totalSize,
                    downloadedBytes = totalDownloaded.get(),
                    speedBytesPerSec = speed,
                    etaMillis = 0,
                    currentChunk = chunks,
                    totalChunks = chunks
                )
            }

            val downloadJobs = activeRanges.map { (start, end) ->
                launch(Dispatchers.IO) {
                    try {
                        val written = downloadRange(currentUrl, start, end, destination)
                        totalDownloaded.addAndGet(written)
                    } finally {
                        completedChunks.incrementAndGet()
                    }
                }
            }
            downloadJobs.forEach { it.join() }
            progressJob.cancel()
        }
        
        // Verify
        val finalSize = destination.length()
        if (finalSize != totalSize) {
            throw Exception("Download incomplete: expected $totalSize bytes, got $finalSize")
        }
    }
    
    private fun downloadRange(
        url: String,
        start: Long,
        end: Long,
        outputFile: File
    ): Long {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 60000
        conn.setRequestProperty("Range", "bytes=$start-$end")
        
        if (conn.responseCode != 206) {
            throw Exception("Server does not support range requests (HTTP ${conn.responseCode})")
        }
        
        val expectedSize = end - start + 1
        var downloaded = 0L
        val buffer = ByteArray(64 * 1024) // 64KB buffer
        
        RandomAccessFile(outputFile, "rw").use { raf ->
            raf.seek(start)
            conn.inputStream.use { input ->
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    raf.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                }
            }
        }
        
        if (downloaded != expectedSize) {
            throw Exception("Chunk download incomplete: expected $expectedSize, got $downloaded")
        }
        return downloaded
    }
    
    private fun downloadFileSingleThread(url: String, destination: File) {
        var currentUrl = url
        var redirectCount = 0
        
        while (true) {
            val conn = URL(currentUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 60000
            conn.instanceFollowRedirects = false
            
            when (conn.responseCode) {
                in 200..299 -> {
                    val contentLength = conn.contentLengthLong
                    val heuristicSize = 200L * 1024 * 1024
                    val effectiveSize = if (contentLength > 0) contentLength else heuristicSize
                    var downloaded = 0L
                    
                    val startTime = System.currentTimeMillis()
                    var lastBytes = 0L
                    var lastTime = System.currentTimeMillis()
                    
                    FileOutputStream(destination).use { output ->
                        conn.inputStream.use { input ->
                            val buffer = ByteArray(64 * 1024)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                downloaded += bytesRead
                                
                                if (downloaded % (256 * 1024) == 0L) {
                                    val now = System.currentTimeMillis()
                                    val elapsed = (now - startTime) / 1000.0
                                    val speed = if (elapsed > 0) (downloaded / elapsed).toLong() else 0L
                                    val remaining = if (contentLength > 0) contentLength - downloaded else -1L
                                    val eta = if (speed > 0 && remaining > 0) (remaining / speed * 1000).toLong() else -1L
                                    
                                    _progress.value = DownloadProgress(
                                        phase = InstallPhase.DOWNLOADING,
                                        totalBytes = if (contentLength > 0) contentLength else -1,
                                        downloadedBytes = downloaded,
                                        speedBytesPerSec = speed,
                                        etaMillis = eta,
                                        totalChunks = 1,
                                        currentChunk = 1
                                    )
                                }
                            }
                        }
                    }
                    
                    if (contentLength > 0 && downloaded != contentLength) {
                        throw Exception("Download incomplete: expected $contentLength bytes, got $downloaded")
                    }
                    return
                }
                in 300..399 -> {
                    if (++redirectCount > 5) throw Exception("Too many redirects")
                    val location = conn.getHeaderField("Location")
                        ?: throw Exception("Redirect with no Location")
                    currentUrl = URL(URL(currentUrl), location).toExternalForm()
                }
                else -> throw Exception("HTTP ${conn.responseCode}")
            }
        }
    }
    
    // --- Parallel Zip Extraction with Progress ---
    
    private suspend fun extractArchiveParallel(archiveFile: File, destinationDir: File) {
        if (archiveFile.name.endsWith(".tar.gz")) {
            // tar.gz: use tar command (can't easily parallelize)
            _progress.value = _progress.value.copy(
                phase = InstallPhase.EXTRACTING,
                totalEntries = -1,
                extractedEntries = 0
            )
            val process = ProcessBuilder("tar", "-xzf", archiveFile.absolutePath, "-C", destinationDir.absolutePath)
                .directory(destinationDir)
                .redirectErrorStream(true)
                .start()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val stderr = try { process.inputStream.bufferedReader().readText() } catch (_: Exception) { "" }
                throw Exception("tar extraction failed with exit code $exitCode: $stderr")
            }
            return
        }
        
        // ZIP: parallel extraction
        ZipFile(archiveFile).use { zipFile ->
            val entries = java.util.Collections.list(zipFile.entries()).filter { !it.isDirectory }
            val totalEntries = entries.size
            
            _progress.value = _progress.value.copy(
                phase = InstallPhase.EXTRACTING,
                totalEntries = totalEntries,
                extractedEntries = 0
            )
            
            val extractedCount = AtomicLong(0)
            val chunkSize = maxOf(1, totalEntries / 4)
            val chunks = entries.chunked(chunkSize)
            
            coroutineScope {
                val jobs = chunks.map { chunk ->
                    launch(Dispatchers.IO) {
                        ZipFile(archiveFile).use { localZip ->
                            for (entry in chunk) {
                                val outFile = File(destinationDir, entry.name)
                                if (entry.isDirectory) {
                                    outFile.mkdirs()
                                } else {
                                    outFile.parentFile?.mkdirs()
                                    localZip.getInputStream(entry).copyTo(FileOutputStream(outFile))
                                }
                                val count = extractedCount.incrementAndGet()
                                if (count % 20L == 0L || count == totalEntries.toLong()) {
                                    _progress.value = _progress.value.copy(extractedEntries = count.toInt())
                                }
                            }
                        }
                    }
                }
                jobs.forEach { it.join() }
            }
            
            _progress.value = _progress.value.copy(extractedEntries = totalEntries)
        }
    }
    
    // --- Validation (unchanged) ---
    
    private fun validateArchive(archiveFile: File) {
        if (!archiveFile.exists() || archiveFile.length() == 0L) {
            throw Exception("Downloaded archive is empty")
        }
        val magicBytes = ByteArray(4)
        val read = archiveFile.inputStream().use { it.read(magicBytes) }
        val isZip = read >= 4 && magicBytes[0] == 0x50.toByte() && magicBytes[1] == 0x4B.toByte() && magicBytes[2] == 0x03.toByte() && magicBytes[3] == 0x04.toByte()
        val isGzip = read >= 2 && magicBytes[0] == 0x1F.toByte() && magicBytes[1] == 0x8B.toByte()
        
        val valid = when {
            archiveFile.name.endsWith(".zip") -> isZip
            archiveFile.name.endsWith(".tar.gz") -> isGzip
            else -> true
        }
        if (!valid) {
            val expected = if (archiveFile.name.endsWith(".zip")) "ZIP" else "gzip"
            val actual = when {
                isZip -> "ZIP"
                isGzip -> "gzip"
                else -> "HTML/text or unknown"
            }
            val message = if (isGzip && archiveFile.name.endsWith(".zip")) {
                "Downloaded a gzip archive for a ZIP download - the mirror returned an archive for another operating system. Retrying will use the correct build for this machine."
            } else {
                "Downloaded file is not a valid $expected archive (got $actual). The download may have failed or returned the wrong platform build."
            }
            archiveFile.delete()
            throw Exception(message)
        }
    }
    
    // --- Other methods (unchanged, but adding missing imports) ---
    
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
    
    // --- Detection (unchanged) ---
    
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
        detectInstalled()
        saveKnownJdks()
        val installations = knownJdks.map { (version, path) ->
            val javaExe = File(path, javaExeName())
            JavaInstallation(
                version = version,
                path = path,
                vendor = if (javaExe.exists()) getJavaVendor(javaExe) else "Unknown",
                isJre = !javaExe.exists() || !File(path, "include").exists()
            )
        }.sortedByDescending { it.version }
        _knownInstallations.value = installations
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
            
            // Try multiple regex patterns for different JDK vendors
            val patterns = listOf(
                """version\s+"(\d+)""",           // standard: version "21.0.5" or version "21"
                """openjdk\s+version\s+"(\d+)""", // OpenJDK: openjdk version "21.0.5"
                """java\s+version\s+"(\d+)""",    // Oracle: java version "21.0.5"
            )
            
            for (pattern in patterns) {
                val regex = pattern.toRegex()
                val match = regex.find(output)
                match?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
            }
            
            // Fallback: try to find any version-like number in the output
            val fallbackRegex = """(\d+)\.(\d+)""".toRegex()
            val fallbackMatch = fallbackRegex.find(output)
            fallbackMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
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
}
