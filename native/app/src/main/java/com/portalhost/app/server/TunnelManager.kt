package com.portalhost.app.server

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TAG = "TunnelManager"

data class TunnelInfo(
    val tunnelId: String = "",
    val type: String = "tcp",
    val localPort: Int = 25565,
    val publicAddress: String = ""
)

enum class TunnelStatus {
    IDLE, DOWNLOADING, CLAIM_REQUIRED, CONNECTING, CONNECTED, ERROR
}

data class TunnelState(
    val status: TunnelStatus = TunnelStatus.IDLE,
    val claimUrl: String? = null,
    val tunnels: List<TunnelInfo> = emptyList(),
    val error: String? = null,
    val lastOutput: String = ""
)

class TunnelManager(private val context: Context) {
    private val playitDir: File get() = File(context.filesDir, "playit")
    private val daemonBinary: File get() = File(playitDir, "playitd")
    private val cliBinary: File get() = File(playitDir, "playit-cli")
    private val configFile: File get() = File(playitDir, "playit.toml")
    private val socketFile: File get() = File(playitDir, "playitd.sock")
    private val playitGgDir: File get() = File(playitDir, "playit_gg")
    private val defaultConfigFile: File get() = File(playitGgDir, "playit.toml")

    private var process: Process? = null
    private var readJob: Job? = null

    private val _state = MutableStateFlow(TunnelState())
    val state: StateFlow<TunnelState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var tunnelAddresses = mutableListOf<TunnelInfo>()

    private var isClaimed: Boolean = false
    private var secretKey: String? = null
    private var currentServerPort: Int = 25565

    val isRunning: Boolean get() = process?.isAlive == true

    init {
        loadExistingConfig()
    }

    private fun loadExistingConfig() {
        val configToRead = if (defaultConfigFile.exists()) defaultConfigFile else configFile
        if (!configToRead.exists()) return
        try {
            val content = configToRead.readText()
            val secretMatch = Regex("""secret_key\s*=\s*['"](.+)['"]""").find(content)
            if (secretMatch != null) {
                secretKey = secretMatch.groupValues[1]
                isClaimed = true
                Log.i(TAG, "Loaded existing secret key from playit.toml")
            } else {
                Log.w(TAG, "playit.toml exists but no secret_key found")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read playit.toml: ${e.message}")
        }
    }

    private suspend fun extractAsset(assetName: String, targetFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            context.assets.open(assetName).use { input ->
                targetFile.outputStream().use { output -> input.copyTo(output) }
            }
            Log.i(TAG, "Extracted $assetName (${targetFile.length()} bytes)")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Asset $assetName not found: ${e.message}")
            false
        }
    }

    suspend fun extractBinaries(): Result<Unit> = withContext(Dispatchers.IO) {
        _state.value = _state.value.copy(status = TunnelStatus.DOWNLOADING)
        try {
            playitDir.mkdirs()
            val daemonOk = extractAsset("playitd-android", daemonBinary)
            val cliOk = extractAsset("playit-cli-android", cliBinary)
            if (!daemonOk) {
                val msg = "playitd binary not found in assets"
                _state.value = _state.value.copy(status = TunnelStatus.ERROR, error = msg)
                return@withContext Result.failure(Exception(msg))
            }
            _state.value = _state.value.copy(status = TunnelStatus.IDLE)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Extract failed: ${e.message}", e)
            _state.value = _state.value.copy(status = TunnelStatus.ERROR, error = e.message)
            Result.failure(e)
        }
    }

    suspend fun start(serverPort: Int = 25565): Result<Unit> {
        stop()
        currentServerPort = serverPort
        val extractResult = extractBinaries()
        if (extractResult.isFailure) return extractResult

        if (secretKey != null) {
            return startDaemon(serverPort)
        } else {
            return startClaimFlow()
        }
    }

