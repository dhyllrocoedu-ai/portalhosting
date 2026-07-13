import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'notifications_provider.g.dart';

class Notification {
  final String id;
  final String title;
  final String message;
  final NotificationType type;
  final DateTime timestamp;

  const Notification({
    required this.id,
    required this.title,
    required this.message,
    required this.type,
    required this.timestamp,
  });
}

enum NotificationType {
  info,
  success,
  warning,
  error,
}

@riverpod
class NotificationCenter extends _$NotificationCenter {
  @override
  Stream<List<Notification>> build() {
    return Stream.value([]);
  }

  void show({
    required String title,
    required String message,
    NotificationType type = NotificationType.info,
  }) {
    final notification = Notification(
      id: DateTime.now().toString(),
      title: title,
      message: message,
      type: type,
      timestamp: DateTime.now(),
    );
    
    state = AsyncValue.data([...?state.valueOrNull, notification]);
    
    // Auto-remove notification after 5 seconds
    Future.delayed(const Duration(seconds: 5), () {
      final current = state.valueOrNull ?? [];
      state = AsyncValue.data(
        current.where((n) => n.id != notification.id).toList(),
      );
    });
  }

  void clear() {
    state = const AsyncValue.data([]);
  }
}
