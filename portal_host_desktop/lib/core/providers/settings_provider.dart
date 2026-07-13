import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

class AppSettings {
  final ThemeMode themeMode;
  final String? javaPath;
  final int maxBackups;
  final int backupIntervalMinutes;
  final bool setupComplete;
  final String serversDir;

  const AppSettings({
    this.themeMode = ThemeMode.system,
    this.javaPath,
    this.maxBackups = 10,
    this.backupIntervalMinutes = 30,
    this.setupComplete = false,
    this.serversDir = '',
  });

  AppSettings copyWith({
    ThemeMode? themeMode,
    String? javaPath,
    int? maxBackups,
    int? backupIntervalMinutes,
    bool? setupComplete,
    String? serversDir,
  }) =>
      AppSettings(
        themeMode: themeMode ?? this.themeMode,
        javaPath: javaPath ?? this.javaPath,
        maxBackups: maxBackups ?? this.maxBackups,
        backupIntervalMinutes:
            backupIntervalMinutes ?? this.backupIntervalMinutes,
        setupComplete: setupComplete ?? this.setupComplete,
        serversDir: serversDir ?? this.serversDir,
      );
}

class SettingsNotifier extends StateNotifier<AppSettings> {
  SettingsNotifier() : super(const AppSettings()) {
    _load();
  }

  Future<void> _load() async {
    final prefs = await SharedPreferences.getInstance();
    final themeStr = prefs.getString('theme_mode') ?? 'system';
    state = AppSettings(
      themeMode: ThemeMode.values.firstWhere(
        (e) => e.name == themeStr,
        orElse: () => ThemeMode.system,
      ),
      javaPath: prefs.getString('java_path'),
      maxBackups: prefs.getInt('max_backups') ?? 10,
      backupIntervalMinutes: prefs.getInt('backup_interval') ?? 30,
      setupComplete: prefs.getBool('setup_complete') ?? false,
      serversDir: prefs.getString('servers_dir') ?? '',
    );
  }

  Future<void> setThemeMode(ThemeMode mode) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('theme_mode', mode.name);
    state = state.copyWith(themeMode: mode);
  }

  Future<void> setJavaPath(String path) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('java_path', path);
    state = state.copyWith(javaPath: path);
  }

  Future<void> setMaxBackups(int value) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setInt('max_backups', value);
    state = state.copyWith(maxBackups: value);
  }

  Future<void> setBackupInterval(int minutes) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setInt('backup_interval', minutes);
    state = state.copyWith(backupIntervalMinutes: minutes);
  }

  Future<void> completeSetup() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('setup_complete', true);
    state = state.copyWith(setupComplete: true);
  }

  Future<void> setServersDir(String dir) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('servers_dir', dir);
    state = state.copyWith(serversDir: dir);
  }
}

final settingsProvider =
    StateNotifierProvider<SettingsNotifier, AppSettings>((ref) {
  return SettingsNotifier();
});
