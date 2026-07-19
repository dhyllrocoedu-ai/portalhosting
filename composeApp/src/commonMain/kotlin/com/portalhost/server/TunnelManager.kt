package com.portalhost.server

import com.portalhost.filesystem.FileSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

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

class TunnelManager(private val fileSystem: FileSystem = com.portalhost.filesystem.FileSystem()) {
    private val playitDir: File get() = File(fileSystem.getAppDirBlocking(), "playit")
    private val configFile: File get() = File(playitDir, "playit.toml")
    private val daemonBinary: File get() = File(playitDir, if (System.getProperty("os.name").lowercase().contains("win")) "playitd.exe" else "playitd")

    private var process: Process? = null
    private var readJob: Job? = null
    private var pollJob: Job? = null

    private val _state = MutableStateFlow(TunnelState())
    val state: StateFlow<TunnelState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var tunnelAddresses = mutableListOf<TunnelInfo>()
    private var secretKey: String? = null
    private var currentServerPort: Int = 25565

    val isRunning: Boolean get() = process?.isAlive == true

    init {
        loadExistingConfig()
    }

    private fun loadExistingConfig() {
        if (!configFile.exists()) return
        try {
            val content = configFile.readText()
            val secretMatch = Regex("""secret_key\s*=\s*['"](.+)['"]""").find(content)
            if (secretMatch != null) {
                secretKey = secretMatch.groupValues[1]
            }
        } catch (_: Exception) {}
    }

