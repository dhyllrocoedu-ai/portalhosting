import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:path/path.dart' as p;
import '../../shared/models/server_state.dart';

class PerformanceMetrics {
  final double cpuUsage;
  final int memoryBytes;
  final double tps;
  final DateTime timestamp;

  const PerformanceMetrics({
    required this.cpuUsage,
    required this.memoryBytes,
    required this.tps,
    required this.timestamp,
  });
}

typedef BackupCallback = Future<void> Function(int serverId);
typedef LogCallback = void Function(int serverId, String line);
typedef StateChangeCallback = void Function(int serverId, ServerState state);

class _ServerConfig {
  final int id;
  final String name;
  final String jarPath;
  final int port;
  final String serverType;
  final String javaArgs;
  final bool autoRestart;
  final int maxRestartAttempts;

  _ServerConfig({
    required this.id,
    required this.name,
    required this.jarPath,
    required this.port,
    required this.serverType,
    required this.javaArgs,
    this.autoRestart = false,
    this.maxRestartAttempts = 3,
  });
}

class ProcessManager {
  final Map<int, Process> _processes = {};
  final Map<int, StreamController<String>> _consoleControllers = {};
  final Map<int, DateTime> _startTimes = {};
  final Map<int, int> _crashCount = {};
  final Map<int, Timer> _backupTimers = {};
  final Map<int, String> _serverDirs = {};
  final Map<int, int> _restartAttempts = {};
  final Map<int, _ServerConfig> _serverConfigs = {};
  
  BackupCallback? onAutoBackup;
  LogCallback? onLog;
  StateChangeCallback? onStateChange;

  void startAutoBackup(int serverId, Duration interval) {
    stopAutoBackup(serverId);
    _backupTimers[serverId] = Timer.periodic(interval, (_) {
      onAutoBackup?.call(serverId);
    });
  }

  void stopAutoBackup(int serverId) {
    _backupTimers[serverId]?.cancel();
    _backupTimers.remove(serverId);
  }

