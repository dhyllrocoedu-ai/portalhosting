import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/providers/console_provider.dart';
import '../../core/providers/process_provider.dart';
import '../../core/server/console_streamer.dart';

class ConsoleScreen extends ConsumerStatefulWidget {
  final int serverId;
  const ConsoleScreen({super.key, required this.serverId});

  @override
  ConsumerState<ConsoleScreen> createState() => _ConsoleScreenState();
}

class _ConsoleScreenState extends ConsumerState<ConsoleScreen> {
  final _scrollCtrl = ScrollController();
  final _searchCtrl = TextEditingController();
  final _inputCtrl = TextEditingController();
  bool _autoScroll = true;
  bool _showSearch = false;
  LogLevel _minLevel = LogLevel.all;
  StreamSubscription? _sub;

  @override
  void initState() {
    super.initState();
    _sub = ref.read(processManagerProvider).consoleStream(widget.serverId).listen((line) {
      ref.read(consoleStreamerProvider).addLine(widget.serverId, line);
      if (_autoScroll) {
        WidgetsBinding.instance.addPostFrameCallback((_) => _scrollToBottom());
      }
    });
  }

  void _scrollToBottom() {
    if (_scrollCtrl.hasClients) {
      _scrollCtrl.animateTo(
        _scrollCtrl.position.maxScrollExtent,
        duration: const Duration(milliseconds: 100),
        curve: Curves.easeOut,
      );
    }
  }

  void _send() {
    final cmd = _inputCtrl.text.trim();
    if (cmd.isEmpty) return;
    ref.read(processManagerProvider).writeCommand(widget.serverId, cmd);
    _inputCtrl.clear();
  }

  @override
  Widget build(BuildContext context) {
    final streamer = ref.watch(consoleStreamerProvider);
    final lines = _searchCtrl.text.isEmpty
        ? streamer.getLines(widget.serverId, minLevel: _minLevel)
        : streamer.search(
            widget.serverId, _searchCtrl.text,
            minLevel: _minLevel);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Console'),
        actions: [
          IconButton(
            icon: Icon(_showSearch ? Icons.search_off : Icons.search),
            onPressed: () => setState(() => _showSearch = !_showSearch),
          ),
          PopupMenuButton<LogLevel>(
            initialValue: _minLevel,
            onSelected: (v) => setState(() => _minLevel = v),
            itemBuilder: (_) => LogLevel.values
                .map((l) => CheckedPopupMenuItem(
                      checked: _minLevel == l,
                      value: l,
                      child: Text(l.name.toUpperCase()),
                    ))
                .toList(),
          ),
          IconButton(
            icon: Icon(_autoScroll ? Icons.vertical_align_bottom : Icons.vertical_align_center),
            onPressed: () => setState(() => _autoScroll = !_autoScroll),
          ),
        ],
      ),
      body: Column(
        children: [
          if (_showSearch)
            Padding(
              padding: const EdgeInsets.fromLTRB(8, 8, 8, 0),
              child: TextField(
                controller: _searchCtrl,
                decoration: const InputDecoration(
                  hintText: 'Search console...',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.search),
                  isDense: true,
                ),
                onChanged: (_) => setState(() {}),
              ),
            ),
          Expanded(
            child: lines.isEmpty
                ? const Center(child: Text('No console output'))
                : ListView.builder(
                    controller: _scrollCtrl,
                    itemCount: lines.length,
                    itemBuilder: (ctx, i) {
                      final line = lines[i];
                      final color = switch (line.level) {
                        LogLevel.error => Colors.red.shade300,
                        LogLevel.warn => Colors.orange.shade300,
                        _ => null,
                      };
                      return Padding(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 8, vertical: 1),
                        child: Text(
                          line.text,
                          style: TextStyle(
                            fontFamily: 'monospace',
                            fontSize: 12,
                            color: color,
                          ),
                        ),
                      );
                    },
                  ),
          ),
          Container(
            color: Theme.of(context).colorScheme.surfaceContainerHighest,
            padding: const EdgeInsets.fromLTRB(8, 4, 8, 4),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _inputCtrl,
                    decoration: const InputDecoration(
                      hintText: 'Type a command...',
                      border: OutlineInputBorder(),
                      isDense: true,
                      contentPadding:
                          EdgeInsets.symmetric(horizontal: 8, vertical: 8),
                    ),
                    onSubmitted: (_) => _send(),
                  ),
                ),
                const SizedBox(width: 8),
                IconButton(
                  icon: const Icon(Icons.send),
                  onPressed: _send,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  @override
  void dispose() {
    _sub?.cancel();
    _scrollCtrl.dispose();
    _searchCtrl.dispose();
    _inputCtrl.dispose();
    super.dispose();
  }
}
