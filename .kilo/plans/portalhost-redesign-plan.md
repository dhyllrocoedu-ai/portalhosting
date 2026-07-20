# PortalHost Desktop UI/UX Redesign & Bug Fixes - Complete Plan

## Overview
Complete redesign of the PortalHost desktop application with a modern, Portalyx-inspired dark theme, plus critical bug fixes for tunnel management, navigation, and missing features.

---

## Phase 1: Critical Bug Fixes (HIGH PRIORITY)

### 1.1 Playit.gg Tunnel Manager Crash
**File:** `composeApp/src/commonMain/kotlin/com/portalhost/server/TunnelManager.kt:187`
**Issue:** `java.io.IOException: Stream closed` when starting tunnel
**Root Cause:** Process output stream gets closed before reading completes. The `startReader` coroutine reads from `process.inputStream` but the process may exit or close the stream before reading finishes.
**Fix Implementation:**

```kotlin
// In TunnelManager.start() - replace the process startup logic:
private suspend fun start(port: Int): Result<TunnelInfo> {
    val playitBinary = getPlayitBinary()
    val logFile = File(playitBinary.parentFile, "playitd.log")
    
    val processBuilder = ProcessBuilder(
        playitBinary.absolutePath,
        "--log-path", logFile.absolutePath,
        "start", "--port", port.toString()
    ).apply {
        directory(playitBinary.parentFile)
        redirectErrorStream(true)  // Critical: merge stderr into stdout
    }
    
    val process = processBuilder.start()
    
    // Start reader IMMEDIATELY before waitFor()
    val readerJob = CoroutineScope(Dispatchers.IO).launch {
        process.inputStream.bufferedReader().use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                // Parse claim URL from output
                if (line?.contains("claim") == true) {
                    val claimUrl = extractClaimUrl(line)
                    if (claimUrl != null) {
                        tunnelClaimUrl = claimUrl
                    }
                }
                // Update status from log lines
                parseLogLine(line)
            }
        }
    }
    
    // Health check: wait for claim URL or timeout
    val claimUrl = withTimeoutOrNull(30_000) {
        while (tunnelClaimUrl == null) {
            delay(500)
            // Also check log file as fallback
            if (logFile.exists()) {
                logFile.readText().let { content ->
                    extractClaimUrl(content)?.also { tunnelClaimUrl = it }
                }
            }
        }
        tunnelClaimUrl!!
    }
    
    if (claimUrl == null) {
        readerJob.cancel()
        process.destroyForcibly()
        return Result.failure(Exception("Timeout waiting for tunnel claim URL"))
    }
    
    // Start log tailer for ongoing monitoring
    startLogTailer(logFile)
    
    return Result.success(TunnelInfo(claimUrl, process))
}
```

**Key Changes:**
1. `redirectErrorStream(true)` - prevents stderr from blocking
2. Reader launches BEFORE `waitFor()` - captures all output
3. `bufferedReader().use { }` - proper resource cleanup
4. 30s timeout with 500ms polling for claim URL
5. Log file fallback parsing
6. Background log tailer for ongoing status updates

**Additional Fixes Needed:**
- Add `extractClaimUrl(String): String?` helper using regex
- Add `parseLogLine(String?)` to update tunnel status from log output
- Add `startLogTailer(File)` coroutine that tails the log file for ongoing status
- Ensure `process.destroyForcibly()` called on all error paths
- Add proper logging for debugging tunnel issues

### 1.2 Missing Back Buttons / Navigation
**Files to Fix - Add TopAppBar with Back Button:**

| Screen | File | Back Target |
|--------|------|-------------|
| CreateServerScreen | `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/screens/CreateServerScreen.kt` | ServersScreen (step 0) or previous step |
| ServerFilesScreen | `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/screens/ServerFilesScreen.kt` | ServerDetailScreen |
| PerformanceScreen | `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/screens/PerformanceScreen.kt` | ServerDetailScreen |
| LogViewerScreen | `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/screens/LogViewerScreen.kt` | ServerDetailScreen |
| RconScreen | `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/screens/RconScreen.kt` | ServerDetailScreen |
| SettingsScreen | `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/screens/SettingsScreen.kt` | Home/Servers (context-dependent) |
| ServerConsoleScreen | `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/screens/ServerConsoleScreen.kt` | Verify existing `onBack` works |

**Implementation Pattern:**
```kotlin
@Composable
fun ScreenName(..., onBack: () -> Unit) {
    Column {
        TopAppBar(
            title = { Text("Screen Title") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PortalyxColors.BackgroundSecondary
            )
        )
        // Screen content
    }
}
```

