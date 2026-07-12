import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/providers/settings_provider.dart';

class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settings = ref.watch(settingsProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Settings')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Appearance',
                      style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 8),
                  DropdownButtonFormField<ThemeMode>(
                    initialValue: settings.themeMode,
                    items: const [
                      DropdownMenuItem(
                          value: ThemeMode.system, child: Text('System')),
                      DropdownMenuItem(
                          value: ThemeMode.light, child: Text('Light')),
                      DropdownMenuItem(
                          value: ThemeMode.dark, child: Text('Dark')),
                    ],
                    onChanged: (mode) {
                      if (mode != null) {
                        ref
                            .read(settingsProvider.notifier)
                            .setThemeMode(mode);
                      }
                    },
                    decoration: const InputDecoration(
                      labelText: 'Theme',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 16),
                  Text('Java Runtime',
                      style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 8),
                  TextFormField(
                    initialValue: settings.javaPath ?? '',
                    decoration: const InputDecoration(
                      labelText: 'Java Path',
                      hintText: 'Leave empty to detect from PATH',
                      border: OutlineInputBorder(),
                    ),
                    onFieldSubmitted: (value) {
                      if (value.isNotEmpty) {
                        ref
                            .read(settingsProvider.notifier)
                            .setJavaPath(value);
                      }
                    },
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
