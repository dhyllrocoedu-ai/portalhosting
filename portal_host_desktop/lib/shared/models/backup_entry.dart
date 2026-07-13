class BackupEntry {
  final int id;
  final int serverId;
  final String name;
  final String path;
  final int size;
  final DateTime createdAt;

  BackupEntry({
    required this.id,
    required this.serverId,
    required this.name,
    required this.path,
    required this.size,
    required this.createdAt,
  });

  String get sizeFormatted {
    if (size < 1024) return '${size}B';
    if (size < 1024 * 1024) return '${size ~/ 1024}KB';
    return '${size ~/ (1024 * 1024)}MB';
  }
}
