# Portal Host - Compose Multiplatform Migration

## Project Structure

```
PortalHost/
├── build.gradle.kts           # Root build config
├── settings.gradle.kts        # Module includes
├── gradle.properties          # Version catalog
├── native/                    # Existing Android app
│   ├── app/                   # Android app module
│   └── settings.gradle.kts    # Updated to include composeApp
├── portal_host_desktop/       # Existing Flutter desktop app (to be deprecated)
├── composeApp/                # NEW: Compose Multiplatform module
│   ├── build.gradle.kts       # Compose MP build config
│   ├── src/
│   │   ├── commonMain/        # Shared Kotlin code (70%+)
│   │   │   ├── kotlin/
│   │   │   │   └── com/portalhost/
│   │   │   │       ├── model/           # Shared data models
│   │   │   │       ├── server/          # Server logic (providers, downloader, manager)
│   │   │   │       ├── java/            # JDK management
│   │   │   │       ├── process/         # Process management (expect/actual)
│   │   │   │       ├── filesystem/      # File system abstraction
│   │   │   │       ├── network/         # Networking
│   │   │   │       ├── backup/          # Backup logic
│   │   │   │       ├── tunnel/          # Playit.gg tunnel
│   │   │   │       ├── rcon/            # RCON protocol
│   │   │   │       ├── preferences/     # Multiplatform settings
│   │   │   │       ├── di/              # Koin DI modules
│   │   │   │       └── db/              # SQLDelight schema
│   │   │   └── sqldelight/              # SQLDelight queries
│   │   ├── androidMain/       # Android implementations
│   │   │   └── kotlin/
│   │   │       └── com/portalhost/
│   │   └── desktopMain/       # Desktop (JVM) implementations
│   │       └── kotlin/
│   │           └── com/portalhost/desktop/
│   └── resources/             # Desktop resources (icons, etc.)
├── .github/workflows/         # CI/CD
└── docs/
```

## Key Shared Components (70%+ code reuse)

| Component | Android | Desktop | Strategy |
|-----------|---------|---------|----------|
| **Server Providers** | ✅ | ✅ | Pure Kotlin, Ktor + kotlinx.serialization |
| **Server Models** | ✅ | ✅ | @Serializable data classes |
| **Server Downloader** | ✅ | ✅ | Ktor Client + expect/actual FileSystem |
| **JDK Manager** | ✅ | ✅ | expect/actual JdkManager |
| **Process Manager** | ✅ | ✅ | expect/actual ProcessManager (ProcessBuilder) |
| **Server Manager** | ✅ | ✅ | Shared orchestration |
| **Database** | ✅ | ✅ | SQLDelight (Room → SQLite) |
| **Preferences** | ✅ | ✅ | Multiplatform Settings |
| **Tunnel/Rcon/Backup** | ✅ | ✅ | Pure Kotlin |
| **UI** | Compose | Compose | Platform-specific screens, shared ViewModels |

## Platform-Specific Implementations

### Android (`androidMain/`)
- `FileSystem` → Context filesDir
- `ProcessManager` → Android ProcessBuilder (limited)
- `Preferences` → SharedPreferences via MultiplatformSettings
- `JdkManager` → Not applicable (servers run on host)

### Desktop (`desktopMain/`)
- `FileSystem` → java.nio.file
- `ProcessManager` → ProcessBuilder (full support)
- `Preferences` → HOCON/JSON via MultiplatformSettings
- `JdkManager` → Full JDK detection/download/extraction
- `TrayManager` → SystemTray (compose-desktop)
- `WindowManager` → Window controls

## Build Configuration

### composeApp/build.gradle.kts highlights:
- Kotlin Multiplatform (android + jvm desktop)
- Compose Multiplatform (Material3)
- SQLDelight for database
- Ktor Client for networking
- Koin for DI
- kotlinx.serialization for JSON
- Compose Desktop window/tray APIs

### Native Android App
- Gradually migrates to depend on `composeApp` shared module
- Keeps existing Android UI screens
- Replaces Room → SQLDelight
- Replaces OkHttp/Retrofit → Ktor

## Migration Phases

1. **Phase 0** ✅ - Foundation: Project structure, build config, DI
2. **Phase 1** - Models & Serialization: ServerConfig, ServerState, BackupEntry
3. **Phase 2** - Server Providers: Paper, Folia, Purpur, Vanilla, Fabric, Forge, NeoForge
4. **Phase 3** - JDK Manager: Cross-platform Java detection/download
5. **Phase 4** - Process Manager: ProcessBuilder abstraction
5. **Phase 5** - Database: SQLDelight migration from Room/Drift
6. **Phase 6** - Server Downloader/Manager: Core orchestration
7. **Phase 7** - Desktop Platform: Tray, Window, FilePicker implementations
8. **Phase 8** - UI Migration: Shared ViewModels, Compose screens
8. **Phase 9** - Android Migration: Replace native modules with shared
9. **Phase 10** - CI/CD & Release: Build pipelines, signing, auto-update

## Running the Project

```bash
# Build desktop app
./gradlew :composeApp:desktopDistribution

# Build Android app
./gradlew :native:app:assembleDebug

# Run desktop dev
./gradlew :composeApp:run

# Run tests
./gradlew check
```

## Key Files to Review

1. **DiModule.kt** - Dependency injection setup
2. **ServerProvider.kt** - Provider interface & registry
3. **JdkManager.kt** - expect/actual for JDK management
4. **ProcessManager.kt** - expect/actual for process control
5. **FileSystem.kt** - expect/actual for file operations
5. **ServerModels.kt** - Shared data models
6. **DatabaseSchema.kt** - SQLDelight schema
6. **DiModule.kt** - Koin modules

## Next Steps

1. Run `./gradlew :composeApp:desktopDistribution` to verify build
2. Implement remaining providers (Fabric, Forge, NeoForge)
3. Add SQLDelight queries for servers/backups/logs
7. Create Desktop UI screens (Servers, Create, Settings)
8. Migrate Android app to use shared module
9. Set up CI/CD for both targets