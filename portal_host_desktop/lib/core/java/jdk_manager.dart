import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:path/path.dart' as p;
import 'package:crypto/crypto.dart';
import 'package:archive/archive.dart';
import 'package:http/http.dart' as http;

class JdkManager {
  late final Directory _jdkDir;
  late final Directory _downloadDir;

  JdkManager() {
    final appDirPath = Platform.isWindows
        ? (Platform.environment['APPDATA'] ?? '') + r'\PortalHost'
        : (Platform.environment['HOME'] ?? '') + '/.portalhost';
    _jdkDir = Directory(p.join(appDirPath, 'jdk'));
    _downloadDir = Directory(p.join(appDirPath, 'downloads'));
  }

  Future<String?> getJavaHome() async {
    if (!await _jdkDir.exists()) {
      return null;
    }

    final entries = await _jdkDir.list().toList();
    for (final entry in entries) {
      if (entry is Directory && entry.path.contains('jdk-21')) {
        final binDir = Directory(p.join(entry.path, 'bin'));
        if (await binDir.exists()) {
          return binDir.path;
        }
      }
    }
    return null;
  }

  Future<bool> isJdkInstalled() async {
    final path = await getJavaHome();
    return path != null && path.isNotEmpty;
  }

  Future<String?> getJavaExecutable() async {
    final javaHome = await getJavaHome();
    if (javaHome == null) return null;
    return p.join(javaHome, Platform.isWindows ? 'java.exe' : 'java');
  }

  Future<JdkInstallResult> downloadAndInstallJdk({
    required Function(double progress, String status) onProgress,
  }) async {
    try {
      onProgress(0.05, 'Fetching Oracle JDK 21 download URL...');

      String expectedSha256 = '';
      try {
        final shaRes = await http.get(Uri.parse('https://download.oracle.com/java/21/latest/jdk-21_windows-x64_bin.zip.sha256'));
        if (shaRes.statusCode == 200) {
          expectedSha256 = sha256.convert(utf8.encode(shaRes.body.trim().split(' ').first)).toString();
        }
      } catch (e) {
        print('Could not fetch SHA256: $e');
      }

      onProgress(0.1, 'Downloading Oracle JDK 21...');

      await _downloadDir.create(recursive: true);
      final downloadPath = p.join(_downloadDir.path, 'jdk-21_windows-x64_bin.zip');
      final downloadFile = File(downloadPath);

      if (await downloadFile.exists()) {
        await downloadFile.delete();
      }

      // Download with progress using send() for streaming
      final client = http.Client();
      final request = http.Request('GET', Uri.parse('https://download.oracle.com/java/21/latest/jdk-21_windows-x64_bin.zip'));
      final streamedResponse = await client.send(request);
      if (streamedResponse.statusCode != 200) {
        throw Exception('Failed to download JDK: HTTP ${streamedResponse.statusCode}');
      }

      final totalBytes = streamedResponse.contentLength ?? 0;
      final sink = File(downloadPath).openWrite();
      var received = 0;

      await for (final chunk in streamedResponse.stream) {
        sink.add(chunk);
        received += chunk.length;
        if (totalBytes > 0) {
          final progress = (received / totalBytes) * 0.9;
          onProgress(0.1 + progress.clamp(0.0, 0.8), 'Downloading JDK 21... ${(progress * 100).toStringAsFixed(1)}%');
        }
      }
      await sink.flush();
      await sink.close();

      // Verify checksum if available
      if (expectedSha256.isNotEmpty) {
        onProgress(0.92, 'Verifying checksum...');
        final actualSha256 = await _calculateSha256(File(downloadPath));
        if (actualSha256.toLowerCase() != expectedSha256.toLowerCase()) {
          throw Exception('SHA256 mismatch. Expected: $expectedSha256, Got: $actualSha256');
        }
      }

      onProgress(0.95, 'Extracting JDK...');

      // Extract ZIP
      await _jdkDir.create(recursive: true);
      final zipBytes = await File(downloadPath).readAsBytes();
      final archive = ZipDecoder().decodeBytes(zipBytes);

      for (final file in archive) {
        if (file.isFile) {
          await File(p.join(_jdkDir.path, file.name))
              .create(recursive: true)
              .then((f) => f.writeAsBytes(file.content as List<int>));
        } else {
          await Directory(p.join(_jdkDir.path, file.name)).create(recursive: true);
        }
      }

      // Clean up download
      try {
        await File(downloadPath).delete();
      } catch (_) {}

      // Verify installation
      final javaHome = await getJavaHome();
      if (javaHome == null || javaHome.isEmpty) {
        throw Exception('JDK installation failed - java not found in extracted directory');
      }

      return JdkInstallResult(
        success: true,
        javaHome: javaHome,
        message: 'Oracle JDK 21 installed successfully at $javaHome',
      );
    } catch (e) {
      return JdkInstallResult(
        success: false,
        javaHome: '',
        message: 'Failed to install JDK: $e',
      );
    }
  }

  Future<void> uninstallJdk() async {
    if (await _jdkDir.exists()) {
      await _jdkDir.delete(recursive: true);
    }
  }

  Future<String> _calculateSha256(File file) async {
    final bytes = await file.readAsBytes();
    final digest = sha256.convert(bytes);
    return digest.toString();
  }
}

class JdkInstallResult {
  final bool success;
  final String javaHome;
  final String message;

  JdkInstallResult({
    required this.success,
    required this.javaHome,
    required this.message,
  });
}
