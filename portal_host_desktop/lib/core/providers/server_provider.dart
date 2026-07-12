import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../shared/models/server_config.dart';
import '../../shared/models/server_state.dart';
import 'database_provider.dart';
import 'process_provider.dart';

part 'server_provider.g.dart';

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
