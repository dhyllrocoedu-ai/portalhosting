import 'package:flutter/material.dart';

const _blueSeed = Color(0xFF1565C0);

class AppTheme {
  static ThemeData light() => ThemeData(
        useMaterial3: true,
        colorSchemeSeed: _blueSeed,
        brightness: Brightness.light,
        fontFamily: 'Minecraft',
        typography: Typography.material2021(),
      );

  static ThemeData dark() => ThemeData(
        useMaterial3: true,
        colorSchemeSeed: _blueSeed,
        brightness: Brightness.dark,
        fontFamily: 'Minecraft',
        typography: Typography.material2021(),
      );
}
