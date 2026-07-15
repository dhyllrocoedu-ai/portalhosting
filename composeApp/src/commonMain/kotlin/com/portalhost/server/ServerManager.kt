package com.portalhost.server

import com.portalhost.db.DatabaseRepository
import com.portalhost.java.JdkManager
import com.portalhost.model.ServerConfig
import com.portalhost.model.ServerState
import com.portalhost.model.ServerStatus
import com.portalhost.process.ManagedProcess
import com.portalhost.process.ProcessManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class ServerManager(
    private val downloader: ServerDownloader,
    private val processManager: ProcessManager,
    private val serversDir: File,
    private val scope: CoroutineScope,
    private val database: DatabaseRepository,
    private val jdkManager: JdkManager,
) {

    private val _servers = MutableStateFlow<Map<String, ServerConfig>>(emptyMap())
    val servers: StateFlow<Map<String, ServerConfig>> = _servers

    private val _serverStates = MutableStateFlow<Map<String, ServerState>>(emptyMap())
    val serverStates: StateFlow<Map<String, ServerState>> = _serverStates

    private val _consoleOutputs = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val consoleOutputs: StateFlow<Map<String, List<String>>> = _consoleOutputs

    private val activeProcesses = mutableMapOf<String, ManagedProcess>()

    init {
        loadServersFromDatabase()
    }

    private fun loadServersFromDatabase() {
        val configs = database.getAllServers()
        val states = database.getAllServerStates()
        val serversMap = configs.associateBy { it.id }
        _servers.value = serversMap
        _serverStates.value = states
    }

    suspend fun createServer(config: ServerConfig): Result<String> {
        val newConfig = config.copy(id = config.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString())
        logger.info { "Creating server: ${newConfig.name} (${newConfig.id})" }

        val downloadResult = downloader.downloadServer(newConfig)
        return downloadResult.fold(
            onSuccess = { jarFile ->
                val updatedConfig = newConfig.copy()
                database.insertServer(updatedConfig)
                _servers.update { it + (updatedConfig.id to updatedConfig) }
                val initialState = ServerState(
                    id = updatedConfig.id,
                    status = ServerStatus.STOPPED,
                )
                database.updateServerState(updatedConfig.id, initialState)
                _serverStates.update { it + (updatedConfig.id to initialState) }
                logger.info { "Server created: ${updatedConfig.name}" }
                Result.success(updatedConfig.id)
            },
            onFailure = { e ->
                logger.error(e) { "Failed to create server: ${newConfig.name}" }
                Result.failure(e)
            }
        )
    }

    fun registerImportedServer(config: ServerConfig, jarFile: java.io.File) {
        val newConfig = config.copy(id = config.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString())
        logger.info { "Registering imported server: ${newConfig.name} (${newConfig.id}) from ${jarFile.name}" }
        database.insertServer(newConfig)
        _servers.update { it + (newConfig.id to newConfig) }
        val initialState = ServerState(id = newConfig.id, status = ServerStatus.STOPPED)
        database.updateServerState(newConfig.id, initialState)
        _serverStates.update { it + (newConfig.id to initialState) }
    }

    suspend fun startServer(serverId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val config = _servers.value[serverId] ?: return@withContext Result.failure(Exception("Server not found: $serverId"))
        logger.info { "Starting server: ${config.name} ($serverId)" }

        _serverStates.update { current ->
            current + (serverId to (current[serverId]?.copy(
                status = ServerStatus.STARTING
            ) ?: ServerState(id = serverId, status = ServerStatus.STARTING)))
        }

        val jarFile = File(serversDir, "${config.id}.jar")
        if (!jarFile.exists()) {
            logger.warn { "Server JAR not found for ${config.name}: $jarFile" }
            return@withContext Result.failure(Exception("Server JAR not found"))
        }

        val processResult = processManager.startProcess(
            command = buildJavaCommand(config),
            workingDir = serversDir,
            environment = buildEnvironment(config),
        )

        val processHandle = processResult.getOrThrow()
        activeProcesses[serverId] = processHandle

        _consoleOutputs.update { it + (serverId to emptyList()) }
        scope.launch {
            processManager.getOutput(processHandle).collect { line ->
                _consoleOutputs.update { output ->
                    val current = (output[serverId] ?: emptyList()).toMutableList()
                    current.add(line)
                    output + (serverId to current)
                }
                // Persist console log to database
                database.insertConsoleLog(serverId, line)
            }
        }

        scope.launch {
            monitorProcess(serverId, processHandle)
        }

        val newState = ServerState(
            id = serverId,
            status = ServerStatus.RUNNING,
            pid = processHandle.pid
        )
        database.updateServerState(serverId, newState)
        _serverStates.update { current ->
            current + (serverId to newState)
        }
        logger.info { "Server started: ${config.name} (PID ${processHandle.pid})" }

        Result.success(Unit)
    }

    suspend fun stopServer(serverId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val config = _servers.value[serverId]
        logger.info { "Stopping server: ${config?.name ?: serverId}" }
        val process = activeProcesses.remove(serverId)
            ?: return@withContext Result.failure(Exception("No running process for $serverId"))

        _serverStates.update { current ->
            current + (serverId to (current[serverId]?.copy(
                status = ServerStatus.STOPPING
            ) ?: ServerState(id = serverId, status = ServerStatus.STOPPING)))
        }

        val stopped = processManager.stopProcess(process)
        if (!stopped) {
            processManager.killProcess(process)
        }

        val newState = ServerState(
            id = serverId,
            status = ServerStatus.STOPPED,
            pid = null
        )
        database.updateServerState(serverId, newState)
        _serverStates.update { current ->
            current + (serverId to newState)
        }
        logger.info { "Server stopped: ${config?.name ?: serverId}" }

        Result.success(Unit)
    }

    suspend fun restartServer(serverId: String): Result<Unit> {
        stopServer(serverId)
        return startServer(serverId)
    }

    suspend fun deleteServer(serverId: String): Result<Unit> {
        val config = _servers.value[serverId]
        logger.info { "Deleting server: ${config?.name ?: serverId}" }
        stopServer(serverId)
        database.deleteServer(serverId)
        _servers.update { it - serverId }
        _serverStates.update { it - serverId }
        activeProcesses.remove(serverId)
        return Result.success(Unit)
    }

    private fun buildJavaCommand(config: ServerConfig): List<String> {
        val javaExe = jdkManager.getJavaExecutable(config.javaVersion)
            ?: return listOf(
                "java",
                "-Xms${config.memoryMin}M",
                "-Xmx${config.memoryMax}M",
                "-jar", "${config.id}.jar",
                "nogui"
            )
        return listOf(
            javaExe.absolutePath,
            "-Xms${config.memoryMin}M",
            "-Xmx${config.memoryMax}M",
            "-jar", "${config.id}.jar",
            "nogui"
        )
    }

    fun getServerJar(serverId: String): File {
        return File(serversDir, "${serverId}.jar")
    }

    fun getProcessForServer(serverId: String): java.lang.Process? {
        val handle = activeProcesses[serverId]
        return handle?.let { processManager.getProcessHandle(it) }
    }

    private fun buildEnvironment(config: ServerConfig): Map<String, String> {
        return mapOf(
            "EULA" to "TRUE"
        )
    }

    private fun monitorProcess(serverId: String, handle: ManagedProcess) {
        scope.launch(Dispatchers.IO) {
            while (processManager.isRunning(handle)) {
                delay(1000)
            }
            val exitCode = processManager.getExitCode(handle)
            logger.info { "Server $serverId exited with code $exitCode" }

            val newStatus = if (exitCode == 0) ServerStatus.STOPPED else ServerStatus.CRASHED
            val newState = ServerState(id = serverId, status = newStatus, pid = null)
            database.updateServerState(serverId, newState)
            _serverStates.update { current ->
                current + (serverId to newState)
            }

            activeProcesses.remove(serverId)

            val config = _servers.value[serverId]
            if (config?.autoRestart == true && exitCode != 0) {
                logger.info { "Auto-restarting server $serverId after crash" }
                startServer(serverId)
            }
        }
    }
}