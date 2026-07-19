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
        } catch (e: Exception) {
            logger.warn(e) { "Failed to read existing playit config" }
        }
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
            val configContent = if (secretKey != null) {
                """secret_key = "${secretKey}"
refresh_from_api = true
mappings = [$mapping]
"""
            } else {
                """refresh_from_api = true
mappings = [$mapping]
"""
            }
            configFile.writeText(configContent.trimIndent())
            logger.info { "Config written to ${configFile.absolutePath}: secretKey=${secretKey != null}, size=${configFile.length()}" }
            if (secretKey != null) {
                logger.info { "Config content (first 200 chars): ${configContent.trimIndent().take(200)}" }
            }

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
            startTunnelPoller(serverPort)

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
                    var lineCount = 0
                    while (reader.readLine().also { line = it } != null) {
                        val text = line!!
                        lineCount++
                        if (lineCount <= 5 || lineCount % 10 == 0) {
                            logger.debug { "playit line $lineCount: ${text.take(100)}" }
                        }
                        _state.value = _state.value.copy(lastOutput = text.take(200))
                        parseLine(text, serverPort)
                    }
                    logger.info { "playit process stdout ended after $lineCount lines" }
                }
            } catch (_: IOException) {
                val logTail = readLogTail()
                if (logTail.isNotBlank() && _state.value.status != TunnelStatus.CONNECTED) {
                    _state.value = _state.value.copy(
                        lastOutput = logTail.take(200)
                    )
                }
            } catch (e: Exception) {
                logger.warn(e) { "playit reader exception" }
            } finally {
                if (process === proc) {
                    logger.info { "playit reader finally block: process=${process?.isAlive}, status=${_state.value.status}" }
                    readJob = null
                    process = null
                    val logTail = readLogTail()
                    val errorMsg = if (logTail.isNotBlank()) {
                        val lines = logTail.lines()
                        val relevant = lines.takeLast(5).filter { it.isNotBlank() }
                        if (relevant.isNotEmpty()) relevant.joinToString(" | ") else "Process exited"
                    } else {
                        "Process exited"
                    }
                    tunnelAddresses.clear()
                    if (_state.value.status != TunnelStatus.CLAIM_REQUIRED && _state.value.status != TunnelStatus.CONNECTED) {
                        _state.value = _state.value.copy(
                            status = TunnelStatus.ERROR,
                            tunnels = emptyList(),
                            error = errorMsg,
                        )
                    } else {
                        logger.info { "Not overwriting status ${_state.value.status} on process exit" }
                    }
                }
            }
        }
    }

    private fun readLogTail(): String {
        return try {
            val logFile = File(playitDir, "playitd.log")
            if (logFile.exists()) {
                logFile.readLines().takeLast(15).joinToString("\n")
            } else ""
        } catch (_: Exception) { "" }
    }

    private fun parseLine(text: String, serverPort: Int) {
        logger.debug { "playit output: $text" }

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
        if (claimMatch != null && secretKey != null) {
            logger.warn { "Claim URL found but secretKey already set - treating as invalid/expired secret, resetting" }
            // Secret is likely invalid/expired - clear it and re-enter claim flow
            secretKey = null
            configFile.delete()
            val url = claimMatch.value
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

        if (text.contains("failed to connect", ignoreCase = true) ||
            text.contains("unable to", ignoreCase = true) ||
            (text.contains("error", ignoreCase = true) && text.contains("exit", ignoreCase = true))) {
            if (_state.value.status == TunnelStatus.CONNECTING || _state.value.status == TunnelStatus.CONNECTED) {
                _state.value = _state.value.copy(status = TunnelStatus.ERROR, error = text.take(200))
                // Detect invalid/expired secret - reset and re-enter claim flow
                if (secretKey != null && (text.contains("unauthorized", ignoreCase = true) ||
                    text.contains("invalid secret", ignoreCase = true) ||
                    text.contains("authentication", ignoreCase = true) ||
                    text.contains("secret", ignoreCase = true) && text.contains("fail", ignoreCase = true))) {
                    logger.warn { "Auth error detected, clearing secret and entering claim flow" }
                    secretKey = null
                    configFile.delete()
                    _state.value = _state.value.copy(status = TunnelStatus.CLAIM_REQUIRED, claimUrl = null)
                }
            }
        }
    }

    private fun startTunnelPoller(serverPort: Int) {
        pollJob?.cancel()
        pollJob = scope.launch {
            delay(5000)
            while (isActive) {
                val key = secretKey ?: break
                try {
                    val conn = URL("https://api.playit.gg/v1/agents/rundata").openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Authorization", "Agent-Key $key")
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    OutputStreamWriter(conn.outputStream).use { it.write("{}") }
                    val body = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()

                    if (body.contains("\"status\":\"success\"") || body.contains("\"status\": \"success\"")) {
                        val addrPattern = Regex(""""display_address"\s*:\s*"([^"]+)"""")
                        val addrs = addrPattern.findAll(body).map { it.groupValues[1] }.toList()
                        for (addr in addrs) {
                            if (tunnelAddresses.none { it.publicAddress == addr }) {
                                val typeMatch = Regex(""""port_type"\s*:\s*"([^"]+)"""").find(body)
                                val tunnelType = if (typeMatch != null) typeMatch.groupValues[1] else "tcp"
                                val tunnel = TunnelInfo(
                                    tunnelId = tunnelAddresses.size.toString(),
                                    type = tunnelType,
                                    localPort = serverPort,
                                    publicAddress = addr
                                )
                                tunnelAddresses.add(tunnel)
                                logger.info { "Tunnel from API: $addr (type=$tunnelType)" }
                            }
                        }
                        if (tunnelAddresses.isNotEmpty()) {
                            _state.value = _state.value.copy(
                                status = TunnelStatus.CONNECTED,
                                tunnels = tunnelAddresses.toList()
                            )
                            break
                        }
                    }
                } catch (e: Exception) {
                    logger.debug { "API poll: ${e.message}" }
                }
                delay(5000)
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
