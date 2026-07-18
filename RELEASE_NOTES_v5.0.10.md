### Changes in v5.0.10

**Build System & Window Fixes**
- Fixed Gradle build by removing problematic `ui-window` dependency
- Reverted to standard decorated window (avoids WindowManager API issues in Compose Desktop 1.6)
- Cleaned up unused `WindowChrome.kt` and `WindowUtils.kt` files

**Previous v5.0.9 Changes (Included)**
- Playit.gg Tunnel Fix: Windows binary renamed to `playitd.exe` for proper execution
- Dashboard Console: Full-featured with filter chips, search, auto-scroll, color coding
- Tunnel Card: Display tunnel domains (TCP/UDP), local IP:port, copy buttons
- ServerCard Icons: Layers (MC version), Laptop (Java), Settings gear (RAM)
- Player Management Screen: Full-screen with back button, 5 tabs (Online/Whitelist/Ops/Banned Players/Banned IPs)
- Shared ConsoleUtils.kt: Common log level classification & coloring

**Build Artifacts**
- MSI: `PortalHost-5.0.7.msi`
- EXE: `PortalHost-5.0.7.exe`