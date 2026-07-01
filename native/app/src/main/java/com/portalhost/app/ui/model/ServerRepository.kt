package com.portalhost.app.ui.model

import android.content.Context
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class ServerRepository(private val context: Context) {
    private val TAG = "ServerRepository"
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val configDir: File get() = File(context.filesDir, "servers")
    private val indexFile: File get() = File(context.filesDir, "servers_index.json")

    private var servers: MutableList<ServerConfig> = mutableListOf()

    init {
        load()
    }

    fun list(): List<ServerConfig> = servers.toList()

    fun getById(id: String): ServerConfig? = servers.find { it.id == id }

    fun add(config: ServerConfig): ServerConfig {
        val server = config.copy(id = UUID.randomUUID().toString().take(8))
        getServerDir(server.id).mkdirs()
        servers.add(server)
        save()
        return server
    }

    fun update(config: ServerConfig) {
        val idx = servers.indexOfFirst { it.id == config.id }
        if (idx >= 0) {
            servers[idx] = config
            save()
        }
    }

    fun remove(id: String) {
        servers.removeAll { it.id == id }
        save()
        val dir = getServerDir(id)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }

    fun clear() {
        servers.forEach { server ->
            val dir = getServerDir(server.id)
            if (dir.exists()) dir.deleteRecursively()
        }
        servers.clear()
        save()
    }

    private fun load() {
        if (!indexFile.exists()) {
            servers = mutableListOf()
            return
        }
        try {
            val text = indexFile.readText()
            servers = json.decodeFromString<List<ServerConfig>>(text).toMutableList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load servers: ${e.message}")
            servers = mutableListOf()
        }
    }

    private fun save() {
        try {
            indexFile.parentFile?.mkdirs()
            indexFile.writeText(json.encodeToString(servers))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save servers: ${e.message}")
        }
    }

    fun getServerDir(serverId: String): File = File(configDir, serverId)
}
