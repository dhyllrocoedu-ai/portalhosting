# Changelog

## v5.1.0--desktopv2 (2026-08-11)

### Features
- **Player Detail screen** — New dedicated screen per player with real Mojang skin bitmap (when online), pixel-art fallback (when offline), UUID with copy button, name history, first/last seen (from usercache.json + console log join events), whitelisted/operator/banned status chips, and quick actions (kick, ban with reason, OP/De-OP, whitelist toggle)
- **World Map tab** — New "Map" tab in ServerDetailScreen. Phase A+D: visualizes which chunks have been generated in the world (region outline), plus live player position markers polled every 3 seconds via RCON (`data get entity`). Pan with drag, zoom 1×-16× per chunk cell, click a chunk to see its coords. World dir dropdown (overworld/nether/end).
- **PlayerProfileRepository** — Reads `<serverDir>/usercache.json`, normalizes UUIDs to dashed form, joins with `console_logs` table for first/last seen timestamps
- **MojangSkinService** — Ktor-based sessionserver.mojang.com client with 5s/10s timeouts, per-UUID mutex dedupe, 429 handled gracefully
- **SkinRenderCache** — Two-tier cache (memory LRU + disk PNG + URL sidecar) so skin bitmaps persist across launches
- **RegionFileIndex** — Parses `.mca` location tables only (Phase A; biome/terrain decoding deferred to a pluggable `ChunkDecoder` interface)
- **EntityPositionService** — RCON-driven live XYZ for online players, used by the World Map

### Bug Fixes
- **MinecraftHeadIcon SKIN_COLORS bounds** — Added missing index 9 (Skeleton bone color) so the Skeleton skin variant no longer crashes with `IndexOutOfBoundsException` when a player's name hash selects it

### Tests
- `RegionFileIndexTest` — synthesized `.mca` location tables covering all-present, all-absent, mixed
- `PlayerProfileRepositoryTest` — UUID normalization (dashes, uppercase, invalid)
- `MojangSkinServiceTest` — MockEngine fixtures for 200 OK and 429 responses

## v5.0.0--desktopv2 (2026-07-17)

### Features
- **Dashboard storage card** — Live used/total GB from app data directory
- **Dashboard TunnelCard** — Live playit.gg tunnel status with Connect/Disconnect button (Not Connected, Downloading, Claim Required, Connecting, Connected, Error)
- **Desktop JDK Install button** — Install/Reinstall JDK 21 from Settings with progress bar
- **Playit.gg tunnel auto-download** — Automatically downloads tunnel binary from GitHub releases if missing
- **Claim URL flow** — Detects claim URLs from tunnel output and opens in browser
- **NeoForge, Folia, Purpur server providers** — Added support for all 3 server types in desktop app

### Improvements
- **Settings persistence** — Rewrote Preferences with auto-save delegate, removed UI-level hacky `putString` calls
- **Data directory configuration** — Properly persists data directory across restarts
- **Adoptium JDK auto-download** — Replaced hardcoded Temurin release URLs with dynamic Adoptium API endpoint (always gets latest GA build, no more manual version tracking)
- **Server provider timeouts** — Added `readTextWithTimeout()` helper (10s connect / 30s read), applied to all 7 providers
- **Server provider SHA256** — Desktop PaperProvider now fetches SHA256 from build response
- **FAB visibility** — FAB now shows only on Servers screen (not Dashboard)

### Bug Fixes
- **Android PaperProvider URL** — Fixed from `fill.papermc.io/v3/` to `api.papermc.io/v2/projects/paper`
- **Desktop FabricProvider URL** — Changed from installer JAR to server JAR URL
- **Android ForgeProvider URL** — Changed from `universal.jar` to `installer.jar`
- **Server provider timeouts** — Added connection-level timeouts (30s / 300s) to all `downloadBuild()` methods
- **All provider `readText()` calls** — Replaced with `readTextWithTimeout()` for Folia and Purpur providers

## v4.0.0--mobilev2 (2026-07-17)

### Features
- **NeoForge, Folia, Purpur providers** — Added all 3 server types to Android Create Source wizard with download option cards, version/build pickers, and server JAR downloads
- **JDK error display** — Install failures now shown in red text in Settings Java Runtime card
- **JDK install progress StateFlow** — Added public `installProgress: StateFlow<Float>` to JavaRuntimeManager

### Improvements
- **Adoptium JDK source** — Replaced self-hosted GitHub Releases tarball with dynamic Adoptium API endpoint (no manual asset upload needed, always gets latest JDK 21 GA build)
- **Simplified JDK extraction** — Uses `tar -xzf` directly (removed XZ decompression, no more Termux .deb parsing)
- **Simplified fixupLibraries()** — No-op since Adoptium JDK doesn't need Termux system library provisioning

### Bug Fixes
- **PaperProvider URL** — Fixed from `fill.papermc.io/v3/` to `api.papermc.io/v2/projects/paper`
- **ForgeProvider URL** — Fixed from `universal.jar` to `installer.jar`

## v4.5.0--desktopv2 (2026-07-15)

### Features
- **Compose Multiplatform desktop app** — Full rewrite from Flutter to Kotlin Multiplatform + Compose Desktop
- **Dashboard** — Server overview with quick actions, live stats, player list, activity timeline
- **Server creation wizard** — 7 server types: Vanilla, Paper, Fabric, Forge, NeoForge, Folia, Purpur
- **Server detail screen** — Tabbed interface with console, files, players, performance, logs, RCON
- **Live console** — Real-time log streaming with command input
- **File manager** — Breadcrumb navigation, sort by Name/Date/Size, rename, delete, inline text editor
- **Player management** — Player list with Kick/Ban/OP command shortcuts
- **Performance monitoring** — CPU/RAM/TPS charts
- **Backup manager** — ZIP backup creation/restoration
- **RCON client** — Remote command interface
- **Process monitor** — CPU/RAM/TPS polling for running servers
- **JDK auto-management** — Downloads Temurin JDKs from Adoptium (8/11/17/21)
- **Server downloader** — Downloads server JARs from 7 providers
- **Playit.gg tunnel integration** — Public access tunneling
- **Koin dependency injection** — Modular DI across all components
- **SQLDelight database** — Server config and state persistence
- **Multiplatform logging** — Structured logging via kotlin-logging + Logback

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
