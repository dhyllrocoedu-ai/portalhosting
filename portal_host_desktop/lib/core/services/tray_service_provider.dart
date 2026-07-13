import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'tray_service.dart';

final trayServiceProvider = Provider<TrayService>((ref) {
  final service = TrayService();
  ref.onDispose(() => service.dispose());
  return service;
});