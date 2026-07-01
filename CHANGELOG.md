# Changelog

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
