# Changelog

## v4.2.5--desktop (2026-07-13)

### Features
- **Home Screen live actions** — Start/Stop/Restart/Delete server buttons all wired to ProcessManager (were stubs). Live console preview showing last 10 lines via real-time stream
- **Auto-backup timer** — Wired to server start/stop on both Home Screen and Server Detail Screen. Settings for interval (5–120 min) now actually start the timer
- **Proper clipboard copy** — `Clipboard.setData()` from `flutter/services.dart` for tunnel URLs and share text; `launchUrl()` for opening claim URLs in browser
- **Backup restore fix** — Extracts to server directory instead of backup directory (was restoring to wrong location)
- **PluginManager paths** — Points to actual server `plugins/`, `mods/`, `datapacks/` directories (was hardcoded to `'.'`)
- **RCON screen** — Full Source RCON protocol with connect/disconnect, command history, settings dialog
- **Server files screen** — Directory browser with breadcrumbs, search, upload, delete, rename
- **Playit.gg tunnel** — Auto-downloads agent binary, starts process, parses claim URL and tunnel URL
- **JDK 21 management** — Built-in Oracle JDK 21 downloader with SHA-256 verification, progress, auto-extract, install/uninstall from Settings
- **Tray service** — Minimize-to-tray, close-to-tray, system tray icon with Show/Settings/Quit menu
- **Console log persistence** — All console output stored in SQLite `ConsoleLogs` table with info/warn/error classification
- **Theme toggle** — System/Light/Dark persisted via SharedPreferences

### Bug Fixes
- `settings_screen.dart` — Complete rewrite (4764→254 lines). Was corrupted by 10x duplicated code from multiple overwrites
- `jdk_manager.dart` — Fixed 6 errors: final fields not initialized, `Response.stream` → `StreamedResponse.stream`, `IOSink.lengthSync` → manual tracking, `crypto` dependency added
- `jdk_management_screen.dart` — Fixed 6 errors: missing imports, `getJdkPath()` → `getJavaHome()`, double `JdkManager()` instantiation, Unix path branch bug
- `main.dart` — Fixed `sharedPreferencesProvider` override type mismatch (was passing `Future` instead of value)
- `PaperProvider` / `FoliaProvider` — Switched from sunset v2 API (410 Gone) to PaperMC Fill API v3
- `PurpurProvider` — Fixed `List` vs `Map` type cast error
- Removed all unused `_fallbackVersions` fields, unused imports, and dead code across the codebase
- `dart analyze` — Clean: **0 errors, 0 warnings**

### Infrastructure
- Desktop stack: Flutter + Riverpod + Drift + `dart:io` at `portal_host_desktop/`
- Android stack: Kotlin + Jetpack Compose at `native/`
- Database schema v3: 4 tables (Servers, ServerProperties, Backups, ConsoleLogs)
- All `.g.dart` files regenerated for Riverpod 2.6.1 + Drift 2.28.0
- Release builds: Windows exe + Android APK

## v2.8.5-native (2026-07-03)

### Improvements
- **Dashboard UI overhaul** — Left color border on server card, FlowRow stats (6-in-1 row), empty state with CTA, storage progress bar with color shift, Clear button in console preview, collapsed empty player section, section tinting
- **Main thread safety** — All file I/O moved to `Dispatchers.IO` in `doStartServer`, console log rotation, `initServerDir`
- **Removed stale Expo codebase** — Cleaned up `mobile/`, `shared/`, `desktop-agent/`, root npm config

### Bug Fixes
- Console log rotation no longer blocks UI thread (ANR fix)
- Server start no longer freezes UI during jar download/dir init

## v2.8.4-native (2026-07-02)

### Bug Fixes
- Fixed stop/restart race condition — `stop()` cancels `processJob`, `start()` cancels stale `processJob`, `processJob.finally` guards with `if (process === proc)`
- Auto-clear console on server start via `consoleStreamer.clear()`

## v2.8.3-native (2026-07-02)

### Improvements
- Replaced eye-straining neon green (`#00FF41`) console default with `#CCCCCC`
- File editor text/cursor changed from neon green to `#E0E0E0`
- PropertiesTab success message from `primary` to `onSurfaceVariant`

