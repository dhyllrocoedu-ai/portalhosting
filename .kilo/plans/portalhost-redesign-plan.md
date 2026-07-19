# PortalHost Desktop UI/UX Redesign & Bug Fixes - Complete Plan

## Overview
Complete redesign of the PortalHost desktop application with a modern, Portalyx-inspired dark theme, plus critical bug fixes for tunnel management, navigation, and missing features.

---

## Phase 1: Critical Bug Fixes (HIGH PRIORITY)

### 1.1 Playit.gg Tunnel Manager Crash
**File:** `composeApp/src/commonMain/kotlin/com/portalhost/server/TunnelManager.kt:187`
**Issue:** `java.io.IOException: Stream closed` when starting tunnel
**Root Cause:** Process output stream gets closed before reading completes. The `startReader` coroutine reads from `process.inputStream` but the process may exit or close the stream before reading finishes.
**Fix Details:**
- Modify `start` function to use `ProcessBuilder.redirectErrorStream(true)` to merge stderr into stdout
- Launch reader coroutine BEFORE calling `process.waitFor()` 
- Use `process.inputStream.bufferedReader().use { reader -> ... }` for proper resource management
- Add a 2-3 second health check delay after process start before marking tunnel as CONNECTED
- Parse claim URL from log file (`playitd.log`) as fallback since process stream is unreliable
- Add proper error handling for `IOException` and `IllegalStateException` in reader
- Consider using `process.errorStream` separately for error detection

### 1.2 Missing Back Buttons / Navigation
**Files to Fix:**
- `CreateServerScreen.kt` - Add TopAppBar with back button for wizard steps
- `ServerConsoleScreen.kt` - Verify back button works (already has onBack callback)
- `ServerFilesScreen.kt` - Add back navigation
- `SettingsScreen.kt` - Add back navigation
- `PerformanceScreen.kt` - Add back navigation
- `LogViewerScreen.kt` - Add back navigation
- `RconScreen.kt` - Add back navigation

### 1.3 Welcome Screen Not Showing on First Install
**Files:**
- `composeApp/src/commonMain/kotlin/com/portalhost/preferences/Preferences.kt` - Add `hasSeenWelcome` flag
- `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/screens/WelcomeScreen.kt` - Create new
- `DesktopMain.kt` - Show welcome screen conditionally

### 1.4 Windows Uninstaller Missing
**Files:**
- `composeApp/src/windows/wix/template.wxs` - Ensure uninstaller registration
- Verify WiX bundle includes uninstall entry

---

## Phase 2: Theme System (FOUNDATION)

### 2.1 Portalyx Color Palette
**New File:** `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/theme/PortalyxColors.kt`

```kotlin
// Backgrounds
BackgroundPrimary = #0D1117      // Deep navy
BackgroundSecondary = #161B22    // Cards
BackgroundTertiary = #21262D     // Elevated
BackgroundHover = #30363D        // Hover states

// Borders
BorderDefault = #30363D
BorderSubtle = #21262D

// Text
TextPrimary = #F0F6FC
TextSecondary = #8B949E
TextMuted = #6E7681

// Accents
Primary = #8B5CF6                // Purple
PrimaryHover = #7C3AED
Success = #10B981                // Green
Error = #EF4444                  // Red
Warning = #F59E0B                // Amber
Info = #3B82F6                   // Blue

// Server Status
ServerRunning = Success
ServerStarting = Warning
ServerStopped = #6B7280
ServerCrashed = Error
```

### 2.2 Material Theme Wrapper
**New File:** `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/theme/PortalyxTheme.kt`
- Configure Material3 ColorScheme from PortalyxColors
- Typography: Inter font family, proper scale
- Shapes: 6dp, 8dp, 12dp, 16dp radius tokens

---

## Phase 3: Core Components (BUILDING BLOCKS)

