import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:file_picker/file_picker.dart';
import '../../core/providers/settings_provider.dart';

class SetupScreen extends ConsumerStatefulWidget {
  final VoidCallback onComplete;
  const SetupScreen({super.key, required this.onComplete});

  @override
  ConsumerState<SetupScreen> createState() => _SetupScreenState();
}

class _SetupScreenState extends ConsumerState<SetupScreen> {
  String _serversDir = '';
  String _javaPath = '';
  bool _busy = false;

  @override
  void initState() {
    super.initState();
    _serversDir =
        '${Directory.current.path}\\servers';
  }

  Future<void> _pickDir() async {
    final result = await FilePicker.platform.getDirectoryPath();
    if (result != null) {
      setState(() => _serversDir = result);
    }
  }

  Future<void> _pickJava() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: ['exe'],
    );
    if (result != null && result.files.isNotEmpty) {
      setState(() => _javaPath = result.files.first.path ?? '');
    }
  }

  Future<void> _complete() async {
    setState(() => _busy = true);
    await Directory(_serversDir).create(recursive: true);
    final notifier = ref.read(settingsProvider.notifier);
    await notifier.setServersDir(_serversDir);
    if (_javaPath.isNotEmpty) {
      await notifier.setJavaPath(_javaPath);
    }
    await notifier.completeSetup();
    widget.onComplete();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 560),
          child: Padding(
            padding: const EdgeInsets.all(32),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(Icons.dns, size: 48, color: theme.colorScheme.primary),
                const SizedBox(height: 16),
                Text('Welcome to Portal Host',
                    style: theme.textTheme.headlineMedium),
                const SizedBox(height: 8),
                Text(
                  'Let\'s get your Minecraft server manager set up.',
                  style: theme.textTheme.bodyLarge
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
                const SizedBox(height: 32),
                Text('Servers Directory',
                    style: theme.textTheme.titleMedium),
                const SizedBox(height: 8),
                Card(
                  child: ListTile(
                    leading: const Icon(Icons.folder),
                    title: Text(_serversDir,
                        style: const TextStyle(fontSize: 13)),
                    trailing: FilledButton.tonal(
                      onPressed: _pickDir,
                      child: const Text('Browse'),
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                Text('Java Runtime (optional)',
                    style: theme.textTheme.titleMedium),
                const SizedBox(height: 8),
                Card(
                  child: ListTile(
                    leading: const Icon(Icons.code),
                    title: Text(
                        _javaPath.isNotEmpty ? _javaPath : 'Auto-detect from PATH',
                        style: const TextStyle(fontSize: 13)),
                    trailing: FilledButton.tonal(
                      onPressed: _pickJava,
                      child: const Text('Browse'),
                    ),
                  ),
                ),
                const SizedBox(height: 32),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    onPressed: _serversDir.isNotEmpty && !_busy
                        ? _complete
                        : null,
                    icon: _busy
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child:
                                CircularProgressIndicator(strokeWidth: 2))
                        : const Icon(Icons.check),
                    label: Text(
                        _busy ? 'Setting up...' : 'Complete Setup'),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
