enum ServerStatus {
  stopped('stopped'),
  starting('starting'),
  running('running'),
  stopping('stopping'),
  crashed('crashed');

  final String value;
  const ServerStatus(this.value);

  static ServerStatus fromString(String s) =>
      ServerStatus.values.firstWhere((e) => e.value == s, orElse: () => stopped);
}

class ServerState {
  final ServerStatus status;
  final int exitCode;
  final int uptimeSeconds;
  final double cpuUsage;
  final int memoryBytes;
  final DateTime? startTime;

  const ServerState({
    this.status = ServerStatus.stopped,
    this.exitCode = 0,
    this.uptimeSeconds = 0,
    this.cpuUsage = 0,
    this.memoryBytes = 0,
    this.startTime,
  });

  ServerState copyWith({
    ServerStatus? status,
    int? exitCode,
    int? uptimeSeconds,
    double? cpuUsage,
    int? memoryBytes,
    DateTime? startTime,
  }) =>
      ServerState(
        status: status ?? this.status,
        exitCode: exitCode ?? this.exitCode,
        uptimeSeconds: uptimeSeconds ?? this.uptimeSeconds,
        cpuUsage: cpuUsage ?? this.cpuUsage,
        memoryBytes: memoryBytes ?? this.memoryBytes,
        startTime: startTime ?? this.startTime,
      );
}
