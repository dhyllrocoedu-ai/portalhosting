import 'dart:collection';

enum LogLevel { all, info, warn, error }

class ConsoleLine {
  final String text;
  final LogLevel level;
  final DateTime timestamp;
  final int index;

  const ConsoleLine({
    required this.text,
    required this.level,
    required this.timestamp,
    required this.index,
  });
}

class ConsoleStreamer {
  static const int maxLines = 5000;
  final Map<int, Queue<ConsoleLine>> _buffers = {};
  int _globalIndex = 0;

  Queue<ConsoleLine> buffer(int serverId) {
    _buffers.putIfAbsent(serverId, () => Queue<ConsoleLine>());
    return _buffers[serverId]!;
  }

  void addLine(int serverId, String text) {
    final buf = buffer(serverId);
    final level = _classify(text);
    buf.add(ConsoleLine(
      text: text,
      level: level,
      timestamp: DateTime.now(),
      index: _globalIndex++,
    ));
    while (buf.length > maxLines) {
      buf.removeFirst();
    }
  }

  List<ConsoleLine> getLines(int serverId, {LogLevel minLevel = LogLevel.all}) {
    return buffer(serverId)
        .where((l) => _levelIndex(l.level) >= _levelIndex(minLevel))
        .toList();
  }

  List<ConsoleLine> search(int serverId, String query,
      {LogLevel minLevel = LogLevel.all}) {
    final lower = query.toLowerCase();
    return buffer(serverId)
        .where((l) =>
            l.text.toLowerCase().contains(lower) &&
            _levelIndex(l.level) >= _levelIndex(minLevel))
        .toList();
  }

  void clear(int serverId) {
    buffer(serverId).clear();
  }

  void dispose() {
    _buffers.clear();
  }

  LogLevel _classify(String text) {
    final lower = text.toLowerCase();
    if (lower.contains('[err]') ||
        lower.contains('error') ||
        lower.contains('exception') ||
        lower.contains('fatal')) {
      return LogLevel.error;
    }
    if (lower.contains('[warn]') ||
        lower.contains('warn') ||
        lower.contains('warning')) {
      return LogLevel.warn;
    }
    return LogLevel.info;
  }

  int _levelIndex(LogLevel level) {
    switch (level) {
      case LogLevel.all:
        return -1;
      case LogLevel.info:
        return 0;
      case LogLevel.warn:
        return 1;
      case LogLevel.error:
        return 2;
    }
  }
}
