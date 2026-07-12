import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

class AppSettings {
  final ThemeMode themeMode;
  final String? javaPath;

  const AppSettings({
    this.themeMode = ThemeMode.system,
    this.javaPath,
  });

  AppSettings copyWith({ThemeMode? themeMode, String? javaPath}) =>
      AppSettings(
        themeMode: themeMode ?? this.themeMode,
        javaPath: javaPath ?? this.javaPath,
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
}

final settingsProvider =
    StateNotifierProvider<SettingsNotifier, AppSettings>((ref) {
  return SettingsNotifier();
});
