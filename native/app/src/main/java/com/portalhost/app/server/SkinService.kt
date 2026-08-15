package com.portalhost.app.server

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.math.min

class SkinService(
    private val maxCacheEntries: Int = 50,
    private val userAgent: String = "PortalHost/${com.portalhost.app.BuildConfig.VERSION_NAME}",
) {
    private val memo = object : LinkedHashMap<String, Bitmap>(maxCacheEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>): Boolean =
            size > maxCacheEntries
    }
    private val mutex = Mutex()

    fun get(playerName: String): Bitmap? = synchronized(memo) { memo[playerName] }

    suspend fun fetch(playerName: String): Bitmap? {
        synchronized(memo) { memo[playerName] }?.let { return it }
        return mutex.withLock {
            synchronized(memo) { memo[playerName] }?.let { return@withLock it }
            withContext(Dispatchers.IO) {
                downloadSkin(playerName)
            }?.also { skin ->
                synchronized(memo) {
                    memo[playerName] = skin
                }
            }
        }
    }

    fun clear() = synchronized(memo) { memo.clear() }

    private fun downloadSkin(playerName: String): Bitmap? {
        val uuid = offlineUuid(playerName)
        val url = "https://sessionserver.mojang.com/session/minecraft/profile/$uuid"
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 10000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", userAgent)
            conn.instanceFollowRedirects = true
            if (conn.responseCode !in 200..299) return null
            val body = conn.inputStream.buffered().use { it.readBytes() }
            conn.disconnect()
            val skinUrl = extractSkinUrl(body) ?: return null
            downloadBitmap(skinUrl)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractSkinUrl(body: ByteArray): String? {
        val text = body.decodeToString()
        val idx = text.indexOf("\"textures\"")
        if (idx < 0) return null
        val valueIdx = text.indexOf("\"value\":\"", idx)
        if (valueIdx < 0) return null
        val start = valueIdx + 9
        val end = text.indexOf("\"", start)
        if (end < 0) return null
        val b64 = text.substring(start, end)
        return try {
            val decoded = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
            val decodedStr = String(decoded)
            val urlIdx = decodedStr.indexOf("\"url\":\"")
            if (urlIdx < 0) return null
            val urlStart = urlIdx + 7
            val urlEnd = decodedStr.indexOf("\"", urlStart)
            if (urlEnd < 0) return null
            decodedStr.substring(urlStart, urlEnd)
        } catch (_: Exception) {
            null
        }
    }

    private fun downloadBitmap(urlStr: String): Bitmap? {
        return try {
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 10000
            conn.instanceFollowRedirects = true
            if (conn.responseCode !in 200..299) return null
            val bytes = conn.inputStream.buffered().use { it.readBytes() }
            val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val size = min(raw.width, raw.height).coerceAtLeast(8)
            Bitmap.createBitmap(raw, 0, 0, size, size).also { if (it != raw) raw.recycle() }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        fun offlineUuid(name: String): String {
            val digest = MessageDigest.getInstance("MD5")
            val bytes = digest.digest("OfflinePlayer:$name".toByteArray())
            bytes[6] = (bytes[6].toInt() and 0x0f or 0x30).toByte()
            bytes[8] = (bytes[8].toInt() and 0x3f or 0x80).toByte()
            return "%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x".format(
                *bytes.map { it.toInt() and 0xFF }.toTypedArray()
            )
        }
    }
}