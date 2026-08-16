package com.portalhost.app.server

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.tukaani.xz.XZInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

enum class JdkInstallPhase {
    CONNECTING,
    DOWNLOADING,
    EXTRACTING,
    VERIFYING,
    COMPLETE,
    ERROR
}

data class JdkInstallState(
    val phase: JdkInstallPhase? = null,
    val progress: Float = 0f,
    val message: String = "",
    val error: String? = null
)

class JavaRuntimeManager(private val context: Context) {
    private val TAG = "JavaRuntime"
    private val _installState = MutableStateFlow(JdkInstallState())
    val installState: StateFlow<JdkInstallState> = _installState.asStateFlow()

    private val runtimeDir: File
        get() = File(context.filesDir, "runtime/jdk-21")

    val javaBinary: File
        get() = File(runtimeDir, "bin/java")

    val isInstalled: Boolean
        get() = javaBinary.exists()

    private val tempDir: File
        get() = File(context.cacheDir, "jdk-extract")

    private fun emit(phase: JdkInstallPhase, message: String, progress: Float) {
        _installState.value = JdkInstallState(phase, progress, message, null)
    }

    suspend fun install(onProgress: ((Float) -> Unit)? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Installing OpenJDK 21 (Termux/bionic) to ${runtimeDir.absolutePath}")
            runtimeDir.mkdirs()
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build()

            val abi = detectAbi()
            val (version, debUrl) = jdkAssetForAbi(abi)
            Log.i(TAG, "Using Termux openjdk-${version} for abi=${abi} from $debUrl")

            emit(JdkInstallPhase.CONNECTING, "Connecting to Termux mirror...", 0f)

            val debFile = File(context.cacheDir, "openjdk-${version}_${abi}.deb")
            val request = Request.Builder().url(debUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val msg = "Download failed: HTTP ${response.code} from $debUrl"
                    Log.e(TAG, msg)
                    _installState.value = JdkInstallState(JdkInstallPhase.ERROR, 0f, "", msg)
                    return@withContext Result.failure(Exception(msg))
                }
                val contentLength = response.body?.contentLength() ?: -1L
                emit(JdkInstallPhase.DOWNLOADING, "Downloading OpenJDK 21...", 0f)
                response.body?.byteStream()?.use { input ->
                    debFile.outputStream().use { output ->
                        val buf = ByteArray(32768)
                        var totalRead = 0L
                        var read: Int
                        while (input.read(buf).also { read = it } != -1) {
                            output.write(buf, 0, read)
                            totalRead += read
                            if (contentLength > 0) {
                                val pct = totalRead.toFloat() / contentLength.toFloat()
                                _installState.value = JdkInstallState(
                                    JdkInstallPhase.DOWNLOADING,
                                    pct,
                                    "Downloading OpenJDK 21...",
                                    null
                                )
                                onProgress?.invoke(pct)
                            }
                        }
                    }
                }
            }
            Log.i(TAG, "Downloaded ${debFile.length()} bytes of .deb")

            emit(JdkInstallPhase.EXTRACTING, "Unpacking .deb archive...", 0.82f)
            tempDir.mkdirs()
            tempDir.deleteRecursively()
            tempDir.mkdirs()
            val dataXz = File(tempDir, "data.tar.xz")
            val wroteXz = extractArMember(debFile, "data.tar.xz", dataXz)
                || extractArMember(debFile, "data.tar.gz", File(tempDir, "data.tar.gz"))
                || extractArMember(debFile, "data.tar.zst", File(tempDir, "data.tar.zst"))
            if (!wroteXz) {
                val msg = "data.tar.xz not found in .deb archive"
                Log.e(TAG, msg)
                _installState.value = JdkInstallState(JdkInstallPhase.ERROR, 0f, "", msg)
                return@withContext Result.failure(Exception(msg))
            }

            val dataTar = File(tempDir, "data.tar")
            if (dataXz.name.endsWith(".xz")) {
                XZInputStream(dataXz.inputStream()).use { xzIn ->
                    dataTar.outputStream().use { out ->
                        val buf = ByteArray(32768)
                        var r: Int
                        while (xzIn.read(buf).also { r = it } != -1) out.write(buf, 0, r)
                    }
                }
            } else if (dataXz.name.endsWith(".gz")) {
                java.util.zip.GZIPInputStream(dataXz.inputStream()).use { gzIn ->
                    dataTar.outputStream().use { out ->
                        val buf = ByteArray(32768)
                        var r: Int
                        while (gzIn.read(buf).also { r = it } != -1) out.write(buf, 0, r)
                    }
                }
            } else {
                dataXz.copyTo(dataTar, overwrite = true)
            }
            Log.i(TAG, "Decompressed to ${dataTar.length()} bytes of tar")

