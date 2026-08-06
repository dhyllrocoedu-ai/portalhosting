## Portal Host v5.0.68

### Desktop
- **Uninstaller completely rewritten.** Detached batch cleanup kills PortalHost/java processes, force-removes install folder, removes Start Menu shortcuts, and self-deletes. MSI path uses direct `msiexec.exe /x` (no PowerShell wrapper). Portable/EXE path now also runs full cleanup instead of just exiting.
- **Marketplace detail screen layout fixed.** Bottom install bar pinned to window bottom via `Scaffold.bottomBar` — never cropped. Slim content: tiny accent "✓ Ready" dot + "Ready to install" label + "Install to Server" button. Accent highlight with border when version selected (no more blending).
- **Version cards highlight refined.** Subtle 4dp top accent strip + 1dp accent border instead of full background tint. Game versions text ellipsized to 2 lines.
- **Server delete now works.** Properly awaits `serverManager.deleteServer()`, only navigates back on success, shows errors via System.err.
- **Tab clamping.** `coerceIn` prevents "index 9 out of bounds for length 10" crash when server type filters tabs.
- **Folder routing: server-type first.** Paper/Purpur/Folia/Vanilla → `plugins`; Forge/NeoForge/Fabric/Quilt → `mods`; Datapack always → `world/datapacks`.

### Android
- **Server delete now works.** `AppState.deleteServer()` stops the server if running, removes from repository, deletes server directory recursively, shows confirmation toast.
- **Coroutine/context fixes.** Fixed `AppState` and `AppNavigation` coroutine scope and `LocalContext` issues for delete flow.
- **All desktop fixes parity.** Folder routing (server-type first), marketplace install folder logic, tab clamping, version card ellipsis.

### Downloads
- **MSI:** [PortalHost-5.0.68.msi](https://github.com/dhyllrocoedu-ai/portalhosting/releases/download/v5.0.68/PortalHost-5.0.68.msi) (~127 MB)
- **EXE:** [PortalHost-5.0.68.exe](https://github.com/dhyllrocoedu-ai/portalhosting/releases/download/v5.0.68/PortalHost-5.0.68.exe) (~127 MB)
- **APK:** [PortalHost.apk](https://github.com/dhyllrocoedu-ai/portalhosting/releases/download/v5.0.68/PortalHost.apk) (~9 MB)