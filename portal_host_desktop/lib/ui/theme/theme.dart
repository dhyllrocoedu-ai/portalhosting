import 'package:flutter/material.dart';

const _blueSeed = Color(0xFF1565C0);

class AppTheme {
  static ThemeData light() => ThemeData(
        useMaterial3: true,
        colorSchemeSeed: _blueSeed,
        brightness: Brightness.light,
        fontFamily: 'Roboto', // default body font
        textTheme: _minecraftTextTheme(Typography.material2021().white),
      );

  static ThemeData dark() => ThemeData(
        useMaterial3: true,
        colorSchemeSeed: _blueSeed,
        brightness: Brightness.dark,
        fontFamily: 'Roboto',
        textTheme: _minecraftTextTheme(Typography.material2021().white),
      );

  static TextTheme _minecraftTextTheme(TextTheme base) => base.copyWith(
        displayLarge: base.displayLarge?.copyWith(fontFamily: 'Minecraft'),
        displayMedium: base.displayMedium?.copyWith(fontFamily: 'Minecraft'),
        displaySmall: base.displaySmall?.copyWith(fontFamily: 'Minecraft'),
        headlineLarge: base.headlineLarge?.copyWith(fontFamily: 'Minecraft'),
        headlineMedium: base.headlineMedium?.copyWith(fontFamily: 'Minecraft'),
        headlineSmall: base.headlineSmall?.copyWith(fontFamily: 'Minecraft'),
        titleLarge: base.titleLarge?.copyWith(fontFamily: 'Minecraft'),
        titleMedium: base.titleMedium?.copyWith(fontFamily: 'Minecraft'),
        titleSmall: base.titleSmall?.copyWith(fontFamily: 'Minecraft'),
      );
}
