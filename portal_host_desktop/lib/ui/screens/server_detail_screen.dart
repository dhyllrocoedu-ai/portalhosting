import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:path/path.dart' as p;
import 'package:url_launcher/url_launcher.dart';
import '../../core/providers/process_provider.dart';
import '../../core/providers/server_provider.dart';
import '../../core/providers/backup_provider.dart';
import '../../core/providers/database_provider.dart';
import '../../core/providers/settings_provider.dart';
import '../../core/providers/playit_provider.dart';
import '../../core/server/process_manager.dart';
import '../../shared/models/server_config.dart';
import '../../shared/models/server_state.dart';
import '../widgets/properties_editor.dart';
import '../widgets/plugin_manager.dart';

class ServerDetailScreen extends ConsumerWidget {
  final String serverId;
  const ServerDetailScreen({super.key, required this.serverId});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final id = int.parse(serverId);
    final configAsync = ref.watch(serverListProvider);
    final state = ref.watch(activeServerProvider(id));
    final pm = ref.watch(processManagerProvider);

    return configAsync.when(
      loading: () => const Scaffold(
          body: Center(child: CircularProgressIndicator())),
      error: (e, _) => Scaffold(
        appBar: AppBar(title: const Text('Error')),
        body: Center(child: Text('$e')),
      ),
      data: (servers) {
        final config = servers.where((s) => s.id == id).firstOrNull;
        if (config == null) {
          return Scaffold(
            appBar: AppBar(title: const Text('Not Found')),
            body: const Center(child: Text('Server not found')),
          );
        }

        return _ServerDetailShell(
          config: config,
          state: state,
          pm: pm,
          serverId: id,
        );
      },
    );
  }
}

class _ServerDetailShell extends ConsumerStatefulWidget {
  final ServerConfig config;
  final ServerState state;
  final dynamic pm;
  final int serverId;

  const _ServerDetailShell({
    required this.config,
    required this.state,
    required this.pm,
    required this.serverId,
  });

  @override
  ConsumerState<_ServerDetailShell> createState() => _ServerDetailShellState();
}

