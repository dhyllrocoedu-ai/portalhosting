import 'dart:io';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import 'package:path/path.dart' as p;
import '../../shared/models/server_config.dart';
import '../../shared/models/server_state.dart';
import 'database_provider.dart';
import 'process_provider.dart';

part 'server_provider.g.dart';

@riverpod
class SelectedServerId extends _$SelectedServerId {
  @override
  int? build() => null;

  void select(int? id) {
    state = id;
  }
}

@riverpod
class ServerList extends _$ServerList {
  @override
  Future<List<ServerConfig>> build() async {
    final db = ref.watch(databaseProvider);
    final rows = await db.getAllServers();
    return rows.map(ServerConfig.fromDb).toList();
  }

  Future<void> addServer(ServerConfig config) async {
    final db = ref.watch(databaseProvider);
    await db.insertServer(config.toInsertCompanion());
    ref.invalidateSelf();
  }

  Future<void> updateServer(ServerConfig config) async {
    final db = ref.watch(databaseProvider);
    await db.updateServer(config.toCompanion());
    ref.invalidateSelf();
  }

  Future<void> deleteServer(int id) async {
    final db = ref.watch(databaseProvider);
    await db.deleteServer(id);
    ref.invalidateSelf();
  }

  Future<void> createServerProperties({
    required String serverName,
    required String serverPath,
    required String motd,
    required String gamemode,
    required String difficulty,
    required int port,
    required int maxPlayers,
    required bool eulaAccepted,
  }) async {
    final propsFile = File(p.join(serverPath, 'server.properties'));
    final eulaFile = File(p.join(serverPath, 'eula.txt'));
    
    final props = '''# Server properties
server-port=${port}
max-players=${maxPlayers}
gamemode=${gamemode}
difficulty=${difficulty}
server-name=${serverName}
motd=${motd}
online-mode=true
prevent-proxy-connections=false
enable-status=true
''';
    
    await propsFile.writeAsString(props);
    
    if (eulaAccepted) {
      await eulaFile.writeAsString('eula=true');
    }
  }
}

@riverpod
class ActiveServer extends _$ActiveServer {
  @override
  ServerState build(int serverId) {
    final pm = ref.watch(processManagerProvider);
    return pm.stateFor(serverId);
  }

  void setStatus(ServerStatus status) {
    state = state.copyWith(status: status);
  }

  void refresh() {
    final pm = ref.watch(processManagerProvider);
    state = pm.stateFor(serverId);
  }
}
