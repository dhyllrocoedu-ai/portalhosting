import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/providers/server_provider.dart';
import '../../core/server/server_downloader.dart';
import '../../core/server/server_type.dart';
import '../../shared/models/server_config.dart';

class CreateServerScreen extends ConsumerStatefulWidget {
  const CreateServerScreen({super.key});

  @override
  ConsumerState<CreateServerScreen> createState() => _CreateServerScreenState();
}

class _CreateServerScreenState extends ConsumerState<CreateServerScreen> {
  final _nameCtrl = TextEditingController();
  final _portCtrl = TextEditingController(text: '25565');
  final _maxPlayersCtrl = TextEditingController(text: '20');
  final _memCtrl = TextEditingController(text: '1024');
  final _javaArgsCtrl = TextEditingController();
  ServerType _selectedType = ServerType.paper;
  List<String> _versions = [];
  String? _selectedVersion;
  List<BuildInfo> _builds = [];
  String? _selectedBuild;
  bool _loadingVersions = false;
  bool _loadingBuilds = false;
  bool _creating = false;
  String? _downloadProgress;

  @override
  void initState() {
    super.initState();
    _loadVersions();
  }

  Future<void> _loadVersions() async {
    setState(() => _loadingVersions = true);
    try {
      final provider = ServerDownloader.providerFor(_selectedType);
      _versions = await provider.getVersions();
    } catch (_) {}
    setState(() => _loadingVersions = false);
  }

  Future<void> _loadBuilds(String version) async {
    setState(() => _loadingBuilds = true);
    _selectedVersion = version;
    _builds = [];
    _selectedBuild = null;
    try {
      final provider = ServerDownloader.providerFor(_selectedType);
      _builds = await provider.getBuilds(version);
    } catch (_) {}
    setState(() => _loadingBuilds = false);
  }

  Future<void> _create() async {
    if (_nameCtrl.text.trim().isEmpty) return;
    setState(() => _creating = true);

    try {
      final dir = _nameCtrl.text.trim().replaceAll(' ', '_');
      final jarName = 'server.jar';
      final jarPath = '$dir\\$jarName';

      if (_selectedVersion != null && _builds.isNotEmpty) {
        final build = _selectedBuild != null
            ? _builds.firstWhere((b) => b.build == _selectedBuild)
            : _builds.first;
        await ServerDownloader.download(build, jarPath,
            onProgress: (rec, total) {
          final pct = total > 0 ? (rec * 100 ~/ total) : 0;
          setState(() =>
              _downloadProgress = 'Downloading... $pct% (${rec ~/ 1024}/${total ~/ 1024} KB)');
        });
      }

      final config = ServerConfig(
        id: 0,
        name: _nameCtrl.text.trim(),
        jarPath: jarPath,
        port: int.tryParse(_portCtrl.text) ?? 25565,
        maxPlayers: int.tryParse(_maxPlayersCtrl.text) ?? 20,
        serverType: _selectedType.key,
        mcVersion: _selectedVersion,
        javaArgs:
            '-Xms${_memCtrl.text.trim()}M -Xmx${_memCtrl.text.trim()}M ${_javaArgsCtrl.text.trim()}',
        autoBackup: true,
        autoRestart: false,
      );

      await ref.read(serverListProvider.notifier).addServer(config);
      if (mounted) Navigator.of(context).pop();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('Error: $e')));
      }
    } finally {
      setState(() => _creating = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Create Server')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          TextField(
            controller: _nameCtrl,
            decoration: const InputDecoration(
              labelText: 'Server Name',
              border: OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 12),
          DropdownButtonFormField<ServerType>(
            initialValue: _selectedType,
            decoration: const InputDecoration(
              labelText: 'Server Type',
              border: OutlineInputBorder(),
            ),
            items: ServerType.values
                .map((t) => DropdownMenuItem(
                      value: t,
                      child: Text(t.displayName),
                    ))
                .toList(),
            onChanged: (v) {
              if (v != null) {
                setState(() => _selectedType = v);
                _loadVersions();
              }
            },
          ),
          const SizedBox(height: 12),
          if (_loadingVersions)
            const LinearProgressIndicator()
          else
            DropdownButtonFormField<String>(
              initialValue: _selectedVersion,
              decoration: const InputDecoration(
                labelText: 'Version',
                border: OutlineInputBorder(),
              ),
              items: _versions
                  .map((v) => DropdownMenuItem(value: v, child: Text(v)))
                  .toList(),
              onChanged: (v) {
                if (v != null) _loadBuilds(v);
              },
            ),
          const SizedBox(height: 12),
          if (_loadingBuilds)
            const LinearProgressIndicator()
          else if (_builds.isNotEmpty)
            DropdownButtonFormField<String>(
              initialValue: _selectedBuild,
              decoration: const InputDecoration(
                labelText: 'Build',
                border: OutlineInputBorder(),
              ),
              items: _builds
                  .map((b) => DropdownMenuItem(
                      value: b.build ?? b.version, child: Text(b.build ?? 'latest')))
                  .toList(),
              onChanged: (v) {
                if (v != null) setState(() => _selectedBuild = v);
              },
            ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _portCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Port',
                    border: OutlineInputBorder(),
                  ),
                  keyboardType: TextInputType.number,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: TextField(
                  controller: _maxPlayersCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Max Players',
                    border: OutlineInputBorder(),
                  ),
                  keyboardType: TextInputType.number,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _memCtrl,
            decoration: const InputDecoration(
              labelText: 'Memory (MB)',
              border: OutlineInputBorder(),
            ),
            keyboardType: TextInputType.number,
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _javaArgsCtrl,
            decoration: const InputDecoration(
              labelText: 'Extra Java Args',
              border: OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 24),
          if (_downloadProgress != null)
            Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: Text(_downloadProgress!,
                  style: Theme.of(context).textTheme.bodySmall),
            ),
          FilledButton.icon(
            onPressed: (_creating || _nameCtrl.text.trim().isEmpty)
                ? null
                : _create,
            icon: _creating
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.add),
            label: Text(_creating ? 'Creating...' : 'Create Server'),
          ),
        ],
      ),
    );
  }

  @override
  void dispose() {
    _nameCtrl.dispose();
    _portCtrl.dispose();
    _maxPlayersCtrl.dispose();
    _memCtrl.dispose();
    _javaArgsCtrl.dispose();
    super.dispose();
  }
}
