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

    /**
     * Downloads the OpenJDK 21 build published by Termux's package mirror.
     *
     * Termux's openjdk-21 is compiled against Android's bionic libc, so the
     * `bin/java` ELF runs directly on Android without libpthread.so.0 (glibc)
     * link errors. The .deb contains:
     *   ar archive
     *     ├── debian-binary   (4 bytes, ignored)
     *     ├── control.tar.xz  (deb metadata, ignored)
     *     └── data.tar.xz     (the JDK files, ~100 MB compressed)
     *
     * The data tarball extracts to paths under
     *   data/data/com.termux/files/usr/lib/jvm/openjdk-21/
     * so we relocate those to our app's filesDir/runtime/jdk-21/.
     */
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

            // Download the .deb to a single file
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

            // Parse the outer `ar` archive and extract `data.tar.xz` to a file.
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

            // Decompress the .xz stream into a plain .tar (Termux packages use xz).
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
                // .gz fallback (definitely not the case for Termux, but cheap to support)
                java.util.zip.GZIPInputStream(dataXz.inputStream()).use { gzIn ->
                    dataTar.outputStream().use { out ->
                        val buf = ByteArray(32768)
                        var r: Int
                        while (gzIn.read(buf).also { r = it } != -1) out.write(buf, 0, r)
                    }
                }
            } else {
                // No compression (.tar) — just rename
                dataXz.copyTo(dataTar, overwrite = true)
            }
            Log.i(TAG, "Decompressed to ${dataTar.length()} bytes of tar")

            // Extract the .tar using the system tar binary. The paths inside the
            // tarball look like `./data/data/com.termux/files/usr/lib/jvm/openjdk-21/...`.
            emit(JdkInstallPhase.EXTRACTING, "Extracting OpenJDK 21...", 0.92f)
            val tarProc = ProcessBuilder(
                "/system/bin/tar", "-xf", dataTar.absolutePath
            ).redirectErrorStream(true).directory(tempDir).start()
            val tarExit = tarProc.waitFor()
            if (tarExit != 0) {
                val err = tarProc.inputStream.bufferedReader().readText()
                throw Exception("tar extraction failed ($tarExit): $err")
            }

            // Find the actual openjdk-21 root inside the extracted tree and move it
            // into our runtime dir. Termux's deb puts it at
            //   data/data/com.termux/files/usr/lib/jvm/openjdk-21/
            val extractedRoot = tempDir.walkTopDown().firstOrNull { d ->
                d.isDirectory && d.name == "openjdk-21" &&
                    d.absolutePath.contains("com.termux") &&
                    d.absolutePath.contains("usr/lib/jvm")
            } ?: tempDir.walkTopDown().firstOrNull { d ->
                d.isDirectory && d.name == "openjdk-21"
            }
            if (extractedRoot == null) {
                throw Exception("openjdk-21 root directory not found in extracted tarball")
            }
            Log.i(TAG, "Found openjdk-21 root at ${extractedRoot.absolutePath}")

            runtimeDir.deleteRecursively()
            runtimeDir.mkdirs()
            extractedRoot.copyRecursively(runtimeDir, overwrite = true)

            emit(JdkInstallPhase.VERIFYING, "Verifying install...", 0.97f)
            javaBinary.setExecutable(true)
            File(runtimeDir, "bin/javac").takeIf { it.exists() }?.setExecutable(true)
            // Mark all bin/ entries executable (defensive — tar usually preserves modes)
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

    fun fixupLibraries() {}

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
        val debName = "openjdk-${version}_${abi}.deb"
        return version to "$TERMUX_JDK_BASE_URL/$debName"
    }

    /**
     * Minimal `ar` archive parser. Extracts the member with the given name to
     * `destFile`. Returns true on success, false if the member was not found.
     *
     * The `ar` format is a sequence of 60-byte member headers followed by each
     * member's data, padded to a 2-byte boundary. The archive begins with the
     * 8-byte signature `!<arch>\n`.
     */
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
                // Skip this member's data + 2-byte padding if size is odd.
                val padded = size + (size % 2L)
                raf.seek(dataStart + padded)
            }
            return false
        }
    }

    companion object {
        /**
         * Termux's openjdk-21 package version. Pinned to a known-stable version.
         * The package is published at a predictable URL on Termux's pool mirror.
         */
        private const val TERMUX_JDK_VERSION = "21.0.12"
        private const val TERMUX_JDK_BASE_URL =
            "https://packages.termux.dev/apt/termux-main/pool/main/o/openjdk-21"
    }
}
