import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../backup/backup_manager.dart';
import '../../shared/models/backup_entry.dart';
import 'database_provider.dart';

part 'backup_provider.g.dart';

@riverpod
BackupManager backupManager(BackupManagerRef ref) {
  final db = ref.watch(databaseProvider);
  return BackupManager(db);
}

@riverpod
Future<List<BackupEntry>> backupList(BackupListRef ref, int serverId) async {
  final mgr = ref.watch(backupManagerProvider);
  return mgr.getBackups(serverId);
}
