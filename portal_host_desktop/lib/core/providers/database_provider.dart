import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../database/database.dart';

part 'database_provider.g.dart';

@riverpod
AppDatabase database(DatabaseRef ref) {
  final db = AppDatabase();
  ref.onDispose(() => db.close());
  return db;
}
