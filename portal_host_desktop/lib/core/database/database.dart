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
  int get schemaVersion => 1;

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

  // ── Backups ──

  Future<List<Backup>> getBackups(int serverId) =>
      (select(backups)..where((b) => b.serverId.equals(serverId)))
          .get();
  Future<int> insertBackup(BackupsCompanion entry) =>
      into(backups).insert(entry);
  Future<int> deleteBackup(int id) =>
      (delete(backups)..where((b) => b.id.equals(id))).go();
}

LazyDatabase _openConnection() {
  return LazyDatabase(() async {
    final dir = await getApplicationSupportDirectory();
    await Directory(dir.path).create(recursive: true);
    final file = File(p.join(dir.path, 'portal_host.db'));
    return NativeDatabase(file);
  });
}
