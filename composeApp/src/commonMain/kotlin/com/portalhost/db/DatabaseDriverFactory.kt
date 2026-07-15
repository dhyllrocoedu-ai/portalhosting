package com.portalhost.db

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class DatabaseDriverFactory {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun createDatabase(): DatabaseRepository {
        val home = System.getProperty("user.home") ?: "."
        val dbDir = File(home, ".portalhost")
        dbDir.mkdirs()
        val dbFile = File(dbDir, "portalhost.db")

        Class.forName("org.sqlite.JDBC")
        val connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
        connection.createStatement().execute(PortalHostDatabase.schema)
        return DatabaseRepository(connection, json)
    }
}