# PortalHost Desktop App — Implementation Plan

**Tech Stack:** Flutter + Riverpod + Drift + dart:io  
**Targets:** Windows (NSIS), macOS (DMG), Linux (AppImage)

---

## 1. Project Structure

```
portal_host_desktop/
├── lib/
│   ├── main.dart
│   ├── app.dart                     # MaterialApp + GoRouter
│   ├── core/
│   │   ├── database/
│   │   │   ├── database.dart        # Drift DB definition
│   │   │   ├── tables.dart          # All tables
│   │   │   └── database.g.dart      # Generated
│   │   ├── server/
│   │   │   ├── process_manager.dart # dart:io Process lifecycle
│   │   │   ├── server_config.dart   # Server model
│   │   │   ├── server_type.dart     # Enum (Paper, Forge, etc.)
│   │   │   ├── server_downloader.dart
│   │   │   └── console_streamer.dart
│   │   ├── backup/
│   │   │   └── backup_manager.dart
│   │   ├── network/
│   │   │   └── rcon_client.dart
│   │   └── providers/               # Riverpod providers
│   │       ├── server_provider.dart
│   │       ├── console_provider.dart
│   │       ├── settings_provider.dart
│   │       └── performance_provider.dart
│   ├── ui/
│   │   ├── theme/
│   │   │   ├── theme.dart           # Material 3 (mirrors Android)
│   │   │   └── fonts.dart
│   │   ├── screens/
│   │   │   ├── home_screen.dart
│   │   │   ├── servers_screen.dart
│   │   │   ├── create_server_screen.dart
│   │   │   ├── server_detail_screen.dart
│   │   │   ├── console_screen.dart
│   │   │   ├── server_files_screen.dart
│   │   │   ├── rcon_screen.dart
│   │   │   └── settings_screen.dart
│   │   └── widgets/
│   │       ├── server_card.dart
│   │       ├── console_output.dart
│   │       ├── file_browser.dart
│   │       └── performance_chart.dart
│   └── shared/
│       ├── models/
│       └── utils/
├── assets/
│   ├── fonts/
│   └── icons/
├── test/
├── pubspec.yaml
└── README.md
```

---

## 2. Database (Drift)

```dart
// servers
CREATE TABLE servers (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  jar_path TEXT NOT NULL,
  port INTEGER DEFAULT 25565,
  max_players INTEGER DEFAULT 20,
  server_type TEXT NOT NULL,         // paper, forge, vanilla, etc.
  mc_version TEXT,
  java_args TEXT DEFAULT '',
  auto_backup INTEGER DEFAULT 1,    // boolean
  auto_restart INTEGER DEFAULT 0,   // boolean
  resource_pack_url TEXT,
  resource_pack_sha1 TEXT,
  status TEXT DEFAULT 'stopped',    // stopped, running, starting, stopping, crashed
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE server_properties (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  server_id INTEGER NOT NULL REFERENCES servers(id) ON DELETE CASCADE,
  key TEXT NOT NULL,
  value TEXT NOT NULL
);

CREATE TABLE backups (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  server_id INTEGER NOT NULL REFERENCES servers(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  path TEXT NOT NULL,
  size INTEGER NOT NULL,            // bytes
  created_at TEXT DEFAULT (datetime('now'))
);

-- Console lines are kept in-memory (capped at 5000 per server).
-- On desktop, auto-save to file every 500 lines (same as Android).
-- No SQLite storage — avoids write amplification during high-throughput logs.
```

---

## 3. Riverpod State Architecture

```
── Server List ──────────────────────────────
  serverListProvider     → StateNotifier<List<ServerConfig>>
  activeServerProvider   → StateNotifier.family<ServerState, int>
  
── Process ───────────────────────────────────
  serverProcessProvider  → StreamProvider.family<ProcessStatus, int>
  consoleProvider        → StreamProvider.family<ConsoleLine, int>
  
── Data ──────────────────────────────────────
  backupListProvider     → FutureProvider.family<List<BackupEntry>, int>
  backupManagerProvider  → Provider<BackupManager>
  processManagerProvider → Provider<ProcessManager>
  
── Settings ──────────────────────────────────
  settingsProvider       → StateNotifier<AppSettings>  (SharedPreferences)
  themeProvider          → Provider<ThemeMode>
```

**Key Riverpod rules:**
- `processManagerProvider` is a singleton — one `ProcessManager` manages all child processes
- Each server's console stream is managed by `consoleProvider.family(serverId)` — creates a new stream per server, cancels when `serverId` leaves scope
- `settingsProvider` persists to SharedPreferences (dark theme, java path, etc.)

---

## 4. Process Management (dart:io)

**Orphan protection:** Register `Isolate.current.addOnExitHook()` to kill all child processes when the app exits. On Windows, use `taskkill /F /PID` as fallback if `Process.kill()` doesn't propagate to children.

