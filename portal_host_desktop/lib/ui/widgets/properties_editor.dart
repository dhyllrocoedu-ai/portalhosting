import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path/path.dart' as p;
import '../../core/database/database.dart';
import '../../core/providers/database_provider.dart';

class PropertiesEditor extends ConsumerStatefulWidget {
  final int serverId;
  const PropertiesEditor({super.key, required this.serverId});

  @override
  ConsumerState<PropertiesEditor> createState() => _PropertiesEditorState();
}

class _PropertiesEditorState extends ConsumerState<PropertiesEditor> {
  List<ServerProperty> _properties = [];
  String _searchQuery = '';
  bool _loading = true;
  String _serverDir = '';

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    final db = ref.read(databaseProvider);
    
    // Get server directory from config
    final server = await db.getServer(widget.serverId);
    if (server != null) {
      _serverDir = server.serverDir;
    }
    
    // Load properties from DB
    final dbProps = await db.getProperties(widget.serverId);
    
    // Also load from server.properties file on disk to sync
    final diskProps = await _loadFromDisk();
    
    // Merge: disk props take precedence for existing keys, but keep DB-only keys
    final Map<String, ServerProperty> merged = {};
    
    for (final prop in dbProps) {
      merged[prop.key] = prop;
    }
    
    for (final entry in diskProps.entries) {
      merged[entry.key] = ServerProperty(
        id: merged[entry.key]?.id ?? 0,
        serverId: widget.serverId,
        key: entry.key,
        value: entry.value,
      );
    }
    
    final props = merged.values.toList()
      ..sort((a, b) => a.key.compareTo(b.key));
    
    setState(() {
      _properties = props;
      _loading = false;
    });
  }

  Future<Map<String, String>> _loadFromDisk() async {
    if (_serverDir.isEmpty) return {};
    final propsFile = File(p.join(_serverDir, 'server.properties'));
    if (!await propsFile.exists()) return {};
    
    final content = await propsFile.readAsString();
    final result = <String, String>{};
    
    for (final line in content.split('\n')) {
      final trimmed = line.trim();
      if (trimmed.isEmpty || trimmed.startsWith('#')) continue;
      final idx = trimmed.indexOf('=');
      if (idx > 0) {
        final key = trimmed.substring(0, idx).trim();
        final value = trimmed.substring(idx + 1).trim();
        result[key] = value;
      }
    }
    
    return result;
  }

  Future<void> _saveToDisk() async {
    if (_serverDir.isEmpty) return;
    final propsFile = File(p.join(_serverDir, 'server.properties'));
    final lines = _properties.map((p) => '${p.key}=${p.value}').toList();
    await propsFile.writeAsString('${lines.join('\n')}\n');
  }

  Future<void> _addOrEdit({ServerProperty? existing}) async {
    final keyCtrl =
        TextEditingController(text: existing?.key ?? '');
    final valueCtrl =
        TextEditingController(text: existing?.value ?? '');
    final result = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(existing != null ? 'Edit Property' : 'Add Property'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: keyCtrl,
              decoration: const InputDecoration(
                labelText: 'Key',
                border: OutlineInputBorder(),
              ),
              enabled: existing == null,
            ),
            const SizedBox(height: 8),
            TextField(
              controller: valueCtrl,
              decoration: const InputDecoration(
                labelText: 'Value',
                border: OutlineInputBorder(),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          TextButton(
              onPressed: () => Navigator.pop(ctx, true),
              child: const Text('Save')),
        ],
      ),
    );
    keyCtrl.dispose();
    valueCtrl.dispose();
    if (result == true && keyCtrl.text.trim().isNotEmpty) {
      final db = ref.read(databaseProvider);
      await db.upsertProperty(
          widget.serverId, keyCtrl.text.trim(), valueCtrl.text);
      await _saveToDisk();
      await _load();
    }
  }

  Future<void> _delete(ServerProperty prop) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Delete'),
        content: Text('Delete ${prop.key}?'),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('Cancel')),
          TextButton(
              onPressed: () => Navigator.pop(ctx, true),
              child: const Text('Delete')),
        ],
      ),
    );
    if (confirmed == true) {
      final db = ref.read(databaseProvider);
      await db.deleteProperty(prop.id);
      await _saveToDisk();
      await _load();
    }
  }

  @override
  Widget build(BuildContext context) {
    final filtered = _searchQuery.isEmpty
        ? _properties
        : _properties
            .where((p) =>
                p.key.toLowerCase().contains(_searchQuery.toLowerCase()) ||
                p.value.toLowerCase().contains(_searchQuery.toLowerCase()))
            .toList();

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(8),
          child: TextField(
            decoration: const InputDecoration(
              hintText: 'Search properties...',
              prefixIcon: Icon(Icons.search),
              border: OutlineInputBorder(),
              isDense: true,
            ),
            onChanged: (v) => setState(() => _searchQuery = v),
          ),
        ),
        Expanded(
          child: _loading
              ? const Center(child: CircularProgressIndicator())
              : filtered.isEmpty
                  ? const Center(child: Text('No properties'))
                  : ListView.builder(
                      itemCount: filtered.length,
                      itemBuilder: (ctx, i) {
                        final prop = filtered[i];
                        return ListTile(
                          dense: true,
                          title: Text(prop.key,
                              style: const TextStyle(
                                  fontFamily: 'monospace', fontSize: 13)),
                          subtitle: Text(prop.value,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(
                                  fontFamily: 'monospace', fontSize: 12)),
                          trailing: PopupMenuButton<String>(
                            onSelected: (v) {
                              switch (v) {
                                case 'edit':
                                  _addOrEdit(existing: prop);
                                case 'delete':
                                  _delete(prop);
                              }
                            },
                            itemBuilder: (_) => [
                              const PopupMenuItem(
                                  value: 'edit', child: Text('Edit')),
                              const PopupMenuItem(
                                  value: 'delete',
                                  child: Text('Delete',
                                      style: TextStyle(color: Colors.red))),
                            ],
                          ),
                        );
                      },
                    ),
        ),
      ],
    );
  }
}
