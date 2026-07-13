import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:file_picker/file_picker.dart';
import 'package:path/path.dart' as p;
import '../../core/java/jdk_manager.dart';
import '../../core/providers/settings_provider.dart';

class JdkManagementScreen extends ConsumerStatefulWidget {
  const JdkManagementScreen({super.key});

  @override
  ConsumerState<JdkManagementScreen> createState() => _JdkManagementScreenState();
}

class _JdkManagementScreenState extends ConsumerState<JdkManagementScreen> {
  final JdkManager _jdkManager = JdkManager();
  String? _javaHome;
  String? _javaVersion;
  bool _isInstalling = false;
  double _installProgress = 0.0;
  String _installStatus = '';

  @override
  void initState() {
    super.initState();
    _checkJdkStatus();
  }

  Future<void> _checkJdkStatus() async {
    final jdkPath = await _jdkManager.getJavaHome();
    if (jdkPath != null && jdkPath.isNotEmpty) {
      final version = await _getJavaVersion(jdkPath);
      setState(() {
        _javaHome = jdkPath;
        _javaVersion = version;
      });
    } else {
      setState(() {
        _javaHome = null;
        _javaVersion = null;
      });
    }
  }

  Future<String?> _getJavaVersion(String javaHome) async {
    try {
      final result = await Process.run(
        '${Platform.isWindows ? '$javaHome\\java.exe' : '$javaHome/bin/java'}',
        ['-version'],
      );
      final output = result.stderr.toString();
      final match = RegExp(r'version "([^"]+)"').firstMatch(output);
      return match?.group(1);
    } catch (e) {
      return null;
    }
  }

  Future<void> _installJdk() async {
    setState(() {
      _isInstalling = true;
      _installProgress = 0.0;
      _installStatus = 'Starting JDK 21 download...';
    });

    final result = await _jdkManager.downloadAndInstallJdk(
      onProgress: (progress, status) {
        setState(() {
          _installProgress = progress;
          _installStatus = status;
        });
      },
    );

    if (result.success) {
      await _checkJdkStatus();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('JDK 21 installed successfully at ${result.javaHome}')),
        );
      }
    } else {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to install JDK: ${result.message}'), backgroundColor: Colors.red),
        );
      }
    }

    setState(() {
      _isInstalling = false;
      _installProgress = 0.0;
      _installStatus = '';
    });
  }

  Future<void> _selectCustomJava() async {
    String? path;
    if (Platform.isWindows) {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['exe'],
        dialogTitle: 'Select java.exe',
      );
      if (result != null && result.files.isNotEmpty) {
        path = result.files.first.path;
      }
    } else {
      final result = await FilePicker.platform.getDirectoryPath(
        dialogTitle: 'Select Java Home Directory',
      );
      if (result != null) {
        final javaBin = p.join(result, 'bin', 'java');
        if (await File(javaBin).exists()) {
          path = result;
        } else {
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('Selected directory does not contain java binary')),
            );
          }
          return;
        }
      }
    }

    if (path != null) {
      final version = await _getJavaVersion(path);
      setState(() {
        _javaHome = path;
        _javaVersion = version;
      });
      final notifier = ref.read(settingsProvider.notifier);
      await notifier.setJavaPath(path);
    }
  }

  Future<void> _uninstallJdk() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Uninstall JDK 21'),
        content: const Text('This will remove the bundled JDK 21. You will need to provide a custom Java path. Continue?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancel')),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            style: FilledButton.styleFrom(backgroundColor: Colors.red),
            child: const Text('Uninstall'),
          ),
        ],
      ),
    );

    if (confirm == true) {
      await _jdkManager.uninstallJdk();
      setState(() {
        _javaHome = null;
        _javaVersion = null;
      });
      final notifier = ref.read(settingsProvider.notifier);
      await notifier.setJavaPath('');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('JDK uninstalled successfully')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('JDK Management')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('JDK 21 Status', style: Theme.of(context).textTheme.titleLarge),
                  const SizedBox(height: 16),
                  if (_javaHome != null) ...[
                    Row(
                      children: [
                        const Icon(Icons.check_circle, color: Colors.green, size: 28),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text('JDK 21 Installed', style: Theme.of(context).textTheme.titleMedium?.copyWith(color: Colors.green)),
                              Text('Version: ${_javaVersion ?? 'Unknown'}', style: Theme.of(context).textTheme.bodyMedium),
                              Text('Location: ${_javaHome}', style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Colors.grey)),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        OutlinedButton.icon(
                          onPressed: _uninstallJdk,
                          icon: const Icon(Icons.delete),
                          label: const Text('Uninstall Bundled JDK'),
                          style: OutlinedButton.styleFrom(foregroundColor: Colors.red),
                        ),
                        const SizedBox(width: 12),
                        OutlinedButton.icon(
                          onPressed: _checkJdkStatus,
                          icon: const Icon(Icons.refresh),
                          label: const Text('Refresh Status'),
                        ),
                      ],
                    ),
                  ] else ...[
                    Row(
                      children: [
                        const Icon(Icons.warning, color: Colors.orange, size: 28),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text('JDK 21 Not Installed', style: Theme.of(context).textTheme.titleMedium?.copyWith(color: Colors.orange)),
                              Text('Portal Host requires JDK 21 to run Minecraft servers.', style: Theme.of(context).textTheme.bodyMedium),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    FilledButton.icon(
                      onPressed: _isInstalling ? null : _installJdk,
                      icon: _isInstalling ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2)) : const Icon(Icons.download),
                      label: Text(_isInstalling ? 'Installing...' : 'Download & Install JDK 21'),
                    ),
                    if (_isInstalling) ...[
                      const SizedBox(height: 16),
                      LinearProgressIndicator(value: _installProgress),
                      const SizedBox(height: 8),
                      Text(_installStatus, style: Theme.of(context).textTheme.bodySmall),
                    ],
                    const SizedBox(height: 12),
                    OutlinedButton.icon(
                      onPressed: _isInstalling ? null : _selectCustomJava,
                      icon: const Icon(Icons.folder_open),
                      label: const Text('Use Custom Java Installation Instead'),
                    ),
                  ],
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('About Bundled JDK', style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 8),
                  const Text(
                    'Portal Host bundles Oracle JDK 21 for optimal Minecraft server compatibility. '
                    'The bundled JDK is automatically downloaded and managed by the application. '
                    'You can also use a custom Java installation if preferred.',
                    style: TextStyle(fontSize: 13),
                  ),
                  const SizedBox(height: 12),
                  const Text('Requirements:', style: TextStyle(fontWeight: FontWeight.bold)),
                  const SizedBox(height: 4),
                  const Text('• Windows 10/11 (64-bit)'),
                  const Text('• ~300 MB disk space for JDK'),
                  const Text('• Internet connection for initial download'),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}