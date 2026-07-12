import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/providers/process_provider.dart';
import '../../core/providers/server_provider.dart';
import '../../shared/models/server_config.dart';
import '../../shared/models/server_state.dart';

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
      loading: () => const Scaffold(body: Center(child: CircularProgressIndicator())),
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

        return Scaffold(
          appBar: AppBar(
            title: Text(config.name),
            actions: [
              _buildStatusChip(state.status),
              const SizedBox(width: 8),
              _buildActionButton(context, ref, config, state, pm),
            ],
          ),
          body: _buildTabs(context, ref, config, id),
        );
      },
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

  Widget _buildActionButton(BuildContext context, WidgetRef ref,
      ServerConfig config, ServerState state, dynamic pm) {
    switch (state.status) {
      case ServerStatus.running:
        return IconButton(
          icon: const Icon(Icons.stop),
          tooltip: 'Stop',
          onPressed: () {
            pm.stop(config.id);
            ref.read(activeServerProvider(config.id).notifier).setStatus(ServerStatus.stopped);
          },
        );
      case ServerStatus.stopped:
      case ServerStatus.crashed:
        return IconButton(
          icon: const Icon(Icons.play_arrow),
          tooltip: 'Start',
          onPressed: () async {
            ref.read(activeServerProvider(config.id).notifier).setStatus(ServerStatus.starting);
            try {
              await pm.start(
                config.id,
                config.jarPath,
                config.javaArgs,
                config.serverType,
                config.port,
                '.',
                null,
              );
              ref.read(activeServerProvider(config.id).notifier).setStatus(ServerStatus.running);
            } catch (e) {
              ref.read(activeServerProvider(config.id).notifier).setStatus(ServerStatus.crashed);
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

  Widget _buildTabs(BuildContext context, WidgetRef ref, ServerConfig config, int id) {
    return DefaultTabController(
      length: 3,
      child: Column(
        children: [
          TabBar(
            tabs: const [
              Tab(text: 'Console'),
              Tab(text: 'Files'),
              Tab(text: 'Properties'),
            ],
          ),
          Expanded(
            child: TabBarView(
              children: [
                _buildConsoleTab(context, ref, id),
                _buildFilesTab(context, ref, id),
                _buildPropertiesTab(context, ref, config),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildConsoleTab(BuildContext context, WidgetRef ref, int id) {
    return Column(
      children: [
        Expanded(
          child: Center(
            child: TextButton.icon(
              onPressed: () => context.push('/servers/$id/console'),
              icon: const Icon(Icons.open_in_full),
              label: const Text('Open Full Console'),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildFilesTab(BuildContext context, WidgetRef ref, int id) {
    return Center(
      child: TextButton.icon(
        onPressed: () => context.push('/servers/$id/files'),
        icon: const Icon(Icons.folder_open),
        label: const Text('Browse Files'),
      ),
    );
  }

  Widget _buildPropertiesTab(BuildContext context, WidgetRef ref, ServerConfig config) {
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _propRow('Name', config.name),
          _propRow('Type', config.serverType),
          _propRow('Version', config.mcVersion ?? '-'),
          _propRow('Port', '${config.port}'),
          _propRow('Max Players', '${config.maxPlayers}'),
          _propRow('Java Args', config.javaArgs),
          _propRow('Auto Backup', config.autoBackup ? 'Yes' : 'No'),
          _propRow('Auto Restart', config.autoRestart ? 'Yes' : 'No'),
        ],
      ),
    );
  }

  Widget _propRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          SizedBox(
            width: 120,
            child: Text(label,
                style: const TextStyle(fontWeight: FontWeight.bold)),
          ),
          Expanded(child: Text(value)),
        ],
      ),
    );
  }
}
