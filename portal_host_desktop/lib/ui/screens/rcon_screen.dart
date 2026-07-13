import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/providers/rcon_provider.dart';
import '../../core/providers/database_provider.dart';

class RconScreen extends ConsumerStatefulWidget {
  final String serverId;
  const RconScreen({super.key, required this.serverId});

  @override
  ConsumerState<RconScreen> createState() => _RconScreenState();
}

class _RconScreenState extends ConsumerState<RconScreen> {
  final _scrollCtrl = ScrollController();
  final _inputCtrl = TextEditingController();
  final List<String> _history = [];
  StreamSubscription<String>? _sub;
  bool _connecting = false;
  bool _connected = false;
  String _host = 'localhost';
  int _port = 25575;
  String _password = '';

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    final db = ref.read(databaseProvider);
    final server = await db.getServer(int.parse(widget.serverId));
    if (server != null) {
      final rconPort = await db.getProperties(int.parse(widget.serverId))
          .then((props) => props.where((p) => p.key == 'rcon.port').firstOrNull?.value);
      final rconPass = await db.getProperties(int.parse(widget.serverId))
          .then((props) => props.where((p) => p.key == 'rcon.password').firstOrNull?.value);
      
      setState(() {
        _host = 'localhost';
        _port = int.tryParse(rconPort ?? '25575') ?? 25575;
        _password = rconPass ?? '';
      });
    }
  }

  Future<void> _connect() async {
    if (_password.isEmpty) {
      _showError('RCON password not set in server.properties');
      return;
    }

    setState(() => _connecting = true);
    final success = await ref.read(rconManagerProvider.notifier).connect(
      int.parse(widget.serverId),
      _host,
      _port,
      _password,
    );
    setState(() {
      _connecting = false;
      _connected = success;
    });

    if (success) {
      _listenToOutput();
    } else {
      _showError('Failed to connect to RCON');
    }
  }

  void _listenToOutput() {
    _sub?.cancel();
    _sub = ref.read(rconManagerProvider.notifier).getOutputStream(int.parse(widget.serverId)).listen((line) {
      if (mounted) {
        setState(() {
          _history.add(line);
        });
        _scrollToBottom();
      }
    });
  }

  void _disconnect() {
    ref.read(rconManagerProvider.notifier).disconnect(int.parse(widget.serverId));
    _sub?.cancel();
    setState(() {
      _connected = false;
      _history.clear();
    });
  }

  void _send() {
    final cmd = _inputCtrl.text.trim();
    if (cmd.isEmpty || !_connected) return;
    
    ref.read(rconManagerProvider.notifier).sendCommand(int.parse(widget.serverId), cmd);
    _inputCtrl.clear();
  }

  void _scrollToBottom() {
    if (_scrollCtrl.hasClients) {
      _scrollCtrl.animateTo(
        _scrollCtrl.position.maxScrollExtent,
        duration: const Duration(milliseconds: 100),
        curve: Curves.easeOut,
      );
    }
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: Colors.red),
    );
  }

  @override
  void dispose() {
    _sub?.cancel();
    _scrollCtrl.dispose();
    _inputCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('RCON Console'),
        actions: [
          if (!_connected)
            FilledButton.tonal(
              onPressed: _connecting ? null : _connect,
              child: _connecting
                  ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                  : const Text('Connect'),
            )
          else
            IconButton(
              onPressed: _disconnect,
              icon: const Icon(Icons.link_off),
              tooltip: 'Disconnect',
            ),
          PopupMenuButton<String>(
            onSelected: (value) {
              if (value == 'settings') _showSettings();
            },
            itemBuilder: (_) => [
              const PopupMenuItem(value: 'settings', child: Text('Connection Settings')),
            ],
          ),
        ],
      ),
      body: Column(
        children: [
          if (!_connected)
            Container(
              color: theme.colorScheme.surfaceContainerHighest,
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Not Connected', style: theme.textTheme.titleMedium),
                  const SizedBox(height: 8),
                  Text('RCON allows remote administration of the server.',
                      style: theme.textTheme.bodySmall?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant)),
                  const SizedBox(height: 16),
                  Row(
                    children: [
                      Expanded(
                        child: TextFormField(
                          initialValue: _host,
                          decoration: const InputDecoration(
                            labelText: 'Host',
                            border: OutlineInputBorder(),
                            isDense: true,
                          ),
                          onChanged: (v) => _host = v,
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: TextFormField(
                          initialValue: _port.toString(),
                          decoration: const InputDecoration(
                            labelText: 'Port',
                            border: OutlineInputBorder(),
                            isDense: true,
                          ),
                          keyboardType: TextInputType.number,
                          onChanged: (v) => _port = int.tryParse(v) ?? 25575,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  TextFormField(
                    initialValue: _password,
                    decoration: const InputDecoration(
                      labelText: 'Password',
                      border: OutlineInputBorder(),
                      isDense: true,
                    ),
                    obscureText: true,
                    onChanged: (v) => _password = v,
                  ),
                  const SizedBox(height: 8),
                  Text('Set rcon.password and rcon.port in server.properties, then enable RCON with enable-rcon=true',
                      style: theme.textTheme.bodySmall?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant)),
                ],
              ),
            ),
          Expanded(
            child: _connected
                ? _history.isEmpty
                    ? Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(Icons.terminal, size: 48, color: theme.colorScheme.onSurfaceVariant),
                            const SizedBox(height: 16),
                            Text('Connected to RCON',
                                style: theme.textTheme.titleMedium),
                            const SizedBox(height: 8),
                            Text('Type commands below to administer the server',
                                style: theme.textTheme.bodySmall?.copyWith(
                                    color: theme.colorScheme.onSurfaceVariant)),
                          ],
                        ),
                      )
                    : ListView.builder(
                        controller: _scrollCtrl,
                        itemCount: _history.length,
                        itemBuilder: (ctx, i) {
                          final line = _history[i];
                          return Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 1),
                            child: SelectableText(
                              line,
                              style: const TextStyle(
                                fontFamily: 'monospace',
                                fontSize: 12,
                              ),
                            ),
                          );
                        },
                      )
                : const SizedBox(),
          ),
          Container(
            color: theme.colorScheme.surfaceContainerHighest,
            padding: const EdgeInsets.fromLTRB(8, 4, 8, 4),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _inputCtrl,
                    decoration: InputDecoration(
                      hintText: _connected ? 'Type a command...' : 'Connect first',
                      border: const OutlineInputBorder(),
                      isDense: true,
                      contentPadding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
                    ),
                    enabled: _connected,
                    onSubmitted: (_) => _send(),
                  ),
                ),
                const SizedBox(width: 8),
                IconButton(
                  icon: const Icon(Icons.send),
                  onPressed: _connected ? _send : null,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  void _showSettings() {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('RCON Settings'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextFormField(
              initialValue: _host,
              decoration: const InputDecoration(
                labelText: 'Host',
                border: OutlineInputBorder(),
              ),
              onChanged: (v) => _host = v,
            ),
            const SizedBox(height: 8),
            TextFormField(
              initialValue: _port.toString(),
              decoration: const InputDecoration(
                labelText: 'Port',
                border: OutlineInputBorder(),
              ),
              keyboardType: TextInputType.number,
              onChanged: (v) => _port = int.tryParse(v) ?? 25575,
            ),
            const SizedBox(height: 8),
            TextFormField(
              initialValue: _password,
              decoration: const InputDecoration(
                labelText: 'Password',
                border: OutlineInputBorder(),
              ),
              obscureText: true,
              onChanged: (v) => _password = v,
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () {
              Navigator.pop(ctx);
              if (_connected) _disconnect();
              _connect();
            },
            child: const Text('Reconnect'),
          ),
        ],
      ),
    );
  }
}