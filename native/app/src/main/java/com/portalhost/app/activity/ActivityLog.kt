package com.portalhost.app.activity

data class ActivityEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val type: ActivityType = ActivityType.INFO
)

enum class ActivityType {
    INFO, SUCCESS, WARNING, ERROR, PLAYER_JOIN, PLAYER_LEAVE
}

class ActivityLog(private val maxEntries: Int = 100) {
    private val _entries = mutableListOf<ActivityEntry>()
    val entries: List<ActivityEntry> get() = synchronized(_entries) { _entries.toList() }

    fun add(message: String, type: ActivityType = ActivityType.INFO) {
        synchronized(_entries) {
            _entries.add(ActivityEntry(message = message, type = type))
            if (_entries.size > maxEntries) {
                _entries.removeAt(0)
            }
        }
    }

    fun addPlayerJoin(player: String) {
        add("$player joined", ActivityType.PLAYER_JOIN)
    }

    fun addPlayerLeave(player: String) {
        add("$player left", ActivityType.PLAYER_LEAVE)
    }

    fun addServerStart() {
        add("Server started", ActivityType.SUCCESS)
    }

    fun addServerStop() {
        add("Server stopped", ActivityType.WARNING)
    }

    fun addServerCrash() {
        add("Server crashed", ActivityType.ERROR)
    }

    fun addBackup() {
        add("Backup completed", ActivityType.SUCCESS)
    }

    fun clear() {
        synchronized(_entries) { _entries.clear() }
    }
}
