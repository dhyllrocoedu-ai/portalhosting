package com.portalhost.server

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ActivityType {
    SERVER_START,
    SERVER_STOP,
    SERVER_CRASH,
    SERVER_ONLINE,
    SERVER_OFFLINE,
    PLAYER_JOIN,
    PLAYER_LEAVE,
    PLAYER_KICK,
    PLAYER_BAN,
    PLAYER_OP,
    PLAYER_DEOP,
    PLAYER_KILL,
    COMMAND_EXECUTED,
    INFO
}

data class ActivityEntry(
    val id: Long,
    val serverId: String,
    val serverName: String,
    val action: String,
    val type: ActivityType = ActivityType.INFO,
    val timestamp: Long = System.currentTimeMillis(),
) {
    val formattedTime: String get() =
        SimpleDateFormat("MMM dd HH:mm", Locale.US).format(Date(timestamp))
}

class ActivityLog {
    private val _activities = MutableStateFlow<List<ActivityEntry>>(emptyList())
    val activities: StateFlow<List<ActivityEntry>> = _activities

    private var nextId = 1L

    fun log(serverId: String, serverName: String, action: String) {
        val entry = ActivityEntry(
            id = nextId++,
            serverId = serverId,
            serverName = serverName,
            action = action,
        )
        _activities.value = listOf(entry) + _activities.value.take(99)
    }

    fun logCommand(serverId: String, serverName: String, player: String, command: String) {
        val entry = ActivityEntry(
            id = nextId++,
            serverId = serverId,
            serverName = serverName,
            action = "<$player> execute command: $command",
            type = ActivityType.COMMAND_EXECUTED,
        )
        _activities.value = listOf(entry) + _activities.value.take(99)
    }

    fun logServerOnline(serverId: String, serverName: String) {
        val entry = ActivityEntry(
            id = nextId++,
            serverId = serverId,
            serverName = serverName,
            action = "Server is online",
            type = ActivityType.SERVER_ONLINE,
        )
        _activities.value = listOf(entry) + _activities.value.take(99)
    }

    fun logServerOffline(serverId: String, serverName: String) {
        val entry = ActivityEntry(
            id = nextId++,
            serverId = serverId,
            serverName = serverName,
            action = "Server is offline",
            type = ActivityType.SERVER_OFFLINE,
        )
        _activities.value = listOf(entry) + _activities.value.take(99)
    }

    fun logServerCrash(serverId: String, serverName: String, exitCode: Int) {
        val entry = ActivityEntry(
            id = nextId++,
            serverId = serverId,
            serverName = serverName,
            action = "Server crashed (exit code $exitCode)",
            type = ActivityType.SERVER_CRASH,
        )
        _activities.value = listOf(entry) + _activities.value.take(99)
    }

    fun logPlayerAction(serverId: String, serverName: String, player: String, type: ActivityType, detail: String = "") {
        val action = when (type) {
            ActivityType.PLAYER_KICK -> "$player was kicked$detail"
            ActivityType.PLAYER_BAN -> "$player was banned$detail"
            ActivityType.PLAYER_OP -> "$player was opped$detail"
            ActivityType.PLAYER_DEOP -> "$player was deopped$detail"
            ActivityType.PLAYER_KILL -> "$player was killed$detail"
            else -> "$player $detail"
        }
        val entry = ActivityEntry(
            id = nextId++,
            serverId = serverId,
            serverName = serverName,
            action = action,
            type = type,
        )
        _activities.value = listOf(entry) + _activities.value.take(99)
    }

    fun clear() {
        _activities.value = emptyList()
    }
}