class _ServerDetailShellState extends ConsumerState<_ServerDetailShell> {
  @override
  Widget build(BuildContext context) {
    final state = widget.state;

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.config.name),
        actions: [
          IconButton(
            icon: const Icon(Icons.cloud_download),
            tooltip: 'Export Server',
            onPressed: () => _exportServer(context),
          ),
          IconButton(
            icon: const Icon(Icons.backup),
            tooltip: 'Backups',
            onPressed: () => _showBackups(context),
          ),
          _buildStatusChip(state.status),
          const SizedBox(width: 8),
          _buildActionButton(context),
        ],
      ),
      body: _buildTabs(),
    );
  }

  Widget _buildStatusChip(ServerStatus status) {
    final color = switch (status) {
      ServerStatus.running => Colors.green,
      ServerStatus.starting => Colors.orange,
      ServerStatus.stopping => Colors.orange,
      ServerStatus.crashed => Colors.red,
      ServerStatus.stopped => Colors.grey,
    };
    return Chip(
      label: Text(status.name.toUpperCase(),
          style: const TextStyle(fontSize: 11, color: Colors.white)),
      backgroundColor: color,
      materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
      visualDensity: VisualDensity.compact,
    );
  }

  Widget _buildActionButton(BuildContext context) {
    final state = widget.state;
    final pm = widget.pm;

    switch (state.status) {
      case ServerStatus.running:
        return IconButton(
          icon: const Icon(Icons.stop),
          tooltip: 'Stop',
          onPressed: () {
            pm.stop(widget.config.id);
            pm.stopAutoBackup(widget.config.id);
            ref
                .read(activeServerProvider(widget.config.id).notifier)
                .setStatus(ServerStatus.stopped);
          },
        );
      case ServerStatus.stopped:
      case ServerStatus.crashed:
        return IconButton(
          icon: const Icon(Icons.play_arrow),
          tooltip: 'Start',
          onPressed: () async {
            ref
                .read(activeServerProvider(widget.config.id).notifier)
                .setStatus(ServerStatus.starting);
            try {
              final settings = ref.read(settingsProvider);
              final javaPath = widget.config.javaPath?.isNotEmpty == true
                  ? widget.config.javaPath
                  : settings.javaPath;
              
              await pm.start(
                widget.config.id,
                widget.config.jarPath,
                widget.config.javaArgs,
                widget.config.serverType,
                widget.config.port,
                widget.config.serverDir,
                javaPath,
                autoRestart: widget.config.autoRestart,
                maxRestartAttempts: 3,
              );
              
              // Wait for server to be ready
              await pm.waitForReady(widget.config.id, timeout: const Duration(seconds: 60));

              final interval = settings.backupIntervalMinutes;
              if (interval > 0) {
                pm.startAutoBackup(widget.config.id, Duration(minutes: interval));
              }
              
              ref
                  .read(activeServerProvider(widget.config.id).notifier)
                  .setStatus(ServerStatus.running);
            } catch (e) {
              ref
                  .read(activeServerProvider(widget.config.id).notifier)
                  .setStatus(ServerStatus.crashed);
              if (context.mounted) {
                ScaffoldMessenger.of(context)
                    .showSnackBar(SnackBar(content: Text('Failed: $e')));
              }
            }
          },
        );
      case ServerStatus.starting:
      case ServerStatus.stopping:
        return const SizedBox(
          width: 24,
          height: 24,
          child: CircularProgressIndicator(strokeWidth: 2),
        );
    }
  }

  Future<void> _exportServer(BuildContext context) async {
    try {
      final serverDir = Directory(widget.config.serverDir);
      final outputDir = Directory('${Directory.systemTemp.path}/server-exports');
      await outputDir.create(recursive: true);
      
      final zipPath = '${outputDir.path}/${widget.config.name}-export.zip';
      
      // Use the system's zip command to create the archive
      final result = await Process.run(
        'powershell',
        [
          '-Command',
          'Compress-Archive -Path "${serverDir.path}/*" -DestinationPath "$zipPath" -Force',
        ],
      );
      
      if (result.exitCode == 0 && context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Server exported to $zipPath')),
        );
      } else if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Export failed')),
        );
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error: $e')),
        );
      }
    }
  }

  void _showBackups(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => _BackupSheet(
        serverId: widget.serverId,
        serverDir: widget.config.serverDir,
      ),
    );
  }

  Widget _buildTabs() {
    return DefaultTabController(
      length: 8,
      child: Column(
        children: [
          TabBar(
            isScrollable: true,
            tabs: const [
              Tab(text: 'Console'),
              Tab(text: 'Files'),
              Tab(text: 'Properties'),
              Tab(text: 'Plugins'),
              Tab(text: 'Players'),
              Tab(text: 'Performance'),
              Tab(text: 'Network'),
              Tab(text: 'Logs'),
            ],
          ),
          Expanded(
            child: TabBarView(
              children: [
                _buildConsoleTab(),
                _buildFilesTab(),
                _buildPropertiesTab(),
                _buildPluginsTab(),
                _buildPlayersTab(),
                _buildPerformanceTab(),
                _buildNetworkTab(),
                _buildLogsTab(),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildConsoleTab() {
    return Column(
      children: [
        Expanded(
          child: Center(
            child: TextButton.icon(
              onPressed: () =>
                  context.push('/servers/${widget.serverId}/console'),
              icon: const Icon(Icons.open_in_full),
              label: const Text('Open Full Console'),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildFilesTab() {
    return Center(
      child: TextButton.icon(
        onPressed: () =>
            context.push('/servers/${widget.serverId}/files'),
        icon: const Icon(Icons.folder_open),
        label: const Text('Browse Files'),
      ),
    );
  }

  Widget _buildPropertiesTab() {
    return PropertiesEditor(serverId: widget.serverId);
  }

  Widget _buildPluginsTab() {
    final base = widget.config.serverDir;
    return PluginManager(
      pluginsDir: '$base/plugins',
      modsDir: '$base/mods',
      datapacksDir: '$base/datapacks',
    );
  }

  Widget _buildPlayersTab() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.all(16.0),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text('Whitelist', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
              FilledButton.icon(
                onPressed: () => _managePlayerList(context, 'whitelist.json'),
                icon: const Icon(Icons.person_add),
                label: const Text('Add Player'),
              ),
            ],
          ),
        ),
        const Divider(height: 1),
        _buildPlayerList('whitelist.json', 'whitelist'),
        const SizedBox(height: 16),
        Padding(
          padding: const EdgeInsets.all(16.0),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text('Ops', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
              FilledButton.icon(
                onPressed: () => _managePlayerList(context, 'ops.json'),
                icon: const Icon(Icons.person_add),
                label: const Text('Add Op'),
              ),
            ],
          ),
        ),
        const Divider(height: 1),
        _buildPlayerList('ops.json', 'ops'),
      ],
    );
  }

  Widget _buildPlayerList(String fileName, String listType) {
    final serverConfig = widget.config;
    final filePath = p.join(serverConfig.serverDir, fileName);
    
    return FutureBuilder<String>(
      future: File(filePath).readAsString().catchError((_) => '{}'),
      builder: (context, snapshot) {
        if (!snapshot.hasData) {
          return const Center(child: CircularProgressIndicator());
        }
        
        final content = snapshot.data ?? '{}';
        final players = <String>[];
        
        try {
          final json = content.replaceAll(RegExp(r'[{}"\[\]]'), '').split(',');
          for (final entry in json) {
            final parts = entry.split(':').where((p) => p.trim().isNotEmpty).toList();
            if (parts.isNotEmpty) {
              final name = parts.first.trim().replaceAll(RegExp(r'["\s]'), '');
              if (name.isNotEmpty && !players.contains(name)) {
                players.add(name);
              }
            }
          }
        } catch (_) {}
        
        return players.isEmpty
            ? const Center(child: Text('No players in list'))
            : ListView.builder(
                itemCount: players.length,
                itemBuilder: (ctx, i) => ListTile(
                  leading: const Icon(Icons.person),
                  title: Text(players[i]),
                  trailing: IconButton(
                    icon: const Icon(Icons.close),
                    onPressed: () => _removePlayer(filePath, players[i]),
                  ),
                ),
              );
      },
    );
  }

  void _managePlayerList(BuildContext context, String fileName) {
    final controller = TextEditingController();
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Add Player'),
        content: TextField(
          controller: controller,
          decoration: InputDecoration(
            labelText: 'Player name or UUID',
            border: OutlineInputBorder(),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () {
              final player = controller.text.trim();
              if (player.isNotEmpty) {
                _addPlayer(widget.config.serverDir, fileName, player);
              }
              Navigator.pop(ctx);
            },
            child: const Text('Add'),
          ),
        ],
      ),
    );
  }

  void _addPlayer(String serverDir, String fileName, String player) async {
    final filePath = p.join(serverDir, fileName);
    final file = File(filePath);
    
    try {
      final content = await file.readAsString();
      final playerEntry = '"$player"{}';
      final newContent = content.isEmpty || content == '{}'
          ? '[$playerEntry]'
          : content.replaceFirst(']', ',$playerEntry]');
      await file.writeAsString(newContent);
    } catch (e) {
      debugPrint('Error adding player: $e');
    }
  }

  void _removePlayer(String filePath, String player) async {
    final file = File(filePath);
    try {
      final content = await file.readAsString();
      final pattern = RegExp('"$player"\\s*:\\s*\\{?\\}?,?');
      final newContent = content
          .replaceAll(pattern, '')
          .replaceAll(RegExp(r'\[\s*,'), '[')
          .replaceAll(RegExp(r',\s*\]'), ']');
      await file.writeAsString(newContent);
    } catch (e) {
      debugPrint('Error removing player: $e');
    }
  }

  Widget _buildPerformanceTab() {
    final pm = widget.pm;
    return StreamBuilder<PerformanceMetrics>(
      stream: pm.metricsStream(widget.serverId, interval: const Duration(seconds: 2)),
      builder: (context, snapshot) {
        if (!snapshot.hasData) {
          return const Center(child: CircularProgressIndicator());
        }

        final metrics = snapshot.data!;
        final memoryMB = metrics.memoryBytes / (1024 * 1024);

        return SingleChildScrollView(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Real-time Metrics', style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 24),
              _buildMetricCard('Memory', '$memoryMB MB', Icons.memory),
              const SizedBox(height: 16),
              _buildMetricCard('TPS', metrics.tps.toStringAsFixed(1), Icons.speed),
              const SizedBox(height: 16),
              _buildMetricCard('CPU Usage', '${metrics.cpuUsage.toStringAsFixed(1)}%', Icons.show_chart),
              const SizedBox(height: 24),
              Text('Server Info', style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 16),
              _buildInfoRow('Started', widget.state.startTime?.toString() ?? 'Not running'),
              _buildInfoRow('Uptime', _calculateUptime(widget.state.startTime)),
            ],
          ),
        );
      },
    );
  }

  Widget _buildMetricCard(String label, String value, IconData icon) {
    return Card(
      child: ListTile(
        leading: Icon(icon),
        title: Text(label),
        trailing: Text(
          value,
          style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
        ),
      ),
    );
  }

  Widget _buildInfoRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(fontWeight: FontWeight.bold)),
          Text(value),
        ],
      ),
    );
  }

  String _calculateUptime(DateTime? startTime) {
    if (startTime == null) return '-';
    final duration = DateTime.now().difference(startTime);
    final days = duration.inDays;
    final hours = duration.inHours % 24;
    final minutes = duration.inMinutes % 60;
    return '${days}d ${hours}h ${minutes}m';
  }

  Widget _buildNetworkTab() {
    final tunnel = ref.watch(playitTunnelManagerProvider.select((map) => map[widget.serverId]));

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Network Settings', style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 24),
          _buildInfoRow('Server Port', widget.config.port.toString()),
          _buildInfoRow('Server Type', widget.config.serverType),
          const SizedBox(height: 24),
          Text('Playit Tunnel', style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Text('Playit.gg Tunnel',
                          style: Theme.of(context).textTheme.titleMedium
                              ?.copyWith(fontWeight: FontWeight.bold)),
                      const Spacer(),
                      if (tunnel != null)
                        Chip(
                          label: Text(tunnel.claimed ? 'Active' : 'Pending Claim',
                              style: const TextStyle(fontSize: 11)),
                          backgroundColor:
                              tunnel.claimed ? Colors.green.shade100 : Colors.orange.shade100,
                          labelStyle: TextStyle(
                              color: tunnel.claimed ? Colors.green.shade800 : Colors.orange.shade800),
                        ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  if (tunnel != null) ...[
                    if (tunnel.claimUrl != null) ...[
                      const Text('Claim URL:', style: TextStyle(fontWeight: FontWeight.bold)),
                      const SizedBox(height: 4),
                      SelectableText(tunnel.claimUrl!,
                          style: const TextStyle(fontFamily: 'monospace', fontSize: 12)),
                      const SizedBox(height: 8),
                      FilledButton.icon(
                        style: FilledButton.styleFrom(
                          backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
                          foregroundColor: Theme.of(context).colorScheme.onSecondaryContainer,
                        ),
                        onPressed: () => _launchUrl(Uri.parse(tunnel.claimUrl!)),
                        icon: const Icon(Icons.open_in_browser, size: 16),
                        label: const Text('Open Claim Page'),
                      ),
                      const SizedBox(height: 12),
                    ],
                    if (tunnel.tunnelUrl != null) ...[
                      const Text('Tunnel URL:', style: TextStyle(fontWeight: FontWeight.bold)),
                      const SizedBox(height: 4),
                      SelectableText(tunnel.tunnelUrl!,
                          style: const TextStyle(fontFamily: 'monospace', fontSize: 12)),
                      const SizedBox(height: 8),
                      FilledButton.icon(
                        style: FilledButton.styleFrom(
                          backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
                          foregroundColor: Theme.of(context).colorScheme.onSecondaryContainer,
                        ),
                        onPressed: () => _copyToClipboard(tunnel.tunnelUrl!),
                        icon: const Icon(Icons.copy, size: 16),
                        label: const Text('Copy Tunnel Address'),
                      ),
                      const SizedBox(height: 12),
                    ],
                    if (tunnel.claimed)
                      Text('Connection: ${tunnel.tunnelUrl ?? 'Ready'}',
                          style: TextStyle(color: Colors.green.shade700, fontWeight: FontWeight.bold))
                    else
                      const Text('Click the claim link above to activate the tunnel',
                          style: TextStyle(color: Colors.orange)),
                  ]
                  else ...[
                    const Text('Tunnel Status: Not started',
                        style: TextStyle(fontSize: 14, color: Colors.grey)),
                    const SizedBox(height: 12),
                    FilledButton.icon(
                      style: FilledButton.styleFrom(
                        backgroundColor: Theme.of(context).colorScheme.primaryContainer,
                        foregroundColor: Theme.of(context).colorScheme.onPrimaryContainer,
                      ),
                      onPressed: () => _startPlayitTunnel(),
                      icon: const Icon(Icons.play_arrow, size: 18),
                      label: const Text('Start Tunnel'),
                    ),
                  ],
                ],
              ),
            ),
          ),
          const SizedBox(height: 24),
          Text('Connection Info', style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _buildInfoRow('Local Address', 'localhost:${widget.config.port}'),
                  const SizedBox(height: 12),
                  _buildInfoRow('Tunnel Address',
                      tunnel?.tunnelUrl ?? (tunnel?.claimed == true ? 'Claim to activate' : 'Not started')),
                  const SizedBox(height: 12),
                  _buildInfoRow('Player Count', '0'),
                  const SizedBox(height: 12),
                  _buildInfoRow('Max Players', widget.config.maxPlayers.toString()),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _startPlayitTunnel() async {
    try {
      await ref.read(playitTunnelManagerProvider.notifier).start(
        serverId: widget.serverId,
        serverName: widget.config.name,
        port: widget.config.port,
        baseDir: widget.config.serverDir,
      );
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Playit tunnel started')),
        );
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to start tunnel: $e')),
        );
      }
    }
  }

  Future<void> _launchUrl(Uri url) async {
    try {
      await launchUrl(url, mode: LaunchMode.externalApplication);
    } catch (_) {
      _copyToClipboard(url.toString());
    }
  }

  void _copyToClipboard(String text) {
    Clipboard.setData(ClipboardData(text: text));
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Copied: $text')),
      );
    }
  }

  Widget _buildLogsTab() {
    final serverConfig = widget.config;
    final logFile = File(p.join(serverConfig.serverDir, 'logs', 'latest.log'));
    
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.all(16.0),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text('Server Logs', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
              FilledButton.tonal(
                onPressed: () => _exportLogs(context),
                child: const Text('Export'),
              ),
            ],
          ),
        ),
        const Divider(height: 1),
        Expanded(
          child: FutureBuilder<String>(
            future: logFile.readAsString().catchError((_) => ''),
            builder: (context, snapshot) {
              if (snapshot.connectionState == ConnectionState.waiting) {
                return const Center(child: CircularProgressIndicator());
              }
              
              if (!snapshot.hasData || snapshot.data!.isEmpty) {
                return const Center(child: Text('No logs available'));
              }
              
              final lines = snapshot.data!.split('\n').where((l) => l.trim().isNotEmpty).toList();
              
              return Scrollbar(
                child: ListView.builder(
                  itemCount: lines.length,
                  itemBuilder: (ctx, i) {
                    final line = lines[i];
                    return Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 1),
                      child: Text(
                        line,
                        style: const TextStyle(
                          fontFamily: 'monospace',
                          fontSize: 11,
                        ),
                      ),
                    );
                  },
                ),
              );
            },
          ),
        ),
      ],
    );
  }

  void _exportLogs(BuildContext context) async {
    final serverConfig = widget.config;
    final logFile = File(p.join(serverConfig.serverDir, 'logs', 'latest.log'));
    
    final exists = await logFile.exists();
    if (!exists) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('No logs to export')),
      );
      return;
    }
    
    final outputPath = '/tmp/server_${serverConfig.name}_logs.txt';
    try {
      await logFile.copy(outputPath);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Logs exported to $outputPath')),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Export failed: $e')),
      );
    }
  }
}

