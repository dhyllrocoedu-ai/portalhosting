package com.portalhost.server.providers

import com.portalhost.model.ServerType
import com.portalhost.model.ServerVersion
import com.portalhost.model.ServerBuild
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

const val USER_AGENT = "PortalHost/5.0"

interface ServerProvider {
    val id: String
    val name: String
    val supportedTypes: Set<ServerType>
    
    suspend fun fetchVersions(): Result<List<ServerVersion>>
    suspend fun fetchBuilds(version: String): Result<List<ServerBuild>>
    suspend fun downloadBuild(
        build: ServerBuild,
        destination: File,
        onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)? = null
    ): Result<File>
}

fun URL.downloadToFile(
    destination: File,
    connectTimeoutMs: Int = 30000,
    readTimeoutMs: Int = 600000,
    headers: Map<String, String> = emptyMap(),
    onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)? = null
) {
    destination.parentFile?.mkdirs()
    val conn = openConnection() as HttpURLConnection
    conn.connectTimeout = connectTimeoutMs
    conn.readTimeout = readTimeoutMs
    conn.instanceFollowRedirects = true
    headers.forEach { (key, value) -> conn.setRequestProperty(key, value) }
    try {
        val responseCode = conn.responseCode
        if (responseCode !in 200..299) {
            val errorBody = try { conn.errorStream?.bufferedReader()?.readText() ?: "" } catch (_: Exception) { "" }
            throw IOException("HTTP $responseCode ${conn.responseMessage} for $this${
                if (errorBody.isNotBlank()) ": $errorBody" else ""
            }")
        }
        val contentLength = conn.contentLengthLong
        var downloaded = 0L
        conn.inputStream.use { input ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead = input.read(buffer)
                while (bytesRead != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    if (contentLength > 0) {
                        onProgress?.invoke(downloaded, contentLength)
                    }
                    bytesRead = input.read(buffer)
                }
            }
        }
    } finally {
        conn.disconnect()
    }
}

fun URL.readTextWithTimeout(
    connectTimeoutMs: Int = 10000,
    readTimeoutMs: Int = 30000,
    headers: Map<String, String> = emptyMap()
): String {
    val conn = openConnection() as HttpURLConnection
    conn.connectTimeout = connectTimeoutMs
    conn.readTimeout = readTimeoutMs
    conn.instanceFollowRedirects = true
    headers.forEach { (key, value) -> conn.setRequestProperty(key, value) }
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
