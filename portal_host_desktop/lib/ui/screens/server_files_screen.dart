import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:file_picker/file_picker.dart';
import 'package:intl/intl.dart';
import '../../shared/models/server_config.dart';

class ServerFilesScreen extends ConsumerStatefulWidget {
  final ServerConfig config;
  const ServerFilesScreen({super.key, required this.config});

  @override
  ConsumerState<ServerFilesScreen> createState() => _ServerFilesScreenState();
}

class _ServerFilesScreenState extends ConsumerState<ServerFilesScreen> {
  late Directory _baseDir;
  Directory? _currentDir;
  List<FileSystemEntity> _entries = [];
  List<String> _breadcrumbs = [];
  String _searchQuery = '';
  bool _loading = false;

  @override
  void initState() {
    super.initState();
    final basePath = widget.config.serverDir.isNotEmpty
        ? widget.config.serverDir
        : Directory(widget.config.jarPath).parent.path;
    _baseDir = Directory(basePath);
    _navigateTo(_baseDir);
  }

  Future<void> _navigateTo(Directory dir) async {
    setState(() => _loading = true);
    _currentDir = dir;
    _breadcrumbs = _buildBreadcrumbs(dir);
    try {
      final list = dir.listSync();
      list.sort((a, b) {
        if (a is Directory && b is! Directory) return -1;
        if (a is! Directory && b is Directory) return 1;
        return _basename(a.path)
            .toLowerCase()
            .compareTo(_basename(b.path).toLowerCase());
      });
      _entries = list;
    } catch (_) {
      _entries = [];
    }
    setState(() => _loading = false);
  }

  List<String> _buildBreadcrumbs(Directory dir) {
    final parts = dir.path.split('\\');
    final crumbs = <String>[];
    for (final part in parts) {
      crumbs.add(part);
    }
    return crumbs;
  }

  String _basename(String path) => path.split('\\').last;

  String _formatSize(int bytes) {
    if (bytes < 1024) return '$bytes B';
    if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(1)} KB';
    return '${(bytes / (1024 * 1024)).toStringAsFixed(1)} MB';
  }

  Future<void> _uploadFile() async {
    final result = await FilePicker.platform.pickFiles();
    if (result == null || _currentDir == null) return;
    for (final file in result.files) {
      if (file.path == null) continue;
      final src = File(file.path!);
      final dest = File('${_currentDir!.path}\\${file.name}');
      if (await dest.exists() && file.size > 10 * 1024 * 1024) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('File exceeds 10MB limit')),
          );
        }
        continue;
      }
      await src.copy(dest.path);
    }
    await _navigateTo(_currentDir!);
  }

  Future<void> _delete(FileSystemEntity entity) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Delete'),
        content: Text('Delete ${_basename(entity.path)}?'),
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
      try {
        if (entity is Directory) {
          await entity.delete(recursive: true);
        } else {
          await entity.delete();
        }
        await _navigateTo(_currentDir!);
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context)
              .showSnackBar(SnackBar(content: Text('Error: $e')));
        }
      }
    }
  }

  Future<void> _rename(FileSystemEntity entity) async {
    final oldName = _basename(entity.path);
    final ctrl = TextEditingController(text: oldName);
    final newName = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Rename'),
        content: TextField(controller: ctrl, autofocus: true),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          TextButton(
            onPressed: () => Navigator.pop(ctx, ctrl.text.trim()),
            child: const Text('Rename'),
          ),
        ],
      ),
    );
    ctrl.dispose();
    if (newName != null && newName.isNotEmpty && newName != oldName) {
      final newPath = '${_currentDir!.path}\\$newName';
      try {
        await entity.rename(newPath);
        await _navigateTo(_currentDir!);
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context)
              .showSnackBar(SnackBar(content: Text('Error: $e')));
        }
      }
    }
  }

  Future<void> _navigateUp() async {
    if (_currentDir == null) return;
    final parent = _currentDir!.parent;
    if (parent.path != _currentDir!.path) {
      await _navigateTo(parent);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final filtered = _searchQuery.isEmpty
        ? _entries
        : _entries
            .where((e) =>
                _basename(e.path)
                    .toLowerCase()
                    .contains(_searchQuery.toLowerCase()))
            .toList();

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.config.name),
        actions: [
          if (_currentDir != null && _currentDir!.path != _baseDir.path)
            IconButton(
              icon: const Icon(Icons.arrow_upward),
              tooltip: 'Up',
              onPressed: _navigateUp,
            ),
        ],
      ),
      body: Column(
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            child: TextField(
              decoration: const InputDecoration(
                hintText: 'Search files...',
                prefixIcon: Icon(Icons.search),
                border: OutlineInputBorder(),
                isDense: true,
              ),
              onChanged: (v) => setState(() => _searchQuery = v),
            ),
          ),
          _buildBreadcrumbBar(theme),
          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator())
                : filtered.isEmpty
                    ? const Center(child: Text('Empty directory'))
                    : ListView.builder(
                        itemCount: filtered.length,
                        itemBuilder: (ctx, i) {
                          final entity = filtered[i];
                          final isDir = entity is Directory;
                          String? statStr;
                          try {
                            final stat = entity.statSync();
                            statStr = isDir
                                ? null
                                : '${_formatSize(stat.size)}  •  ${DateFormat('yyyy-MM-dd HH:mm').format(stat.modified)}';
                          } catch (_) {}
                          return ListTile(
                            dense: true,
                            leading: Icon(
                              isDir
                                  ? Icons.folder
                                  : Icons.insert_drive_file,
                              color: isDir
                                  ? Colors.amber.shade600
                                  : theme.colorScheme.primary,
                            ),
                            title: Text(_basename(entity.path)),
                            subtitle: statStr != null ? Text(statStr) : null,
                            trailing: PopupMenuButton<String>(
                              onSelected: (v) {
                                switch (v) {
                                  case 'rename':
                                    _rename(entity);
                                  case 'delete':
                                    _delete(entity);
                                }
                              },
                              itemBuilder: (_) => [
                                const PopupMenuItem(
                                    value: 'rename', child: Text('Rename')),
                                const PopupMenuItem(
                                    value: 'delete',
                                    child: Text('Delete',
                                        style: TextStyle(color: Colors.red))),
                              ],
                            ),
                            onTap: entity is Directory ? () => _navigateTo(entity) : null,
                          );
                        },
                      ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        heroTag: 'upload',
        onPressed: _uploadFile,
        child: const Icon(Icons.upload),
      ),
    );
  }

  Widget _buildBreadcrumbBar(ThemeData theme) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      color: theme.colorScheme.surfaceContainerHighest,
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: Row(
          children: _breadcrumbs.asMap().entries.map((entry) {
            final i = entry.key;
            final crumb = entry.value;
            return Row(
              children: [
                if (i > 0) const Icon(Icons.chevron_right, size: 16),
                GestureDetector(
                  onTap: () {
                    final path = _breadcrumbs.sublist(0, i + 1).join('\\');
                    _navigateTo(Directory(path));
                  },
                  child: Text(
                    crumb,
                    style: TextStyle(
                      fontWeight: i == _breadcrumbs.length - 1
                          ? FontWeight.bold
                          : FontWeight.normal,
                      fontSize: 13,
                    ),
                  ),
                ),
              ],
            );
          }).toList(),
        ),
      ),
    );
  }
}
