import 'dart:io';

class JavaDetector {
  static Future<String?> detect() async {
    final fromEnv = Platform.environment['JAVA_HOME'];
    if (fromEnv != null) {
      final candidate = '$fromEnv\\bin\\java.exe';
      if (await File(candidate).exists()) return candidate;
    }

    final fromPath = await _findOnPath();
    if (fromPath != null) return fromPath;

    return null;
  }

  static Future<String?> _findOnPath() async {
    final path = Platform.environment['PATH'] ?? '';
    for (final dir in path.split(';')) {
      if (dir.trim().isEmpty) continue;
      final candidate = '${dir.trim()}\\java.exe';
      if (await File(candidate).exists()) return candidate;
      final candidate2 = '${dir.trim()}\\java';
      if (await File(candidate2).exists()) return candidate2;
    }
    return null;
  }

  static Future<bool> isValidJava(String path) async {
    try {
      final result = await Process.run(path, ['-version'],
          runInShell: true);
      return result.exitCode == 0;
    } catch (_) {
      return false;
    }
  }

  static Future<String?> detectVersion(String path) async {
    try {
      final result = await Process.run(path, ['-version'],
          runInShell: true);
      if (result.exitCode == 0) {
        final output = result.stderr as String;
        final match = RegExp(r'"(.*?)"').firstMatch(output);
        return match?.group(1);
      }
    } catch (_) {}
    return null;
  }
}
