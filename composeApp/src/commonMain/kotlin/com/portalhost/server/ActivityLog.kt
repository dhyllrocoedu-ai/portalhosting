package com.portalhost.server

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ActivityEntry(
    val id: Long,
    val serverId: String,
    val serverName: String,
    val action: String,
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

    fun clear() {
        _activities.value = emptyList()
    }
}