### 1.3 Welcome Screen Not Showing on First Install
**Files:**
- `composeApp/src/commonMain/kotlin/com/portalhost/preferences/Preferences.kt` - Add `hasSeenWelcome` flag
- `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/screens/WelcomeScreen.kt` - Create new
- `DesktopMain.kt` - Show welcome screen conditionally

**Preferences Addition:**
```kotlin
// In Preferences.kt
val hasSeenWelcome by preference(false, "has_seen_welcome")
```

**WelcomeScreen Logic:**
```kotlin
@Composable
fun WelcomeScreen(onFinish: () -> Unit) {
    val prefs = koinInject<Preferences>()
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        // Hero graphic/illustration
        Text("Welcome to PortalHost", style = MaterialTheme.typography.headlineLarge)
        Text("Manage your Minecraft servers with ease", color = PortalyxColors.TextSecondary)
        
        // 3 feature cards
        Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FeatureCard(Icons.Filled.Dns, "Server Management", "Create & manage Paper, Fabric, Forge servers")
            FeatureCard(Icons.Filled.Tunnel, "Playit.gg Tunnels", "Free tunneling for public access")
            FeatureCard(Icons.Filled.MonitorHeart, "Real-time Monitoring", "CPU, RAM, TPS, players live")
        }
        
        Button(onClick = {
            prefs.hasSeenWelcome = true
            onFinish()
        }, modifier = Modifier.padding(top = 32.dp)) {
            Text("Get Started")
        }
    }
}
```

**DesktopMain.kt Integration:**
```kotlin
val prefs = koinInject<Preferences>()
val hasSeenWelcome by prefs.hasSeenWelcome.collectAsState()

if (!hasSeenWelcome) {
    WelcomeScreen(onFinish = { currentScreen = Screen.Home })
} else {
    // Normal app content
}
```

### 1.4 Windows Uninstaller Missing
**Files:**
- `composeApp/src/windows/wix/template.wxs` - Ensure uninstaller registration
- Verify WiX bundle includes uninstall entry

**WiX Template Checklist:**
```xml
<!-- Ensure these elements exist in template.wxs -->
<Product ...>
  <!-- UpgradeCode must be stable across versions -->
  <UpgradeCode>{{YOUR_UPGRADE_CODE}}</UpgradeCode>
  
  <!-- MajorUpgrade handles uninstall of previous versions -->
  <MajorUpgrade Schedule="afterInstallInitialize" DowngradeErrorMessage="..." />
  
  <!-- Feature with uninstall support -->
  <Feature Id="MainApplication" Title="PortalHost" Level="1">
    <ComponentGroupRef Id="ApplicationFiles" />
    <ComponentRef Id="ApplicationShortcut" />
    <ComponentRef Id="UninstallShortcut" />  <!-- Add this -->
  </Feature>
  
  <!-- Uninstall shortcut in Start Menu -->
  <Component Id="UninstallShortcut" Directory="ProgramMenuFolder">
    <Shortcut Id="UninstallShortcut"
              Name="Uninstall PortalHost"
              Description="Uninstall PortalHost"
              Target="[SystemFolder]msiexec.exe"
              Arguments="/x [ProductCode]"
              WorkingDirectory="INSTALLFOLDER" />
    <RemoveFolder Id="ProgramMenuFolder" On="uninstall" />
    <RegistryValue Root="HKCU" Key="Software\PortalHost" Name="Installed" Type="integer" Value="1" KeyPath="yes" />
  </Component>
</Product>
```

**Build Verification:**
- Run `./gradlew packageMsi` 
- Install MSI, verify "Uninstall PortalHost" appears in:
  - Windows Settings > Apps > Installed apps
  - Start Menu > PortalHost folder
  - Control Panel > Programs and Features

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

**PortalyxTheme.kt Implementation:**
```kotlin
@Composable
fun PortalyxTheme(
    darkTheme: Boolean = true, // Force dark for now
    content: @Composable () -> Unit
) {
    val colorScheme = ColorScheme(
        primary = PortalyxColors.Primary,
        primaryContainer = PortalyxColors.Primary.copy(alpha = 0.2f),
        secondary = PortalyxColors.Info,
        secondaryContainer = PortalyxColors.Info.copy(alpha = 0.2f),
        tertiary = PortalyxColors.Warning,
        tertiaryContainer = PortalyxColors.Warning.copy(alpha = 0.2f),
        error = PortalyxColors.Error,
        errorContainer = PortalyxColors.ErrorBg,
        background = PortalyxColors.BackgroundPrimary,
        surface = PortalyxColors.BackgroundSecondary,
        surfaceVariant = PortalyxColors.BackgroundTertiary,
        onPrimary = PortalyxColors.TextPrimary,
        onSecondary = PortalyxColors.TextPrimary,
        onTertiary = PortalyxColors.TextPrimary,
        onError = PortalyxColors.TextPrimary,
        onBackground = PortalyxColors.TextPrimary,
        onSurface = PortalyxColors.TextPrimary,
        onSurfaceVariant = PortalyxColors.TextSecondary,
        outline = PortalyxColors.BorderDefault,
        outlineVariant = PortalyxColors.BorderSubtle,
        shadow = Color.Black,
        scrim = Color.Black,
        inverseSurface = PortalyxColors.TextPrimary,
        inverseOnSurface = PortalyxColors.BackgroundPrimary,
        inversePrimary = PortalyxColors.PrimaryLight
    )

    val typography = Typography(
        displayLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = -0.25.sp
        ),
        displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp),
        displaySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
        headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
        headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
        headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
        titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
        titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
        titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp)
    )

    val shapes = Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(24.dp)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = shapes,
        content = content
    )
}
```

