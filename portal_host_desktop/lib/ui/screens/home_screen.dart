import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/providers/server_provider.dart';
import '../../shared/models/server_state.dart';

class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final serversAsync = ref.watch(serverListProvider);
    final servers = serversAsync.asData?.value ?? [];

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
