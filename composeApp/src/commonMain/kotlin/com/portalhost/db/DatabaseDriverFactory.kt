package com.portalhost.db

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.portalhost.filesystem.defaultDataDir
import mu.KotlinLogging
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

private val logger = KotlinLogging.logger {}

class DatabaseDriverFactory(private val customDataDir: String? = null) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun createDatabase(): DatabaseRepository? {
        val dataDir = if (!customDataDir.isNullOrBlank()) {
            File(customDataDir)
        } else {
            System.getProperty("portalhost.data.dir")?.takeIf { it.isNotBlank() }?.let { File(it) }
                ?: defaultDataDir()
        }
        dataDir.mkdirs()
        val dbFile = File(dataDir, "portalhost.db")

        // Try multiple ways to load the SQLite JDBC driver
        try {
            Class.forName("org.sqlite.JDBC")
        } catch (e: ClassNotFoundException) {
            logger.warn { "org.sqlite.JDBC not found, trying DriverManager autoload: ${e.message}" }
        }

        // Retry a few times so a custom data directory that is still mounting
        // (external drive, OneDrive, network share) right after a machine
        // restart can recover instead of taking the whole app down.
        var lastError: Exception? = null
        repeat(3) { attempt ->
            try {
                val connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
                try {
                    connection.createStatement().use { st ->
                        st.execute("PRAGMA journal_mode=WAL")
                        st.execute("PRAGMA busy_timeout=5000")
                    }
                } catch (_: Exception) {
                    // WAL/busy_timeout unsupported here (e.g. read-only file system) - continue.
                }

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
                return DatabaseRepository(connection, json)
            } catch (e: Exception) {
                lastError = e
                if (attempt < 2) {
                    logger.warn { "Database open failed (attempt ${attempt + 1}/3): ${e.message}" }
                    try { Thread.sleep(2000) } catch (_: InterruptedException) { }
                }
            }
        }
        logger.error(lastError) { "Failed to initialize database at ${dbFile.absolutePath}" }
        return null
    }
}