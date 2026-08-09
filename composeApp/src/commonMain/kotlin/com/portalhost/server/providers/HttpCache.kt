package com.portalhost.server.providers

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CachedResponse(
    val body: String,
    val etag: String?,
    val lastModified: String?
)

object HttpCache {
    private val memoryCache = mutableMapOf<String, CachedResponse>()
    private val cacheDir: File? = run {
        try {
            val dir = File(System.getProperty("java.io.tmpdir"), "portalhost-httpcache")
            dir.mkdirs()
            dir
        } catch (_: Exception) { null }
    }

    fun fetchWithCache(
        url: String,
        connectTimeoutMs: Int = 5000,
        readTimeoutMs: Int = 10000,
        maxRetries: Int = 1
    ): Result<String> {
        for (attempt in 0..maxRetries) {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = connectTimeoutMs
                conn.readTimeout = readTimeoutMs
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", "PortalHost/1.0")

                val cached = getCached(url)
                if (cached != null) {
                    cached.etag?.let { conn.setRequestProperty("If-None-Match", it) }
                    cached.lastModified?.let { conn.setRequestProperty("If-Modified-Since", it) }
                }

                val responseCode = conn.responseCode
                if (responseCode == 304 && cached != null) {
                    conn.disconnect()
                    return Result.success(cached.body)
                }

                if (responseCode !in 200..299) {
                    val errorBody = try { conn.errorStream?.bufferedReader()?.readText() ?: "" } catch (_: Exception) { "" }
                    conn.disconnect()
                    if (responseCode == 404 || responseCode == 410) {
                        return Result.failure(Exception("HTTP $responseCode for $url${if (errorBody.isNotBlank()) ": $errorBody" else ""}"))
                    }
                    if (attempt < maxRetries) continue
                    return Result.failure(Exception("HTTP $responseCode for $url${if (errorBody.isNotBlank()) ": $errorBody" else ""}"))
                }

                val body = conn.inputStream.bufferedReader().readText()
                val etag = conn.getHeaderField("ETag")
                val lastModified = conn.getHeaderField("Last-Modified")
                conn.disconnect()

                val cr = CachedResponse(body, etag, lastModified)
                putCache(url, cr)
                return Result.success(body)
            } catch (e: Exception) {
                if (attempt < maxRetries) {
                    Thread.sleep(500L * (attempt + 1))
                    continue
                }
                return Result.failure(e)
            }
        }
        return Result.failure(Exception("Exhausted retries for $url"))
    }

    private fun getCached(url: String): CachedResponse? {
        memoryCache[url]?.let { return it }
        val file = cacheFile(url)
        if (file != null && file.exists()) {
            try {
                val lines = file.readLines()
                if (lines.size >= 2) {
                    val etag = lines[0].ifBlank { null }?.takeIf { it != "null" }
                    val lastModified = lines[1].ifBlank { null }?.takeIf { it != "null" }
                    val body = lines.drop(2).joinToString("\n")
                    val cr = CachedResponse(body, etag, lastModified)
                    memoryCache[url] = cr
                    return cr
                }
            } catch (_: Exception) { }
        }
        return null
    }

    private fun putCache(url: String, cr: CachedResponse) {
        memoryCache[url] = cr
        val file = cacheFile(url) ?: return
        try {
            file.writeText("${cr.etag ?: ""}\n${cr.lastModified ?: ""}\n${cr.body}")
        } catch (_: Exception) { }
    }

    private fun cacheFile(url: String): File? {
        val dir = cacheDir ?: return null
        val hash = MessageDigest.getInstance("MD5").digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(dir, hash)
    }

    fun clear() {
        memoryCache.clear()
        cacheDir?.listFiles()?.forEach { it.delete() }
    }

    suspend fun fetchWithCacheSuspend(
        url: String,
        connectTimeoutMs: Int = 5000,
        readTimeoutMs: Int = 10000,
        maxRetries: Int = 1
    ): Result<String> = withContext(Dispatchers.IO) {
        fetchWithCache(url, connectTimeoutMs, readTimeoutMs, maxRetries)
    }
}
