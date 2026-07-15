package com.portalhost.server

import com.portalhost.filesystem.resolveAppDataDir
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
    IDLE, CONNECTING, CONNECTED, ERROR
}

data class TunnelState(
    val status: TunnelStatus = TunnelStatus.IDLE,
    val tunnels: List<TunnelInfo> = emptyList(),
    val error: String? = null,
    val lastOutput: String = ""
)

class TunnelManager {
    private val playitDir: File get() = File(resolveAppDataDir(), "playit")
    private val configFile: File get() = File(playitDir, "playit.toml")
    private val daemonBinary: File get() = File(playitDir, "playitd")

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

    fun setSecretKey(key: String) {
        secretKey = key
        try {
            playitDir.mkdirs()
            configFile.writeText("secret_key = '$key'\nrefresh_from_api = true\nmappings = []\n")
        } catch (_: Exception) {}
    }

    fun getSecretKey(): String? = secretKey

    suspend fun start(serverPort: Int = 25565): Result<Unit> {
        logger.info { "Starting tunnel on port $serverPort" }
        stop()
        currentServerPort = serverPort

        if (!daemonBinary.exists()) {
            logger.warn { "playitd binary not found at ${daemonBinary.absolutePath}" }
            return Result.failure(Exception("playitd binary not found at ${daemonBinary.absolutePath}"))
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

            val args = mutableListOf(daemonBinary.absolutePath)
            val logFile = File(playitDir, "playitd.log")
            args.add("--log-path")
            args.add(logFile.absolutePath)
            if (secretKey != null) {
                args.add("--secret")
                args.add(secretKey!!)
            }

            val proc = ProcessBuilder(args)
                .directory(playitDir)
                .redirectErrorStream(true)
                .start()

            process = proc
            startReader(proc, serverPort)

            _state.value = _state.value.copy(status = TunnelStatus.CONNECTING)
            logger.info { "Tunnel process started" }
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