### 3.1 Sidebar Navigation
**File:** `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/components/Sidebar.kt`
- 240px fixed width
- Logo + app name at top
- Nav items: Home, Servers, Settings
- Active state: purple background (#1F1935) + purple text
- Hover state: subtle background
- Bottom: System status + user profile

### 3.2 Top Bar
**File:** `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/components/TopBar.kt`
- 56px height
- Server selector dropdown (40% width)
- Search, Notifications, Profile actions
- Background: BackgroundSecondary

### 3.3 Status Badge
**File:** `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/components/StatusBadge.kt`
- Colored dot + label
- Variants: running, starting, stopped, crashed, connecting

### 3.4 Server Card (List Item)
**File:** `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/components/ServerCard.kt`
- 48px server icon
- Name + version/type subtitle
- Status badge
- Player count (X/Y)
- Overflow menu

### 3.5 Stat Card with Sparkline
**Files:**
- `Sparkline.kt` - Canvas-based mini chart
- `StatCard.kt` - Label, value, sparkline, trend

### 3.6 Server Hero Card
**File:** `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/components/ServerHero.kt`
- Large card with optional background image
- Server info: address, software, Java, uptime
- Action buttons: Start/Restart/Stop (context-aware)
- Info row: Address, Software, Java, Uptime
- Player count + status badges row

### 3.7 Server Tools Grid
**File:** `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/components/ServerTools.kt`
- Horizontal scrolling row of tool buttons
- Each: Icon + label in 96x80 card
- Tools: Console, Files, Plugins, Mods, Datapacks, Worlds, Backups, Performance, Network, Settings

### 3.8 Activity Feed Item
**File:** `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/components/ActivityItem.kt`
- Icon (colored by type) + message + timestamp
- Types: join, leave, backup, error, plugin, command

---

## Phase 4: Screen Redesigns

### 4.1 DesktopMain.kt - New Layout Structure
```
Row (full height)
├── Sidebar (240px)
└── Column (flex)
    ├── TopBar (56px)
    └── Content (flex, padding 24px)
        └── Current Screen
```

**Navigation State:**
```kotlin
sealed class Screen {
    object Home : Screen()
    object Servers : Screen()
    data class ServerDetail(val serverId: String) : Screen()
    data class Console(val serverId: String) : Screen()
    object Create : Screen()
    object Settings : Screen()
    // Add: Welcome, PlayerManagement
}
```

### 4.2 DashboardScreen.kt - Complete Redesign
```
Column (gap 24px)
├── ServerHero (selected server or "Select a server")
├── Row (gap 16px, wrap)
│   ├── StatCard: CPU + sparkline
│   ├── StatCard: RAM + sparkline
│   ├── StatCard: TPS + sparkline
│   ├── StatCard: Players + sparkline
│   ├── StatCard: Network ↑ + sparkline
│   └── StatCard: Network ↓ + sparkline
├── Row (gap 16px, wrap)
│   ├── Card: Recent Activity (6 items, "View All")
│   ├── Card: Resource Usage (bars)
│   └── Card: Server Status (checklist)
└── ServerTools (horizontal scroll)
```

### 4.3 ServersScreen.kt - Card Grid Layout
```
Column
├── TopBar: "My Servers" + [New Server] button
├── Filter/Search row
└── LazyVerticalGrid (columns = calculated, min 320px)
    └── ServerCard items
    └── Empty state: "Create your first server"
```

### 4.4 ServerDetailScreen.kt - Modernized Tabs
- Horizontal ScrollableTabRow (icon + label)
- Tabs: Overview, Console, Files, Plugins, Mods, Datapacks, Worlds, Backups, Performance, Network, Settings
- Each tab content in Card with proper padding

### 4.5 ServerConsoleScreen.kt - Three-Panel Layout
```
Row (full height)
├── Left Panel (280px): Quick Actions + Recent Activity
│   ├── Start/Restart/Stop/Kill buttons
│   ├── Backup Now
│   └── Recent Activity list
├── Center Panel (flex): Console Output
│   ├── Log view (monospace, colored lines)
│   └── Command input bar
└── Right Panel (200px): Online Players
    ├── Player count badge
    └── Player list with actions
```

### 4.6 PlayerManagementScreen.kt - Full Screen (from Android)
**New File:** `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/screens/PlayerManagementScreen.kt`
- Port from `native/app/src/main/java/com/portalhost/app/ui/screens/PlayersScreen.kt`
- TopAppBar with back button
- Tabs: Online, Whitelist, Operators, Banned Players, Banned IPs
- Each tab: LazyColumn with proper actions
- Access from Dashboard player stat card + Console right panel

### 4.7 CreateServerScreen.kt - Wizard with Back Button
- TopAppBar with back button (closes wizard or goes to previous step)
- Step indicator: Basic → Software → Advanced → Review
- Each step in Card with proper validation

### 4.8 WelcomeScreen.kt - First Launch Experience
- Hero illustration
- "Welcome to PortalHost"
- 3 feature highlights
- [Get Started] button → sets `hasSeenWelcome = true` → navigates to Home

### 4.9 SettingsScreen.kt - Categorized Sections
- General, Appearance, Server Defaults, Network, Advanced
- Each section in Card
- Theme selector with live preview

---

## Phase 5: Integration & Polish

### 5.1 Navigation Wiring
**DesktopMain.kt updates:**
- Add `PlayerManagement` screen state
- Add `Welcome` screen state
- Wire all `onBack` callbacks
- Handle server selection from Dashboard → ServerDetail

### 5.2 Theme Application
- Wrap `DesktopApp()` in `PortalyxTheme`
- Update all screens to use new components
- Replace old Card/Tab/Button usage with new styled versions

### 5.3 Animations
- Screen transitions: fade + slide (200ms)
- Tab switches: crossfade (150ms)
- Hover states: 100ms color/background transitions
- Sidebar item: background fill animation

### 5.4 Responsive Behavior
- Min window: 1024x768
- Sidebar collapses to icons at < 1100px
- Grid columns auto-fit
- Console three-panel → stacked on narrow

---

## File Creation Checklist

### New Theme Files
- [ ] `desktop/theme/PortalyxColors.kt`
- [ ] `desktop/theme/PortalyxTheme.kt`

### New Component Files
- [ ] `desktop/components/Sidebar.kt`
- [ ] `desktop/components/TopBar.kt`
- [ ] `desktop/components/StatusBadge.kt`
- [ ] `desktop/components/ServerCard.kt`
- [ ] `desktop/components/Sparkline.kt`
- [ ] `desktop/components/StatCard.kt`
- [ ] `desktop/components/ServerHero.kt`
- [ ] `desktop/components/ServerTools.kt`
- [ ] `desktop/components/ActivityItem.kt`

### New Screen Files
- [ ] `desktop/screens/WelcomeScreen.kt`
- [ ] `desktop/screens/PlayerManagementScreen.kt`

### Modified Files
- [ ] `DesktopMain.kt` - New layout, navigation, theme
- [ ] `DashboardScreen.kt` - Complete redesign
- [ ] `ServersScreen.kt` - Card grid layout
- [ ] `ServerDetailScreen.kt` - Modern tabs, hero card
- [ ] `ServerConsoleScreen.kt` - Three-panel layout
- [ ] `CreateServerScreen.kt` - TopAppBar with back
- [ ] `SettingsScreen.kt` - Categorized sections
- [ ] `ServerFilesScreen.kt` - Back navigation
- [ ] `PerformanceScreen.kt` - Back navigation
- [ ] `LogViewerScreen.kt` - Back navigation
- [ ] `RconScreen.kt` - Back navigation
- [ ] `TunnelManager.kt` - Fix stream handling
- [ ] `Preferences.kt` - Add `hasSeenWelcome`
- [ ] `template.wxs` - Verify uninstaller

---

## Testing Checklist

### Bug Fixes
- [ ] Tunnel starts and stays connected
- [ ] All screens have working back navigation
- [ ] Welcome screen shows on first launch only
- [ ] Windows installer creates uninstaller entry

### UI/UX
- [ ] Dark theme renders correctly
- [ ] Sidebar navigation works
- [ ] Dashboard stats show sparklines
- [ ] Server selection updates dashboard
- [ ] Player management opens from dashboard & console
- [ ] Console three-panel layout works
- [ ] Create server wizard navigable
- [ ] Settings organized and functional

### Responsive
- [ ] Window resize handles gracefully
- [ ] Min size 1024x768 usable
- [ ] Sidebar collapse at narrow widths

---

## Implementation Order

1. **PortalyxColors.kt + PortalyxTheme.kt** (foundation)
2. **TunnelManager.kt fix** (critical bug)
3. **Sidebar.kt + TopBar.kt** (layout shell)
4. **DesktopMain.kt** (new navigation structure)
5. **StatusBadge, ServerCard, Sparkline, StatCard** (primitives)
6. **ServerHero, ServerTools, ActivityItem** (dashboard parts)
7. **DashboardScreen.kt** (assemble dashboard)
8. **ServersScreen.kt** (server list)
9. **ServerDetailScreen.kt** (tabs + content)
10. **ServerConsoleScreen.kt** (three-panel)
11. **PlayerManagementScreen.kt** (port from Android)
12. **CreateServerScreen.kt + WelcomeScreen.kt** (wizard + onboarding)
13. **SettingsScreen + other screens** (back buttons, theme)
14. **Preferences.kt + template.wxs** (welcome flag + uninstaller)
15. **Polish pass** (animations, hover states, responsive)

---

## Notes

- **Excluded:** Auth/Subscription system (budget constraints)
- **Reference:** Android `PlayersScreen.kt` for player management UI
- **Font:** Use system UI font stack (Inter if available)
- **Icons:** Material Icons Filled (already in project)
- **State:** Keep using Koin + StateFlow + Compose state