package com.portalhost.app.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Manages console history and searching. */
class ConsoleStreamer {
    private var _lines = mutableListOf<String>()
    val lines: List<String> get() = _lines.toList()

    private val _linesState = MutableStateFlow<List<String>>(emptyList())
    val linesState: StateFlow<List<String>> = _linesState.asStateFlow()

    private var _searchResults: List<Int> = emptyList()
    val searchResults: List<Int> get() = _searchResults

    private var maxLines = 5_000
    private val trimAmount = 1_000
    private val scope = CoroutineScope(Dispatchers.Default)
    private var snapshotJob: Job? = null

    fun append(line: String) {
        synchronized(_lines) {
            _lines.add(line)
            if (_lines.size > maxLines) {
                _lines = _lines.drop(trimAmount).toMutableList()
            }
        }
        scheduleSnapshot()
    }

    private fun scheduleSnapshot() {
        snapshotJob?.cancel()
        snapshotJob = scope.launch {
            delay(100)
            synchronized(_lines) {
                _linesState.value = _lines.toList()
            }
        }
    }

    fun search(query: String) {
        synchronized(_lines) {
            _searchResults = _lines.mapIndexedNotNull { index, line ->
                if (line.contains(query, ignoreCase = true)) index else null
            }
        }
    }

    fun clearSearch() {
        _searchResults = emptyList()
    }

    fun clear() {
        snapshotJob?.cancel()
        synchronized(_lines) {
            _lines.clear()
        }
        _linesState.value = emptyList()
    }

    fun toFormattedText(): String {
        synchronized(_lines) {
            return _lines.joinToString("\n")
        }
    }
}