    private suspend fun startClaimFlow(): Result<Unit> = withContext(Dispatchers.IO) {
        _state.value = _state.value.copy(status = TunnelStatus.CONNECTING, error = null)
        try {
            val is64Bit = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
            val linker = if (is64Bit) "/system/bin/linker64" else "/system/bin/linker"
            val cliPath = cliBinary.absolutePath

            val code = runCliCapture(linker, cliPath, "claim", "generate")
            val url = runCliCapture(linker, cliPath, "claim", "url", code)

            Log.i(TAG, "Claim URL: $url")
            _state.value = _state.value.copy(
                status = TunnelStatus.CLAIM_REQUIRED,
                claimUrl = url,
                lastOutput = url.take(200)
            )
            
            startDaemonInClaimMode(currentServerPort)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Claim flow failed", e)
            _state.value = _state.value.copy(status = TunnelStatus.ERROR, error = e.message)
            Result.failure(e)
        }
    }

    private suspend fun startDaemon(serverPort: Int): Result<Unit> = withContext(Dispatchers.IO) {
        _state.value = _state.value.copy(status = TunnelStatus.CONNECTING, error = null)
        tunnelAddresses.clear()
        try {
            workDir.mkdirs()
            socketFile.delete()
            playitGgDir.mkdirs()
            if (!defaultConfigFile.exists()) {
                defaultConfigFile.writeText("secret_key = \"${secretKey!!}\"\nrefresh_from_api = true\nmappings = []\n")
            }
            val is64Bit = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
            val linker = if (is64Bit) "/system/bin/linker64" else "/system/bin/linker"
            val args = mutableListOf(linker, daemonBinary.absolutePath)
            args.add("--socket-path")
            args.add(socketFile.absolutePath)
            Log.i(TAG, "Daemon command: ${args.joinToString(" ")}")

            val proc = ProcessBuilder(args)
                .directory(workDir)
                .redirectErrorStream(true)
                .start()

            process = proc
            startReader(proc, serverPort)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start playitd", e)
            _state.value = _state.value.copy(status = TunnelStatus.ERROR, error = e.message)
            Result.failure(e)
        }
    }
    
    private suspend fun startDaemonInClaimMode(serverPort: Int): Result<Unit> = withContext(Dispatchers.IO) {
        _state.value = _state.value.copy(status = TunnelStatus.CONNECTING, error = null)
        tunnelAddresses.clear()
        try {
            workDir.mkdirs()
            socketFile.delete()
            playitGgDir.mkdirs()
            if (!defaultConfigFile.exists()) {
                defaultConfigFile.writeText("secret_key = \"\"\nrefresh_from_api = true\nmappings = []\n")
            }
            val is64Bit = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
            val linker = if (is64Bit) "/system/bin/linker64" else "/system/bin/linker"
            val args = mutableListOf(linker, daemonBinary.absolutePath)
            args.add("--socket-path")
            args.add(socketFile.absolutePath)
            Log.i(TAG, "Daemon claim-mode command: ${args.joinToString(" ")}")

            val proc = ProcessBuilder(args)
                .directory(workDir)
                .redirectErrorStream(true)
                .start()

            process = proc
            startReader(proc, serverPort)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start playitd in claim mode", e)
            _state.value = _state.value.copy(status = TunnelStatus.ERROR, error = e.message)
            Result.failure(e)
        }
    }

    private fun runCliCapture(linker: String, cliPath: String, vararg args: String): String {
        val fullArgs = mutableListOf(linker, cliPath)
        fullArgs.addAll(args)
        val proc = ProcessBuilder(fullArgs)
            .directory(workDir)
            .redirectErrorStream(false)
            .start()
        val output = proc.inputStream.bufferedReader().readText().trim()
        val exitCode = proc.waitFor()
        if (exitCode != 0) {
            val err = proc.errorStream.bufferedReader().readText().trim()
            throw IOException("playit-cli exited $exitCode: $err")
        }
        if (output.isEmpty()) throw IOException("playit-cli returned empty output")
        return output
    }

