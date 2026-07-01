package com.portalhost.app.server

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import org.tukaani.xz.XZInputStream

class JavaRuntimeManager(private val context: Context) {
    private val TAG = "JavaRuntime"

    private val runtimeDir: File
        get() = File(context.filesDir, "runtime/jdk-21")

    val javaBinary: File
        get() = File(runtimeDir, "bin/java")

    val isInstalled: Boolean
        get() = javaBinary.exists()

    private val tempDir: File
        get() = File(context.cacheDir, "jdk-extract")

    suspend fun install(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Installing OpenJDK 21 to ${runtimeDir.absolutePath}")
            runtimeDir.mkdirs()
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build()

            val version = "21.0.11"
            val arch = "aarch64"
            val debUrl =
                "https://packages.termux.dev/apt/termux-main/pool/main/o/openjdk-21/openjdk-21_${version}_${arch}.deb"
            Log.i(TAG, "Downloading JDK from: $debUrl")
            val debFile = File(context.cacheDir, "openjdk-21.deb")

            val request = Request.Builder().url(debUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val msg = "Download failed: HTTP ${response.code}"
                    Log.e(TAG, msg)
                    return@withContext Result.failure(Exception(msg))
                }
                response.body?.byteStream()?.use { input ->
                    debFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
            Log.i(TAG, "Downloaded ${debFile.length()} bytes")

            Log.i(TAG, "Extracting data.tar.xz from .deb...")
            val dataTarXz = File(context.cacheDir, "data.tar.xz")
            extractDataTarFromDeb(debFile, dataTarXz)
            Log.i(TAG, "data.tar.xz extracted (${dataTarXz.length()} bytes)")

            // Extract to temp dir first, then resolve the actual JDK path
            tempDir.mkdirs()
            tempDir.deleteRecursively()
            tempDir.mkdirs()

            Log.i(TAG, "Extracting tar.xz to temp dir ${tempDir.absolutePath}...")
            extractTarXz(dataTarXz, tempDir)

            // Find the actual java binary in the extracted tree
            val actualJava = tempDir.walkTopDown()
                .firstOrNull { it.name == "java" && it.isFile && it.parentFile?.name == "bin" }
            if (actualJava != null) {
                Log.i(TAG, "Found java at ${actualJava.absolutePath}")
                // Move the entire jdk tree to runtimeDir
                val jdkRoot = actualJava.parentFile?.parentFile
                if (jdkRoot != null && jdkRoot.exists()) {
                    runtimeDir.deleteRecursively()
                    runtimeDir.mkdirs()
                    jdkRoot.copyRecursively(runtimeDir, overwrite = true)
                    Log.i(TAG, "Moved JDK to ${runtimeDir.absolutePath}")
                }
            } else {
                // Last resort: search for any java binary
                val anyJava = tempDir.walkTopDown()
                    .firstOrNull { it.name == "java" && it.isFile }
                if (anyJava != null) {
                    Log.i(TAG, "Found java (deep search) at ${anyJava.absolutePath}")
                    val jdkRoot = anyJava.parentFile?.parentFile
                    if (jdkRoot != null && jdkRoot.exists()) {
                        runtimeDir.deleteRecursively()
                        runtimeDir.mkdirs()
                        jdkRoot.copyRecursively(runtimeDir, overwrite = true)
                    }
                } else {
                    // Copy everything flat — should not happen
                    Log.w(TAG, "java binary not found in extracted tree, listing contents:")
                    tempDir.walkTopDown().forEach { Log.w(TAG, "  ${it.absolutePath}") }
                    tempDir.copyRecursively(runtimeDir, overwrite = true)
                }
            }

            javaBinary.setExecutable(true)
            val javacFile = File(runtimeDir, "bin/javac")
            if (javacFile.exists()) javacFile.setExecutable(true)

            // Provide missing system libraries for Termux JDK
            provisionSystemLibraries(runtimeDir)

            debFile.delete()
            dataTarXz.delete()
            tempDir.deleteRecursively()

            val installed = javaBinary.exists()
            Log.i(TAG, "JDK installed: $installed at ${javaBinary.absolutePath}")
            if (!installed) {
                return@withContext Result.failure(Exception("java binary not found after install"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "JDK install failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun provisionSystemLibraries(jdkDir: File) {
        val libDir = File(jdkDir, "lib")

        // libz.so.1 — needs proper SONAME; download from Termux repo
        val zlibTarget = File(libDir, "libz.so.1")
        if (!zlibTarget.exists()) {
            try {
                downloadTermuxLibrary("zlib", "1.3.2", zlibTarget)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to provide libz.so.1: ${e.message}")
            }
        }

        // System libs that just need the versioned name (no SONAME issue)
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
                    Log.i(TAG, "Provided $versionedName from system")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to provide $versionedName: ${e.message}")
                }
            }
        }

        // libandroid-shmem and libandroid-spawn are Termux-specific, not on stock Android
        val shmemLib = File(libDir, "libandroid-shmem.so")
        if (!shmemLib.exists()) {
            try {
                downloadTermuxLibrary("libandroid-shmem", "0.7", shmemLib)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to provide libandroid-shmem.so: ${e.message}")
            }
        }
        val spawnLib = File(libDir, "libandroid-spawn.so")
        if (!spawnLib.exists()) {
            try {
                downloadTermuxLibrary("libandroid-spawn", "0.3", spawnLib)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to provide libandroid-spawn.so: ${e.message}")
            }
        }
    }

    private fun downloadTermuxLibrary(pkg: String, version: String, target: File) {
        val arch = "aarch64"
        // Debian pool convention: packages starting with "lib" use first 4 chars,
        // all others use first char
        val prefix = if (pkg.startsWith("lib")) pkg.substring(0, 4) else pkg.substring(0, 1)
        val url = "https://packages.termux.dev/apt/termux-main/pool/main/$prefix/$pkg/${pkg}_${version}_${arch}.deb"
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
            // Extract .so from deb
            val soTempDir = File(context.cacheDir, "${pkg}-extract")
            soTempDir.mkdirs()
            val dataTarXz = File(context.cacheDir, "${pkg}-data.tar.xz")
            extractDataTarFromDeb(debFile, dataTarXz)
            val tarFile = File(context.cacheDir, "${pkg}-data.tar")
            XZInputStream(FileInputStream(dataTarXz)).use { xzIn ->
                FileOutputStream(tarFile).use { out ->
                    val buf = ByteArray(32768)
                    var read: Int
                    while (xzIn.read(buf).also { read = it } != -1) out.write(buf, 0, read)
                }
            }
            ProcessBuilder("/system/bin/tar", "-xf", tarFile.absolutePath, "-C", soTempDir.absolutePath)
                .redirectErrorStream(true).start().waitFor()
            // Find .so files and copy them
            val soFiles = soTempDir.walkTopDown().filter { it.extension == "so" }.toList()
            if (soFiles.isEmpty()) throw Exception("No .so found in $pkg")
            soFiles.first().inputStream().use { input -> target.outputStream().use { output -> input.copyTo(output) } }
            target.setExecutable(true)
            Log.i(TAG, "Provided ${target.name}")
            // Cleanup
            debFile.delete(); dataTarXz.delete(); tarFile.delete(); soTempDir.deleteRecursively()
        }
    }