  void stopAllAutoBackups() {
    for (final timer in _backupTimers.values) {
      timer.cancel();
    }
    _backupTimers.clear();
  }

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
    String? javaPath, {
    bool autoRestart = false,
    int maxRestartAttempts = 3,
    String serverName = '',
  }) async {
    final config = _ServerConfig(
      id: serverId,
      name: serverName,
      jarPath: jarPath,
      port: port,
      serverType: serverType,
      javaArgs: javaArgs,
      autoRestart: autoRestart,
      maxRestartAttempts: maxRestartAttempts,
    );
    _serverConfigs[serverId] = config;
    _restartAttempts[serverId] = 0;
    _serverDirs[serverId] = workDir;

    return _startProcess(serverId, workDir, javaPath);
  }

  Future<Process> _startProcess(int serverId, String workDir, String? javaPath) async {
    final config = _serverConfigs[serverId];
    if (config == null) throw StateError('Server config not found for $serverId');

    final java = javaPath ?? 'java';
    final args = [
      ...config.javaArgs.split(' ').where((a) => a.isNotEmpty),
      '-jar',
      config.jarPath,
      '--port',
      '${config.port}',
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

    final readyCompleter = Completer<void>();
    bool readyNotified = false;

    void handleLine(String line) {
      final cleanLine = _stripAnsi(line);
      controller.add(cleanLine);
      onLog?.call(serverId, cleanLine);
      
      if (!readyNotified && _isServerReady(cleanLine)) {
        readyNotified = true;
        if (!readyCompleter.isCompleted) {
          readyCompleter.complete();
        }
      }
    }

    process.stdout
        .transform(utf8.decoder)
        .transform(const LineSplitter())
        .listen(handleLine);

    process.stderr
        .transform(utf8.decoder)
        .transform(const LineSplitter())
        .listen((line) {
      final cleanLine = '[ERR] ${_stripAnsi(line)}';
      controller.add(cleanLine);
      onLog?.call(serverId, cleanLine);
    });

    final crashCount = _crashCount[serverId] ?? 0;
    process.exitCode.then((code) {
      _processes.remove(serverId);
      _startTimes.remove(serverId);
      _serverDirs.remove(serverId);
      
      if (code != 0) {
        final newCrashCount = crashCount + 1;
        _crashCount[serverId] = newCrashCount;
        
        if (config.autoRestart && newCrashCount <= config.maxRestartAttempts) {
          final attempts = _restartAttempts[serverId]! + 1;
          _restartAttempts[serverId] = attempts;
          final delay = Duration(seconds: 5 * attempts);
          
          Timer(delay, () {
            if (!_processes.containsKey(serverId)) {
              _startProcess(serverId, workDir, javaPath);
            }
          });
        } else {
          _notifyStateChange(serverId, ServerState(
            status: ServerStatus.crashed,
            exitCode: code,
            uptimeSeconds: uptimeSeconds(serverId),
          ));
        }
      } else {
        _crashCount.remove(serverId);
        _restartAttempts.remove(serverId);
        _notifyStateChange(serverId, ServerState(
          status: ServerStatus.stopped,
          exitCode: code,
          uptimeSeconds: uptimeSeconds(serverId),
        ));
      }
    });

    try {
      await readyCompleter.future.timeout(const Duration(seconds: 60));
      _notifyStateChange(serverId, ServerState(
        status: ServerStatus.running,
        startTime: _startTimes[serverId],
        uptimeSeconds: uptimeSeconds(serverId),
      ));
    } on TimeoutException {
      _notifyStateChange(serverId, ServerState(
        status: ServerStatus.crashed,
        exitCode: -1,
        uptimeSeconds: uptimeSeconds(serverId),
      ));
    }

    return process;
  }

  bool _isServerReady(String line) {
    final lower = line.toLowerCase();
    return lower.contains('done (') || 
           lower.contains('server started') ||
           (lower.contains('starting minecraft server') && lower.contains('version'));
  }

  void _notifyStateChange(int serverId, ServerState state) {
    onStateChange?.call(serverId, state);
  }

  Future<void> waitForReady(int serverId, {Duration timeout = const Duration(seconds: 60)}) async {
    final completer = Completer<void>();
    bool completed = false;
    
    void checkReady(String line) {
      if (!completed && _isServerReady(line)) {
        completed = true;
        completer.complete();
      }
    }
    
    final sub = consoleStream(serverId).listen(checkReady);
    
    try {
      await completer.future.timeout(timeout);
    } on TimeoutException {
      if (!completed) {
        throw TimeoutException('Server did not become ready within timeout');
      }
    } finally {
      sub.cancel();
    }
  }

  Future<PerformanceMetrics> getMetrics(int serverId) async {
    final process = _processes[serverId];
    if (process == null) {
      return PerformanceMetrics(
        cpuUsage: 0,
        memoryBytes: 0,
        tps: 20.0,
        timestamp: DateTime.now(),
      );
    }

    int memoryBytes = 0;
    double cpuUsage = 0.0;
    double tps = 20.0;

    try {
      final pid = process.pid;
      final result = await Process.run('tasklist', ['/FI', "PID eq $pid", '/FO', 'CSV']);
      final lines = result.stdout.toString().split('\n');
      if (lines.length > 1) {
        final parts = lines[1].split(',');
        if (parts.length >= 6) {
          final memStr = parts[5].replaceAll('KB', '').trim();
          memoryBytes = int.parse(memStr) * 1024;
        }
      }
    } catch (_) {}

    try {
      final serverDir = _serverDirs[serverId];
      if (serverDir != null) {
        final logFile = File(p.join(serverDir, 'logs', 'latest.log'));
        if (await logFile.exists()) {
          final lines = await logFile.readAsLines();
          for (final line in lines.reversed) {
            final match = RegExp(r'\[TPS:\s*([\d.]+)\]').firstMatch(line);
            if (match != null) {
              tps = double.parse(match.group(1) ?? '20.0');
              break;
            }
          }
        }
      }
    } catch (_) {}

    return PerformanceMetrics(
      cpuUsage: cpuUsage,
      memoryBytes: memoryBytes,
      tps: tps,
      timestamp: DateTime.now(),
    );
  }

  Stream<PerformanceMetrics> metricsStream(int serverId, {Duration interval = const Duration(seconds: 1)}) {
    final controller = StreamController<PerformanceMetrics>.broadcast();
    Timer.periodic(interval, (_) async {
      if (_processes.containsKey(serverId)) {
        controller.add(await getMetrics(serverId));
      }
    });
    return controller.stream;
  }

  Future<void> stop(int serverId, {bool force = false, Duration gracefulTimeout = const Duration(seconds: 30)}) async {
    final process = _processes[serverId];
    if (process == null) return;

    _notifyStateChange(serverId, ServerState(
      status: ServerStatus.stopping,
      uptimeSeconds: uptimeSeconds(serverId),
      startTime: _startTimes[serverId],
    ));

    if (force) {
      process.kill(ProcessSignal.sigkill);
    } else {
      // Try graceful shutdown first
      process.stdin.writeln('stop');
      
      try {
        await process.exitCode.timeout(gracefulTimeout);
      } on TimeoutException {
        // Force kill after timeout
        process.kill(ProcessSignal.sigkill);
      }
    }

    _processes.remove(serverId);
    _startTimes.remove(serverId);
    _serverDirs.remove(serverId);
    _restartAttempts.remove(serverId);
    stopAutoBackup(serverId);
    
    _notifyStateChange(serverId, ServerState(
      status: ServerStatus.stopped,
      uptimeSeconds: uptimeSeconds(serverId),
    ));
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
    _serverDirs.clear();
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
      startTime: _startTimes[serverId],
    );
  }

  void dispose() {
    killAll();
    stopAllAutoBackups();
    for (final controller in _consoleControllers.values) {
      controller.close();
    }
    _consoleControllers.clear();
  }

  String _stripAnsi(String text) {
    return text.replaceAll(RegExp(r'\x1B\[[0-9;]*[a-zA-Z]'), '');
  }
}