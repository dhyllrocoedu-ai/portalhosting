import 'dart:io';
import 'package:drift/drift.dart';
import 'package:drift/native.dart';
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as p;
import 'tables.dart';

part 'database.g.dart';

@DriftDatabase(tables: [Servers, ServerProperties, Backups, ConsoleLogs])
class AppDatabase extends _$AppDatabase {
  AppDatabase() : super(_openConnection());

  @override
  int get schemaVersion => 3;

  @override
  MigrationStrategy get migration => MigrationStrategy(
        onCreate: (m) async {
          await m.createAll();
          await m.addColumn(servers, servers.serverDir);
          await m.addColumn(servers, servers.iconPath);
        },
        onUpgrade: (m, from, to) async {
          if (from < 2) {
            await m.addColumn(servers, servers.serverDir);
          }
          if (from < 3) {
            await m.addColumn(servers, servers.iconPath);
          }
        },
      );

  // ── Servers ──

  Future<List<Server>> getAllServers() => select(servers).get();
  Future<Server?> getServer(int id) =>
      (select(servers)..where((s) => s.id.equals(id))).getSingleOrNull();
  Future<int> insertServer(ServersCompanion entry) =>
      into(servers).insert(entry);
  Future<bool> updateServer(ServersCompanion entry) =>
      update(servers).replace(entry);
  Future<int> deleteServer(int id) =>
      (delete(servers)..where((s) => s.id.equals(id))).go();

  // ── Properties ──

  Future<List<ServerProperty>> getProperties(int serverId) =>
      (select(serverProperties)..where((p) => p.serverId.equals(serverId)))
          .get();
  Future<int> upsertProperty(
      int serverId, String key, String value) async {
    final existing = await (select(serverProperties)
          ..where((p) =>
              p.serverId.equals(serverId) & p.key.equals(key)))
        .getSingleOrNull();
    if (existing != null) {
      return (update(serverProperties)
            ..where((p) => p.id.equals(existing.id)))
          .write(const ServerPropertiesCompanion())
          .then((_) => existing.id);
    }
    return into(serverProperties).insert(ServerPropertiesCompanion(
      serverId: Value(serverId),
      key: Value(key),
      value: Value(value),
    ));
  }

  Future<int> deleteProperty(int id) =>
      (delete(serverProperties)..where((p) => p.id.equals(id))).go();

  Future<int> deletePropertyByKey(int serverId, String key) =>
      (delete(serverProperties)
            ..where((p) => p.serverId.equals(serverId) & p.key.equals(key)))
          .go();

  // ── Backups ──

  Future<List<Backup>> getBackups(int serverId) =>
      (select(backups)..where((b) => b.serverId.equals(serverId)))
          .get();
  Future<int> insertBackup(BackupsCompanion entry) =>
      into(backups).insert(entry);
  Future<int> deleteBackup(int id) =>
      (delete(backups)..where((b) => b.id.equals(id))).go();
  Future<int> deleteBackupsByServer(int serverId) =>
      (delete(backups)..where((b) => b.serverId.equals(serverId))).go();
  Future<int> backupCount(int serverId) =>
      (select(backups)..where((b) => b.serverId.equals(serverId)))
          .map((b) => b.id)
          .get()
          .then((ids) => ids.length);

  // ── Console ──

  Future<List<ConsoleLog>> getConsoleLogs(int serverId) =>
      (select(consoleLogs)
            ..where((c) => c.serverId.equals(serverId))
            ..orderBy([(c) => OrderingTerm(expression: c.id)]))
          .get();
  Future<int> insertConsoleLog(ConsoleLogsCompanion entry) =>
      into(consoleLogs).insert(entry);
  Future<int> deleteConsoleLogs(int serverId) =>
      (delete(consoleLogs)..where((c) => c.serverId.equals(serverId))).go();
}

LazyDatabase _openConnection() {
  return LazyDatabase(() async {
    final dir = await getApplicationSupportDirectory();
    await Directory(dir.path).create(recursive: true);
    final file = File(p.join(dir.path, 'portal_host.db'));
    return NativeDatabase(file);
  });
}
