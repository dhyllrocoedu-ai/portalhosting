import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../server/playit_tunnel.dart';

part 'playit_provider.g.dart';

@riverpod
class PlayitTunnelManager extends _$PlayitTunnelManager {
  @override
  Map<int, PlayitTunnel> build() {
    return {};
  }

  PlayitTunnel getOrCreate(int serverId, String serverName, int port, String baseDir) {
    return state.putIfAbsent(serverId, () => PlayitTunnel(
      serverId: serverId,
      serverName: serverName,
      port: port,
      baseDir: baseDir,
    ));
  }

  Future<bool> start({
    required int serverId,
    required String serverName,
    required int port,
    required String baseDir,
  }) async {
    final tunnel = getOrCreate(serverId, serverName, port, baseDir);
    return tunnel.start();
  }

  void disposeTunnel(int serverId) {
    final tunnel = state[serverId];
    if (tunnel != null) {
      tunnel.dispose();
      state = Map.from(state)..remove(serverId);
    }
  }

  void disposeAll() {
    for (final tunnel in state.values) {
      tunnel.dispose();
    }
    state = {};
  }
}