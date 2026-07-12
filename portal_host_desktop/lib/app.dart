import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'core/providers/settings_provider.dart';
import 'ui/screens/home_screen.dart';
import 'ui/screens/servers_screen.dart';
import 'ui/screens/create_server_screen.dart';
import 'ui/screens/server_detail_screen.dart';
import 'ui/screens/console_screen.dart';
import 'ui/screens/server_files_screen.dart';
import 'ui/screens/rcon_screen.dart';
import 'ui/screens/settings_screen.dart';

final _router = GoRouter(
  initialLocation: '/',
  routes: [
    GoRoute(path: '/', builder: (_, _) => const HomeScreen()),
    GoRoute(path: '/servers', builder: (_, _) => const ServersScreen()),
    GoRoute(
        path: '/servers/create',
        builder: (_, _) => const CreateServerScreen()),
    GoRoute(
        path: '/servers/:id',
        builder: (_, state) => ServerDetailScreen(
            serverId: state.pathParameters['id']!)),
    GoRoute(
        path: '/servers/:id/console',
        builder: (_, state) => ConsoleScreen(
            serverId: int.parse(state.pathParameters['id']!))),
    GoRoute(
        path: '/servers/:id/files',
        builder: (_, state) =>
            ServerFilesScreen(serverId: state.pathParameters['id']!)),
    GoRoute(
        path: '/servers/:id/rcon',
        builder: (_, state) =>
            RconScreen(serverId: state.pathParameters['id']!)),
    GoRoute(
        path: '/settings', builder: (_, _) => const SettingsScreen()),
  ],
);

class App extends ConsumerWidget {
  const App({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settings = ref.watch(settingsProvider);
    return MaterialApp.router(
      title: 'Portal Host',
      themeMode: settings.themeMode,
      theme: ThemeData(
        useMaterial3: true,
        colorSchemeSeed: const Color(0xFF1565C0),
        brightness: Brightness.light,
        fontFamily: 'Minecraft',
      ),
      darkTheme: ThemeData(
        useMaterial3: true,
        colorSchemeSeed: const Color(0xFF1565C0),
        brightness: Brightness.dark,
        fontFamily: 'Minecraft',
      ),
      routerConfig: _router,
      debugShowCheckedModeBanner: false,
    );
  }
}