## v2.8.2-native (2026-07-02)

### Bug Fixes
- Fixed import filename — `getFileName()` queries `ContentResolver` for `OpenableColumns.DISPLAY_NAME` instead of `uri.lastPathSegment` (which returns content URI hash like `msf:16353`)

## v2.8.1-native (2026-07-02)

### Bug Fixes
- Fixed RAM allocation ignoring config — `AppNavigation.kt` now passes `javaArgs` from `server.minRam`/`server.maxRam` to `serverManager.start()` instead of hardcoded `-Xms512M -Xmx2G`
- Fixed base-2 unit accuracy (`times(1024)` in G→MB, `1_048_576L` byte factor, binary formatting)

## v2.8.0-native (2026-07-02)

### Features
- Added "Datapacks" tab to `ServerDetailScreen` — manage world/datapacks/ directory (list, upload, extract ZIP, delete)

## v2.0.0-native (2026-07-01)

Native Android rewrite — Kotlin + Jetpack Compose. Runs Minecraft Java servers on-device without Termux or root.

### New Features
- **Native Android app** — Full rewrite from React Native/Expo to Kotlin + Jetpack Compose
- **JDK 21 auto-management** — Downloads OpenJDK 21 from Termux repos, extracts, provisions system libs (`libz.so.1`, `libcrypto.so.3`, `libssl.so.3`, `libandroid-shmem.so`, `libandroid-spawn.so`)
- **Server downloader** — Paper, Vanilla, Fabric server JAR downloads with SHA-256 verification and progress callback
- **5-step server creation wizard** — Choose source (Paper/Vanilla/Fabric/Pick File) → Name → RAM → Config → Storage Check → EULA
- **Console** — Live log streaming, search with prev/next navigation, copy to clipboard, save logs to file, command history (up/down arrows)
- **Dashboard** — 9-section home: server card, quick actions (Start/Stop/Restart), live stats grid (CPU/RAM/TPS/Players), connection info, console preview, player list, activity timeline, storage breakdown, shortcuts
- **Server detail screen** — 7 scrollable tabs: Overview (player commands), Console, Properties, Worlds, Plugins, Mods, Backups
- **File manager** — Breadcrumb navigation, sort by Name/Date/Size, file-type icons, import via SAF, rename, share, compress (ZIP), export, delete, inline text editor for config files
- **Backup manager** — Create ZIP backups of worlds + config, list with size/date, restore, delete
- **Plugin manager** — Upload `.jar` files via SAF to `plugins/`, list, remove
- **Mod manager** — Same for `mods/` directory
- **World manager** — List world directories, rename, delete
- **Player manager** — Player list in Overview tab with Kick/Ban/OP command shortcuts
- **Quick commands** — Save-all, List, TPS, weather, time set, gamemode, whitelist toggle chips
- **Foreground service** — Server runs as foreground service with ongoing notification
- **Process monitor** — CPU from `/proc/<pid>/stat`, RAM from `/proc/<pid>/status` VmRSS, TPS from console parsing, all polled every 3 seconds
- **Network manager** — WiFi/cellular IP via `ConnectivityManager`, local IP detection
- **Storage info** — `StatFs` available space + per-directory walk for world/log/backup sizes
- **Activity log** — Timestamped event types for recent activity timeline
- **Auto-retry on hash failure** — Up to 2 restarts when Paperclip reports "Hash check failed"
- **Pre-seed Mojang jar** — Downloads Mojang jar before starting Paper so Paperclip skips its own download
- **Settings** — JDK status display, Reinstall/Fix Libraries/Remove Java buttons, Clear All Data with confirmation
- **Adaptive app icon** — `portal_host_icon.png` with dark adaptive background, all mipmap densities for pre-API 26 devices

### Bug Fixes
- FileProvider registered for file sharing and export
- `LinearProgressIndicator` uses lambda overload (Compose API update)
- `eula.txt` and `server.properties` read/writable via file editor

### Infrastructure
- Min SDK 24, target SDK 36, compile SDK 36
- AGP 8.8.2, Gradle 8.11.1
- Compose BOM, Material3, Navigation Compose
- OkHttp, kotlinx-serialization, tukaani-xz dependencies