            emit(JdkInstallPhase.EXTRACTING, "Extracting OpenJDK 21...", 0.92f)
            val tarProc = ProcessBuilder(
                "/system/bin/tar", "-xf", dataTar.absolutePath
            ).redirectErrorStream(true).directory(tempDir).start()
            val tarExit = tarProc.waitFor()
            if (tarExit != 0) {
                val err = tarProc.inputStream.bufferedReader().readText()
                throw Exception("tar extraction failed ($tarExit): $err")
            }

            val extractedRoot = tempDir.walkTopDown().firstOrNull { d ->
                d.isDirectory && File(d, "bin/java").isFile
            } ?: tempDir.walkTopDown().firstOrNull { d ->
                d.isDirectory && d.name == "java-21-openjdk" &&
                    d.absolutePath.contains("usr/lib/jvm")
            } ?: tempDir.walkTopDown().firstOrNull { d ->
                d.isDirectory && d.name == "openjdk-21"
            }
            if (extractedRoot == null) {
                throw Exception("JDK root (bin/java) not found in extracted tarball")
            }
            Log.i(TAG, "Found JDK root at ${extractedRoot.absolutePath}")

            runtimeDir.deleteRecursively()
            runtimeDir.mkdirs()
            extractedRoot.copyRecursively(
                runtimeDir,
                overwrite = true,
                onError = { file, e ->
                    Log.w(TAG, "Skipping ${file.absolutePath} during JDK copy: ${e.message}")
                    kotlin.io.OnErrorAction.SKIP
                }
            )

            provisionSystemLibraries(runtimeDir, extractedRoot)

            emit(JdkInstallPhase.VERIFYING, "Verifying install...", 0.97f)
            javaBinary.setExecutable(true)
            File(runtimeDir, "bin/javac").takeIf { it.exists() }?.setExecutable(true)
            File(runtimeDir, "bin").listFiles()?.forEach { f ->
                runCatching { f.setExecutable(true, false) }
            }

            debFile.delete()
            tempDir.deleteRecursively()

