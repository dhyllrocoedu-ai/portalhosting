import 'dart:io';
import 'package:archive/archive.dart';
import 'package:path/path.dart' as p;
import '../../shared/models/backup_entry.dart';
import '../database/database.dart';

class BackupManager {
  final AppDatabase _db;

  BackupManager(this._db);

  Future<BackupEntry> createBackup(
      int serverId, String serverName, String worldDir,
      {String? customName}) async {
    final timestamp = DateTime.now();
    final name =
        customName ?? '${serverName}_backup_${timestamp.millisecondsSinceEpoch}';
    final dir = await _backupDir(serverId);
    await dir.create(recursive: true);

    final zipPath = p.join(dir.path, '$name.zip');
    final zipBytes = await _zipDirectory(worldDir);
    await File(zipPath).writeAsBytes(zipBytes);

    final size = await File(zipPath).length();
    final entry = BackupsCompanion.insert(
      serverId: serverId,
      name: name,
      path: zipPath,
      size: size,
    );
    final id = await _db.insertBackup(entry);
    return BackupEntry(
      id: id,
      serverId: serverId,
      name: name,
      path: zipPath,
      size: size,
      createdAt: timestamp,
    );
  }

  Future<void> restoreBackup(BackupEntry entry, String serverPath) async {
    final bytes = await File(entry.path).readAsBytes();
    final archive = ZipDecoder().decodeBytes(bytes);

    for (final file in archive) {
      final filePath = p.join(serverPath, file.name);
      if (!filePath.startsWith(serverPath)) {
        throw Exception('Zip-slip detected: ${file.name}');
      }
      if (file.isFile) {
        await File(filePath).create(recursive: true);
        await File(filePath).writeAsBytes(file.content as List<int>);
      } else {
        await Directory(filePath).create(recursive: true);
      }
    }
  }

  Future<void> deleteBackup(int id) async {
    final entry = await _db.getBackups(0).then((list) => list.where((b) => b.id == id));
    for (final b in entry) {
      try {
        await File(b.path).delete();
      } catch (_) {}
    }
    await _db.deleteBackup(id);
  }

  Future<List<BackupEntry>> getBackups(int serverId) async {
    final rows = await _db.getBackups(serverId);
    return rows
        .map((r) => BackupEntry(
              id: r.id,
              serverId: r.serverId,
              name: r.name,
              path: r.path,
              size: r.size,
              createdAt: r.createdAt ?? DateTime.now(),
            ))
        .toList()
      ..sort((a, b) => b.createdAt.compareTo(a.createdAt));
  }

  Future<void> enforceRetention(int serverId, int maxBackups) async {
    final backups = await getBackups(serverId);
    if (backups.length <= maxBackups) return;
    final toDelete = backups.sublist(maxBackups);
    for (final b in toDelete) {
      await deleteBackup(b.id);
    }
  }

  Future<Directory> _backupDir(int serverId) async {
    final appDir = await _appDir();
    return Directory(p.join(appDir.path, 'backups', '$serverId'));
  }

  Future<Directory> _appDir() async {
    final dir = Directory(
        p.join(Directory.current.path, 'portal_host_data'));
    await dir.create(recursive: true);
    return dir;
  }

  Future<List<int>> _zipDirectory(String dirPath) async {
    final archive = Archive();
    final dir = Directory(dirPath);
    if (!await dir.exists()) return [];

    await _addDirToArchive(archive, dir, '');
    return ZipEncoder().encode(archive) ?? [];
  }

  Future<void> _addDirToArchive(
      Archive archive, Directory dir, String prefix) async {
    await for (final entity in dir.list()) {
      final name = p.join(prefix, p.basename(entity.path));
      if (entity is File) {
        try {
          final bytes = await entity.readAsBytes();
          archive.addFile(ArchiveFile(name, bytes.length, bytes));
        } catch (_) {}
      } else if (entity is Directory) {
        archive.addFile(ArchiveFile('$name/', 0, []));
        await _addDirToArchive(archive, entity, name);
      }
    }
  }
}
