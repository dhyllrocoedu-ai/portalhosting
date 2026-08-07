package com.portalhost.app.server

import android.content.Context
import android.util.Log
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * In-memory + disk-backed cache for server version / build lists.
 *
 * Entries are "fresh" for [ttlMs] and served by [get]. Once stale, they remain
 * on disk so [getStale] can still return them as an offline fallback when the
 * network request fails.
 */
class ServerCache(
    context: Context,
    private val ttlMs: Long = 5 * 60 * 1000L
) {
    private val dir = File(context.filesDir, "version-cache")
    private val json = Json { ignoreUnknownKeys = true }
    private val memory = mutableMapOf<String, MemoryEntry>()

    private data class MemoryEntry(val data: Any?, val timestamp: Long)

    @Serializable
    private data class DiskEntry(val payload: String, val timestamp: Long)

    private fun fileFor(key: String): File =
        File(dir, "${key.hashCode().toUInt().toString(16)}.json")

    private fun isFresh(timestamp: Long): Boolean =
        System.currentTimeMillis() - timestamp <= ttlMs

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String, serializer: KSerializer<T>): T? {
        memory[key]?.let { entry ->
            if (isFresh(entry.timestamp)) return entry.data as T
        }
        val disk = readDisk(key, serializer)
        if (disk != null) {
            val ts = diskTimestamp(key)
            memory[key] = MemoryEntry(disk, ts)
            if (isFresh(ts)) return disk
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getStale(key: String, serializer: KSerializer<T>): T? {
        memory[key]?.let { return it.data as T }
        return readDisk(key, serializer)
    }

    fun <T> set(key: String, serializer: KSerializer<T>, data: T) {
        val timestamp = System.currentTimeMillis()
        memory[key] = MemoryEntry(data, timestamp)
        try {
            dir.mkdirs()
            val payload = json.encodeToString(serializer, data)
            fileFor(key).writeText(json.encodeToString(DiskEntry(payload, timestamp)))
        } catch (e: Exception) {
            Log.w("ServerCache", "Failed to persist '$key': ${e.message}")
        }
    }

    fun clear() {
        memory.clear()
        try {
            if (dir.exists()) dir.deleteRecursively()
        } catch (e: Exception) {
            Log.w("ServerCache", "Failed to clear cache dir: ${e.message}")
        }
    }

    private fun <T> readDisk(key: String, serializer: KSerializer<T>): T? {
        return try {
            val file = fileFor(key)
            if (!file.exists()) return null
            val wrapper = json.decodeFromString<DiskEntry>(file.readText())
            json.decodeFromString(serializer, wrapper.payload)
        } catch (e: Exception) {
            Log.w("ServerCache", "Disk read failed for '$key': ${e.message}")
            null
        }
    }

    private fun diskTimestamp(key: String): Long {
        return try {
            val wrapper = json.decodeFromString<DiskEntry>(fileFor(key).readText())
            wrapper.timestamp
        } catch (_: Exception) {
            0L
        }
    }
}
