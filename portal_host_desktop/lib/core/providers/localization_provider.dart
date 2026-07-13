import 'package:flutter/material.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'localization_provider.g.dart';

enum SupportedLocale {
  en('en', 'English'),
  es('es', 'Español'),
  fr('fr', 'Français'),
  de('de', 'Deutsch'),
  ja('ja', '日本語'),
  zh('zh', '中文');

  final String code;
  final String name;

  const SupportedLocale(this.code, this.name);
}

class AppStrings {
  // App
  static const appName = 'Portal Host';
  static const version = 'v4.0.0 Desktop Edition';

  // Navigation
  static const home = 'Home';
  static const servers = 'Servers';
  static const settings = 'Settings';
  static const console = 'Console';
  static const files = 'Files';
  static const properties = 'Properties';
  static const plugins = 'Plugins';
  static const players = 'Players';
  static const performance = 'Performance';
  static const network = 'Network';
  static const logs = 'Logs';

  // Actions
  static const start = 'Start';
  static const stop = 'Stop';
  static const create = 'Create';
  static const edit = 'Edit';
  static const delete = 'Delete';
  static const cancel = 'Cancel';
  static const save = 'Save';
  static const export = 'Export';
  static const import = 'Import';
  static const backup = 'Backup';
  static const restore = 'Restore';
  
  // Messages
  static const noServers = 'No servers created yet';
  static const serverCreated = 'Server created successfully';
  static const serverDeleted = 'Server deleted';
  static const errorOccurred = 'An error occurred';
  
  // Settings
  static const theme = 'Theme';
  static const language = 'Language';
  static const javaPath = 'Java Path';
  static const maxBackups = 'Max Backups';
  static const backupInterval = 'Backup Interval';
  static const about = 'About';
}

@riverpod
class LocalizationManager extends _$LocalizationManager {
  @override
  SupportedLocale build() {
    // Default to English
    return SupportedLocale.en;
  }

  void setLocale(SupportedLocale locale) {
    state = locale;
    // In a real app, you would persist this to SharedPreferences
  }

  String translate(String key, [Map<String, String>? params]) {
    // This is a simple implementation
    // In a real app, you would load translations from JSON/YAML files
    String text = key;
    
    if (params != null) {
      params.forEach((k, v) {
        text = text.replaceAll('{$k}', v);
      });
    }
    
    return text;
  }
}

// Helper extension for easy access
extension AppLocalization on BuildContext {
  String tr(String key, [Map<String, String>? params]) {
    return AppStrings.appName; // Placeholder
  }
}
