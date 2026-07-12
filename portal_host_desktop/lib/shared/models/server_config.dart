import 'package:drift/drift.dart';
import '../../core/database/database.dart' as db;
import 'server_state.dart';

class ServerConfig {
  final int id;
  final String name;
  final String jarPath;
  final int port;
  final int maxPlayers;
  final String serverType;
  final String? mcVersion;
  final String javaArgs;
  final bool autoBackup;
  final bool autoRestart;
  final String? resourcePackUrl;
  final String? resourcePackSha1;
  final ServerStatus status;
  final String? javaPath;
  final DateTime? createdAt;

  ServerConfig({
    required this.id,
    required this.name,
    required this.jarPath,
    this.port = 25565,
    this.maxPlayers = 20,
    required this.serverType,
    this.mcVersion,
    this.javaArgs = '',
    this.autoBackup = true,
    this.autoRestart = false,
    this.resourcePackUrl,
    this.resourcePackSha1,
    this.status = ServerStatus.stopped,
    this.javaPath,
    this.createdAt,
  });

  factory ServerConfig.fromDb(db.Server row) => ServerConfig(
        id: row.id,
        name: row.name,
        jarPath: row.jarPath,
        port: row.port,
        maxPlayers: row.maxPlayers,
        serverType: row.serverType,
        mcVersion: row.mcVersion,
        javaArgs: row.javaArgs,
        autoBackup: row.autoBackup == 1,
        autoRestart: row.autoRestart == 1,
        resourcePackUrl: row.resourcePackUrl,
        resourcePackSha1: row.resourcePackSha1,
        status: ServerStatus.fromString(row.status),
        javaPath: row.javaPath,
        createdAt: row.createdAt,
      );

  db.ServersCompanion toCompanion() => db.ServersCompanion(
        id: Value(id),
        name: Value(name),
        jarPath: Value(jarPath),
        port: Value(port),
        maxPlayers: Value(maxPlayers),
        serverType: Value(serverType),
        mcVersion: Value(mcVersion),
        javaArgs: Value(javaArgs),
        autoBackup: Value(autoBackup ? 1 : 0),
        autoRestart: Value(autoRestart ? 1 : 0),
        resourcePackUrl: Value(resourcePackUrl),
        resourcePackSha1: Value(resourcePackSha1),
        status: Value(status.value),
        javaPath: Value(javaPath),
        createdAt: Value(createdAt),
      );

  db.ServersCompanion toInsertCompanion() => db.ServersCompanion.insert(
        name: name,
        jarPath: jarPath,
        serverType: serverType,
        port: Value(port),
        maxPlayers: Value(maxPlayers),
        javaArgs: Value(javaArgs),
        autoBackup: Value(autoBackup ? 1 : 0),
        autoRestart: Value(autoRestart ? 1 : 0),
        status: Value(status.value),
      );

  ServerConfig copyWith({
    int? id,
    String? name,
    String? jarPath,
    int? port,
    int? maxPlayers,
    String? serverType,
    String? mcVersion,
    String? javaArgs,
    bool? autoBackup,
    bool? autoRestart,
    String? resourcePackUrl,
    String? resourcePackSha1,
    ServerStatus? status,
    String? javaPath,
    DateTime? createdAt,
  }) =>
      ServerConfig(
        id: id ?? this.id,
        name: name ?? this.name,
        jarPath: jarPath ?? this.jarPath,
        port: port ?? this.port,
        maxPlayers: maxPlayers ?? this.maxPlayers,
        serverType: serverType ?? this.serverType,
        mcVersion: mcVersion ?? this.mcVersion,
        javaArgs: javaArgs ?? this.javaArgs,
        autoBackup: autoBackup ?? this.autoBackup,
        autoRestart: autoRestart ?? this.autoRestart,
        resourcePackUrl: resourcePackUrl ?? this.resourcePackUrl,
        resourcePackSha1: resourcePackSha1 ?? this.resourcePackSha1,
        status: status ?? this.status,
        javaPath: javaPath ?? this.javaPath,
        createdAt: createdAt ?? this.createdAt,
      );
}
