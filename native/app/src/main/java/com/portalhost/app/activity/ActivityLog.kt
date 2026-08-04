package com.portalhost.app.activity

data class ActivityEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val type: ActivityType = ActivityType.INFO
)

enum class ActivityType {
    INFO, SUCCESS, WARNING, ERROR, PLAYER_JOIN, PLAYER_LEAVE,
    SERVER_ONLINE, SERVER_OFFLINE, SERVER_CRASH, PLAYER_KICK, PLAYER_BAN,
    PLAYER_OP, PLAYER_DEOP, PLAYER_KILL, COMMAND_EXECUTED
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

    fun addServerOnline() {
        add("Server is online", ActivityType.SERVER_ONLINE)
    }

    fun addServerOffline() {
        add("Server is offline", ActivityType.SERVER_OFFLINE)
    }

    fun addPlayerKick(player: String, reason: String = "") {
        add("$player was kicked${if (reason.isNotBlank()) " ($reason)" else ""}", ActivityType.PLAYER_KICK)
    }

    fun addPlayerBan(player: String, reason: String = "") {
        add("$player was banned${if (reason.isNotBlank()) " ($reason)" else ""}", ActivityType.PLAYER_BAN)
    }

    fun addPlayerOp(player: String) {
        add("$player was opped", ActivityType.PLAYER_OP)
    }

    fun addPlayerDeop(player: String) {
        add("$player was deopped", ActivityType.PLAYER_DEOP)
    }

    fun addPlayerKill(player: String, detail: String = "") {
        add("$player was killed${if (detail.isNotBlank()) " ($detail)" else ""}", ActivityType.PLAYER_KILL)
    }

    fun addCommandExecuted(player: String, command: String) {
        add("<$player> execute command: $command", ActivityType.COMMAND_EXECUTED)
    }

    fun clear() {
        synchronized(_entries) { _entries.clear() }
    }
}
