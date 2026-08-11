# Changelog

## v5.1.0--desktopv2 (2026-08-11)

### Bug Fixes (hotfix, same version)
- **Update flow: process no longer duplicates after auto-restart.** `SingleInstanceLock` (FileChannel lock on `<dataDir>/portalhost.instance.lock`) is acquired in `main()` and released by a JVM shutdown hook. The PowerShell installer script now force-kills any lingering PortalHost.exe / portalhost-tagged Java processes, waits 3 s, runs the MSI, waits 5 s, then starts the freshly installed PortalHost.exe — the new process instantly acquires the single-instance lock, so duplicates cannot start.
- **Update flow: running servers and DB closed before install.** The update dialog now stops every running server and gives SQLite a brief moment to flush handles before launching the installer, so the MSI can replace `portalhost.db` and any child-process files without lock conflicts.
- **Version display no longer stale after self-update.** `BuildConfig.VERSION_NAME` was hardcoded to `5.0.69` in source, so every build (including `5.1.0`) still showed `5.0.69`. A Gradle task (`generateVersionResource`) now writes `<version>` into `src/desktopMain/resources/version.txt`, and `BuildConfig` reads it from the classpath. The MSI bundles `version.txt = "5.1.0"` inside the JAR so the title bar / About / Settings all show the real version after install.
- **World Map accuracy.** Map rendering now puts world (0,0) at the canvas center, computes the chunk bounding box from the actual loaded regions (with padding), and renders ungenerated areas as a darker grid so chunk boundaries are visible at high zoom. Spawn marker (yellow crosshair) is fixed at chunk (0,0). Player positions use `pos.x / pos.z` directly (not chunk-rounded), so a player at `x=125.3 z=-47.1` lands at the exact sub-chunk location, not the chunk corner. Hit-testing and the player-dot draw loop share the same coordinate transform.
- **Map download location.** Update installer no longer writes to `user.home`; downloads now go to `defaultDataDir()`.

### Hotfix (map rendering + zoom)
- **Map content no longer overflows onto toolbar / tabs.** ServerDetailScreen wraps the tab content in `Box(modifier = Modifier.weight(1f).fillMaxWidth())` so the Map tab gets a constrained area instead of stretching over the rest of the page. The WorldMapScreen root and Canvas also add `.clipToBounds()` defensively.
- **Decimal zoom levels.** Toolbar now exposes 10 zoom steps (1.5×, 2×, 3×, 4×, 6×, 8×, 12×, 16×, 24×, 32×) instead of the previous 5. Display label rounds to one decimal (e.g. `1.5x`, `2.5x`).
- **Mouse scroll-wheel zoom.** Pinch / scroll gestures on the map now change zoom by stepping through `ZOOM_LEVELS` instead of being ignored. The +/- toolbar buttons and the scroll wheel stay in sync.

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

### Hotfix (biome-colored world map + player management rework)
- **Biome-colored World Map.** The Map tab now paints each generated chunk with its actual biome. `NbtParser` (compact NBT binary reader with bounds checks + 32 MB cap), `ChunkBiomeReader` (Anvil payload decoder — gzip/zlib/raw, modern paletted `sections[].biomes` and legacy `Level.Biomes` int/byte arrays, 50-entry legacy biome name map), and `AnvilChunkDecoder` (per-region file cache keyed on `path:lastModified:x:z`) decode `.mca` chunks on `Dispatchers.IO`. The new Terrain toolbar toggle switches between biome colors and the flat region-outline view; ungenerated chunks render as a darker grid; modded/unmapped biomes fall back to neutral gray.
- **Player Management rework.** The Online Players tab now resolves real Mojang UUIDs (`NameToUuidResolver` — usercache.json first, `api.mojang.com` fallback) and renders real skin heads via `sessionserver.mojang.com` (per-UUID dedupe, 429 handled). Each player row has a ⋮ overflow menu with Kick / Ban / OP / De-OP; clicking the row opens the existing Player Detail screen.
- **Biome tests.** `NbtParserTest` (root compounds, byte/int arrays, nested list of compounds, malformed input), `ChunkBiomeReaderTest` (legacy `Level.Biomes`, modern paletted containers, uncompressed payloads, garbage input), and `BiomePaletteTest` (vanilla biome→color mapping + neutral fallback). Full desktop suite: 27/27 green.

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
