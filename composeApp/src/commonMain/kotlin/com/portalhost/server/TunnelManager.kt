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
import java.security.SecureRandom
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

private const val MAX_OUTPUT_LINES = 8

private val json = Json { isLenient = true; ignoreUnknownKeys = true }

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
    private val apiBase = "https://api.playit.gg"

    private var process: Process? = null
    private var readJob: Job? = null
    private var pollJob: Job? = null
    private var exchangeJob: Job? = null
    private var provisionStartTime: Long = 0

    private val _state = MutableStateFlow(TunnelState())
    val state: StateFlow<TunnelState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var tunnelAddresses = mutableListOf<TunnelInfo>()
    private var secretKey: String? = null
    private var currentServerPort: Int = 25565
    private val outputBuffer = ArrayDeque<String>(MAX_OUTPUT_LINES)

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

            playitDir.mkdirs()

            val daemonUrl = "https://github.com/playit-cloud/playit-agent/releases/latest/download/playit-$suffix"
            logger.info { "Downloading playit agent from $daemonUrl" }
            val conn = URL(daemonUrl).openConnection()
            conn.connectTimeout = 30000
            conn.readTimeout = 120000
            conn.connect()
            val input = conn.getInputStream()
            val targetDaemon = if (isWindows) File(playitDir, "playitd.exe") else daemonBinary
            targetDaemon.outputStream().use { output -> input.copyTo(output) }
            targetDaemon.setExecutable(true)
            logger.info { "Downloaded playit agent (${targetDaemon.length()} bytes)" }

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
        logger.info { "Starting API claim flow from UI" }
        scope.launch {
            forceStop()
            _state.value = TunnelState(status = TunnelStatus.CONNECTING)
            start(currentServerPort)
        }
    }

    private fun generateClaimCode(): String {
        val bytes = ByteArray(5)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun claimUrl(code: String): String = "https://playit.gg/claim/$code"

    private suspend fun apiPost(path: String, jsonBody: String, authHeader: String? = null): String? {
        return try {
            val conn = URL("$apiBase$path").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            if (authHeader != null) conn.setRequestProperty("Authorization", authHeader)
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            OutputStreamWriter(conn.outputStream).use { it.write(jsonBody) }
            val body = conn.inputStream.bufferedReader().readText().trim()
            conn.disconnect()
            body
        } catch (_: Exception) { null }
    }

    private fun parseStatus(resp: String): String? {
        return try {
            json.parseToJsonElement(resp).jsonObject["status"]?.jsonPrimitive?.contentOrNull
        } catch (_: Exception) { null }
    }

    private fun parseDataString(resp: String): String? {
        return try {
            val data = json.parseToJsonElement(resp).jsonObject["data"] ?: return null
            data.jsonPrimitive.contentOrNull
        } catch (_: Exception) { null }
    }

    private fun parseDataObject(resp: String): JsonObject? {
        return try {
            json.parseToJsonElement(resp).jsonObject["data"]?.jsonObject
        } catch (_: Exception) { null }
    }

    private fun parseTunnelsFromRundata(body: String, serverPort: Int): List<TunnelInfo> {
        return try {
            val element = json.parseToJsonElement(body)
            val tunnelsArray = element.jsonObject["data"]?.jsonObject?.get("tunnels")?.jsonArray
            tunnelsArray?.mapNotNull { tunnelElement ->
                val tunnel = tunnelElement.jsonObject
                val addr = tunnel["display_address"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val tunnelType = tunnel["port_type"]?.jsonPrimitive?.contentOrNull ?: "tcp"
                TunnelInfo(
                    tunnelId = tunnel["id"]?.jsonPrimitive?.contentOrNull ?: "",
                    type = tunnelType,
                    localPort = serverPort,
                    publicAddress = addr
                )
            } ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    private suspend fun startClaimFlowInternal(): Result<Unit> = withContext(Dispatchers.IO) {
        _state.value = _state.value.copy(status = TunnelStatus.CONNECTING, error = null)
        try {
            playitDir.mkdirs()
            if (!configFile.exists()) {
                configFile.writeText("""secret_key = ""
refresh_from_api = true
mappings = []
""".trimIndent())
            }

            val code = generateClaimCode()
            val url = claimUrl(code)
            logger.info { "Claim code: $code, URL: $url" }

            _state.value = _state.value.copy(
                status = TunnelStatus.CLAIM_REQUIRED,
                claimUrl = url
            )

            exchangeAndStartDaemon(code)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Claim flow failed" }
            _state.value = _state.value.copy(status = TunnelStatus.ERROR, error = e.message)
            Result.failure(e)
        }
    }

    private fun exchangeAndStartDaemon(claimCode: String) {
        tunnelAddresses.clear()
        val mapping = """{protocol = "tcp", local_address = "0.0.0.0:$currentServerPort", public_address = ""}"""
        try {
            configFile.writeText("""secret_key = ""
refresh_from_api = true
mappings = [$mapping]
""".trimIndent())
        } catch (_: Exception) {}

        exchangeJob = scope.launch {
            try {
                val setupPayload = """{"code":"$claimCode","agent_type":"self-managed","version":"playit 1.0.10"}"""
                val exchangePayload = """{"code":"$claimCode"}"""

                logger.info { "Polling claim setup for code $claimCode" }

                var userVisited = false
                while (isActive) {
                    val resp = apiPost("/claim/setup", setupPayload) ?: run {
                        delay(2000)
                        continue
                    }

                    when (parseStatus(resp)) {
                        "success" -> {
                            when (parseDataString(resp)) {
                                "UserAccepted" -> {
                                    logger.info { "Claim accepted, exchanging for secret key" }
                                    break
                                }
                                "UserRejected" -> {
                                    throw IOException("Claim was rejected in the browser")
                                }
                                "WaitingForUser" -> {
                                    if (!userVisited) {
                                        userVisited = true
                                        logger.info { "User visited claim URL, waiting for approval" }
                                    }
                                }
                                else -> { /* WaitingForUserVisit */ }
                            }
                        }
                        "fail" -> {
                            val failData = parseDataString(resp) ?: "unknown"
                            throw IOException("Claim setup failed: $failData")
                        }
                        "error" -> {
                            logger.warn { "Claim setup API error: ${resp.take(200)}" }
                        }
                        null -> {
                            logger.warn { "Claim setup: unexpected response: ${resp.take(200)}" }
                        }
                    }
                    delay(2000)
                }

                val exchangeResp = apiPost("/claim/exchange", exchangePayload)
                    ?: throw IOException("No response from claim exchange")

                if (parseStatus(exchangeResp) != "success") {
                    throw IOException("Claim exchange failed: ${exchangeResp.take(200)}")
                }

                val dataObj = parseDataObject(exchangeResp)
                val newSecret = dataObj?.get("secret_key")?.jsonPrimitive?.contentOrNull

                if (!newSecret.isNullOrEmpty()) {
                    secretKey = newSecret
                    logger.info { "Claim confirmed, secret key stored" }
                    configFile.writeText("""secret_key = "$newSecret"
refresh_from_api = true
mappings = [$mapping]
""".trimIndent())
                } else {
                    throw IOException("Empty secret key from exchange")
                }

                _state.value = _state.value.copy(status = TunnelStatus.CONNECTING, error = null)
                start(currentServerPort)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                logger.error(e) { "Failed to exchange claim" }
                _state.value = _state.value.copy(status = TunnelStatus.ERROR, error = e.message)
            }
        }
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

        if (secretKey == null) {
            return startClaimFlowInternal()
        }

        _state.value = _state.value.copy(status = TunnelStatus.CONNECTING, error = null)
        tunnelAddresses.clear()
        outputBuffer.clear()

        return try {
            playitDir.mkdirs()
            val mapping = """{protocol = "tcp", local_address = "0.0.0.0:$serverPort", public_address = ""}"""
            val configContent = """secret_key = "${secretKey}"
refresh_from_api = true
mappings = [$mapping]
"""
            configFile.writeText(configContent.trimIndent())
            logger.info { "Config written to ${configFile.absolutePath}" }

            if (daemonBinary.exists()) {
                daemonBinary.setExecutable(true)
            }

            val args = mutableListOf(daemonBinary.absolutePath)
            val logFile = File(playitDir, "playitd.log")
            args.add("--log-path")
            args.add(logFile.absolutePath)
            args.add("--secret")
            args.add(secretKey!!)

            logger.info { "Starting tunnel with command: ${args.joinToString(" ")}" }
            logger.info { "Working directory: ${playitDir.absolutePath}" }

            val proc = ProcessBuilder(args)
                .directory(playitDir)
                .redirectErrorStream(true)
                .start()

            process = proc
            provisionStartTime = System.currentTimeMillis()
            startReader(proc, serverPort)
            startTunnelPoller(serverPort)

            logger.info { "Tunnel process started with PID: ${proc.pid()}" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Failed to start tunnel" }
            _state.value = _state.value.copy(status = TunnelStatus.ERROR, error = e.message)
            Result.failure(e)
        }
    }

    private fun pushOutput(text: String) {
        if (outputBuffer.size >= MAX_OUTPUT_LINES) {
            outputBuffer.removeFirst()
        }
        outputBuffer.addLast(text.take(200))
        _state.value = _state.value.copy(lastOutput = outputBuffer.joinToString("\n"))
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
                        pushOutput(text)
                        parseLine(text, serverPort)
                    }
                    logger.info { "playit process stdout ended after $lineCount lines" }
                }
            } catch (_: IOException) {
                val logTail = readLogTail()
                if (logTail.isNotBlank() && _state.value.status != TunnelStatus.CONNECTED) {
                    val lines = logTail.lines().filter { it.isNotBlank() }.takeLast(MAX_OUTPUT_LINES)
                    _state.value = _state.value.copy(lastOutput = lines.joinToString("\n"))
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
            secretKey = null
            configFile.delete()
            val url = claimMatch.value
            _state.value = _state.value.copy(
                status = TunnelStatus.CLAIM_REQUIRED,
                claimUrl = url
            )
            return
        }

        if (text.contains("Waiting for frontend secret provisioning over IPC", ignoreCase = true) && secretKey != null) {
            logger.warn { "Detected IPC provisioning wait - clearing stale secret and re-entering claim flow" }
            secretKey = null
            configFile.delete()
            _state.value = _state.value.copy(
                status = TunnelStatus.CLAIM_REQUIRED,
                claimUrl = null
            )
            scope.launch { start(currentServerPort) }
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
            var lastSuccessfulPoll = System.currentTimeMillis()
            while (isActive) {
                val key = secretKey ?: break
                var pollSucceeded = false
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
                    pollSucceeded = true
                    lastSuccessfulPoll = System.currentTimeMillis()

                    val tunnelsFromApi = parseTunnelsFromRundata(body, serverPort)
                        for (tunnel in tunnelsFromApi) {
                            if (tunnelAddresses.none { it.publicAddress == tunnel.publicAddress }) {
                                tunnelAddresses.add(tunnel)
                                logger.info { "Tunnel from API: ${tunnel.publicAddress} (type=${tunnel.type})" }
                            }
                        }
                        if (tunnelAddresses.isNotEmpty()) {
                            _state.value = _state.value.copy(
                                status = TunnelStatus.CONNECTED,
                                tunnels = tunnelAddresses.toList()
                            )
                            break
                        }
                } catch (e: Exception) {
                    logger.warn { "API poll failed: ${e.message}" }
                }

                if (secretKey != null && provisionStartTime > 0 &&
                    System.currentTimeMillis() - provisionStartTime > 120_000 &&
                    tunnelAddresses.isEmpty() &&
                    _state.value.status == TunnelStatus.CONNECTING) {
                    val elapsedSinceSuccess = System.currentTimeMillis() - lastSuccessfulPoll
                    if (elapsedSinceSuccess > 60_000) {
                        logger.error { "Provisioning timeout (120s) and no successful API poll for 60s - stopping tunnel" }
                        secretKey = null
                        provisionStartTime = 0
                        try { process?.destroyForcibly() } catch (_: Exception) {}
                        process = null
                        _state.value = _state.value.copy(status = TunnelStatus.ERROR, error = "Provisioning timed out - no tunnels established after 120s")
                        break
                    } else {
                        logger.warn { "Provisioning taking longer than expected (120s), but API polls still succeeding - continuing to wait..." }
                    }
                }
            delay(1000)
            }
        }
    }

    fun stop() {
        logger.info { "Stopping tunnel" }
        readJob?.cancel()
        pollJob?.cancel()
        readJob = null
        pollJob = null
        exchangeJob?.cancel()
        exchangeJob = null
        try {
            process?.destroyForcibly()
            process?.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
        } catch (_: Exception) {}
        process = null
        tunnelAddresses.clear()
        outputBuffer.clear()
        provisionStartTime = 0
        _state.value = TunnelState()
    }

    fun forceStop() {
        logger.info { "Force stopping tunnel (cancel)" }
        stop()
        configFile.delete()
        secretKey = null
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
