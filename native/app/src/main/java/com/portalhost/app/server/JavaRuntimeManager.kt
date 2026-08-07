package com.portalhost.app.server

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
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
            Log.i(TAG, "Installing OpenJDK 21 to ${runtimeDir.absolutePath}")
            runtimeDir.mkdirs()
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build()

            emit(JdkInstallPhase.CONNECTING, "Connecting to JDK server...", 0f)

            // Download from Adoptium API – always resolves to the latest JDK 21 GA build
            val downloadUrl =
                "https://api.adoptium.net/v3/binary/latest/21/ga/linux/aarch64/jdk/hotspot/normal/eclipse"
            Log.i(TAG, "Downloading JDK from: $downloadUrl")
            val archiveFile = File(context.cacheDir, "jdk-21.tar.gz")

            val request = Request.Builder().url(downloadUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val msg = "Download failed: HTTP ${response.code}"
                    Log.e(TAG, msg)
                    _installState.value = JdkInstallState(JdkInstallPhase.ERROR, 0f, "", msg)
                    return@withContext Result.failure(Exception(msg))
                }
                val contentLength = response.body?.contentLength() ?: -1L
                emit(JdkInstallPhase.DOWNLOADING, "Downloading JDK 21...", 0f)
                response.body?.byteStream()?.use { input ->
                    archiveFile.outputStream().use { output ->
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
                                    "Downloading JDK 21...",
                                    null
                                )
                                onProgress?.invoke(pct)
                            }
                        }
                    }
                }
            }
            Log.i(TAG, "Downloaded ${archiveFile.length()} bytes")

            // Extract tar.gz directly using tar
            tempDir.mkdirs()
            tempDir.deleteRecursively()
            tempDir.mkdirs()

            emit(JdkInstallPhase.EXTRACTING, "Extracting JDK...", 0.82f)
            Log.i(TAG, "Extracting ${archiveFile.name} to ${tempDir.absolutePath}...")
            val process = ProcessBuilder(
                "/system/bin/tar", "-xzf", archiveFile.absolutePath, "-C", tempDir.absolutePath
            ).redirectErrorStream(true).start()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val err = process.inputStream.bufferedReader().readText()
                throw Exception("tar extraction failed ($exitCode): $err")
            }

            // Find the java binary (archive has a versioned top-level dir like jdk-21.0.7+6/)
            val actualJava = tempDir.walkTopDown()
                .firstOrNull { it.name == "java" && it.isFile && it.parentFile?.name == "bin" }
            if (actualJava != null) {
                Log.i(TAG, "Found java at ${actualJava.absolutePath}")
                val jdkRoot = actualJava.parentFile?.parentFile
                if (jdkRoot != null && jdkRoot.exists()) {
                    runtimeDir.deleteRecursively()
                    runtimeDir.mkdirs()
                    jdkRoot.copyRecursively(runtimeDir, overwrite = true)
                    Log.i(TAG, "Moved JDK to ${runtimeDir.absolutePath}")
                }
            } else {
                throw Exception("java binary not found in extracted archive")
            }

            emit(JdkInstallPhase.VERIFYING, "Verifying install...", 0.95f)
            javaBinary.setExecutable(true)
            val javacFile = File(runtimeDir, "bin/javac")
            if (javacFile.exists()) javacFile.setExecutable(true)

            archiveFile.delete()
            tempDir.deleteRecursively()

            val installed = javaBinary.exists()
            Log.i(TAG, "JDK installed: $installed at ${javaBinary.absolutePath}")
            if (!installed) {
                _installState.value = JdkInstallState(JdkInstallPhase.ERROR, 0f, "", "java binary not found after install")
                return@withContext Result.failure(Exception("java binary not found after install"))
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
}
