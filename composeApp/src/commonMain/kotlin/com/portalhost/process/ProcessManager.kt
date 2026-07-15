package com.portalhost.process

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class ProcessManager {
    private val processes = ConcurrentHashMap<Int, java.lang.Process>()
    private val outputChannels = ConcurrentHashMap<Int, Channel<String>>()
    private var nextPid = 1

    suspend fun startProcess(
        command: List<String>,
        workingDir: File,
        environment: Map<String, String>
    ): Result<ManagedProcess> = withContext(Dispatchers.IO) {
        logger.info { "Starting process: ${command.first()} (${command.size - 1} args)" }
        try {
            val builder = ProcessBuilder(command)
                .directory(workingDir)
                .redirectErrorStream(true)

            environment.forEach { (key, value) ->
                builder.environment()[key] = value
            }

            val process = builder.start()
            val pid = nextPid++
            val osPid = getOsPid(process)
            val handle = ManagedProcess(
                pid = pid,
                command = command,
                workingDir = workingDir,
                osPid = osPid
            )

            processes[pid] = process
            logger.info { "Process started: PID=$pid OS_PID=$osPid" }

            val channel = Channel<String>(Channel.UNLIMITED)
            outputChannels[pid] = channel

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        channel.send(line!!)
                    }
                } catch (_: Exception) {
                } finally {
                    channel.close()
                }
            }

            Result.success(handle)
        } catch (e: Exception) {
            logger.error(e) { "Failed to start process" }
            Result.failure(e)
        }
    }

    private fun getOsPid(process: java.lang.Process): Long {
        return try {
            // Try to get the actual OS PID via reflection
            val pidField = process::class.java.getDeclaredField("pid")
            pidField.isAccessible = true
            pidField.getLong(process)
        } catch (_: Exception) {
            process.pid()
        }
    }

    suspend fun stopProcess(handle: ManagedProcess): Boolean = withContext(Dispatchers.IO) {
        val process = processes[handle.pid] ?: return@withContext false
        logger.info { "Stopping process PID=${handle.pid}" }
        process.destroy()
        try {
            process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun killProcess(handle: ManagedProcess): Boolean = withContext(Dispatchers.IO) {
        val process = processes[handle.pid] ?: return@withContext false
        logger.warn { "Killing process PID=${handle.pid}" }
        process.destroyForcibly()
        try {
            process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getOutput(handle: ManagedProcess): Flow<String> = channelFlow {
        val channel = outputChannels[handle.pid] ?: return@channelFlow
        for (line in channel) {
            send(line)
        }
    }

    fun getOutputChannel(handle: ManagedProcess): ReceiveChannel<String> {
        return outputChannels[handle.pid] ?: Channel<String>(0).also { it.close() }
    }

    fun isRunning(handle: ManagedProcess): Boolean {
        val process = processes[handle.pid] ?: return false
        return process.isAlive
    }

    fun getExitCode(handle: ManagedProcess): Int? {
        val process = processes[handle.pid] ?: return null
        return if (process.isAlive) null else process.exitValue()
    }

    fun getPid(handle: ManagedProcess): Int = handle.pid

    fun getOsPid(handle: ManagedProcess): Long = handle.osPid

    fun getProcessHandle(handle: ManagedProcess): java.lang.Process? = processes[handle.pid]
}