    private fun extractDataTarFromDeb(deb: File, output: File) {
        RandomAccessFile(deb, "r").use { raf ->
            val magic = ByteArray(8)
            raf.readFully(magic)
            if (String(magic) != "!<arch>\n") throw Exception("Not a valid deb archive")

            while (raf.filePointer < raf.length()) {
                val header = ByteArray(60)
                raf.readFully(header)
                val name = String(header, 0, 16).trim()
                val sizeStr = String(header, 48, 10).trim()
                val size = sizeStr.toLongOrNull() ?: 0

                if (name == "data.tar.xz" || name == "data.tar.xz/") {
                    output.outputStream().use { out ->
                        val buf = ByteArray(8192)
                        var remaining = size
                        while (remaining > 0) {
                            val read = raf.read(buf, 0, minOf(buf.size.toLong(), remaining).toInt())
                            if (read == -1) break
                            out.write(buf, 0, read)
                            remaining -= read
                        }
                    }
                    return
                }

                raf.seek(raf.filePointer + size)
                if (size % 2L == 1L) raf.seek(raf.filePointer + 1)
            }
            throw Exception("data.tar.xz not found in deb")
        }
    }

    private fun extractTarXz(tarXz: File, destDir: File) {
        // Decompress xz in pure Java (no system xz binary needed)
        val tarFile = File(tarXz.parentFile, "data.tar")
        try {
            XZInputStream(FileInputStream(tarXz)).use { xzIn ->
                FileOutputStream(tarFile).use { out ->
                    val buf = ByteArray(32768)
                    var read: Int
                    while (xzIn.read(buf).also { read = it } != -1) {
                        out.write(buf, 0, read)
                    }
                }
            }
            Log.i(TAG, "XZ decompressed to ${tarFile.length()} bytes")
        } catch (e: Exception) {
            throw Exception("XZ decompression failed: ${e.message}", e)
        }

        // Extract tar with system tar (no -J flag needed since it's plain tar now)
        val process = ProcessBuilder(
            "/system/bin/tar", "-xf", tarFile.absolutePath, "-C", destDir.absolutePath
        ).redirectErrorStream(true).start()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val err = process.inputStream.bufferedReader().readText()
            throw Exception("tar extraction failed ($exitCode): $err")
        }

        tarFile.delete()
    }

    fun resolveJavaPath(): String = javaBinary.absolutePath

    /** Ensure system library shims exist (run on every startup). */
    fun fixupLibraries() {
        Log.i(TAG, "fixupLibraries: isInstalled=$isInstalled javaBinary=${javaBinary.absolutePath}")
        if (isInstalled) provisionSystemLibraries(runtimeDir)
    }

    fun uninstall() {
        runtimeDir.deleteRecursively()
    }
}