class _BackupSheet extends ConsumerWidget {
  final int serverId;
  final String serverDir;
  const _BackupSheet({required this.serverId, required this.serverDir});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final backupsAsync = ref.watch(backupListProvider(serverId));
    final mgr = ref.watch(backupManagerProvider);

    return DraggableScrollableSheet(
      initialChildSize: 0.6,
      minChildSize: 0.3,
      maxChildSize: 0.9,
      expand: false,
      builder: (ctx, scrollCtrl) => Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(12),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text('Backups',
                    style: TextStyle(
                        fontSize: 18, fontWeight: FontWeight.bold)),
                FilledButton.icon(
                  onPressed: () async {
                    final server = await ref
                        .read(databaseProvider)
                        .getServer(serverId);
                    if (server == null) return;
                    await mgr.createBackup(
                      serverId,
                      server.name,
                      '.',
                    );
                    ref.invalidate(backupListProvider(serverId));
                  },
                  icon: const Icon(Icons.add),
                  label: const Text('Backup Now'),
                ),
              ],
            ),
          ),
          const Divider(height: 1),
          Expanded(
            child: backupsAsync.when(
              loading: () =>
                  const Center(child: CircularProgressIndicator()),
              error: (e, _) => Center(child: Text('Error: $e')),
              data: (backups) {
                if (backups.isEmpty) {
                  return const Center(child: Text('No backups yet'));
                }
                return ListView.builder(
                  controller: scrollCtrl,
                  itemCount: backups.length,
                  itemBuilder: (ctx, i) {
                    final b = backups[i];
                    return ListTile(
                      leading: const Icon(Icons.archive),
                      title: Text(b.name),
                      subtitle: Text(
                          '${b.sizeFormatted}  •  ${b.createdAt.toString().substring(0, 19)}'),
                      trailing: IconButton(
                        icon: const Icon(Icons.restore,
                            color: Colors.orange),
                        tooltip: 'Restore',
                        onPressed: () async {
                          final confirmed =
                              await showDialog<bool>(
                            context: context,
                            builder: (dCtx) => AlertDialog(
                              title: const Text('Restore Backup'),
                              content: Text(
                                  'Restore ${b.name}? This will overwrite current files.'),
                              actions: [
                                TextButton(
                                    onPressed: () =>
                                        Navigator.pop(dCtx, false),
                                    child: const Text('Cancel')),
                                TextButton(
                                    onPressed: () =>
                                        Navigator.pop(dCtx, true),
                                    child: const Text('Restore')),
                              ],
                            ),
                          );
                          if (confirmed == true) {
                            try {
                              await mgr.restoreBackup(b, serverDir);
                              if (context.mounted) {
                                ScaffoldMessenger.of(context).showSnackBar(
                                  const SnackBar(
                                      content:
                                          Text('Backup restored')),
                                );
                              }
                            } catch (e) {
                              if (context.mounted) {
                                ScaffoldMessenger.of(context).showSnackBar(
                                  SnackBar(
                                      content:
                                          Text('Restore failed: $e')),
                                );
                              }
                            }
                          }
                        },
                      ),
                    );
                  },
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
