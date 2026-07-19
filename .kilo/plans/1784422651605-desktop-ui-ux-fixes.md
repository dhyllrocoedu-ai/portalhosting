# PortalHost Desktop UI/UX Fixes Plan

## Overview
This plan addresses multiple UI/UX issues and missing functionalities in the PortalHost desktop application identified from user feedback and log analysis.

## Issues to Fix

### 1. Tunnel Manager - Playit.gg Connection Error (HIGH PRIORITY)
**Problem:** The tunnel process crashes immediately after starting with `java.io.IOException: Stream closed` error.

**Root Cause:** The `startReader` function in `TunnelManager.kt:203-229` tries to read from the process input stream, but the stream gets closed before reading completes. The issue is in the `start` function where the process is started and immediately checked.

**Solution:**
- Modify `startReader` to handle stream lifecycle properly
- Add a delay/health check before marking the tunnel as connected
- Use `ProcessBuilder.Redirect.PIPE` explicitly
- Add better error handling for stream closure
- Consider reading from the log file instead of process stream for stability

**Files:**
- `composeApp/src/commonMain/kotlin/com/portalhost/server/TunnelManager.kt`

### 2. Dashboard Player Display - Not a Button (MEDIUM PRIORITY)
**Problem:** In the PerformanceCard, the player count appears clickable (like a button) but should just be a display showing the player count.

**Current Location:** `DashboardScreen.kt:688-709` - The player stat card uses `Surface` with `clickable` modifier calling `onOpenPlayers()`.

**Solution:**
- Remove the clickable behavior from the player stat card
- Make it a simple display-only stat card like CPU/RAM/TPS
- Keep the "View Details" button functionality for opening the full player management screen

**Files:**
- `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/screens/DashboardScreen.kt`

### 3. Player List - Full Screen Below Console (MEDIUM PRIORITY)
**Problem:** The player list should be displayed as a full-screen view (like the console screen) rather than in the dashboard card. The Android native app has a proper full-screen `PlayersScreen` with tabs.

**Solution:**
- The `PlayerManagementScreen.kt` already exists but needs navigation integration
- Add navigation from Dashboard's PerformanceCard to `PlayerManagementScreen`
- Ensure `PlayerManagementScreen` matches Android functionality:
  - TopAppBar with back button
  - Tabs: Online, Whitelist, Operators, Banned Players, Banned IPs
  - Proper file reading for each tab

**Files:**
- `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/screens/PlayerManagementScreen.kt`
- `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/DesktopMain.kt`

### 4. Missing Back Buttons (HIGH PRIORITY)
**Problem:** Several screens lack back button functionality:
- `CreateServerScreen` - No back button during wizard
- `ServerConsoleScreen` - Back button exists but may not be functioning
- Other screens may be missing navigation

**Solution:**

#### CreateServerScreen:
- Add a TopAppBar with back button at the top of the wizard
- Back button should navigate to previous screen (Servers or Home)
- Handle step 0 back button to cancel creation

**Files:**
- `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/screens/CreateServerScreen.kt`

#### ServerConsoleScreen:
- Verify back button is visible and functional
- Ensure `onBack` callback is properly wired in `DesktopMain.kt`

**Files:**
- `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/screens/ServerConsoleScreen.kt`
- `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/DesktopMain.kt`

### 5. Welcome Screen - First Install (LOW PRIORITY)
**Problem:** No welcome/onboarding screen appears on first installation.

**Solution:**
- Create a `WelcomeScreen` composable that shows:
  - App introduction
  - Quick start guide
  - "Get Started" button
- Store a preference flag `hasSeenWelcome` in `Preferences.kt`
- Show welcome screen only on first launch
- Allow users to skip and access later from Settings

**Files:**
- Create: `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/screens/WelcomeScreen.kt`
- Modify: `composeApp/src/desktopMain/kotlin/com/portalhost/desktop/DesktopMain.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/portalhost/preferences/Preferences.kt`

### 6. Windows Uninstaller (MEDIUM PRIORITY)
**Problem:** After installation, no uninstaller executable is created in the installation directory.

**Solution:**
- Check the Windows installer configuration (WiX template)
- Ensure uninstaller is registered in Windows Add/Remove Programs
- Optionally create a convenience `uninstall.exe` in the installation directory

**Files:**
- `composeApp/src/windows/wix/template.wxs`
- Check build configuration for Windows packaging

### 7. Additional Missing Functionality Audit (MEDIUM PRIORITY)
**Problem:** There may be other missing back buttons or navigation issues.

**Screens to Audit:**
- `ServerFilesScreen` - Check for back navigation
- `SettingsScreen` - Check for back navigation  
- `PerformanceScreen` - Check for back navigation
- `LogViewerScreen` - Check for back navigation
- `RconScreen` - Check for back navigation (already has back button in ServerDetailScreen tab structure)

**Solution:**
- Review each screen for consistent navigation patterns
- Add TopAppBar with back button where appropriate
- Ensure all `onBack` callbacks are properly wired

---

## Implementation Order

1. **Tunnel Manager Fix** - Critical functionality issue
2. **Back Button Fixes** - Navigation blocking issue
3. **Player List Full Screen** - UX improvement
4. **Dashboard Player Display** - Visual clarity
5. **Windows Uninstaller** - Installation completeness
6. **Welcome Screen** - Nice-to-have onboarding
7. **Additional Navigation Audit** - Polish pass

---

## Testing Checklist

After implementation:
- [ ] Tunnel connects successfully and stays connected
- [ ] All screens have working back buttons
- [ ] Player management screen opens from dashboard
- [ ] Player count in dashboard is not clickable
- [ ] Welcome screen appears on first launch (after clearing prefs)
- [ ] Windows installer includes uninstaller
- [ ] Navigation flow is consistent across all screens

---

## Notes

- The Android native app (`PlayersScreen.kt`) should be used as reference for player management UI
- All navigation callbacks should follow the existing pattern in `DesktopMain.kt`
- Tunnel fix should preserve existing claim URL flow and tunnel address display