**Shape Usage Mapping:**
- `extraSmall` (6dp): Badges, chips, small buttons
- `small` (8dp): Cards, text fields, buttons
- `medium` (12dp): Panels, dialogs, hero card
- `large` (16dp): Main containers, modals
- `extraLarge` (24dp): Full-screen sheets

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

**Data Flow:**
- Selected server ID stored in `DesktopMain.kt` state
- `ServerManager.selectedServer` StateFlow drives hero card
- Performance stats from `ServerManager.serverStates` (per-server StateFlow)
- Sparkline data: maintain rolling 60-point history in `ServerState` model
- Activity feed from `DatabaseRepository.getRecentActivity(serverId, limit=10)`

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

**Tab Content Specs:**

| Tab | Component | Key Features |
|-----|-----------|--------------|
| Overview | `ServerHero` + `ServerTools` | Summary view |
| Console | `ServerConsoleScreen` (embedded) | Three-panel layout |
| Files | `ServerFilesScreen` | File tree with edit/upload/delete |
| Plugins | `PluginsTab` | List + upload + disable/enable |
| Mods | `ModsTab` | List + upload + disable/enable |
| Datapacks | `DatapacksTab` | List + upload + disable/enable |
| Worlds | `WorldsTab` | List + import/export + backup |
| Backups | `BackupsTab` | List + create + restore + delete |
| Performance | `PerformanceScreen` | Charts + historical data |
| Network | `NetworkTab` | Tunnel status + RCON config |
| Settings | `ServerSettingsTab` | Properties editor |

**Data Loading:** Each tab loads data on-demand via `LaunchedEffect(serverId)` to avoid unnecessary work.

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

**Console Log Implementation:**
- Use `LazyColumn` with `itemsIndexed` for virtualized log lines
- Each line: `AnnotatedString` with Minecraft color codes parsed
- Auto-scroll to bottom on new lines (disable if user scrolled up)
- Max 10,000 lines in memory, older lines purged
- Color mapping: ERROR=Red, WARN=Amber, INFO=Green, DEBUG=Gray, PLAYER_JOIN=Blue, PLAYER_LEAVE=Yellow

**Command Input:**
- TextField at bottom with Enter to send
- History: Up/Down arrows cycle through last 50 commands
- Send via `ServerManager.sendCommand(serverId, command)`

**Player List (Right Panel):**
- Shows `serverState.playersOnline` list
- Each player: avatar (Minecraft head), name, [Kick] [Ban] [OP/Deop] buttons
- "View All Players" button → navigates to PlayerManagementScreen

### 4.6 PlayerManagementScreen.kt - Full Screen (Port from Android)
**Source:** `native/app/src/main/java/com/portalhost/app/ui/screens/PlayersScreen.kt` (482 lines)
**New File:** `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/screens/PlayerManagementScreen.kt`

**Porting Checklist:**
- [ ] Copy all data classes: `WhitelistEntry`, `OpEntry`, `BannedPlayerEntry`, `BannedIpEntry`
- [ ] Port file helper functions: `readWhitelist`, `addToWhitelist`, `removeFromWhitelist`, `readOpsList`, `addToOps`, `removeFromOps`, `readBannedPlayers`, `removeFromBanned`, `readBannedIps`, `removeFromBannedIps`, `generateOfflineUuid`
- [ ] Adapt `PlayersScreen` composable signature:
  ```kotlin
  @Composable
  fun PlayerManagementScreen(
      serverId: String,
      onBack: () -> Unit,
      serverDir: File,  // from ServerManager
      onCommand: (String) -> Unit  // sends RCON command
  )
  ```
