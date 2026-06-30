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
- **Native module**: `android/app/src/main/java/com/portalhost/app/server/ServerProcessModule.kt`
- `ReactContextBaseJavaModule` subclass using `ProcessBuilder` to spawn Java
- Stdin/stdout/stderr piped via daemon threads; events emitted via `DeviceEventEmitter`
- stderr merged into stdout (`redirectErrorStream(true)`) for simpler console display
- Java path is configurable in Settings (default `"java"` — install Termux Java for device use)
- `file://` scheme stripped from jarPath before passing to native module

## Server Setup Flow
1. User taps "Create Server" (dashboard or settings)
2. 5-step form: Pick JAR → Name → RAM → Config → EULA
3. User provides their own server .jar file via document picker
4. EULA and server.properties written to server directory before first start
5. Server stored in app document directory (`FileSystem.documentDirectory/servers/<name>/`)

## Key Changes in v1.1.0
- **Fixed**: `file://` prefix on jarPath caused native module to fail silently (status showed "online" but nothing ran)
- **Added**: Configurable Java path in Settings (for Termux users: `/data/data/com.termux/files/usr/bin/java`)
- **Added**: `redirectErrorStream(true)` so stderr appears in console
- **Fixed**: `startServer` now throws if native module is unavailable (status correctly shows "offline")
- **Icon**: Updated to `portal_host_icon.png`
- APK renamed to `PortalHost.apk`

## Critical Context
- `mobile/android/` is gitignored (standard Expo), but `android/app/src/main/java/com/portalhost/app/server/` is un-ignored via `.gitignore` negation to preserve the custom native module
- If `expo prebuild` regenerates the android directory, the native module files in `.../server/` must be recreated (run `expo prebuild --clean` then manually restore the `server/` directory)
- Release APK: `mobile/android/app/build/outputs/apk/release/PortalHost.apk`
- Build: `cd mobile/android; .\gradlew.bat assembleRelease`
- Version: `1.1.0`, package: `com.portalhost.app`
- The app has no production keystore; release builds use `signingConfig signingConfigs.debug`
- `expo-file-system/legacy` import used (SDK 56 has new File/Directory API)

## Relevant Files
- `mobile/android/app/src/main/java/com/portalhost/app/server/ServerProcessModule.kt` — native Kotlin module
- `mobile/android/app/src/main/java/com/portalhost/app/server/ServerProcessPackage.kt` — ReactPackage registration
- `mobile/android/app/src/main/java/com/portalhost/app/MainApplication.kt` — `add(ServerProcessPackage())` in packageList
- `mobile/src/lib/serverManager.ts` — NativeModules + NativeEventEmitter bridge, strips `file://` prefix
- `mobile/src/stores/serverStore.ts` — State: status, players, uptime, javaPath, serverDir, jarPath
- `mobile/src/hooks/useProcessEvents.ts` — Process events → store wiring (logs, players, uptime, exit cleanup)
- `mobile/src/app/server/create.tsx` — 5-step server creation wizard (JAR picker, name, RAM, config, EULA)
- `mobile/src/app/(tabs)/settings.tsx` — Java path, tunnel address, server management
- `mobile/src/lib/fileManager.ts` — File system operations (EULA, server.properties, directory management)
- `mobile/app.json` — App config (icon: `portal_host_icon.png`, version: `1.1.0`)
- `portal_host_icon.png` — App icon source (root); copied to `mobile/assets/images/` at build time
