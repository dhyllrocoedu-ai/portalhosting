import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'core/providers/settings_provider.dart';
import 'core/providers/server_provider.dart';
import 'ui/screens/home_screen.dart';
import 'ui/screens/servers_screen.dart';
import 'ui/screens/create_server_screen.dart';
import 'ui/screens/server_detail_screen.dart';
import 'ui/screens/console_screen.dart';
import 'ui/screens/server_files_screen.dart';
import 'ui/screens/rcon_screen.dart';
import 'ui/screens/settings_screen.dart';
import 'ui/screens/setup_screen.dart';
import 'ui/widgets/app_shell.dart';

final _routerProvider = Provider<GoRouter>((ref) {
  final settings = ref.watch(settingsProvider);

  return GoRouter(
    initialLocation: '/',
    redirect: (ctx, state) {
      if (!settings.setupComplete && state.uri.toString() != '/setup') {
        return '/setup';
      }
      if (settings.setupComplete && state.uri.toString() == '/setup') {
        return '/';
      }
      return null;
    },
    routes: [
      GoRoute(
        path: '/setup',
        builder: (_, _) => SetupScreen(
          onComplete: () {
            // navigation handled by redirect
          },
        ),
      ),
      StatefulShellRoute.indexedStack(
        builder: (_, __, navigationShell) =>
            AppShell(shell: navigationShell),
        branches: [
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/',
                builder: (_, _) => const HomeScreen(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/servers',
                builder: (_, _) => const ServersScreen(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/settings',
                builder: (_, _) => const SettingsScreen(),
              ),
            ],
          ),
        ],
      ),
      GoRoute(
        path: '/servers/create',
        builder: (_, _) => const CreateServerScreen(),
      ),
      GoRoute(
        path: '/servers/:id',
        builder: (_, state) => ServerDetailScreen(
            serverId: state.pathParameters['id']!),
      ),
      GoRoute(
        path: '/servers/:id/console',
        builder: (_, state) => ConsoleScreen(
            serverId: int.parse(state.pathParameters['id']!)),
      ),
      GoRoute(
        path: '/servers/:id/files',
        builder: (_, state) {
          final id = state.pathParameters['id']!;
          return _ServerFilesWrapper(serverId: id);
        },
      ),
      GoRoute(
        path: '/servers/:id/rcon',
        builder: (_, state) =>
            RconScreen(serverId: state.pathParameters['id']!),
      ),
    ],
  );
});

class _ServerFilesWrapper extends ConsumerWidget {
  final String serverId;
  const _ServerFilesWrapper({required this.serverId});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final id = int.parse(serverId);
    return ref.watch(serverListProvider).when(
          loading: () =>
              const Scaffold(body: Center(child: CircularProgressIndicator())),
          error: (e, _) => Scaffold(
              appBar: AppBar(title: const Text('Error')),
              body: Center(child: Text('$e'))),
          data: (servers) {
            final config = servers.where((s) => s.id == id).firstOrNull;
            if (config == null) {
              return Scaffold(
                appBar: AppBar(title: const Text('Not Found')),
                body: const Center(child: Text('Server not found')),
              );
            }
            return ServerFilesScreen(config: config);
          },
        );
  }
}

class App extends ConsumerWidget {
  const App({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settings = ref.watch(settingsProvider);
    final router = ref.watch(_routerProvider);
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
      routerConfig: router,
      debugShowCheckedModeBanner: false,
    );
  }
}
