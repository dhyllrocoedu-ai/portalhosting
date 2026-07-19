### Changes in v5.0.13

**Phase 0.2: MenuBar Implementation**
- ✅ Implemented "About Portal Host" dialog (Help menu)
- ✅ Implemented "Refresh List" action (Server menu) - triggers server refresh via ServerManager
- ✅ Added keyboard shortcuts: Ctrl+N (New Server), Ctrl+Q (Quit), Ctrl+R (Refresh), Ctrl+O (Open Servers Folder), Ctrl+, (Settings)

**Phase 0.3: Window State Persistence**
- ✅ Added window geometry preferences (width, height, x, y) to Preferences.kt
- ✅ Window size/position now saved on close and restored on startup
- ✅ Default: 1200×800 centered

**Phase 0.4: Unified Status Colors**
- ✅ Created `ThemeColors.kt` in commonMain with centralized color definitions
- ✅ Semantic status colors: Success, Warning, Error, Neutral, Info
- ✅ Server status badge colors (RUNNING, STARTING, STOPPING, CRASHED, etc.)
- ✅ Server type colors (Paper, Fabric, Forge, NeoForge, Purpur, Folia, Vanilla)
- ✅ Toast notification colors (background + icon)
- ✅ Activity log colors (start, error, warning, leave, player)
- ✅ Log viewer line colors (error, warn, info, default)
- ✅ RCON connection status colors (connecting, connected, disconnected, error, sent, received)
- ✅ Performance stat colors (CPU, RAM, TPS, Players, Network RX/TX)
- ✅ Console line colors (error, warn, player join/leave, chat, debug, info, default)
- ✅ Updated screens: ServersScreen, DashboardScreen, ServerDetailScreen, LogViewerScreen, PerformanceScreen, RconScreen, ToastHost, ConsoleUtils

**Artifacts**
- `PortalHost-5.0.13.msi` (Windows Installer)
- `PortalHost-5.0.13.exe` (Portable Executable)