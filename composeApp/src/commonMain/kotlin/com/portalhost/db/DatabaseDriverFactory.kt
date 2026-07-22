package com.portalhost.db

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

private val logger = KotlinLogging.logger {}

class DatabaseDriverFactory(private val customDataDir: String? = null) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun createDatabase(): DatabaseRepository? {
        return try {
            val dataDir = if (!customDataDir.isNullOrBlank()) {
                File(customDataDir)
            } else {
                val home = System.getProperty("user.home") ?: "."
                File(home, ".portalhost")
            }
            dataDir.mkdirs()
            val dbFile = File(dataDir, "portalhost.db")

            // Try multiple ways to load the SQLite JDBC driver
            try {
                Class.forName("org.sqlite.JDBC")
            } catch (e: ClassNotFoundException) {
                logger.warn { "org.sqlite.JDBC not found, trying DriverManager autoload: ${e.message}" }
            }

            val connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
            val statement = connection.createStatement()

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS schema_version (version INTEGER PRIMARY KEY NOT NULL)")
            val rs = statement.executeQuery("SELECT version FROM schema_version ORDER BY version DESC LIMIT 1")
            val currentVersion = if (rs.next()) rs.getInt(1) else 0
            rs.close()

            if (currentVersion == 0) {
                PortalHostDatabase.schema.split(";").map { it.trim() }.filter { it.isNotBlank() }.forEach { sql ->
                    statement.executeUpdate(sql)
                }
                statement.executeUpdate("INSERT OR REPLACE INTO schema_version (version) VALUES (${PortalHostDatabase.CURRENT_VERSION})")
            } else if (currentVersion < PortalHostDatabase.CURRENT_VERSION) {
                for (v in (currentVersion + 1)..PortalHostDatabase.CURRENT_VERSION) {
                    PortalHostDatabase.migrations[v]?.forEach { sql ->
                        statement.executeUpdate(sql)
                    }
                    statement.executeUpdate("INSERT OR REPLACE INTO schema_version (version) VALUES ($v)")
                }
            }

            statement.close()
            logger.info { "Database initialized at ${dbFile.absolutePath}" }
            DatabaseRepository(connection, json)
        } catch (e: Exception) {
            logger.error(e) { "Failed to initialize database: ${e.message}" }
            null
        }
    }
}