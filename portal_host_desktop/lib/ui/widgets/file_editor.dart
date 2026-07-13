import 'dart:io';
import 'package:flutter/material.dart';

class FileEditor extends StatefulWidget {
  final String filePath;
  const FileEditor({super.key, required this.filePath});

  @override
  State<FileEditor> createState() => _FileEditorState();
}

class _FileEditorState extends State<FileEditor> {
  late TextEditingController _ctrl;
  bool _loading = true;
  bool _modified = false;

  @override
  void initState() {
    super.initState();
    _ctrl = TextEditingController();
    _load();
  }

  Future<void> _load() async {
    final file = File(widget.filePath);
    final stat = await file.stat();
    if (stat.size > 10 * 1024 * 1024) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('File too large to edit (>10MB)')),
        );
      }
      setState(() => _loading = false);
      return;
    }
    final content = await file.readAsString();
    _ctrl.text = content;
    setState(() => _loading = false);
  }

  Future<void> _save() async {
    final file = File(widget.filePath);
    await file.writeAsString(_ctrl.text);
    setState(() => _modified = false);
    if (mounted) {
      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text('Saved')));
    }
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.filePath.split('\\').last),
        actions: [
          if (_modified)
            IconButton(
              icon: const Icon(Icons.save),
              onPressed: _save,
            ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : TextField(
              controller: _ctrl,
              maxLines: null,
              expands: true,
              style: const TextStyle(fontFamily: 'monospace', fontSize: 13),
              decoration: const InputDecoration(
                border: InputBorder.none,
                contentPadding: EdgeInsets.all(12),
              ),
              onChanged: (_) {
                if (!_modified) setState(() => _modified = true);
              },
            ),
    );
  }
}
