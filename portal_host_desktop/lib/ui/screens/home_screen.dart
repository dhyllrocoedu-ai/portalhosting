import 'dart:async';
import 'dart:io';
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
  bool _serverDropdownExpanded = false;
  OverlayEntry? _dropdownOverlay;

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
    _dropdownOverlay?.remove();
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

  void _showServerDropdown(BuildContext context, List<ServerConfig> servers, int? selectedId) {
    final renderBox = context.findRenderObject() as RenderBox?;
    if (renderBox == null) return;

    final overlay = Overlay.of(context);
    final size = renderBox.size;

    _dropdownOverlay?.remove();

    _dropdownOverlay = OverlayEntry(
      builder: (context) => Positioned(
        width: size.width,
        child: CompositedTransformFollower(
          link: _dropdownLink,
          showWhenUnlinked: false,
          offset: Offset(0, size.height + 4),
          child: Material(
            elevation: 8,
            borderRadius: BorderRadius.circular(12),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxHeight: 300),
              child: _ServerDropdownList(
                servers: servers,
                selectedId: ref.read(selectedServerIdProvider),
                onSelect: (id) {
                  ref.read(selectedServerIdProvider.notifier).select(id);
                  _dropdownOverlay?.remove();
                  _dropdownOverlay = null;
                  setState(() => _serverDropdownExpanded = false);
                },
              ),
            ),
          ),
        ),
      ),
    );

    overlay.insert(_dropdownOverlay!);
  }

  final LayerLink _dropdownLink = LayerLink();

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
          _buildServerSelector(theme, context),
          const SizedBox(height: 24),
          _buildMainStatsRow(theme, servers),
          const SizedBox(height: 16),
          _buildQuickActions(theme, context),
        ],
      ),
    );
  }

  Widget _buildServerSelector(ThemeData theme, BuildContext context) {
    final serversAsync = ref.watch(serverListProvider);
    final servers = serversAsync.asData?.value ?? [];

    return CompositedTransformTarget(
      link: _dropdownLink,
      child: Card(
        elevation: 2,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        child: InkWell(
          onTap: () {
            final servers = ref.read(serverListProvider).asData?.value ?? [];
            if (servers.isNotEmpty) {
              setState(() => _serverDropdownExpanded = !_serverDropdownExpanded);
              if (_serverDropdownExpanded) {
                _showServerDropdown(context, servers, ref.read(selectedServerIdProvider));
              } else {
                _dropdownOverlay?.remove();
                _dropdownOverlay = null;
              }
            }
          },
          borderRadius: BorderRadius.circular(12),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                _buildServerAvatar(theme),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        _getSelectedServerName(servers),
                        style: theme.textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w600,
                        ),
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 4),
                      Text(
                        _getSelectedServerSubtitle(servers),
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 12),
                Icon(
                  _serverDropdownExpanded
                      ? Icons.keyboard_arrow_up
                      : Icons.keyboard_arrow_down,
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  String _getSelectedServerName(List<ServerConfig> servers) {
    final selectedId = ref.watch(selectedServerIdProvider);
    if (selectedId == null) return 'No server selected';
    final server = servers.where((s) => s.id == selectedId).firstOrNull;
    return server?.name ?? 'Unknown Server';
  }

  String _getSelectedServerSubtitle(List<ServerConfig> servers) {
    final selectedId = ref.watch(selectedServerIdProvider);
    if (selectedId == null) {
      return servers.isEmpty ? 'Create your first server' : 'Tap to select a server';
    }
    final server = servers.where((s) => s.id == selectedId).firstOrNull;
    if (server == null) return 'Server not found';
    final state = ref.watch(activeServerProvider(server.id));
    return '${server.mcVersion ?? 'Unknown version'} \u2022 Port: ${server.port} \u2022 ${state.status.name}';
  }

  Widget _buildServerAvatar(ThemeData theme) {
    final selectedId = ref.watch(selectedServerIdProvider);
    final servers = ref.watch(serverListProvider).asData?.value ?? [];
    final server = servers.where((s) => s.id == selectedId).firstOrNull;

    if (server == null || server.iconPath == null) {
      return CircleAvatar(
        radius: 20,
        backgroundColor: theme.colorScheme.surfaceContainerHighest,
        child: Icon(Icons.dns, color: theme.colorScheme.onSurfaceVariant),
      );
    }
    return CircleAvatar(
      radius: 20,
      backgroundImage: FileImage(File(server.iconPath!)),
      backgroundColor: theme.colorScheme.surfaceContainerHighest,
    );
  }

  Widget _buildMainStatsRow(ThemeData theme, List<ServerConfig> servers) {
    final runningCount = servers.where((s) =>
        ref.watch(activeServerProvider(s.id)).status == ServerStatus.running).length;

    return Row(
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
            value: '$runningCount',
            color: Colors.green,
          ),
        ),
      ],
    );
  }

  Widget _buildQuickActions(ThemeData theme, BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Quick Actions', style: theme.textTheme.titleMedium),
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
            onPressed: () => _confirmDelete(context, server),
            tooltip: 'Delete Server',
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _buildStatusCard(theme, server, serverState),
          const SizedBox(height: 16),
          _buildDashboardStatsRow(theme, server, serverState),
          const SizedBox(height: 16),
          _buildActionsRow(theme, server, serverState),
          const SizedBox(height: 16),
          _buildConsoleCard(theme, server),
        ],
      ),
    );
  }

  Widget _buildStatusCard(ThemeData theme, ServerConfig server, ServerState state) {
    Color statusColor;
    IconData statusIcon;
    String statusText;

    switch (state.status) {
      case ServerStatus.running:
        statusColor = Colors.green;
        statusIcon = Icons.play_circle_fill;
        statusText = 'Running';
        break;
      case ServerStatus.crashed:
        statusColor = Colors.red;
        statusIcon = Icons.error;
        statusText = 'Crashed';
        break;
      case ServerStatus.starting:
        statusColor = Colors.orange;
        statusIcon = Icons.hourglass_empty;
        statusText = 'Starting...';
        break;
      case ServerStatus.stopping:
        statusColor = Colors.orange;
        statusIcon = Icons.stop_circle;
        statusText = 'Stopping...';
        break;
      default:
        statusColor = theme.colorScheme.onSurfaceVariant;
        statusIcon = Icons.stop_circle;
        statusText = 'Stopped';
    }

    return Card(
      color: statusColor.withValues(alpha: 0.1),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Icon(statusIcon, size: 48, color: statusColor),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    statusText,
                    style: theme.textTheme.titleLarge?.copyWith(color: statusColor),
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
    );
  }

  Widget _buildDashboardStatsRow(ThemeData theme, ServerConfig server, ServerState state) {
    return Row(
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
    );
  }

  Widget _buildActionsRow(ThemeData theme, ServerConfig server, ServerState state) {
    final isStartingOrStopping = state.status == ServerStatus.starting || state.status == ServerStatus.stopping;

    return Row(
      children: [
        Expanded(
          child: FilledButton.icon(
            icon: Icon(state.status == ServerStatus.running ? Icons.stop : Icons.play_arrow),
            label: Text(state.status == ServerStatus.running ? 'Stop Server' : 'Start Server'),
            onPressed: isStartingOrStopping ? null : () => _toggleServer(server),
            style: FilledButton.styleFrom(
              backgroundColor: state.status == ServerStatus.running ? Colors.red : Colors.green,
              padding: const EdgeInsets.symmetric(vertical: 16),
            ),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: OutlinedButton.icon(
            icon: const Icon(Icons.restart_alt),
            label: const Text('Restart'),
            onPressed: isStartingOrStopping ? null : () => _restartServer(server),
          ),
        ),
      ],
    );
  }

  Future<void> _toggleServer(ServerConfig server) async {
    final pm = ref.read(processManagerProvider);
    final state = ref.read(activeServerProvider(server.id));

    if (state.status == ServerStatus.running) {
      ref.read(activeServerProvider(server.id).notifier).setStatus(ServerStatus.stopping);
      try {
        await pm.stop(server.id);
        pm.stopAutoBackup(server.id);
        ref.read(activeServerProvider(server.id).notifier).setStatus(ServerStatus.stopped);
      } catch (e) {
        if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Stop failed: $e')));
      }
    } else if (state.status == ServerStatus.stopped || state.status == ServerStatus.crashed) {
      ref.read(activeServerProvider(server.id).notifier).setStatus(ServerStatus.starting);
      try {
        final settings = ref.read(settingsProvider);
        final javaPath = server.javaPath?.isNotEmpty == true ? server.javaPath : settings.javaPath;
        await pm.start(
          server.id,
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
        await pm.waitForReady(server.id);
        ref.read(activeServerProvider(server.id).notifier).setStatus(ServerStatus.running);

        final interval = settings.backupIntervalMinutes;
        if (interval > 0) {
          pm.startAutoBackup(server.id, Duration(minutes: interval));
        }
      } catch (e) {
        ref.read(activeServerProvider(server.id).notifier).setStatus(ServerStatus.crashed);
        if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Failed to start: $e')));
      }
    }
  }

  Future<void> _restartServer(ServerConfig server) async {
    final pm = ref.read(processManagerProvider);

    ref.read(activeServerProvider(server.id).notifier).setStatus(ServerStatus.stopping);
    try {
      await pm.stop(server.id);
      pm.stopAutoBackup(server.id);
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Stop failed: $e')));
      return;
    }

    ref.read(activeServerProvider(server.id).notifier).setStatus(ServerStatus.starting);
    try {
      final settings = ref.read(settingsProvider);
      final javaPath = server.javaPath?.isNotEmpty == true ? server.javaPath : settings.javaPath;
      await pm.start(
        server.id,
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
      await pm.waitForReady(server.id);
      ref.read(activeServerProvider(server.id).notifier).setStatus(ServerStatus.running);

      final interval = settings.backupIntervalMinutes;
      if (interval > 0) {
        pm.startAutoBackup(server.id, Duration(minutes: interval));
      }
    } catch (e) {
      ref.read(activeServerProvider(server.id).notifier).setStatus(ServerStatus.crashed);
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Restart failed: $e')));
    }
  }

  Widget _buildConsoleCard(ThemeData theme, ServerConfig server) {
    final displayLines = _consoleLines.length >= 10
        ? _consoleLines.sublist(_consoleLines.length - 10)
        : _consoleLines;

    return Card(
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
            Container(
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
            ),
          ],
        ),
      ),
    );
  }

  void _confirmDelete(BuildContext context, ServerConfig server) {
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
              final pm = ref.read(processManagerProvider);
              if (pm.isRunning(server.id)) {
                await pm.stop(server.id, force: true);
              }
              ref.read(serverListProvider.notifier).deleteServer(server.id);
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

class _ServerDropdownList extends StatelessWidget {
  final List<ServerConfig> servers;
  final int? selectedId;
  final ValueChanged<int> onSelect;

  const _ServerDropdownList({
    required this.servers,
    required this.selectedId,
    required this.onSelect,
  });

  @override
  Widget build(BuildContext context) {
    return ListView.separated(
      shrinkWrap: true,
      padding: const EdgeInsets.symmetric(vertical: 8),
      itemCount: servers.length,
      separatorBuilder: (_, _) => const Divider(height: 1, indent: 16, endIndent: 16),
      itemBuilder: (_, i) {
        final server = servers[i];
        final isSelected = server.id == selectedId;
        return ListTile(
          leading: CircleAvatar(
            radius: 16,
            backgroundColor: isSelected ? Theme.of(context).colorScheme.primaryContainer : Theme.of(context).colorScheme.surfaceContainerHighest,
            child: server.iconPath != null
                ? CircleAvatar(radius: 14, backgroundImage: FileImage(File(server.iconPath!)))
                : Icon(Icons.dns, size: 18, color: Theme.of(context).colorScheme.onSurfaceVariant),
          ),
          title: Text(server.name, style: TextStyle(fontWeight: isSelected ? FontWeight.w600 : FontWeight.normal)),
          subtitle: Text('${server.mcVersion ?? 'Unknown'} \u2022 Port: ${server.port}'),
          trailing: isSelected ? Icon(Icons.check, color: Theme.of(context).colorScheme.primary) : null,
          selected: isSelected,
          onTap: () => onSelect(server.id),
        );
      },
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
            Text(value, style: theme.textTheme.headlineMedium?.copyWith(color: color)),
            Text(label, style: theme.textTheme.bodySmall),
          ],
        ),
      ),
    );
  }
}