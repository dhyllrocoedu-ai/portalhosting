import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'tray_service_provider.dart';

class AppLifecycleManager extends ConsumerWidget {
  final Widget child;
  const AppLifecycleManager({super.key, required this.child});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final trayService = ref.watch(trayServiceProvider);

    Future.microtask(() async {
      await trayService.initialize();
    });

    return child;
  }
}