**Java runtime detection:**
1. Check `JAVA_HOME` env var → `$JAVA_HOME/bin/java`
2. Check `PATH` for `java` executable
3. If not found, prompt user to locate JDK via file picker
4. Cache path in SharedPreferences (same as Android's `JavaRuntimeManager`)

```dart
class ProcessManager {
  final Map<int, Process> _processes = {};
  final Map<int, StreamSubscription> _subscriptions = {};

  // Starts Java server, streams stdout/stderr via StreamController
  Future<Process> start(int serverId, String jarPath, String args, String workDir);

  // Sends command via stdin, handles SIGKILL/SIGTERM
  Future<void> stop(int serverId, {bool force = false});
  
  // Writes command to stdin
  void writeCommand(int serverId, String command);

  // Returns broadcast stream for a server's console output
  Stream<String> consoleStream(int serverId);
  
  // Cleanup all processes
  void dispose();
}
```

**Console streaming:** `Process.stdout.transform(utf8.decoder).transform(const LineSplitter())` → each line emitted to a `StreamController.broadcast()` per serverId. `consoleProvider` wraps this with `StreamProvider`.

---

## 5. Routing (go_router)

```
/                              → HomeScreen (dashboard)
/servers                       → ServersScreen (list + create)
/servers/create                → CreateServerScreen (wizard)
/servers/:id                   → ServerDetailScreen (tabs: console, files, props, backups, plugins)
/servers/:id/console           → ConsoleScreen (full-screen console)
/servers/:id/files             → ServerFilesScreen (file browser)
/servers/:id/rcon              → RconScreen
/settings                      → SettingsScreen
```

---

## 6. Theme Parity with Android

| Android | Desktop |
|---------|---------|
| Material 3 | ✅ Same `MaterialTheme` + `ColorScheme` |
| Dark/Light toggle | ✅ SharedPreferences via settings |
| Dynamic (Monet) color | ❌ Not available on desktop — default to Material Blue |
| Minecraft pixel font | ✅ Same font asset, applied to headline/title styles |
| System font for body | ✅ Same |

---

## 7. Feature Parity Matrix

| Feature | Android | Desktop | Notes |
|---------|---------|---------|-------|
| Server CRUD | ✅ | ✅ | Migrate existing repo |
| Start/Stop/Restart | ✅ | ✅ | dart:io Process |
| Console (streaming) | ✅ | ✅ | LineSplitter + Stream |
| Console search | ✅ | ✅ | Same |
| Server properties editor | ✅ | ✅ | Same |
| File browser | ✅ | ✅ | file_picker for import |
| Plugin/mod/datapack mgmt | ✅ | ✅ | Same |
| Server type downloader | ✅ | ✅ | Paper, Forge, Purpur, etc. |
| RCON | ✅ | ✅ | Socket |
| Auto-backup | ✅ | ✅ | Timer-based |
| Backup restore | ✅ | ✅ | ZIP decode |
| Performance monitoring | ✅ | ✅ | system_info2 |
| Theme (dark/light) | ✅ | ✅ | |
| Minecraft pixel font | ✅ | ✅ | |
| Notifications | ✅ | ✅ | Desktop toast/tray |
| Auto-update | ❌ (manual APK) | ✅ | flutter_distributor |
| System tray | ❌ | ✅ | window_manager |
| Multi-window | ❌ | ✅ | Separate console window |

---

## 8. Dependencies (pubspec.yaml)

```yaml
dependencies:
  flutter_riverpod: ^2.5.0
  riverpod_annotation: ^2.3.0
  drift: ^2.16.0
  sqlite3_flutter_libs: ^0.5.0
  path_provider: ^2.1.0
  go_router: ^14.0.0
  window_manager: ^0.3.0
  http: ^1.2.0
  system_info2: ^4.0.0
  flutter_local_notifications: ^17.0.0
  file_picker: ^8.0.0
  path: ^1.9.0
  intl: ^0.19.0
  archive: ^3.6.0
  shared_preferences: ^2.2.0
  url_launcher: ^6.2.0

dev_dependencies:
  flutter_test:
    sdk: flutter
  drift_dev: ^2.16.0
  build_runner: ^2.4.0
  riverpod_generator: ^2.4.0
  flutter_lints: ^4.0.0
```

---

## 9. Implementation Phases

### Phase 1: Foundation (Week 1-2)

- [ ] 1.1 `flutter create portal_host_desktop` with platform configs
- [ ] 1.2 Add all dependencies to pubspec.yaml
- [ ] 1.3 Drift database: `tables.dart` (servers, server_properties, backups) + `database.dart` + DAOs
- [ ] 1.4 Run build_runner for drift codegen
- [ ] 1.5 Theme: Material 3 dark/light ColorScheme (mirror Android), pixel font bundle + Typography
- [ ] 1.6 GoRouter: all 9 routes with placeholder screens (`const Placeholder()`)
- [ ] 1.7 `ServerConfig` model class (data class matching `servers` table)
- [ ] 1.8 `ServerState` model (status enum, uptime, exit code)
- [ ] 1.9 Riverpod providers: `serverListProvider`, `activeServerProvider.family`
- [ ] 1.10 Riverpod providers: `settingsProvider` + ThemeMode derived provider
- [ ] 1.11 `ServersNotifier` (StateNotifier) — CRUD via Drift DAO
- [ ] 1.12 `SettingsNotifier` (StateNotifier) — SharedPreferences for dark mode, java path
- [ ] 1.13 **HomeScreen** — dashboard with server count, storage, quick actions (no live stats yet)
- [ ] 1.14 **ServersScreen** — server list from `serverListProvider`, create FAB, swipe-to-delete
- [ ] 1.15 Build: verify clean compile + Drift codegen works on Windows

### Phase 2: Server Management (Week 3-4)

- [ ] 2.1 `ProcessManager` — `start()`, `stop()`, `restart()`, `writeCommand()`, `consoleStream()`
- [ ] 2.2 Orphan cleanup: `addOnExitHook` kills all child processes
- [ ] 2.3 `ConsoleStreamer` — UTF-8 decode, line splitting, ANSI stripping, 5000-line ring buffer
- [ ] 2.4 Java runtime detection (JAVA_HOME → PATH → file picker fallback)
- [ ] 2.5 `consoleProvider` — `StreamProvider.family` wrapping `consoleStream(serverId)`
- [ ] 2.6 `ServerDownloader` — port all 7 providers (Paper, Vanilla, Forge, NeoForge, Purpur, Folia, Fabric)
- [ ] 2.7 **CreateServerScreen** — name, type selector, version/build picker, port, memory slider
- [ ] 2.8 **ConsoleScreen** — LazyColumn with auto-scroll, search, log level filter, copy/save
- [ ] 2.9 **ServerDetailScreen** — tabbed (Console, Files, Properties, Backups, Plugins/Mods)
- [ ] 2.10 Wire start/stop/restart to ProcessManager + update provider state
- [ ] 2.11 Auto-restart on crash (respect retry limit, same as Android)
- [ ] 2.12 Server status polling (ProcessManager emits status changes)

### Phase 3: Files, Config & Backups (Week 5-6)

- [ ] 3.1 **ServerFilesScreen** — directory tree, file list, breadcrumbs, sort/search
- [ ] 3.2 File operations: rename, delete (with .trash undo), compress, export, upload
- [ ] 3.3 File editor: monospace editor for text files, 10MB size guard
- [ ] 3.4 Properties editor: key-value grid with add/remove/edit, live search
- [ ] 3.5 Plugin/mod/datapack management: list, upload, delete, enable/disable
- [ ] 3.6 `BackupManager` — create (ZIP worlds+config), restore (with zip-slip guard), list, delete
- [ ] 3.7 Auto-backup timer (every 6h when server running, configurable)
- [ ] 3.8 Backup retention (max 10 by default, configurable)
- [ ] 3.9 **SettingsScreen** — Java path, theme toggle, backup interval, max backups, about

### Phase 4: Advanced Features (Week 7-8)

- [ ] 4.1 **RconScreen** — socket connect/disconnect, command history, response list
- [ ] 4.2 RCON reconnection logic with exponential backoff
- [ ] 4.3 Performance monitoring — CPU (system-wide + per-process), RAM, disk usage
- [ ] 4.4 `PerformanceProvider` — polls every 3s, emits to chart
- [ ] 4.5 Desktop notifications — server crash, backup complete, low disk space
- [ ] 4.6 System tray — minimize to tray, tray menu (start/stop all, quit)
- [ ] 4.7 Close-to-tray (app stays running in background)
- [ ] 4.8 Auto-update — check GitHub releases on startup, download + apply
- [ ] 4.9 Cross-platform test: Windows (dev), macOS (CI), Linux (CI)
- [ ] 4.10 Packaging: NSIS (Windows), DMG (macOS), AppImage (Linux)
- [ ] 4.11 README with build instructions + system requirements

---

## 10. Error Handling Strategy

- **Process crash:** `ProcessManager` detects exit via `process.exitCode` future → emits `ProcessStatus.crashed` → notification
- **Port conflict:** Catch `SocketException` on bind → show dialog with suggestion
- **Disk full:** Catch `FileSystemException` on backup → disable auto-backup, show persistent warning
- **Network failure:** Retry with backoff in ServerDownloader; show offline indicator
- **Unexpected restart:** If process exits with code != 0, check auto-restart flag & count retries (max 3)

---

## 11. Testing Strategy

| Layer | Approach | Tools |
|-------|----------|-------|
| Database | Unit test DAOs with in-memory Drift | `drift_test` |
| Providers | Unit test Riverpod with overrides | `riverpod_test` |
| ProcessManager | Integration test with mock Java process | Custom test script |
| UI | Widget tests per screen | `flutter_test` |
| Integration | End-to-end with real process | `integration_test` |
