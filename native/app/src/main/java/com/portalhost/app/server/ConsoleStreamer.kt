package com.portalhost.app.server

/** Manages console history and searching. */
class ConsoleStreamer {
    private val _lines = mutableListOf<String>()
    val lines: List<String> get() = _lines.toList()
    private var _searchResults: List<Int> = emptyList()
    val searchResults: List<Int> get() = _searchResults

    private var maxLines = 10_000

    fun append(line: String) {
        synchronized(_lines) {
            _lines.add(line)
            if (_lines.size > maxLines) {
                _lines.removeAt(0)
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
        synchronized(_lines) {
            _lines.clear()
        }
    }

    fun toFormattedText(): String {
        synchronized(_lines) {
            return _lines.joinToString("\n")
        }
    }
}
