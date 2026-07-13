import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/providers/server_provider.dart';
import '../../core/providers/process_provider.dart';
import '../../core/providers/settings_provider.dart';
import '../../shared/models/server_state.dart';
import '../../shared/models/server_config.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  List<String> _consoleLines = [];
  StreamSubscription<String>? _consoleSub;

  @override
  void initState() {
    super.initState();
    ref.listen<int?>(selectedServerIdProvider, (prev, next) {
      if (next != prev) _subscribeToConsole(next);
    });
    _subscribeToConsole(ref.read(selectedServerIdProvider));
  }

  @override
  void dispose() {
    _consoleSub?.cancel();
    super.dispose();
  }

  void _subscribeToConsole(int? serverId) {
    _consoleSub?.cancel();
    _consoleLines = [];
    if (serverId == null) return;
    final pm = ref.read(processManagerProvider);
    _consoleSub = pm.consoleStream(serverId).listen((line) {
      if (!mounted) return;
      setState(() {
        _consoleLines.add(line);
        if (_consoleLines.length > 200) {
          _consoleLines = _consoleLines.sublist(_consoleLines.length - 100);
        }
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final serversAsync = ref.watch(serverListProvider);
    final selectedServerId = ref.watch(selectedServerIdProvider);
    final servers = serversAsync.asData?.value ?? [];

    if (selectedServerId != null) {
      final selectedServer = servers.where((s) => s.id == selectedServerId).firstOrNull;
      if (selectedServer != null) {
        return _buildServerDashboard(context, theme, selectedServer);
      }
    }

    return _buildMainDashboard(context, theme, servers);
  }

  Widget _buildMainDashboard(BuildContext context, ThemeData theme, List<ServerConfig> servers) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Portal Host'),
        centerTitle: false,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Center(
            child: Column(
              children: [
                Image.asset('assets/icons/portal_host_icon.png',
                    width: 80, height: 80, errorBuilder: (_, _, _) =>
                        const Icon(Icons.dns, size: 80)),
                const SizedBox(height: 8),
                Text('Portal Host',
                    style: theme.textTheme.headlineLarge),
                const SizedBox(height: 4),
                Text('Host. Manage. Play.',
                    style: theme.textTheme.bodyMedium
                        ?.copyWith(color: theme.colorScheme.primary)),
              ],
            ),
          ),
          const SizedBox(height: 24),
          Row(
            children: [
              Expanded(
                child: _StatCard(
                  icon: Icons.dns,
                  label: 'Servers',
                  value: '${servers.length}',
                  color: theme.colorScheme.primary,
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: _StatCard(
                  icon: Icons.play_circle,
                  label: 'Running',
                  value: '${servers.where((s) => ref.watch(activeServerProvider(s.id)).status == ServerStatus.running).length}',
                  color: Colors.green,
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Quick Actions',
                      style: theme.textTheme.titleMedium),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: ActionChip(
                          avatar: const Icon(Icons.dns),
                          label: const Text('Servers'),
                          onPressed: () => context.go('/servers'),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: ActionChip(
                          avatar: const Icon(Icons.add),
                          label: const Text('New Server'),
                          onPressed: () => context.go('/servers/create'),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: ActionChip(
                          avatar: const Icon(Icons.settings),
                          label: const Text('Settings'),
                          onPressed: () => context.go('/settings'),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildServerDashboard(BuildContext context, ThemeData theme, ServerConfig server) {
    final serverState = ref.watch(activeServerProvider(server.id));

    return Scaffold(
      appBar: AppBar(
        title: Text(server.name),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () {
            ref.read(selectedServerIdProvider.notifier).select(null);
          },
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.delete_outline),
            onPressed: () => _confirmDelete(context),
            tooltip: 'Delete Server',
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            color: serverState.status == ServerStatus.running
                ? Colors.green.withValues(alpha: 0.1)
                : serverState.status == ServerStatus.crashed
                    ? Colors.red.withValues(alpha: 0.1)
                    : theme.colorScheme.surfaceContainerHighest,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                children: [
                  Icon(
                    serverState.status == ServerStatus.running
                        ? Icons.play_circle_fill
                        : serverState.status == ServerStatus.crashed
                            ? Icons.error
                            : Icons.stop_circle,
                    size: 48,
                    color: serverState.status == ServerStatus.running
                        ? Colors.green
                        : serverState.status == ServerStatus.crashed
                            ? Colors.red
                            : theme.colorScheme.onSurfaceVariant,
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          serverState.status == ServerStatus.running ? 'Running' :
                          serverState.status == ServerStatus.crashed ? 'Crashed' :
                          serverState.status == ServerStatus.starting ? 'Starting...' :
                          serverState.status == ServerStatus.stopping ? 'Stopping...' : 'Stopped',
                          style: theme.textTheme.titleLarge?.copyWith(
                            color: serverState.status == ServerStatus.running
                                ? Colors.green
                                : serverState.status == ServerStatus.crashed
                                    ? Colors.red
                                    : theme.colorScheme.onSurface,
                          ),
                        ),
                        Text(
                          'Version: ${server.mcVersion ?? 'Unknown'}  \u2022  Port: ${server.port}',
                          style: theme.textTheme.bodyMedium?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),

          const SizedBox(height: 16),

          Row(
            children: [
              Expanded(
                child: _StatCard(
                  icon: Icons.memory,
                  label: 'Memory',
                  value: server.javaArgs.contains('-Xmx')
                      ? server.javaArgs.substring(server.javaArgs.indexOf('-Xmx') + 4)
                          .split(' ')[0].replaceAll('G', 'G').replaceAll('M', 'M')
                      : 'N/A',
                  color: Colors.blue,
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: _StatCard(
                  icon: Icons.people,
                  label: 'Max Players',
                  value: '${server.maxPlayers}',
                  color: Colors.orange,
                ),
              ),
            ],
          ),

          const SizedBox(height: 16),

          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text('Console', style: theme.textTheme.titleMedium),
                      TextButton(
                        onPressed: () => context.go('/servers/${server.id}/console'),
                        child: const Text('Open Console'),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  _buildConsolePreview(theme),
                ],
              ),
            ),
          ),

          const SizedBox(height: 16),

          Row(
            children: [
              Expanded(
                child: FilledButton.icon(
                  icon: Icon(
                    serverState.status == ServerStatus.running
                        ? Icons.stop
                        : Icons.play_arrow,
                  ),
                  label: Text(
                    serverState.status == ServerStatus.running
                        ? 'Stop Server'
                        : 'Start Server',
                  ),
                  onPressed: serverState.status == ServerStatus.starting || serverState.status == ServerStatus.stopping
                      ? null
                      : () => _toggleServer(),
                  style: FilledButton.styleFrom(
                    backgroundColor: serverState.status == ServerStatus.running
                        ? Colors.red
                        : Colors.green,
                    padding: const EdgeInsets.symmetric(vertical: 16),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: OutlinedButton.icon(
                  icon: const Icon(Icons.restart_alt),
                  label: const Text('Restart'),
                  onPressed: serverState.status == ServerStatus.starting || serverState.status == ServerStatus.stopping
                      ? null
                      : () => _restartServer(),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildConsolePreview(ThemeData theme) {
    final displayLines = _consoleLines.length >= 10
        ? _consoleLines.sublist(_consoleLines.length - 10)
        : _consoleLines;

    return Container(
      height: 150,
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(
        color: Colors.black87,
        borderRadius: BorderRadius.circular(8),
      ),
      child: displayLines.isEmpty
          ? const Text(
              'Console preview - tap "Open Console" for full view',
              style: TextStyle(
                fontFamily: 'monospace',
                color: Colors.green,
                fontSize: 12,
              ),
            )
          : ListView.builder(
              itemCount: displayLines.length,
              itemBuilder: (_, i) => Text(
                displayLines[i],
                style: const TextStyle(
                  fontFamily: 'monospace',
                  color: Colors.green,
                  fontSize: 11,
                ),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ),
    );
  }

  Future<void> _toggleServer() async {
    final serverId = ref.read(selectedServerIdProvider);
    if (serverId == null) return;
    final servers = ref.read(serverListProvider).asData?.value ?? [];
    final server = servers.where((s) => s.id == serverId).firstOrNull;
    if (server == null) return;

    final pm = ref.read(processManagerProvider);
    final state = ref.read(activeServerProvider(serverId));

    if (state.status == ServerStatus.running) {
      ref.read(activeServerProvider(serverId).notifier).setStatus(ServerStatus.stopping);
      try {
        await pm.stop(serverId);
        ref.read(activeServerProvider(serverId).notifier).setStatus(ServerStatus.stopped);
        pm.stopAutoBackup(serverId);
      } catch (e) {
        if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Stop failed: $e')));
      }
    } else if (state.status == ServerStatus.stopped || state.status == ServerStatus.crashed) {
      ref.read(activeServerProvider(serverId).notifier).setStatus(ServerStatus.starting);
      try {
        final settings = ref.read(settingsProvider);
        final javaPath = server.javaPath?.isNotEmpty == true ? server.javaPath : settings.javaPath;
        await pm.start(
          serverId,
          server.jarPath,
          server.javaArgs,
          server.serverType,
          server.port,
          server.serverDir,
          javaPath,
          autoRestart: server.autoRestart,
          maxRestartAttempts: 3,
          serverName: server.name,
        );
        await pm.waitForReady(serverId);
        ref.read(activeServerProvider(serverId).notifier).setStatus(ServerStatus.running);

        final interval = settings.backupIntervalMinutes;
        if (interval > 0) {
          pm.startAutoBackup(serverId, Duration(minutes: interval));
        }
      } catch (e) {
        ref.read(activeServerProvider(serverId).notifier).setStatus(ServerStatus.crashed);
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Failed to start: $e')));
        }
      }
    }
  }

  Future<void> _restartServer() async {
    final serverId = ref.read(selectedServerIdProvider);
    if (serverId == null) return;
    final servers = ref.read(serverListProvider).asData?.value ?? [];
    final server = servers.where((s) => s.id == serverId).firstOrNull;
    if (server == null) return;

    final pm = ref.read(processManagerProvider);

    ref.read(activeServerProvider(serverId).notifier).setStatus(ServerStatus.stopping);
    try {
      await pm.stop(serverId);
      pm.stopAutoBackup(serverId);
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Stop failed: $e')));
      return;
    }

    ref.read(activeServerProvider(serverId).notifier).setStatus(ServerStatus.starting);
    try {
      final settings = ref.read(settingsProvider);
      final javaPath = server.javaPath?.isNotEmpty == true ? server.javaPath : settings.javaPath;
      await pm.start(
        serverId,
        server.jarPath,
        server.javaArgs,
        server.serverType,
        server.port,
        server.serverDir,
        javaPath,
        autoRestart: server.autoRestart,
        maxRestartAttempts: 3,
        serverName: server.name,
      );
      await pm.waitForReady(serverId);
      ref.read(activeServerProvider(serverId).notifier).setStatus(ServerStatus.running);

      final interval = settings.backupIntervalMinutes;
      if (interval > 0) {
        pm.startAutoBackup(serverId, Duration(minutes: interval));
      }
    } catch (e) {
      ref.read(activeServerProvider(serverId).notifier).setStatus(ServerStatus.crashed);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Restart failed: $e')));
      }
    }
  }

  void _confirmDelete(BuildContext context) {
    showDialog(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Delete Server'),
        content: const Text('Are you sure you want to delete this server? This action cannot be undone.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () async {
              final serverId = ref.read(selectedServerIdProvider);
              if (serverId == null) {
                Navigator.pop(dialogContext);
                return;
              }
              final pm = ref.read(processManagerProvider);
              if (pm.isRunning(serverId)) {
                await pm.stop(serverId, force: true);
              }
              ref.read(serverListProvider.notifier).deleteServer(serverId);
              ref.read(selectedServerIdProvider.notifier).select(null);
              if (dialogContext.mounted) Navigator.pop(dialogContext);
            },
            style: FilledButton.styleFrom(backgroundColor: Colors.red),
            child: const Text('Delete'),
          ),
        ],
      ),
    );
  }
}

class _StatCard extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;
  final Color color;

  const _StatCard({
    required this.icon,
    required this.label,
    required this.value,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Icon(icon, color: color, size: 28),
            const SizedBox(height: 8),
            Text(value,
                style: theme.textTheme.headlineMedium
                    ?.copyWith(color: color)),
            Text(label, style: theme.textTheme.bodySmall),
          ],
        ),
      ),
    );
  }
}
