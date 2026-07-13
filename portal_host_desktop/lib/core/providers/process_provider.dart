import 'package:drift/drift.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../server/process_manager.dart';
import '../database/database.dart';
import 'database_provider.dart';
import 'backup_provider.dart';
import 'settings_provider.dart';

part 'process_provider.g.dart';

@riverpod
ProcessManager processManager(ProcessManagerRef ref) {
  final pm = ProcessManager();
  final db = ref.watch(databaseProvider);

  pm.onAutoBackup = (serverId) async {
    final settings = ref.read(settingsProvider);
    final mgr = ref.read(backupManagerProvider);
    try {
      final server = await db.getServer(serverId);
      if (server == null) return;
      await mgr.createBackup(serverId, server.name, '.');
      await mgr.enforceRetention(serverId, settings.maxBackups);
    } catch (_) {}
  };

  // Persist console logs to database
  pm.onLog = (serverId, line) async {
    try {
      final lineType = line.contains('[ERR]') ? 2 : 
                      line.toLowerCase().contains('warn') ? 1 : 0;
      await db.insertConsoleLog(ConsoleLogsCompanion.insert(
        serverId: serverId,
        line: line,
        lineType: Value(lineType),
      ));
    } catch (_) {}
  };

  ref.onDispose(() => pm.dispose());
  return pm;
}
