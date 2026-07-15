package com.portalhost.db

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

private val logger = KotlinLogging.logger {}

class DatabaseDriverFactory {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun createDatabase(): DatabaseRepository? {
        return try {
            val home = System.getProperty("user.home") ?: "."
            val dbDir = File(home, ".portalhost")
            dbDir.mkdirs()
            val dbFile = File(dbDir, "portalhost.db")

            // Try multiple ways to load the SQLite JDBC driver
            try {
                Class.forName("org.sqlite.JDBC")
            } catch (e: ClassNotFoundException) {
                logger.warn { "org.sqlite.JDBC not found, trying DriverManager autoload: ${e.message}" }
            }

            val connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
            connection.createStatement().execute(PortalHostDatabase.schema)
            logger.info { "Database initialized at ${dbFile.absolutePath}" }
            DatabaseRepository(connection, json)
        } catch (e: Exception) {
            logger.error(e) { "Failed to initialize database: ${e.message}" }
            null
        }
    }
}