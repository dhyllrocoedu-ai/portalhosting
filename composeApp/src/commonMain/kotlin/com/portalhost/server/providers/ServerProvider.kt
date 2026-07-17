package com.portalhost.server.providers

import com.portalhost.model.ServerType
import com.portalhost.model.ServerVersion
import com.portalhost.model.ServerBuild
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

interface ServerProvider {
    val id: String
    val name: String
    val supportedTypes: Set<ServerType>
    
    suspend fun fetchVersions(): Result<List<ServerVersion>>
    suspend fun fetchBuilds(version: String): Result<List<ServerBuild>>
    suspend fun downloadBuild(build: ServerBuild, destination: File): Result<File>
}

fun URL.readTextWithTimeout(connectTimeoutMs: Int = 10000, readTimeoutMs: Int = 30000): String {
    val conn = openConnection() as HttpURLConnection
    conn.connectTimeout = connectTimeoutMs
    conn.readTimeout = readTimeoutMs
    conn.instanceFollowRedirects = true
    try {
        val responseCode = conn.responseCode
        if (responseCode !in 200..299) {
            val errorBody = try { conn.errorStream?.bufferedReader()?.readText() ?: "" } catch (_: Exception) { "" }
            throw IOException("HTTP $responseCode ${conn.responseMessage} for $this${
                if (errorBody.isNotBlank()) ": $errorBody" else ""
            }")
        }
        return conn.inputStream.bufferedReader().readText()
    } finally {
        conn.disconnect()
    }
}