    suspend fun downloadBinary(): Result<Unit> {
        _state.value = _state.value.copy(status = TunnelStatus.DOWNLOADING)
        return try {
            val os = System.getProperty("os.name").lowercase()
            val arch = System.getProperty("os.arch").lowercase()
            val isWindows = os.contains("win")
            val is64 = arch.contains("64") || arch.contains("amd")
            val suffix = when {
                isWindows -> if (is64) "windows-x86_64.exe" else "windows-x86_32.exe"
                os.contains("mac") -> if (arch.contains("aarch64")) "darwin-arm64" else "darwin-amd64"
                else -> if (is64) "linux-amd64" else "linux-386"
            }
            val downloadUrl = "https://github.com/playit-cloud/playit-agent/releases/latest/download/playit-$suffix"
            logger.info { "Downloading playit agent from $downloadUrl" }

            playitDir.mkdirs()
            val conn = URL(downloadUrl).openConnection()
            conn.connectTimeout = 30000
            conn.readTimeout = 120000
            conn.connect()
            val input = conn.getInputStream()
            
            val targetBinary = if (isWindows) File(playitDir, "playitd.exe") else daemonBinary
            targetBinary.outputStream().use { output -> input.copyTo(output) }
            targetBinary.setExecutable(true)

            logger.info { "Downloaded playit agent (${targetBinary.length()} bytes) to ${targetBinary.absolutePath}" }
            _state.value = _state.value.copy(status = TunnelStatus.IDLE)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Failed to download playit agent" }
            _state.value = _state.value.copy(status = TunnelStatus.ERROR, error = "Download failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun setSecretKey(key: String) {
        secretKey = key
        try {
            playitDir.mkdirs()
            configFile.writeText("secret_key = '$key'\nrefresh_from_api = true\nmappings = []\n")
            _state.value = _state.value.copy(status = TunnelStatus.CONNECTING, claimUrl = null)
            kotlinx.coroutines.runBlocking { start(currentServerPort) }
        } catch (_: Exception) {}
    }

    fun startClaimFlow() {
        stop()
        configFile.delete()
        secretKey = null
        _state.value = TunnelState(status = TunnelStatus.CONNECTING)
        kotlinx.coroutines.runBlocking { start(currentServerPort) }
    }

    fun getSecretKey(): String? = secretKey

    suspend fun start(serverPort: Int = 25565): Result<Unit> {
        logger.info { "Starting tunnel on port $serverPort" }
        stop()
        currentServerPort = serverPort

        if (!daemonBinary.exists()) {
            logger.info { "playit agent not found, downloading..." }
            val downloadResult = downloadBinary()
            if (downloadResult.isFailure) return downloadResult
        }

        _state.value = _state.value.copy(status = TunnelStatus.CONNECTING, error = null)
        tunnelAddresses.clear()

        return try {
            playitDir.mkdirs()
            val mapping = """{protocol = "tcp", local_address = "0.0.0.0:$serverPort", public_address = ""}"""
            configFile.writeText("""secret_key = "${secretKey ?: ""}"
refresh_from_api = true
mappings = [$mapping]
""".trimIndent())

            // Ensure binary is executable on Windows
            if (daemonBinary.exists()) {
                daemonBinary.setExecutable(true)
            }

            val args = mutableListOf(daemonBinary.absolutePath)
            val logFile = File(playitDir, "playitd.log")
            args.add("--log-path")
            args.add(logFile.absolutePath)
            if (secretKey != null) {
                args.add("--secret")
                args.add(secretKey!!)
            }

            logger.info { "Starting tunnel with command: ${args.joinToString(" ")}" }
            logger.info { "Working directory: ${playitDir.absolutePath}" }
            logger.info { "Binary exists: ${daemonBinary.exists()}, executable: ${daemonBinary.canExecute()}" }

            val proc = ProcessBuilder(args)
                .directory(playitDir)
                .redirectErrorStream(true)
                .start()

            process = proc
            startReader(proc, serverPort)

            // Wait briefly to check if process crashes immediately
            delay(1000)
            if (!proc.isAlive) {
                val exitCode = proc.exitValue()
                logger.error { "Tunnel process exited immediately with code $exitCode" }
                _state.value = _state.value.copy(status = TunnelStatus.ERROR, error = "Process exited with code $exitCode")
                return Result.failure(Exception("Process exited with code $exitCode"))
            }

            _state.value = _state.value.copy(status = TunnelStatus.CONNECTING)
            logger.info { "Tunnel process started with PID: ${proc.pid()}" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Failed to start tunnel" }
            _state.value = _state.value.copy(status = TunnelStatus.ERROR, error = e.message)
            Result.failure(e)
        }
    }

    private fun startReader(proc: Process, serverPort: Int) {
        readJob?.cancel()
        readJob = scope.launch {
            try {
                proc.inputStream.bufferedReader().use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val text = line!!
                        _state.value = _state.value.copy(lastOutput = text.take(200))
                        parseLine(text, serverPort)
                    }
                }
            } catch (_: IOException) {
            } catch (_: Exception) {
            } finally {
                if (process === proc) {
                    readJob = null
                    tunnelAddresses.clear()
                    process = null
                    _state.value = _state.value.copy(
                        status = TunnelStatus.ERROR,
                        tunnels = emptyList(),
                        error = "Process exited",
                    )
                }
            }
        }
    }

    private fun parseLine(text: String, serverPort: Int) {
        val claimMatch = Regex("""https://playit\.gg/claim/[\w-]+""").find(text)
        if (claimMatch != null && secretKey == null) {
            val url = claimMatch.value
            logger.info { "Claim URL detected: $url" }
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
                val tunnel = TunnelInfo(
                    tunnelId = tunnelAddresses.size.toString(),
                    type = "tcp",
                    localPort = serverPort,
                    publicAddress = address
                )
                tunnelAddresses.add(tunnel)
                _state.value = _state.value.copy(
                    status = TunnelStatus.CONNECTED,
                    tunnels = tunnelAddresses.toList()
                )
            }
            return
        }

        if (text.contains("error", ignoreCase = true) || text.contains("failed", ignoreCase = true)) {
            if (_state.value.status == TunnelStatus.CONNECTING) {
                _state.value = _state.value.copy(status = TunnelStatus.ERROR, error = text.take(200))
            }
        }
    }

    fun stop() {
        logger.info { "Stopping tunnel" }
        readJob?.cancel()
        pollJob?.cancel()
        readJob = null
        pollJob = null
        try { process?.destroyForcibly() } catch (_: Exception) {}
        try { process?.waitFor(5, java.util.concurrent.TimeUnit.SECONDS) } catch (_: Exception) {}
        process = null
        tunnelAddresses.clear()
        _state.value = TunnelState()
    }

    fun dispose() {
        stop()
        scope.cancel()
    }

    fun resetKey() {
        logger.info { "Resetting tunnel secret key" }
        stop()
        configFile.delete()
        secretKey = null
        _state.value = TunnelState()
    }
}
