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
private const val RENICE_SERVER = 10

enum class ServerStatus {
    OFFLINE, STARTING, ONLINE, STOPPING, STOPPED, CRASHED
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
    private var stoppedJob: Job? = null
    private var serverStartTime: Long = 0
    private val pendingCommands = java.util.concurrent.ConcurrentLinkedQueue<String>()

    private var lastJarPath: String? = null
    private var lastJavaArgs: List<String>? = null
    var effectiveJavaArgs: List<String>? = null
    private var lastServerDir: String? = null
    private var restartCount = 0
    private var sawHashFailure = false
    private var autoRestartEnabled = false
    private var stoppingManually = false

    private val _state = MutableStateFlow(ServerState())
    val state: StateFlow<ServerState> = _state.asStateFlow()

    private val _processStats = MutableStateFlow(ProcessStats())
    val processStats: StateFlow<ProcessStats> = _processStats.asStateFlow()

    private val _consoleLines = MutableSharedFlow<String>(extraBufferCapacity = 512)
    val consoleLines: SharedFlow<String> = _consoleLines.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var statsJob: Job? = null

    val isRunning: Boolean get() = process?.isAlive == true

    /** Safe check that guards against [IllegalThreadStateException] on some Android runtimes. */
    private fun isAlive(proc: java.lang.Process): Boolean = try {
        proc.isAlive
    } catch (_: IllegalThreadStateException) {
        false
    }

    private fun getPid(proc: java.lang.Process): Int {
        return try {
            // Android's ProcessImpl exposes PID via reflection
            val f = proc.javaClass.getDeclaredField("pid")
            f.isAccessible = true
            f.getInt(proc)
        } catch (_: Exception) { -1 }
    }

    private fun renice(pid: Int, priority: Int) {
        if (pid <= 0) return
        try {
            Runtime.getRuntime().exec(arrayOf("renice", "-n", priority.toString(), "-p", pid.toString()))
            Log.i(TAG, "reniced pid $pid to $priority")
        } catch (e: Exception) {
            Log.w(TAG, "renice failed for pid $pid: ${e.message}")
        }
    }

    private var startingLock = false

    /** Set STOPPED state and schedule transition to OFFLINE after 3 seconds. */
    private fun scheduleOfflineTransition(exitCode: Int) {
        _state.value = _state.value.copy(
            status = ServerStatus.STOPPED,
            exitCode = exitCode,
            uptimeSeconds = (System.currentTimeMillis() - serverStartTime) / 1000
        )
        if (exitCode != 0) {
            activityLog.addServerCrash()
        } else {
            activityLog.addServerStop()
        }
        stoppedJob?.cancel()
        stoppedJob = scope.launch {
            delay(3000)
            if (process == null && _state.value.status == ServerStatus.STOPPED) {
                _state.value = _state.value.copy(status = ServerStatus.OFFLINE)
            }
        }
    }

