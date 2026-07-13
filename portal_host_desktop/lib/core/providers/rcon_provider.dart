import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../server/rcon_client.dart';

part 'rcon_provider.g.dart';

class RconConnection {
  final RconClient client;
  final String serverId;
  final String host;
  final int port;
  final String password;

  RconConnection({
    required this.client,
    required this.serverId,
    required this.host,
    required this.port,
    required this.password,
  });
}

@riverpod
class RconManager extends _$RconManager {
  @override
  Map<int, RconConnection> build() {
    return {};
  }

  Future<bool> connect(int serverId, String host, int port, String password) async {
    if (state.containsKey(serverId)) {
      // Already connected
      return true;
    }

    final client = RconClient(host: host, port: port, password: password);
    final success = await client.connect();

    if (success) {
      state = {
        ...state,
        serverId: RconConnection(
          client: client,
          serverId: serverId.toString(),
          host: host,
          port: port,
          password: password,
        ),
      };
      return true;
    } else {
      client.dispose();
      return false;
    }
  }

  Future<void> disconnect(int serverId) async {
    final connection = state[serverId];
    if (connection != null) {
      await connection.client.disconnect();
      state = Map.from(state)..remove(serverId);
    }
  }

  Future<String?> sendCommand(int serverId, String command) async {
    final connection = state[serverId];
    if (connection == null) return null;
    return connection.client.sendCommand(command);
  }

  Stream<String> getOutputStream(int serverId) {
    final connection = state[serverId];
    if (connection == null) return Stream.empty();
    return connection.client.outputStream;
  }

  void disposeAll() {
    for (final connection in state.values) {
      connection.client.dispose();
    }
    state = {};
  }
}