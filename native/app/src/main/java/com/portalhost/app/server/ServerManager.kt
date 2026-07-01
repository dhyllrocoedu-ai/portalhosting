package com.portalhost.app.server

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import android.os.Build
import android.util.Log
import com.portalhost.app.activity.ActivityLog
import com.portalhost.app.ui.model.ServerConfig
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TAG = "ServerManager"
private const val MAX_RESTART_RETRIES = 2

enum class ServerStatus {
    OFFLINE, STARTING, ONLINE, STOPPING, CRASHED
}

data class ServerState(
    val status: ServerStatus = ServerStatus.OFFLINE,
    val uptimeSeconds: Long = 0,
    val players: List<String> = emptyList(),
    val exitCode: Int? = null,
    val error: String? = null
)

class ServerManager(
    private val javaRuntimeManager: JavaRuntimeManager,
    private val consoleStreamer: ConsoleStreamer,
    private val activityLog: ActivityLog = ActivityLog(),
    private val processMonitor: ProcessMonitor = ProcessMonitor()
) {
    private var process: Process? = null
    private var processJob: Job? = null
    private var uptimeJob: Job? = null
    private var serverStartTime: Long = 0

    private var lastJarPath: String? = null
    private var lastJavaArgs: List<String>? = null
    private var lastServerDir: String? = null
    private var restartCount = 0
    private var sawHashFailure = false

    private val _state = MutableStateFlow(ServerState())
    val state: StateFlow<ServerState> = _state.asStateFlow()

    private val _processStats = MutableStateFlow(ProcessStats())
    val processStats: StateFlow<ProcessStats> = _processStats.asStateFlow()

    private val _consoleLines = MutableSharedFlow<String>(extraBufferCapacity = 512)
    val consoleLines: SharedFlow<String> = _consoleLines.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var statsJob: Job? = null

    val isRunning: Boolean get() = process?.isAlive == true

    /** Create essential server files and directories. */
    private fun initServerDir(workDir: File, config: ServerConfig? = null) {
        val port = config?.port ?: 25565
        val gamemode = config?.gamemode ?: "survival"
        val difficulty = config?.difficulty ?: "easy"
        val motd = config?.motd ?: "A Minecraft Server"
        // Create standard directories
        for (dir in listOf("logs", "world", "plugins", "mods", "worlds")) {
            File(workDir, dir).mkdirs()
        }

        // Ensure eula.txt
        val eula = File(workDir, "eula.txt")
        if (!eula.exists()) {
            eula.writeText("eula=true\n")
        } else {
            val current = eula.readText().trim()
            if ("eula=false" in current) {
                eula.writeText(current.replace("eula=false", "eula=true"))
            }
        }

        // Generate server.properties if missing
        val props = File(workDir, "server.properties")
        if (!props.exists()) {
            props.writeText("""
#Minecraft server properties
motd=$motd
server-port=$port
gamemode=$gamemode
difficulty=$difficulty
max-players=20
online-mode=true
allow-nether=true
spawn-animals=true
spawn-monsters=true
pvp=true
view-distance=10
generator-settings=
level-name=world
level-seed=
enable-command-block=false
allow-flight=false
white-list=false
enforce-whitelist=false
resource-pack=
resource-pack-sha1=
op-permission-level=4
player-idle-timeout=0
max-world-size=29999984
network-compression-threshold=256
max-tick-time=60000
rate-limit=0
hardcore=false
spawn-protection=16
force-gamemode=false
broadcast-console-to-ops=true
broadcast-rcon-to-ops=true
enable-rcon=false
rcon.password=
rcon.port=25575
enable-query=false
query.port=25565
max-chained-neighbor-updates=100
sync-chunk-writes=true
enable-jmx-monitoring=false
enable-status=true
enforce-secure-profile=false
hide-online-players=false
initial-enabled-packs=vanilla
initial-disabled-packs=
log-ips=true
max-chat-prompted=10
pause-when-empty-seconds=60
previews-chat=false
simulation-distance=10
text-filtering-config=
use-native-transport=true
""".trimStart())
        }
    }

    /** Start the Minecraft server. */
    suspend fun start(
        jarPath: String,
        javaArgs: List<String> = listOf("-Xms512M", "-Xmx2G"),
        serverDir: String = File(jarPath).parent ?: ".",
        config: ServerConfig? = null
    ): Result<Unit> {
        if (isRunning) return Result.failure(Exception("Server already running"))
        restartCount = 0

        return try {
            _state.value = _state.value.copy(status = ServerStatus.STARTING, error = null)

            val javaPath = javaRuntimeManager.resolveJavaPath()
            Log.i(TAG, "Starting server: jar=$jarPath java=$javaPath dir=$serverDir")
            val workDir = File(serverDir).also { it.mkdirs() }
            val jarFile = File(jarPath)

            if (!jarFile.exists()) {
                val msg = "Server jar not found: $jarPath"
                Log.e(TAG, msg)
                _state.value = _state.value.copy(status = ServerStatus.OFFLINE, error = msg)
                return Result.failure(Exception(msg))
            }
            if (!File(javaPath).exists()) {
                val msg = "Java not found at: $javaPath — install JDK first"
                Log.e(TAG, msg)
                _state.value = _state.value.copy(status = ServerStatus.OFFLINE, error = msg)
                return Result.failure(Exception(msg))
            }

            // Save args for restart
            lastJarPath = jarPath
            lastJavaArgs = javaArgs
            lastServerDir = serverDir

            // Ensure system library shims exist before starting JVM
            javaRuntimeManager.fixupLibraries()

            // Initialize server directory (eula, properties, subdirs)
            initServerDir(workDir, config)

            val javaDir = File(javaPath).parentFile ?: File(serverDir)
            val jdkHome = javaDir.parentFile ?: File(serverDir)
            val libDir = File(jdkHome, "lib")

            // Android 10+ blocks exec from /data/data/ (noexec mount).
            // Use /system/bin/linker64 to bypass the restriction.
            val is64Bit = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
            val linker = if (is64Bit) "/system/bin/linker64" else "/system/bin/linker"

            val env = mapOf("LD_LIBRARY_PATH" to "${libDir.absolutePath}:${libDir.absolutePath}/server:${libDir.absolutePath}/jli")
            val cmd = listOf(linker, javaPath, javaPath) + javaArgs + listOf("-jar", jarFile.name, "nogui")
            val proc = ProcessBuilder(cmd)
                .directory(workDir)
                .redirectErrorStream(true)
                .also { pb -> env.forEach { (k, v) -> pb.environment()[k] = v } }
                .start()

            process = proc
            serverStartTime = System.currentTimeMillis()
            activityLog.addServerStart()

            // Stream console output
            processJob = scope.launch {
                try {
                    proc.inputStream.bufferedReader().use { reader ->
                        var line: String?
                        var lineCount = 0
                        while (reader.readLine().also { line = it } != null) {
                            val text = line!!
                            if (lineCount < 50) {
                                Log.i(TAG, "OUT: $text")
                            }
                            lineCount++
                            _consoleLines.emit(text)
                            if (text.contains("Hash check failed")) {
                                sawHashFailure = true
                            }
                            parsePlayerEvents(text)
                            // Parse TPS from console output
                            processMonitor.parseTps(text)?.let { tps ->
                                _processStats.value = _processStats.value.copy(tps = tps)
                            }
                        }
                    }
                } catch (_: IOException) {
                    // Process terminated, stream closed
                } finally {
                    val code = proc.exitValue()
                    Log.i(TAG, "Process exited with code $code")
                    if (code != 0) {
                        activityLog.addServerCrash()
                    } else {
                        activityLog.addServerStop()
                    }
                    _state.value = _state.value.copy(
                        status = ServerStatus.OFFLINE,
                        exitCode = code,
                        uptimeSeconds = (System.currentTimeMillis() - serverStartTime) / 1000
                    )
                    process = null
                    // Auto-retry on Paperclip hash failure
                    if (sawHashFailure && restartCount < MAX_RESTART_RETRIES) {
                        sawHashFailure = false
                        restartCount++
                        Log.i(TAG, "Hash failure detected, auto-restart attempt $restartCount/$MAX_RESTART_RETRIES")
                        delay(3000)
                        lastJarPath?.let { jar ->
                            lastJavaArgs?.let { args ->
                                start(jar, args, lastServerDir ?: File(jar).parent)
                            }
                        }
                    }
                }
            }

            // Uptime counter
            uptimeJob = scope.launch {
                while (isActive && proc.isAlive) {
                    delay(1000)
                    val elapsed = (System.currentTimeMillis() - serverStartTime) / 1000
                    _state.value = _state.value.copy(uptimeSeconds = elapsed)
                }
            }

            // Process stats polling (CPU, RAM)
            statsJob = scope.launch {
                while (isActive && proc.isAlive) {
                    val maxRam = lastJavaArgs?.let { args ->
                        args.find { it.startsWith("-Xmx") }?.drop(4)?.let {
                            when {
                                it.endsWith("G") -> (it.dropLast(1).toFloatOrNull()?.times(1000) ?: 2048f).toInt()
                                it.endsWith("M") -> it.dropLast(1).toIntOrNull() ?: 2048
                                else -> 2048
                            }
                        }
                    } ?: 2048
                    _processStats.value = processMonitor.getStats(proc, maxRam)
                    delay(3000)
                }
            }

            // Mark online after a brief delay (server fully started)
            scope.launch {
                delay(5000)
                if (proc.isAlive) {
                    _state.value = _state.value.copy(status = ServerStatus.ONLINE)
                }
            }

            Log.i(TAG, "Server process started successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            val msg = "Server start failed: ${e.message}"
            Log.e(TAG, msg, e)
            _state.value = _state.value.copy(status = ServerStatus.CRASHED, error = msg)
            process = null
            Result.failure(e)
        }
    }

    /** Gracefully stop the server. */
    suspend fun stop() {
        val proc = process ?: return
        _state.value = _state.value.copy(status = ServerStatus.STOPPING)

        withContext(Dispatchers.IO) {
            try {
                // Send "stop" command via stdin
                proc.outputStream.write("stop\n".toByteArray())
                proc.outputStream.flush()

                // Wait up to 10 seconds for graceful shutdown
                if (!proc.waitFor(10, TimeUnit.SECONDS)) {
                    proc.destroyForcibly()
                }
            } catch (_: Exception) {
                proc.destroyForcibly()
            }
        }
        activityLog.addServerStop()
    }

    /** Restart the server with the same args. */
    suspend fun restart(): Result<Unit> {
        val jarPath = lastJarPath ?: return Result.failure(Exception("No previous server to restart"))
        val javaArgs = lastJavaArgs ?: listOf("-Xms512M", "-Xmx2G")
        val serverDir = lastServerDir ?: File(jarPath).parent ?: "."
        stop()
        delay(2000)
        return start(jarPath, javaArgs, serverDir)
    }

    /** Force-kill the server. */
    fun kill() {
        process?.destroyForcibly()
        process = null
        _state.value = _state.value.copy(status = ServerStatus.OFFLINE)
    }

    /** Write a command to the server console (stdin). */
    fun writeCommand(command: String) {
        val proc = process ?: return
        try {
            proc.outputStream.write("$command\n".toByteArray())
            proc.outputStream.flush()
        } catch (_: IOException) {}
    }

    /** Parse Minecraft log lines for player join/leave events. */
    private fun parsePlayerEvents(line: String) {
        val joinRegex = Regex("""(\w+)\s+joined the game""")
        val leaveRegex = Regex("""(\w+)\s+left the game""")

        joinRegex.find(line)?.let { match ->
            val name = match.groupValues[1]
            val current = _state.value.players.toMutableList()
            if (name !in current) {
                current.add(name)
                _state.value = _state.value.copy(players = current)
                activityLog.addPlayerJoin(name)
            }
        }

        leaveRegex.find(line)?.let { match ->
            val name = match.groupValues[1]
            _state.value = _state.value.copy(
                players = _state.value.players - name
            )
            activityLog.addPlayerLeave(name)
        }
    }

    fun resolveJavaPath(): String = javaRuntimeManager.resolveJavaPath()

    fun destroy() {
        kill()
        scope.cancel()
    }
}
