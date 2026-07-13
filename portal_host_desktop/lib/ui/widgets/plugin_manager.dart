import 'dart:io';
import 'package:flutter/material.dart';
import 'package:file_picker/file_picker.dart';

class PluginManager extends StatefulWidget {
  final String pluginsDir;
  final String modsDir;
  final String datapacksDir;
  const PluginManager({
    super.key,
    required this.pluginsDir,
    required this.modsDir,
    required this.datapacksDir,
  });

  @override
  State<PluginManager> createState() => _PluginManagerState();
}

class _PluginManagerState extends State<PluginManager>
    with SingleTickerProviderStateMixin {
  late TabController _tabCtrl;
  final Map<String, List<FileSystemEntity>> _cache = {};

  @override
  void initState() {
    super.initState();
    _tabCtrl = TabController(length: 3, vsync: this);
    _loadAll();
  }

  Future<void> _loadAll() async {
    for (final key in ['plugins', 'mods', 'datapacks']) {
      await _load(key);
    }
    setState(() {});
  }

  Future<void> _load(String type) async {
    final dir = _dirFor(type);
    if (await dir.exists()) {
      final entries = dir.listSync().toList()
        ..sort((a, b) => p.basename(a.path)
            .toLowerCase()
            .compareTo(p.basename(b.path).toLowerCase()));
      _cache[type] = entries;
    } else {
      _cache[type] = [];
    }
  }

  Directory _dirFor(String type) {
    switch (type) {
      case 'plugins':
        return Directory(widget.pluginsDir);
      case 'mods':
        return Directory(widget.modsDir);
      case 'datapacks':
        return Directory(widget.datapacksDir);
      default:
        return Directory(widget.pluginsDir);
    }
  }

  Future<void> _upload(String type) async {
    final result = await FilePicker.platform.pickFiles();
    if (result == null) return;
    final dir = _dirFor(type);
    await dir.create(recursive: true);
    for (final file in result.files) {
      if (file.path == null) continue;
      if (file.size > 50 * 1024 * 1024) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('File exceeds 50MB limit')),
          );
        }
        continue;
      }
      final src = File(file.path!);
      final dest = File('${dir.path}\\${file.name}');
      await src.copy(dest.path);
    }
    await _load(type);
    if (mounted) setState(() {});
  }

  Future<void> _deleteFile(String type, FileSystemEntity entity) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Delete'),
        content: Text('Delete ${p.basename(entity.path)}?'),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('Cancel')),
          TextButton(
              onPressed: () => Navigator.pop(ctx, true),
              child:
                  const Text('Delete', style: TextStyle(color: Colors.red))),
        ],
      ),
    );
    if (confirmed == true) {
      try {
        await entity.delete();
        await _load(type);
        if (mounted) setState(() {});
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context)
              .showSnackBar(SnackBar(content: Text('Error: $e')));
        }
      }
    }
  }

  Future<void> _toggleEnabled(String type, FileSystemEntity entity) async {
    final path = entity.path;
    final dotIdx = path.lastIndexOf('.');
    if (dotIdx == -1) return;
    final base = path.substring(0, dotIdx);
    final disabledPath = '$base.disabled';
    try {
      if (entity is File) {
        await entity.rename(disabledPath);
      } else {
        final disabledDir = Directory(disabledPath);
        if (await disabledDir.exists()) {
          await entity.rename(path.replaceFirst('.disabled', ''));
        } else {
          await entity.rename(disabledPath);
        }
      }
      await _load(type);
      if (mounted) setState(() {});
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('Error: $e')));
      }
    }
  }

  @override
  void dispose() {
    _tabCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      children: [
        TabBar(
          controller: _tabCtrl,
          tabs: const [
            Tab(text: 'Plugins'),
            Tab(text: 'Mods'),
            Tab(text: 'Datapacks'),
          ],
        ),
        Expanded(
          child: TabBarView(
            controller: _tabCtrl,
            children: ['plugins', 'mods', 'datapacks'].map((type) {
              return _buildList(type, theme);
            }).toList(),
          ),
        ),
      ],
    );
  }

  Widget _buildList(String type, ThemeData theme) {
    final entries = _cache[type] ?? [];
    return Scaffold(
      body: entries.isEmpty
          ? const Center(child: Text('No files'))
          : ListView.builder(
              itemCount: entries.length,
              itemBuilder: (ctx, i) {
                final entity = entries[i];
                final name = p.basename(entity.path);
                final isDisabled = name.endsWith('.disabled');
                final stat = entity.statSync();
                return ListTile(
                  leading: Icon(
                    Icons.extension,
                    color: isDisabled ? Colors.grey : Colors.green.shade600,
                  ),
                  title: Text(
                    isDisabled ? name.replaceFirst('.disabled', '') : name,
                    style: TextStyle(
                      color: isDisabled ? Colors.grey : null,
                    ),
                  ),
                  subtitle: stat.size > 0
                      ? Text(_formatSize(stat.size))
                      : null,
                  trailing: PopupMenuButton<String>(
                    onSelected: (v) {
                      switch (v) {
                        case 'toggle':
                          _toggleEnabled(type, entity);
                        case 'delete':
                          _deleteFile(type, entity);
                      }
                    },
                    itemBuilder: (_) => [
                      PopupMenuItem(
                        value: 'toggle',
                        child: Text(isDisabled ? 'Enable' : 'Disable'),
                      ),
                      const PopupMenuItem(
                        value: 'delete',
                        child:
                            Text('Delete', style: TextStyle(color: Colors.red)),
                      ),
                    ],
                  ),
                );
              },
            ),
      floatingActionButton: FloatingActionButton(
        heroTag: type,
        onPressed: () => _upload(type),
        child: const Icon(Icons.upload),
      ),
    );
  }

  String _formatSize(int bytes) {
    if (bytes < 1024) return '$bytes B';
    if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(1)} KB';
    return '${(bytes / (1024 * 1024)).toStringAsFixed(1)} MB';
  }
}

final p = _PathUtils();
class _PathUtils {
  String basename(String path) => path.split('\\').last;
  String join(String a, String b) => '$a\\$b';
}
