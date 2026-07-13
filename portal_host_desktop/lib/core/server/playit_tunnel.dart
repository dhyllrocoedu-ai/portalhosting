import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:path/path.dart' as p;
import 'package:http/http.dart' as http;

class PlayitTunnel {
  final int serverId;
  final String serverName;
  final int port;
  final String playitDir;
  final String agentPath;
  
  Process? _process;
  StreamController<String>? _outputController;
  Timer? _claimCheckTimer;
  String? _claimUrl;
  bool _claimed = false;
  String? _tunnelUrl;

  PlayitTunnel({
    required this.serverId,
    required this.serverName,
    required this.port,
    required String baseDir,
  }) : playitDir = p.join(baseDir, 'playit'),
       agentPath = p.join(baseDir, 'playit', _agentBinaryName());

  static String _agentBinaryName() {
    if (Platform.isWindows) return 'playit.exe';
    if (Platform.isMacOS) return 'playit';
    return 'playit';
  }

  Stream<String> get outputStream {
    _outputController ??= StreamController<String>.broadcast();
    return _outputController!.stream;
  }

  String? get claimUrl => _claimUrl;
  bool get claimed => _claimed;
  String? get tunnelUrl => _tunnelUrl;
  bool get isRunning => _process != null;

  Future<bool> start() async {
    if (_process != null) return true;

    await _ensureAgent();
    
    _outputController = StreamController<String>.broadcast();
    _claimed = false;
    _claimUrl = null;
    _tunnelUrl = null;

    try {
      _process = await Process.start(
        agentPath,
        ['--port', port.toString()],
        workingDirectory: playitDir,
        runInShell: true,
      );

      _process!.stdout
          .transform(utf8.decoder)
          .transform(const LineSplitter())
          .listen(_handleOutput);
          
      _process!.stderr
          .transform(utf8.decoder)
          .transform(const LineSplitter())
          .listen((line) => _handleOutput('[ERR] $line'));

      _process!.exitCode.then((code) {
        _process = null;
        _outputController?.add('[Tunnel stopped with code $code]');
      });

      _startClaimPolling();
      return true;
    } catch (e) {
      _outputController?.add('[Error starting tunnel: $e]');
      return false;
    }
  }

  void _handleOutput(String line) {
    _outputController?.add(line);
    
    // Parse claim URL from output
    final claimMatch = RegExp(r'claim\s*[=:]\s*(https?://[^\s]+)').firstMatch(line);
    if (claimMatch != null && _claimUrl == null) {
      _claimUrl = claimMatch.group(1);
      _outputController?.add('[CLAIM_URL] $_claimUrl');
    }

    // Parse tunnel URL
    final tunnelMatch = RegExp(r'(tcp|udp)://([^\s]+)').firstMatch(line);
    if (tunnelMatch != null && _tunnelUrl == null) {
      _tunnelUrl = tunnelMatch.group(0);
      _outputController?.add('[TUNNEL_URL] $_tunnelUrl');
    }

    // Check for claimed status
    if (line.contains('claimed') || line.contains('Claimed')) {
      _claimed = true;
      _stopClaimPolling();
    }
  }

  void _startClaimPolling() {
    _stopClaimPolling();
    _claimCheckTimer = Timer.periodic(const Duration(seconds: 3), (_) {
      if (!_claimed && _claimUrl != null) {
        // The claim URL is shown to user, they click it in browser
        // We just wait for the agent to report claimed status
      }
    });
  }

  void _stopClaimPolling() {
    _claimCheckTimer?.cancel();
    _claimCheckTimer = null;
  }

  Future<void> stop() async {
    _stopClaimPolling();
    _process?.kill(ProcessSignal.sigterm);
    try {
      await _process?.exitCode.timeout(const Duration(seconds: 5));
    } on TimeoutException {
      _process?.kill(ProcessSignal.sigkill);
    }
    _process = null;
    await _outputController?.close();
    _outputController = null;
  }

  Future<void> _ensureAgent() async {
    await Directory(playitDir).create(recursive: true);
    
    if (await File(agentPath).exists()) return;

    final downloadUrl = _getDownloadUrl();
    _outputController?.add('[Downloading Playit agent...]');
    
    try {
      final response = await http.get(Uri.parse(downloadUrl));
      if (response.statusCode == 200) {
        await File(agentPath).writeAsBytes(response.bodyBytes);
        if (!Platform.isWindows) {
          await Process.run('chmod', ['+x', agentPath]);
        }
        _outputController?.add('[Playit agent downloaded]');
      } else {
        throw Exception('Failed to download: ${response.statusCode}');
      }
    } catch (e) {
      _outputController?.add('[Error downloading agent: $e]');
      rethrow;
    }
  }

  String _getDownloadUrl() {
    final platform = Platform.operatingSystem;
    final arch = Platform.operatingSystemVersion.contains('arm64') || 
                   Platform.operatingSystemVersion.contains('aarch64') ? 'arm64' : 'amd64';
    
    if (platform == 'windows') {
      return 'https://github.com/playit-cloud/playit-agent/releases/latest/download/playit-windows-$arch.exe';
    } else if (platform == 'macos') {
      return 'https://github.com/playit-cloud/playit-agent/releases/latest/download/playit-darwin-$arch';
    } else {
      return 'https://github.com/playit-cloud/playit-agent/releases/latest/download/playit-linux-$arch';
    }
  }

  void dispose() {
    stop();
    _outputController?.close();
  }
}

class PlayitTunnelManager {
  final Map<int, PlayitTunnel> _tunnels = {};

  PlayitTunnel getTunnel(int serverId, String serverName, int port, String baseDir) {
    return _tunnels.putIfAbsent(serverId, () => PlayitTunnel(
      serverId: serverId,
      serverName: serverName,
      port: port,
      baseDir: baseDir,
    ));
  }

  Future<void> stopAll() async {
    for (final tunnel in _tunnels.values) {
      await tunnel.stop();
    }
    _tunnels.clear();
  }

  PlayitTunnel? get(int serverId) => _tunnels[serverId];
}