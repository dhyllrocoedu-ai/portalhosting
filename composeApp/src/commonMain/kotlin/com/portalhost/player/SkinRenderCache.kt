package com.portalhost.player

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class SkinRenderCache(
    private val cacheDir: File,
    private val maxMemoryEntries: Int = 50,
) {
    private val memory = object : LinkedHashMap<String, CacheEntry>(maxMemoryEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>): Boolean =
            size > maxMemoryEntries
    }
    private val lock = Any()

    data class CacheEntry(val url: String, val bitmap: ImageBitmap)

    init {
        cacheDir.mkdirs()
    }

    fun get(uuid: String): CacheEntry? = synchronized(lock) { memory[uuid] }

    suspend fun load(uuid: String, skinUrl: String): ImageBitmap? = withContext(Dispatchers.IO) {
        val existing = get(uuid)
        if (existing != null && existing.url == skinUrl) {
            return@withContext existing.bitmap
        }

        val sidecar = File(cacheDir, "$uuid.url")
        val png = File(cacheDir, "$uuid.png")
        if (png.exists() && sidecar.exists() && sidecar.readText().trim() == skinUrl) {
            val decoded = decodePng(png.readBytes())
            if (decoded != null) {
                synchronized(lock) { memory[uuid] = CacheEntry(skinUrl, decoded) }
                return@withContext decoded
            }
        }

        val downloaded = downloadBitmap(skinUrl)
        if (downloaded != null) {
            try {
                png.writeBytes(downloaded.bytes)
                sidecar.writeText(skinUrl)
            } catch (_: Exception) {
            }
            val decoded = decodePng(downloaded.bytes)
            if (decoded != null) {
                synchronized(lock) { memory[uuid] = CacheEntry(skinUrl, decoded) }
                return@withContext decoded
            }
        }
        null
    }

    private data class Downloaded(val bytes: ByteArray, val contentType: String?)

    private fun downloadBitmap(url: String): Downloaded? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 5_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("User-Agent", "PortalHost/5.1.0")
            conn.instanceFollowRedirects = true
            if (conn.responseCode in 200..299) {
                Downloaded(conn.inputStream.readBytes(), conn.contentType)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodePng(bytes: ByteArray): ImageBitmap? {
        return try {
            SkiaImage.makeFromEncoded(bytes)?.toComposeImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    fun clear() {
        synchronized(lock) { memory.clear() }
    }

    companion object {
        fun sha1(text: String): String {
            val md = MessageDigest.getInstance("SHA-1")
            val bytes = md.digest(text.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