    /** Create essential server files and directories. */
    private suspend fun initServerDir(workDir: File, config: ServerConfig? = null) = withContext(Dispatchers.IO) {
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
            val escapedMotd = motd.replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("§", "\\u00A7")
            props.writeText("""
#Minecraft server properties
motd=$escapedMotd
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
        if (startingLock) return Result.failure(Exception("Server already starting"))
        startingLock = true
        restartCount = 0
        stoppedJob?.cancel()
        processJob?.cancel()
        processJob = null

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
                startingLock = false
                return Result.failure(Exception(msg))
            }
            if (!File(javaPath).exists()) {
                val msg = "Java not found at: $javaPath — install JDK first"
                Log.e(TAG, msg)
                _state.value = _state.value.copy(status = ServerStatus.OFFLINE, error = msg)
                startingLock = false
                return Result.failure(Exception(msg))
            }

            // Save args for restart
            lastJarPath = jarPath
            lastJavaArgs = javaArgs
            effectiveJavaArgs = javaArgs
            lastServerDir = serverDir
            autoRestartEnabled = config?.autoRestart ?: false

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
            processMonitor.resetNetworkStats()
            serverStartTime = System.currentTimeMillis()
            activityLog.addServerStart()
            renice(getPid(proc), RENICE_SERVER)

            // Drain pending commands
            while (true) {
                val cmd = pendingCommands.poll() ?: break
                try {
                    proc.outputStream.write("$cmd\n".toByteArray())
                    proc.outputStream.flush()
                } catch (_: IOException) { break }
            }

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
                            // Detect server fully started via "Done" message
                            if (_state.value.status == ServerStatus.STARTING &&
                                (Regex("Done \\([\\d.]+s\\)").containsMatchIn(text) || text.contains("For help"))
                            ) {
                                _state.value = _state.value.copy(status = ServerStatus.ONLINE)
                            }
                        }
                        Log.i(TAG, "processJob: read loop ended normally (EOF)")
                    }
                } catch (e: IOException) {
                    Log.i(TAG, "processJob: read loop ended via IOException: ${e.message}")
                } catch (e: Exception) {
                    Log.w(TAG, "processJob: read loop ended via unexpected exception: ${e.message}", e)
                } finally {
                    val code = try {
                        proc.exitValue()
                    } catch (_: IllegalThreadStateException) {
                        if (!proc.waitFor(10, TimeUnit.SECONDS)) {
                            proc.destroyForcibly()
                            proc.waitFor()
                        }
                        try {
                            proc.exitValue()
                        } catch (_: IllegalThreadStateException) {
                            -1
                        }
                    }
                    Log.i(TAG, "Process exited with code $code")
                    // Only clean up if we're still the current process (avoid race with stop+restart)
                    if (process === proc) {
                        scheduleOfflineTransition(code)
                        process = null
                        if (code != 0) {
                            saveCrashLog(workDir, code)
                        }
                    }

                    // Auto-restart if enabled and within retry limit
                    val shouldRestart = (autoRestartEnabled || sawHashFailure) && restartCount < MAX_RESTART_RETRIES
                    if (shouldRestart) {
                        sawHashFailure = false
                        restartCount++
                        Log.i(TAG, "Auto-restart attempt $restartCount/$MAX_RESTART_RETRIES")
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
                while (isActive) {
                    delay(1000)
                    if (!isAlive(proc)) break
                    val elapsed = (System.currentTimeMillis() - serverStartTime) / 1000
                    _state.value = _state.value.copy(uptimeSeconds = elapsed)
                }
            }

            // Process stats polling (CPU, RAM)
            statsJob = scope.launch {
                while (isActive) {
                    if (!isAlive(proc)) break
                    val maxRam = lastJavaArgs?.let { args ->
                        args.find { it.startsWith("-Xmx") }?.drop(4)?.let {
                            when {
                                it.endsWith("G") -> (it.dropLast(1).toFloatOrNull()?.times(1024) ?: 2048f).toInt()
                                it.endsWith("M") -> it.dropLast(1).toIntOrNull() ?: 2048
                                else -> 2048
                            }
                        }
                    } ?: 2048
                    _processStats.value = processMonitor.getStats(proc, maxRam)
                    delay(3000)
                }
            }

            Log.i(TAG, "Server process started successfully")
            startingLock = false
            Result.success(Unit)
        } catch (e: Exception) {
            val msg = "Server start failed: ${e.message}"
            Log.e(TAG, msg, e)
            _state.value = _state.value.copy(status = ServerStatus.CRASHED, error = msg)
            process = null
            startingLock = false
            Result.failure(e)
        }
    }

    /** Gracefully stop the server. */
    suspend fun stop() {
        val proc = process ?: run {
            Log.w(TAG, "stop() called but process is null")
            return
        }
        Log.i(TAG, "stop() called — capturing caller stack trace", Exception("caller trace"))
        autoRestartEnabled = false
        sawHashFailure = false
        _state.value = _state.value.copy(status = ServerStatus.STOPPING)

        // Cancel polling jobs *before* waiting so they don't race with exit
        uptimeJob?.cancel()
        statsJob?.cancel()
        // Set process null before cancelling processJob so its finally block
        // sees process !== proc and skips state transition
        process = null
        processJob?.cancel()
        processJob = null

        withContext(Dispatchers.IO) {
            try {
                // Send "stop" command via stdin
                Log.i(TAG, "stop(): writing 'stop\\n' to stdin")
                proc.outputStream.write("stop\n".toByteArray())
                proc.outputStream.flush()
                Log.i(TAG, "stop(): waiting up to 10s for process to exit")
                // Wait up to 10 seconds for graceful shutdown
                if (!proc.waitFor(10, TimeUnit.SECONDS)) {
                    Log.w(TAG, "stop(): timeout — force killing")
                    proc.destroyForcibly()
                } else {
                    Log.i(TAG, "stop(): process exited gracefully")
                }
            } catch (e: Exception) {
                Log.w(TAG, "stop(): exception, force killing: ${e.message}")
                proc.destroyForcibly()
            }
        }
        scheduleOfflineTransition(0)
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
        stoppedJob?.cancel()
        process?.destroyForcibly()
        process = null
        _state.value = _state.value.copy(status = ServerStatus.OFFLINE)
    }

    /** Write a command to the server console (stdin). */
    fun writeCommand(command: String) {
        val proc = process
        if (proc == null) {
            pendingCommands.add(command)
            return
        }
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

    private fun saveCrashLog(workDir: File, exitCode: Int) {
        try {
            val logDir = File(workDir, "logs").also { it.mkdirs() }
            val crashFile = File(logDir, "crash_${System.currentTimeMillis()}.log")
            val lines = consoleStreamer.lines
            val content = buildString {
                appendLine("=== Crash Report ===")
                appendLine("Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}")
                appendLine("Exit code: $exitCode")
                appendLine("Uptime: ${_state.value.uptimeSeconds}s")
                appendLine("Players: ${_state.value.players.joinToString(", ")}")
                appendLine("=== Last ${lines.size} console lines ===")
                lines.forEach { appendLine(it) }
            }
            crashFile.writeText(content)
            Log.i(TAG, "Crash log saved: ${crashFile.absolutePath}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save crash log: ${e.message}")
        }
    }

    fun resolveJavaPath(): String = javaRuntimeManager.resolveJavaPath()

    /**
     * Called when activity is destroyed (user switches apps).
     * Does NOT kill the process — the foreground service keeps everything running.
     * Scope is kept alive so polling jobs continue and notification stays fresh.
     * Full cleanup happens in dispose() when the service stops.
     */
    fun destroy() {
        // No-op — process, scope, and polling all survive activity destruction.
    }

    /** Full cleanup — call from Service.onDestroy(), not Activity.onDestroy(). */
    fun dispose() {
        kill()
        scope.cancel()
    }
}
