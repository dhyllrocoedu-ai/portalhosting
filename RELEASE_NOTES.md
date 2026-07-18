### Changes in v5.0.8

**Playit.gg Tunnel Fix**
- Fixed playit binary execution on Windows by ensuring `.exe` extension is used
- Renamed downloaded binary to `playitd.exe` on Windows for proper execution

**Dashboard Console (Full-Featured)**
- Added log level filter chips (ALL, ERROR, WARN, INFO, PLAYER, CHAT, OTHER)
- Added search functionality with match navigation
- Added auto-scroll to bottom with manual scroll detection
- Added scroll-to-bottom floating action button
- Color-coded console lines using shared `ConsoleUtils.kt`
- Command input with history (up/down arrows)

**Tunnel Card Enhancement**
- Display all tunnel addresses when connected (TCP for Java, UDP for Bedrock)
- Show public domain:port (e.g., `abc123.playit.gg:12345`)
- Show local address: `0.0.0.0:25565 (TCP/UDP)`
- Copy-to-clipboard buttons for each tunnel address
- Display claim URL when available

**ServerCard Icon Updates**
- Minecraft version: `Layers` icon
- Java version: `Laptop` icon
- RAM: `Settings` gear icon
- Removed suggestion chips, using inline icons

**Player Management Screen**
- Full-screen with TopAppBar and back button
- Tabs: Online, Whitelist, Operators, Banned Players, Banned IPs
- Live query from usercache.json for online players
- Add/remove players for each tab with persistence

**Shared Console Utilities**
- Created `ConsoleUtils.kt` in commonMain with `LogLevel`, `classifyLogLevel()`, `consoleLineColor()`
- Used by both Dashboard ConsoleCard and ServerConsoleScreen

**Build Artifacts**
- MSI: `PortalHost-5.0.7.msi`
- EXE: `PortalHost-5.0.7.exe`