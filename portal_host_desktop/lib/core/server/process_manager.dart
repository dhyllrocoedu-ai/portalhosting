import 'dart:async';
import 'dart:convert';
import 'dart:io';
import '../../shared/models/server_state.dart';

class ProcessManager {
  final Map<int, Process> _processes = {};
  final Map<int, StreamController<String>> _consoleControllers = {};
  final Map<int, DateTime> _startTimes = {};
  final Map<int, int> _crashCount = {};

  Stream<String> consoleStream(int serverId) {
    _consoleControllers.putIfAbsent(
        serverId, () => StreamController<String>.broadcast());
    return _consoleControllers[serverId]!.stream;
  }

  void writeCommand(int serverId, String command) {
    final process = _processes[serverId];
    if (process != null) {
      process.stdin.writeln(command);
    }
  }

  Future<Process> start(
    int serverId,
    String jarPath,
    String javaArgs,
    String serverType,
    int port,
    String workDir,
    String? javaPath,
  ) async {
    final java = javaPath ?? 'java';
    final args = [
      ...javaArgs.split(' ').where((a) => a.isNotEmpty),
      '-jar',
      jarPath,
      '--port',
      '$port',
    ];

    await Directory(workDir).create(recursive: true);

    final process = await Process.start(
      java,
      args,
      workingDirectory: workDir,
      runInShell: true,
    );

    _processes[serverId] = process;
    _startTimes[serverId] = DateTime.now();

    final controller = _consoleControllers.putIfAbsent(
        serverId, () => StreamController<String>.broadcast());

    process.stdout
        .transform(utf8.decoder)
        .transform(const LineSplitter())
        .listen((line) {
      controller.add(_stripAnsi(line));
    });

    process.stderr
        .transform(utf8.decoder)
        .transform(const LineSplitter())
        .listen((line) {
      controller.add('[ERR] ${_stripAnsi(line)}');
    });

    final crashCount = _crashCount[serverId] ?? 0;
    process.exitCode.then((code) {
      _processes.remove(serverId);
      _startTimes.remove(serverId);
      if (code != 0) {
        _crashCount[serverId] = crashCount + 1;
      } else {
        _crashCount.remove(serverId);
      }
    });

    return process;
  }

  Future<void> stop(int serverId, {bool force = false}) async {
    final process = _processes[serverId];
    if (process == null) return;

    if (force) {
      process.kill(ProcessSignal.sigkill);
    } else {
      process.stdin.writeln('stop');
      try {
        await process.exitCode.timeout(const Duration(seconds: 10));
      } on TimeoutException {
        process.kill(ProcessSignal.sigkill);
      }
    }

    _processes.remove(serverId);
    _startTimes.remove(serverId);
  }

  void stopAll() {
    for (final id in _processes.keys.toList()) {
      stop(id, force: true);
    }
  }

  void killAll() {
    for (final process in _processes.values) {
      try {
        process.kill(ProcessSignal.sigkill);
      } catch (_) {}
    }
    _processes.clear();
    _startTimes.clear();
  }

  bool isRunning(int serverId) => _processes.containsKey(serverId);

  int uptimeSeconds(int serverId) {
    final start = _startTimes[serverId];
    if (start == null) return 0;
    return DateTime.now().difference(start).inSeconds;
  }

  int crashCount(int serverId) => _crashCount[serverId] ?? 0;

  ServerState stateFor(int serverId) {
    final running = isRunning(serverId);
    return ServerState(
      status: running ? ServerStatus.running : ServerStatus.stopped,
      uptimeSeconds: uptimeSeconds(serverId),
    );
  }

  void dispose() {
    killAll();
    for (final controller in _consoleControllers.values) {
      controller.close();
    }
    _consoleControllers.clear();
  }

  String _stripAnsi(String text) {
    return text.replaceAll(RegExp(r'\x1B\[[0-9;]*[a-zA-Z]'), '');
  }
}