            val installed = javaBinary.exists()
            Log.i(TAG, "JDK installed: $installed at ${javaBinary.absolutePath}")
            if (!installed) {
                val msg = "java binary not found after install"
                _installState.value = JdkInstallState(JdkInstallPhase.ERROR, 0f, "", msg)
                return@withContext Result.failure(Exception(msg))
            }
            _installState.value = JdkInstallState(JdkInstallPhase.COMPLETE, 1f, "Java runtime installed", null)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "JDK install failed: ${e.message}", e)
            _installState.value = JdkInstallState(JdkInstallPhase.ERROR, 0f, "", e.message ?: "JDK install failed")
            Result.failure(e)
        }
    }

    fun resolveJavaPath(): String = javaBinary.absolutePath

    fun fixupLibraries() {
        Log.i(TAG, "fixupLibraries: isInstalled=$isInstalled javaBinary=${javaBinary.absolutePath}")
        if (isInstalled) provisionSystemLibraries(runtimeDir, runtimeDir)
    }

    fun uninstall() {
        runtimeDir.deleteRecursively()
        _installState.value = JdkInstallState(phase = null, progress = 0f, message = "", error = null)
    }

    private fun detectAbi(): String {
        val abis = Build.SUPPORTED_ABIS
        return when {
            abis.any { it == "arm64-v8a" } -> "aarch64"
            abis.any { it == "x86_64" } -> "x86_64"
            abis.any { it == "armeabi-v7a" } -> "arm"
            abis.any { it == "x86" } -> "i686"
            else -> abis.firstOrNull() ?: "aarch64"
        }
    }

    private fun jdkAssetForAbi(abi: String): Pair<String, String> {
        val version = TERMUX_JDK_VERSION
        val debName = "openjdk-21_${version}_${abi}.deb"
        return version to "$TERMUX_JDK_BASE_URL/$debName"
    }

    private fun extractArMember(arFile: File, memberName: String, destFile: File): Boolean {
        RandomAccessFile(arFile, "r").use { raf ->
            val sig = ByteArray(8)
            if (raf.read(sig) != 8) return false
            if (String(sig) != "!<arch>\n") return false
            while (raf.filePointer < raf.length()) {
                val hdr = ByteArray(60)
                if (raf.read(hdr) != 60) return false
                val nameField = String(hdr, 0, 16).trim()
                val sizeField = String(hdr, 48, 10).trim()
                val name = nameField.trimEnd('/')
                val size = sizeField.toLongOrNull() ?: 0L
                val dataStart = raf.filePointer
                if (name == memberName || nameField.startsWith(memberName)) {
                    destFile.outputStream().use { out ->
                        val buf = ByteArray(32768)
                        var remaining = size
                        while (remaining > 0) {
                            val toRead = minOf(buf.size.toLong(), remaining).toInt()
                            val n = raf.read(buf, 0, toRead)
                            if (n <= 0) break
                            out.write(buf, 0, n)
                            remaining -= n
                        }
                    }
                    Log.i(TAG, "Extracted ar member '$name' (${size} bytes)")
                    return true
                }
                val padded = size + (size % 2L)
                raf.seek(dataStart + padded)
            }
            return false
        }
    }

    private fun provisionSystemLibraries(runtimeDir: File, sourceRoot: File) {
        val libDir = File(runtimeDir, "lib")

        val needed = listOf("libz.so.1")

        for (libName in needed) {
            val dest = File(libDir, libName)
            if (dest.exists()) continue
            val found = sourceRoot.walkTopDown().firstOrNull { f ->
                f.isFile && f.name == libName
            }
            if (found != null) {
                found.copyTo(dest, overwrite = true)
                runCatching { dest.setExecutable(true, false) }
                Log.i(TAG, "Provided $libName from extracted tree: ${found.absolutePath}")
                continue
            }
            try {
                provideTermuxLibrary("zlib", "1.3.2", dest)
                Log.i(TAG, "Provided $libName from Termux pool (zlib package)")
            } catch (e: Exception) {
                Log.w(TAG, "Could not provide $libName: ${e.message}")
            }
        }

        val systemLibDir = if (Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()) "/system/lib64" else "/system/lib"
        val systemLib = File(systemLibDir)
        for ((versionedName, systemName) in listOf(
            "libcrypto.so.3" to "libcrypto.so",
            "libssl.so.3" to "libssl.so"
        )) {
            val target = File(libDir, versionedName)
            if (target.exists()) continue
            val source = File(systemLib, systemName)
            if (source.exists()) {
                try {
                    source.inputStream().use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                    runCatching { target.setExecutable(true, false) }
                    Log.i(TAG, "Provided $versionedName from system ($systemLibDir)")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to provide $versionedName: ${e.message}")
                }
            } else {
                Log.w(TAG, "$versionedName not found on system at ${source.absolutePath}")
            }
        }

        val shmemLib = File(libDir, "libandroid-shmem.so")
        if (!shmemLib.exists()) {
            try { provideTermuxLibrary("libandroid-shmem", "0.7", shmemLib) } catch (e: Exception) { Log.w(TAG, "Failed to provide libandroid-shmem.so: ${e.message}") }
        }
        val spawnLib = File(libDir, "libandroid-spawn.so")
        if (!spawnLib.exists()) {
            try { provideTermuxLibrary("libandroid-spawn", "0.3", spawnLib) } catch (e: Exception) { Log.w(TAG, "Failed to provide libandroid-spawn.so: ${e.message}") }
        }
    }

    private fun provideTermuxLibrary(pkg: String, version: String, target: File) {
        val abi = detectAbi()
        val prefix = if (pkg.startsWith("lib")) pkg.substring(0, 4) else pkg.substring(0, 1)
        val url = "$TERMUX_JDK_BASE_URL/../$prefix/$pkg/${pkg}_${version}_${abi}.deb"
        Log.i(TAG, "Downloading $pkg from: $url")
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            val debFile = File(context.cacheDir, "${pkg}.deb")
            response.body?.byteStream()?.use { input ->
                debFile.outputStream().use { output -> input.copyTo(output) }
            }
            val soTempDir = File(context.cacheDir, "${pkg}-extract")
            soTempDir.mkdirs()
            val dataXz = File(context.cacheDir, "${pkg}-data.tar.xz")
            extractArMember(debFile, "data.tar.xz", dataXz)
                || extractArMember(debFile, "data.tar.gz", File(context.cacheDir, "${pkg}-data.tar.gz"))
            val dataTar = File(context.cacheDir, "${pkg}-data.tar")
            if (dataXz.extension == "xz") {
                XZInputStream(FileInputStream(dataXz)).use { xzIn ->
                    FileOutputStream(dataTar).use { out ->
                        val buf = ByteArray(32768)
                        var r: Int
                        while (xzIn.read(buf).also { r = it } != -1) out.write(buf, 0, r)
                    }
                }
            } else {
                java.util.zip.GZIPInputStream(FileInputStream(dataXz)).use { gzIn ->
                    FileOutputStream(dataTar).use { out ->
                        val buf = ByteArray(32768)
                        var r: Int
                        while (gzIn.read(buf).also { r = it } != -1) out.write(buf, 0, r)
                    }
                }
            }
            ProcessBuilder("/system/bin/tar", "-xf", dataTar.absolutePath, "-C", soTempDir.absolutePath)
                .redirectErrorStream(true).start().waitFor()
            val soFile = soTempDir.walkTopDown().firstOrNull { it.isFile && it.extension == "so" }
                ?: throw Exception("No .so found in $pkg")
            soFile.inputStream().use { input -> target.outputStream().use { output -> input.copyTo(output) } }
            target.setExecutable(true)
            Log.i(TAG, "Provided ${target.name} from $pkg ${version}")
            debFile.delete(); dataXz.delete(); dataTar.delete(); soTempDir.deleteRecursively()
        }
    }

    companion object {
        private const val TERMUX_JDK_VERSION = "21.0.12"
        private const val TERMUX_JDK_BASE_URL =
            "https://packages.termux.dev/apt/termux-main/pool/main/o/openjdk-21"
    }
}