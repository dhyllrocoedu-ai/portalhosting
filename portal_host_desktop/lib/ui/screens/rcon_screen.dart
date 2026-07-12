import 'package:flutter/material.dart';

class RconScreen extends StatelessWidget {
  final String serverId;
  const RconScreen({super.key, required this.serverId});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('RCON $serverId')),
      body: const Center(child: Text('RCON — Phase 4')),
    );
  }
}
