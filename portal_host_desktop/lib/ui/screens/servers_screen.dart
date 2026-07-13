import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/providers/server_provider.dart';
import '../../shared/models/server_config.dart';
import '../../shared/models/server_state.dart';

class ServersScreen extends ConsumerWidget {
  const ServersScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final serversAsync = ref.watch(serverListProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Servers'),
        centerTitle: false,
      ),
      floatingActionButton: FloatingActionButton(
        heroTag: 'create_server',
        onPressed: () => context.push('/servers/create'),
        child: const Icon(Icons.add),
      ),
      body: serversAsync.when(
        loading: () =>
            const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('Error: $e')),
        data: (servers) {
          if (servers.isEmpty) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.dns,
                      size: 64,
                      color: theme.colorScheme.onSurfaceVariant),
                  const SizedBox(height: 16),
                  Text('No servers yet',
                      style: theme.textTheme.titleLarge),
                  const SizedBox(height: 8),
                  Text(
                    'Tap + to create your first server',
                    style: theme.textTheme.bodyMedium?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant),
                  ),
                ],
              ),
            );
          }

          return ListView.builder(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
            itemCount: servers.length,
            itemBuilder: (ctx, i) {
              final server = servers[i];
              final state =
                  ref.watch(activeServerProvider(server.id));
              return _ServerCard(
                config: server,
                state: state,
                onTap: () {
                  ref.read(selectedServerIdProvider.notifier).select(server.id);
                  context.push('/servers/${server.id}');
                },
              );
            },
          );
        },
      ),
    );
  }
}

class _ServerCard extends ConsumerWidget {
  final ServerConfig config;
  final ServerState state;
  final VoidCallback onTap;

  const _ServerCard({
    required this.config,
    required this.state,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final statusColor = switch (state.status) {
      ServerStatus.running => Colors.green,
      ServerStatus.starting => Colors.orange,
      ServerStatus.stopping => Colors.orange,
      ServerStatus.crashed => Colors.red,
      ServerStatus.stopped => Colors.grey,
    };

    return Card(
      margin: const EdgeInsets.symmetric(vertical: 4),
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: statusColor.withAlpha(40),
          child: Icon(Icons.dns, color: statusColor, size: 24),
        ),
        title: Text(config.name),
        subtitle: Text(
          '${config.serverType}  •  ${state.status.name}'
          '${state.uptimeSeconds > 0 ? '  •  ${state.uptimeSeconds ~/ 60}m' : ''}',
          style: theme.textTheme.bodySmall,
        ),
        trailing: const Icon(Icons.chevron_right),
        onTap: onTap,
      ),
    );
  }
}
