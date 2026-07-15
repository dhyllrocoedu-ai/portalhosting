package com.portalhost.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class RconClient(private val host: String, private val port: Int, private val password: String) {
    private var socket: Socket? = null
    private var out: DataOutputStream? = null
    private var `in`: DataInputStream? = null
    private var requestId: Int = 0

    private val SERVERDATA_AUTH = 3
    private val SERVERDATA_EXECCOMMAND = 2
    private val SERVERDATA_RESPONSE_VALUE = 0

    suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        logger.info { "Connecting RCON to $host:$port" }
        try {
            socket = Socket()
            socket!!.connect(InetSocketAddress(host, port), 5000)
            socket!!.soTimeout = 5000
            out = DataOutputStream(socket!!.getOutputStream())
            `in` = DataInputStream(socket!!.getInputStream())
            val authResult = sendPacket(SERVERDATA_AUTH, password)
            val authId = readPacketResponse()
            if (authId.first == -1) {
                logger.warn { "RCON authentication failed for $host:$port" }
                disconnect()
                Result.failure(Exception("RCON authentication failed"))
            } else {
                requestId = authId.first
                logger.info { "RCON connected to $host:$port" }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            logger.error(e) { "RCON connection failed to $host:$port" }
            disconnect()
            Result.failure(e)
        }
    }

    suspend fun command(cmd: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            sendPacket(SERVERDATA_EXECCOMMAND, cmd)
            val (id, body) = readPacketResponse()
            Result.success(body.trim())
        } catch (e: Exception) {
            logger.error(e) { "RCON command failed: $cmd" }
            Result.failure(e)
        }
    }

    fun disconnect() {
        logger.info { "Disconnecting RCON" }
        try { socket?.close() } catch (_: Exception) {}
        socket = null; out = null; `in` = null
    }

    private fun sendPacket(type: Int, body: String) {
        val bodyBytes = body.toByteArray(Charsets.UTF_8)
        val size = 10 + bodyBytes.size
        val buf = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(size - 4)
        buf.putInt(++requestId)
        buf.putInt(type)
        buf.put(bodyBytes)
        buf.put(0.toByte())
        buf.put(0.toByte())
        out?.write(buf.array())
        out?.flush()
    }

    private fun readPacketResponse(): Pair<Int, String> {
        val `in` = this.`in` ?: return 0 to ""
        val size = `in`.readInt()
        if (size < 8) return 0 to ""
        val buf = ByteArray(size - 4)
        `in`.readFully(buf)
        val bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)
        val id = bb.getInt()
        val type = bb.getInt()
        val bodyLen = buf.size - 8
        val bodyBytes = ByteArray(bodyLen)
        bb.get(bodyBytes)
        val endIdx = bodyBytes.indexOf(0.toByte())
        val body = if (endIdx >= 0) String(bodyBytes, 0, endIdx, Charsets.UTF_8) else String(bodyBytes, Charsets.UTF_8)
        return id to body
    }
}
