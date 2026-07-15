package com.portalhost.log

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class LogEvent(
    val id: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val level: String,
    val logger: String,
    val message: String,
    val throwable: String? = null,
)

class LogRepository {
    private val _events = MutableStateFlow<List<LogEvent>>(emptyList())
    val events: StateFlow<List<LogEvent>> = _events

    private var nextId = 1L
    private val maxEvents = 1000

    fun publish(level: String, logger: String, message: String, throwable: String? = null) {
        val event = LogEvent(
            id = nextId++,
            level = level,
            logger = logger,
            message = message,
            throwable = throwable,
        )
        _events.value = (_events.value + event).takeLast(maxEvents)
    }

    fun clear() {
        _events.value = emptyList()
    }
}
