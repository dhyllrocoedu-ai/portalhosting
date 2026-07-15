package com.portalhost.db

import com.portalhost.model.BackupEntry
import com.portalhost.model.BackupType
import com.portalhost.model.ServerConfig
import com.portalhost.model.ServerState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.sql.Connection
import java.sql.ResultSet
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class DatabaseRepository(
    private val connection: Connection,
    private val json: Json,
) {
    fun getAllServers(): List<ServerConfig> {
        val stmt = connection.createStatement()
        val rs = stmt.executeQuery("SELECT config_json FROM servers ORDER BY created_at DESC")
        val results = mutableListOf<ServerConfig>()
        while (rs.next()) {
            val config = json.decodeFromString<ServerConfig>(rs.getString("config_json"))
            results.add(config)
        }
        rs.close()
        stmt.close()
        return results
    }

    fun getServer(id: String): ServerConfig? {
        val stmt = connection.prepareStatement("SELECT config_json FROM servers WHERE id = ?")
        stmt.setString(1, id)
        val rs = stmt.executeQuery()
        val result = if (rs.next()) {
            json.decodeFromString<ServerConfig>(rs.getString("config_json"))
        } else null
        rs.close()
        stmt.close()
        return result
    }

    fun insertServer(server: ServerConfig) {
        try {
            val stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO servers (id, config_json, created_at, updated_at) VALUES (?, ?, ?, ?)"
            )
            stmt.setString(1, server.id)
            stmt.setString(2, json.encodeToString(server))
            stmt.setLong(3, server.createdAt)
            stmt.setLong(4, System.currentTimeMillis())
            stmt.executeUpdate()
            stmt.close()
            logger.debug { "Server inserted/updated: ${server.name} (${server.id})" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to insert server ${server.name}" }
        }
    }

    fun updateServer(server: ServerConfig) {
        insertServer(server)
    }

    fun deleteServer(id: String) {
        logger.debug { "Deleting server $id from database" }
        val stmt = connection.prepareStatement("DELETE FROM servers WHERE id = ?")
        stmt.setString(1, id)
        stmt.executeUpdate()
        stmt.close()
    }

    fun updateServerState(id: String, state: ServerState) {
        val stmt = connection.prepareStatement(
            "UPDATE servers SET state_json = ?, updated_at = ? WHERE id = ?"
        )
        stmt.setString(1, json.encodeToString(state))
        stmt.setLong(2, System.currentTimeMillis())
        stmt.setString(3, id)
        stmt.executeUpdate()
        stmt.close()
    }

    fun getAllServerStates(): Map<String, ServerState> {
        val stmt = connection.createStatement()
        val rs = stmt.executeQuery("SELECT id, state_json FROM servers WHERE state_json IS NOT NULL")
        val results = mutableMapOf<String, ServerState>()
        while (rs.next()) {
            val state = json.decodeFromString<ServerState>(rs.getString("state_json"))
            results[rs.getString("id")] = state
        }
        rs.close()
        stmt.close()
        return results
    }

    fun insertConsoleLog(serverId: String, message: String) {
        val stmt = connection.prepareStatement(
            "INSERT INTO console_logs (server_id, timestamp, level, message) VALUES (?, ?, ?, ?)"
        )
        stmt.setString(1, serverId)
        stmt.setLong(2, System.currentTimeMillis())
        stmt.setString(3, "INFO")
        stmt.setString(4, message)
        stmt.executeUpdate()
        stmt.close()
    }

    fun getConsoleLogs(serverId: String, limit: Int = 500): List<String> {
        val stmt = connection.prepareStatement(
            "SELECT message FROM console_logs WHERE server_id = ? ORDER BY timestamp ASC LIMIT ?"
        )
        stmt.setString(1, serverId)
        stmt.setInt(2, limit)
        val rs = stmt.executeQuery()
        val results = mutableListOf<String>()
        while (rs.next()) {
            results.add(rs.getString("message"))
        }
        rs.close()
        stmt.close()
        return results
    }

    // Backup CRUD
    fun insertBackup(backup: BackupEntry) {
        val stmt = connection.prepareStatement(
            "INSERT OR REPLACE INTO backups (id, server_id, path, size, type, created_at) VALUES (?, ?, ?, ?, ?, ?)"
        )
        stmt.setString(1, backup.id)
        stmt.setString(2, backup.serverId)
        stmt.setString(3, backup.path)
        stmt.setLong(4, backup.size)
        stmt.setString(5, backup.type.name)
        stmt.setLong(6, backup.createdAt)
        stmt.executeUpdate()
        stmt.close()
    }

    fun getBackups(serverId: String): List<BackupEntry> {
        val stmt = connection.prepareStatement(
            "SELECT id, server_id, path, size, type, created_at FROM backups WHERE server_id = ? ORDER BY created_at DESC"
        )
        stmt.setString(1, serverId)
        val rs = stmt.executeQuery()
        val results = mutableListOf<BackupEntry>()
        while (rs.next()) {
            val type = BackupType.valueOf(rs.getString("type"))
            results.add(BackupEntry(
                id = rs.getString("id"),
                serverId = rs.getString("server_id"),
                path = rs.getString("path"),
                size = rs.getLong("size"),
                type = type,
                createdAt = rs.getLong("created_at")
            ))
        }
        rs.close()
        stmt.close()
        return results
    }

    fun deleteBackup(id: String) {
        val stmt = connection.prepareStatement("DELETE FROM backups WHERE id = ?")
        stmt.setString(1, id)
        stmt.executeUpdate()
        stmt.close()
    }

    fun close() {
        connection.close()
    }
}