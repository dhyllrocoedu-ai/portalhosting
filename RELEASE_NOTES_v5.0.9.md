### Changes in v5.0.9

**ServerConsoleScreen Layout Fix**
- Fixed duplicate "Console" header text appearing in the standalone server console screen
- Root cause: Surface acts like a Box - multiple children (header Column, search bar, filter chips Row) were siblings inside Surface, causing them to overlap visually
- Fix: Wrapped all Surface content in a single Column so elements stack vertically instead of overlapping

**Build Artifacts**
- MSI: `PortalHost-5.0.7.msi`
- EXE: `PortalHost-5.0.7.exe`

**Previous v5.0.8 Changes (Included)**
- Playit.gg Tunnel Fix: Windows binary renamed to `playitd.exe` for proper execution
- Dashboard Console: Full-featured with filter chips, search, auto-scroll, color coding
- Tunnel Card: Display tunnel domains (TCP/UDP), local IP:port, copy buttons
- ServerCard Icons: Layers (MC version), Laptop (Java), Settings gear (RAM)
- Player Management Screen: Full-screen with back button, 5 tabs, live queries
- Shared ConsoleUtils.kt: Common log level classification & coloring