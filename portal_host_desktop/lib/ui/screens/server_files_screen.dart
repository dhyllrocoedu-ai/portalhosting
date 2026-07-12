import 'package:flutter/material.dart';

class ServerFilesScreen extends StatelessWidget {
  final String serverId;
  const ServerFilesScreen({super.key, required this.serverId});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Files $serverId')),
      body: const Center(child: Text('File Browser — Phase 3')),
    );
  }
}
