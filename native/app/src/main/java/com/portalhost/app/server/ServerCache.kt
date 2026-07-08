package com.portalhost.app.server

class ServerCache(private val ttlMs: Long = 5 * 60 * 1000L) {
    private data class Entry(val data: Any, val timestamp: Long = System.currentTimeMillis())
    private val cache = mutableMapOf<String, Entry>()

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        val entry = cache[key] ?: return null
        if (System.currentTimeMillis() - entry.timestamp > ttlMs) {
            cache.remove(key)
            return null
        }
        return entry.data as T
    }

    fun <T> set(key: String, data: T) {
        cache[key] = Entry(data as Any)
    }

    fun clear() {
        cache.clear()
    }
}