- [ ] Replace Android `File("servers/$serverId")` with `File(serverManager.getServerDir(serverId))`
- [ ] Keep tab structure: Online, Whitelist, Operators, Banned Players, Banned IPs
- [ ] Use desktop `TopAppBar` with back button
- [ ] Use `MinecraftHeadIcon` component (port or reuse from commonMain)
- [ ] Use desktop `SmallChip` / `SmallFloatingActionButton` equivalents
- [ ] Add `Scaffold` with `topBar` for proper Material3 layout
- [ ] Wire from Dashboard player stat card → `onNavigateToPlayers`
- [ ] Wire from Console right panel → `onNavigateToPlayers`

**Access Points:**
1. Dashboard → Player Stat Card "View Details" button
2. ServerConsoleScreen → Right panel "View All Players" button
3. ServerDetailScreen → Performance tab → Players section

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
- Sidebar item: background fill animation (150ms)

### 5.4 Responsive Behavior
**Breakpoints:**
- `< 1024px`: Min window size, sidebar collapses to icons-only (64px)
- `1024-1280px`: Full sidebar, single-column dashboard stats
- `1280-1600px`: Two-column dashboard stats grid
- `> 1600px`: Three-column dashboard stats grid

**Console Layout Adaptation:**
- `> 1400px`: Three-panel (280px / flex / 200px)
- `1100-1400px`: Two-panel (Left+Center merged, Right drawer)
- `< 1100px`: Stacked panels with tabs

**Grid Columns (ServersScreen):**
- `min-width: 320px` per card
- `columns = max(1, floor(availableWidth / 320))`

### 5.5 Accessibility
- Focus order follows visual layout
- All interactive elements have focus indicators (2px primary ring)
- Color contrast: AA minimum (4.5:1 for text, 3:1 for UI)
- Semantic colors: never rely on color alone for status
- Screen reader labels for icon-only buttons

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

## Phase 6: Data Model Updates (REQUIRED FOR NEW UI)

### 6.1 ServerState Extensions for Sparklines
**File:** `composeApp/src/commonMain/kotlin/com/portalhost/model/ServerState.kt`

```kotlin
// Add to ServerState data class:
data class ServerState(
    // ... existing fields ...
    
    // Rolling history for sparklines (max 60 points = 5 min at 5s interval)
    val cpuHistory: List<Float> = emptyList(),
    val ramHistory: List<Float> = emptyList(), 
    val tpsHistory: List<Float> = emptyList(),
    val playersHistory: List<Int> = emptyList(),
    val networkRxHistory: List<Long> = emptyList(),
    val networkTxHistory: List<Long> = emptyList(),
) {
    fun withAddedMetrics(cpu: Float, ram: Float, tps: Float, players: Int, rx: Long, tx: Long): ServerState {
        return copy(
            cpuHistory = (cpuHistory + cpu).takeLast(60),
            ramHistory = (ramHistory + ram).takeLast(60),
            tpsHistory = (tpsHistory + tps).takeLast(60),
            playersHistory = (playersHistory + players).takeLast(60),
            networkRxHistory = (networkRxHistory + rx).takeLast(60),
            networkTxHistory = (networkTxHistory + tx).takeLast(60),
        )
    }
}
```

**Collection:** Update `ServerManager` to call `withAddedMetrics()` on each stats poll (every 5s).

### 6.2 Activity Feed Model
**File:** `composeApp/src/commonMain/kotlin/com/portalhost/model/ActivityEvent.kt` (NEW)

```kotlin
@Serializable
data class ActivityEvent(
    val id: String = UUID.randomUUID().toString(),
    val serverId: String,
    val type: ActivityType,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
)

enum class ActivityType {
    PLAYER_JOIN,        // icon: PersonAdd, color: Success
    PLAYER_LEAVE,       // icon: PersonRemove, color: Warning  
    SERVER_START,       // icon: PlayArrow, color: Success
    SERVER_STOP,        // icon: Stop, color: Error
    SERVER_CRASH,       // icon: Error, color: Error
    BACKUP_CREATED,     // icon: Backup, color: Info
    BACKUP_RESTORED,    // icon: Restore, color: Warning
    PLUGIN_INSTALLED,   // icon: Extension, color: Info
    PLUGIN_REMOVED,     // icon: Delete, color: Warning
    COMMAND_EXECUTED,   // icon: Terminal, color: TextMuted
    ERROR               // icon: Error, color: Error
}
```

**Database:** Add `activity_events` table to SQLDelight schema.

### 6.3 Tunnel Status Enhancement
**File:** `composeApp/src/commonMain/kotlin/com/portalhost/server/TunnelManager.kt`

```kotlin
enum class TunnelStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
    CLAIM_REQUIRED  // New: waiting for user to claim URL
}

data class TunnelInfo(
    val status: TunnelStatus,
    val claimUrl: String? = null,
    val tunnelAddress: String? = null,  // e.g., "portalhost.playit.gg:12345"
    val lastError: String? = null,
    val connectedAt: Long? = null
)
```

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