    private fun startReader(proc: Process, serverPort: Int) {
        readJob?.cancel()
        readJob = scope.launch {
            try {
                proc.inputStream.bufferedReader().use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val text = line!!
                        Log.d(TAG, "playitd: $text")
                        _state.value = _state.value.copy(lastOutput = text.take(200))
                        parseLine(text, serverPort)
                    }
                    Log.i(TAG, "playitd read loop ended (EOF)")
                }
            } catch (e: IOException) {
                Log.i(TAG, "playitd read loop ended: ${e.message}")
            } catch (e: Exception) {
                Log.w(TAG, "playitd read loop error: ${e.message}", e)
            } finally {
                if (process === proc) {
                    val hadClaim = _state.value.claimUrl
                    val lastOut = _state.value.lastOutput
                    readJob = null
                    tunnelAddresses.clear()
                    process = null
                    val errMsg = if (lastOut.isNotEmpty()) "Process exited (last: $lastOut)" else "Process exited with no output"
                    _state.value = _state.value.copy(
                        status = TunnelStatus.ERROR,
                        tunnels = emptyList(),
                        error = errMsg,
                        claimUrl = hadClaim,
                        lastOutput = lastOut
                    )
                }
            }
        }
    }

    private fun parseLine(text: String, serverPort: Int) {
        val claimMatch = Regex("""https://playit\.gg/claim/[\w-]+""").find(text)
        if (claimMatch != null && !isClaimed) {
            val url = claimMatch.value
            Log.i(TAG, "*** CLAIM URL: $url ***")
            _state.value = _state.value.copy(
                status = TunnelStatus.CLAIM_REQUIRED,
                claimUrl = url
            )
            return
        }

        val tunnelMatch = Regex("""([\w.-]+\.(?:playit\.gg|ply\.gg|at\.ply\.gg|joinmc\.link)):(\d+)""").find(text)
        if (tunnelMatch != null) {
            val host = tunnelMatch.groupValues[1]
            val port = tunnelMatch.groupValues[2].toIntOrNull() ?: 0
            val address = "$host:$port"
            if (tunnelAddresses.none { it.publicAddress == address }) {
                val tunnelType = if (text.contains("udp", ignoreCase = true)) "udp" else "tcp"
                val tunnel = TunnelInfo(
                    tunnelId = tunnelAddresses.size.toString(),
                    type = tunnelType,
                    localPort = serverPort,
                    publicAddress = address
                )
                tunnelAddresses.add(tunnel)
                Log.i(TAG, "Tunnel address detected: $address (type=$tunnelType)")
                _state.value = _state.value.copy(
                    status = TunnelStatus.CONNECTED,
                    tunnels = tunnelAddresses.toList()
                )
            }
            return
        }

        if (text.contains("assigned", ignoreCase = true) ||
            text.contains("tunnel", ignoreCase = true) ||
            text.contains("allocation", ignoreCase = true)) {
            Log.d(TAG, "Tunnel event: $text")
        }

        if (text.contains("msg=account verified", ignoreCase = true) ||
            text.contains("status: listening", ignoreCase = true)) {
            Log.i(TAG, "Agent connected to control server, waiting for tunnel assignment")
        }

        if (text.contains("failed to connect", ignoreCase = true) ||
            text.contains("error", ignoreCase = true) ||
            text.contains("unable to", ignoreCase = true)) {
            if (_state.value.status == TunnelStatus.CONNECTING || _state.value.status == TunnelStatus.CONNECTED) {
                _state.value = _state.value.copy(
                    status = TunnelStatus.ERROR,
                    error = text.take(200)
                )
            }
        }
    }

    fun stop() {
        val proc = process ?: return
        Log.i(TAG, "Stopping process...")
        readJob?.cancel()
        process = null
        try {
            proc.destroyForcibly()
            proc.waitFor(5, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Log.w(TAG, "Error killing process: ${e.message}")
        }
        readJob = null
        tunnelAddresses.clear()
        _state.value = _state.value.copy(
            status = TunnelStatus.IDLE,
            tunnels = emptyList(),
            error = null,
            claimUrl = _state.value.claimUrl
        )
    }

    fun dispose() {
        stop()
        scope.cancel()
    }

    fun resetClaim() {
        stop()
        configFile.delete()
        defaultConfigFile.delete()
        socketFile.delete()
        secretKey = null
        isClaimed = false
        _state.value = TunnelState()
    }

    fun setSecretKey(key: String) {
        secretKey = key
        isClaimed = true
        try {
            playitGgDir.mkdirs()
            defaultConfigFile.writeText("secret_key = '$key'\nrefresh_from_api = true\nmappings = []\n")
            Log.i(TAG, "Secret key saved to ${defaultConfigFile.absolutePath}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save secret key: ${e.message}")
        }
    }

    val binaryExists: Boolean get() = daemonBinary.exists() && daemonBinary.length() > 0

    private val workDir: File get() = File(context.filesDir, "playit")
}
