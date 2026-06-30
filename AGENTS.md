# Expo HAS CHANGED

Read the exact versioned docs at https://docs.expo.dev/versions/v56.0.0/ before writing any code.

# PortalHost — Project Context

## Architecture
- **Monorepo** with npm workspaces: `mobile/`, `desktop-agent/`, `shared/`
- **Mobile** (React Native / Expo 56 / React 19) — primary app, Android-first
- **Desktop agent** (Express + Socket.IO) — spawns Java process on PC, optional (v1.5+)

## Key Decisions
- **No cloud backend**: Server runs locally on Android via native Kotlin `ProcessBuilder` module
- **Android only** for local servers (needs Java — Termux or similar); iOS/Web connect to remote desktop agent (v1.5+)
- **No Modrinth content, no playit.gg tunnel**: removed to keep scope minimal for v1
- **4 tabs**: Dashboard, Console, Players, Settings
- **Server creation**: User picks a .jar file via document picker (Paper/Vanilla/Fabric), configures name/RAM/properties, accepts EULA
- **Console**: Real-time streaming from server stdout/stderr via `NativeEventEmitter`
- **Player tracking**: Parses `joined the game` / `left the game` from log output (best-effort)
- **Uptime counter**: Interval increments every second while status is `"online"`

## Server Runner (Android)
- **Native module**: `mobile/modules/server-process-module/android/src/main/java/expo/modules/serverprocessmodule/ServerProcessModule.kt`
- Expo Module (not legacy `ReactContextBaseJavaModule`) using `ProcessBuilder` to spawn Java
- Stdin/stdout/stderr piped via daemon threads; events emitted via `Events("onStdout", "onExit")`
- stderr merged into stdout (`redirectErrorStream(true)`) for simpler console display
- Java path is configurable in Settings (default `"java"` — install Termux Java for device use)
- `file://` scheme stripped from jarPath before passing to native module

## Server Setup Flow
1. User taps "Create Server" (dashboard or settings)
2. 5-step form: Pick JAR → Name → RAM → Config → EULA
3. User provides their own server .jar file via document picker
4. EULA and server.properties written to server directory before first start
5. Server stored in app document directory (`FileSystem.documentDirectory/servers/<name>/`)
6. Store state persisted to `portalhost_config.json` via `expo-file-system` (survives app restart)

## Key Changes in v1.1.0
- **Fixed**: `file://` prefix on jarPath caused native module to fail silently (status showed "online" but nothing ran)
- **Added**: Configurable Java path in Settings (for Termux users: `/data/data/com.termux/files/usr/bin/java`)
- **Added**: `redirectErrorStream(true)` so stderr appears in console
- **Fixed**: `startServer` now throws if native module is unavailable (status correctly shows "offline")
- **Icon**: Updated to `portal_host_icon.png`
- APK renamed to `PortalHost.apk`

## Key Changes in v1.1.1
- **Migrated** native module from legacy `ReactContextBaseJavaModule` to Expo Modules API (`mobile/modules/server-process-module/`)
- **Added** `portalhost_config.json` persistence via `mobile/src/lib/persistence.ts` — Java path, server config, tunnel address all survive app restarts
- **Added** explicit "Save Java Path" button and "Auto-detect" button in Settings
- **Fixed** server data loss on app restart (was only in-memory via Zustand)
- **Module source**: `mobile/modules/server-process-module/android/src/main/java/expo/modules/serverprocessmodule/ServerProcessModule.kt`
- **Autolinking**: Module found by `expo-modules-autolinking` from `mobile/modules/server-process-module/expo-module.config.json`

## Key Changes in v1.1.2 (this build)
- **Version bump** to 1.1.2
- Release APK: `PortalHost.apk` (copied to repo root)

## Critical Context
- `mobile/android/` is gitignored (standard Expo), but `mobile/modules/server-process-module/` is the live source
- If `expo prebuild --clean` runs, the old `android/` dir is regenerated; the module lives in `modules/` so it's always picked up by autolinking
- Release APK: `mobile/android/app/build/outputs/apk/release/app-release.apk`
- Build: `cd mobile/android; .\gradlew.bat assembleRelease`
- Version: `1.1.2`, package: `com.portalhost.app`
- The app has no production keystore; release builds use `signingConfig signingConfigs.debug`
- `expo-file-system/legacy` import used (SDK 56 has new File/Directory API)

## Relevant Files
- `mobile/modules/server-process-module/android/src/main/java/expo/modules/serverprocessmodule/ServerProcessModule.kt` — native Kotlin module (Expo Modules API)
- `mobile/modules/server-process-module/index.ts` — TS wrapper, re-exports native module
- `mobile/modules/server-process-module/expo-module.config.json` — autolinking config
- `mobile/src/lib/serverManager.ts` — Wraps module TS functions, strips `file://` prefix
- `mobile/src/lib/persistence.ts` — Saves/loads config to `portalhost_config.json`
- `mobile/src/stores/serverStore.ts` — State: status, players, uptime, javaPath, serverDir, jarPath; now persisted
- `mobile/src/hooks/useProcessEvents.ts` — Process events → store wiring (logs, players, uptime, exit cleanup)
- `mobile/src/app/server/create.tsx` — 5-step server creation wizard (JAR picker, name, RAM, config, EULA)
- `mobile/src/app/(tabs)/settings.tsx` — Java path (save + auto-detect), tunnel address, server management
- `mobile/src/lib/fileManager.ts` — File system operations (EULA, server.properties, directory management)
- `mobile/app.json` — App config (icon: `portal_host_icon.png`, version: `1.1.2`)
- `portal_host_icon.png` — App icon source (root); copied to `mobile/assets/images/` at build time
