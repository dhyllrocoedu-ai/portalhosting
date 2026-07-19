# PortalHost Desktop UI Redesign - Portalyx Style

## Design System

### Color Palette
```kotlin
// Backgrounds
Background Primary: #0D1117 (Deep navy)
Background Secondary: #161B22 (Card background)
Background Tertiary: #21262D (Elevated elements)
Background Hover: #30363D

// Borders
Border Default: #30363D
Border Subtle: #21262D

// Accent Colors
Primary: #8B5CF6 (Purple)
Primary Hover: #7C3AED
Success: #10B981 (Green)
Error: #EF4444 (Red)
Warning: #F59E0B (Amber)
Info: #3B82F6 (Blue)

// Text
Text Primary: #F0F6FC
Text Secondary: #8B949E
Text Muted: #6E7681
```

### Typography
- Font Family: 'Inter', system-ui, sans-serif
- Headings: Bold (600-700)
- Body: Regular (400)
- Small/Captions: Medium (500)

### Spacing Scale
- 4px, 8px, 12px, 16px, 20px, 24px, 32px

### Border Radius
- Small: 6px
- Medium: 8px
- Large: 12px
- XLarge: 16px

---

## Screen Redesigns

### 1. Main App Structure (DesktopMain.kt)

**Layout:**
```
┌─────────────────────────────────────────────────────────────┐
│ Sidebar │              Main Content Area                    │
│ (240px) │  - Top Bar (search, notifications, profile)       │
│         │  - Content (dynamic based on selection)           │
│ Home    │                                                    │
│ Servers │                                                    │
│ Settings│                                                    │
│ Account │                                                    │
│         │                                                    │
│ ─────── │                                                    │
│ Status  │                                                    │
│ Profile │                                                    │
└─────────┴────────────────────────────────────────────────────┘
```

**Sidebar Component:**
- Logo + App name at top
- Navigation items with icons
- Active state with purple background
- System status at bottom
- User profile card at bottom

---

### 2. Home/Dashboard Screen

**Layout:**
```
┌──────────────────────────────────────────────────────────────────┐
│ SELECTED SERVER                                                  │
│ ┌─────────────┐  Survival SMP  ● Online  ▼                      │
│ │ Server Img  │  ┌──────┐ ┌─────────┐ ┌──────┐                  │
│ │             │  │ Start│ │ Restart │ │ Stop │ ⋮               │
│ └─────────────┘                                                  │
├──────────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ Survival SMP                              │ CPU │ RAM │ TPS │ │
│ │ ───────────────────────────────────────── │─────│─────│─────│ │
│ │ Address: survival.ph.com:25565 [copy]     │ 18% │3.4G │20.0 │ │
│ │ Software: Paper 1.21.6                    │ ~~~ │ ~~~ │─── │ │
│ │ Java: OpenJDK 21                          │     │     │     │ │
│ │ Uptime: 2d 14h 32m                        ├─────┴─────┴─────┤ │
│ │ Players: 6/20 [View Players]              │ Storage │Network│ │
│ │                                           │ 12.4GB  │ ↑1.2 │ │
│ │ [World] [Survival] [Hard] [PVP ✓]         │ 64GB    │ ↓2.4 │ │
│ └───────────────────────────────────────────┴─────────┴───────┘ │
├─────────────────┬──────────────────────┬────────────────────────┤
│ Recent Activity │ Resource Usage       │ Server Status          │
│ • Steve joined  │ CPU: ████░ 18%       │ ● Server: Running      │
│ • Plugin inst.  │ RAM: █████░ 3.4/6GB  │ ● Network: Healthy     │
│ • Backup done   │ DISK: ██░ 12.4/64GB  │ ● Database: Online     │
│ • Server restart│ │                    │ ● Backups: 15m ago     │
│ • World saved   │ [View Charts]        │ ● Auth: Online         │
├─────────────────┴──────────────────────┴────────────────────────┤
│ SERVER TOOLS                                                     │
│ [Console] [Files] [Plugins] [Mods] [Worlds] [Backups] [Settings]│
└──────────────────────────────────────────────────────────────────┘
```

---

### 3. Servers List Screen

**Layout:**
```
┌─────────────────────────────────────────────────────────────────┐
│ My Servers                    [+ New Server]                    │
│ ┌──────────────────┐ [All ▼]  [Search...]                       │
│ ├───────────────────────────────────────────────────────────────┤
│ │ ┌───────────────────────────────────────────────────────────┐ │
│ │ │ 🖼️  Survival SMP                        Online   6/20 ⋮  │ │
│ │ │     1.21.6 • Paper                                      │ │
│ │ └───────────────────────────────────────────────────────────┘ │
│ │ ┌───────────────────────────────────────────────────────────┐ │
│ │ │ 🖼️  Creative                          Online   2/20 ⋮    │ │
│ │ │     1.21.6 • Paper                                      │ │
│ │ └───────────────────────────────────────────────────────────┘ │
│ │ ┌───────────────────────────────────────────────────────────┐ │
│ │ │ 🖼️  SkyBlock                          Starting   0/20 ⋮  │ │
│ │ │     1.21.6 • Paper                                      │ │
│ │ └───────────────────────────────────────────────────────────┘ │
│ │ ┌───────────────────────────────────────────────────────────┐ │
│ │ │ 🖼️  Modded World                      Stopped    0/20 ⋮  │ │
│ │ │     1.20.1 • Forge                                      │ │
│ │ └───────────────────────────────────────────────────────────┘ │
│ │                                                               │
│ │ 6/10 servers used                                             │
│ │ ████████████░░░░░░░░░░░░░░░░░░░░░░░░ [Upgrade Plan]          │
│ └───────────────────────────────────────────────────────────────┘
```

---

### 4. Server Detail/Console Screen

**Tab Navigation (horizontal, icon + label):**
```
[Overview] [Console] [Files] [Plugins] [Mods] [Datapacks] [Worlds] [Backups] [Performance] [Network] [Settings]
```

**Console Tab Layout:**
```
┌─────────────────────────────────────────────────────────────────┐
│ Quick Actions        │ Console (Live)                  │ Players│
│ ┌──────┬──────────┐  │ [12:35:10] [INFO] Starting...   │ 6/20   │
│ │ ▶️   │ 🔄       │  │ [12:35:11] [INFO] Loading libs  │ ────── │
│ │Start │ Restart  │  │ [12:35:12] [INFO] Done (1.234s) │ 🧑Steve│
│ └──────┴──────────┘  │                                 │ 🧑Alex │
│ ┌──────┬──────────┐  │                                 │ 🧑Notch│
│ │ ⬛   │ 💀       │  │                                 │ 🧑Hero │
│ │ Stop  │Kill Proc│  │                                 │ 👾Creeper│
│ └──────┴──────────┘  │                                 │ 👾Ender│
│ ┌──────────────────┐  │ ┌─────────────────────────────┐│        │
│ │ 💾 Backup Now    │  │ │ Type a command...    [Send]││        │
│ └──────────────────┘  │ └─────────────────────────────┘│        │
├───────────────────────┴────────────────────────────────┴────────┤
│ Recent Activity          │ Storage                              │
│ • Steve joined (now)     │ ████████░░░░░░░░░░░░░░░░ 14.4/64GB  │
│ • Alex joined (1m)       │ 🟪 Worlds: 6.8GB                    │
│ • Backup completed (15m) │ 🟪 Plugins: 512MB                   │
│ • Plugin updated (25m)   │ 🟧 Mods: 2.1GB                      │
│                        │ 🟩 Backups: 3.2GB                   │
│ [View All]               │ ⬜ Other: 1.8GB                     │
└─────────────────────────────────────────────────────────────────┘
```

---

## Component Library

### Stat Card
```kotlin
Card(
    colors = cardColors(Background Secondary),
    border = Border(1px, Border Subtle),
    radius = 12px
) {
    Column {
        Text(label, color = Text Secondary, fontSize = 12.sp)
        Spacer(8px)
        Text(value, color = Text Primary, fontSize = 24.sp, fontWeight = Bold)
        Sparkline(data, color = accentColor)
    }
}
```

### Server Card (List Item)
```kotlin
Card(
    modifier = clickable,
    selected = isSelected
) {
    Row {
        ServerIcon(server, size = 48.dp)
        Column(Modifier.weight(1f)) {
            Text(server.name, fontWeight = SemiBold)
            Text("${version} • ${type}", color = Text Secondary, fontSize = 12.sp)
        }
        StatusBadge(status)
        Text("$players/$maxPlayers", color = Text Secondary)
        IconButton { MoreOptions() }
    }
}
```

### Action Button
```kotlin
Button(
    colors = when (type) {
        Start -> Green
        Stop -> Red
        Restart -> Amber
        Default -> Primary
    }
) {
    Icon(icon)
    Spacer(8px)
    Text(label)
}
```

---

## Implementation Files

### New Files to Create:

#### 1. Theme Files
**`composeApp/src/desktopMain/kotlin/com/portalhost/desktop/theme/PortalyxColors.kt`**
- Color palette object with all Portalyx-inspired colors
- Background, text, accent, status, and semantic colors

**`composeApp/src/desktopMain/kotlin/com/portalhost/desktop/theme/PortalyxTheme.kt`**
- MaterialTheme composition with Portalyx colors
- Typography definitions
- Shape definitions

#### 2. Component Files
**`composeApp/src/desktopMain/kotlin/com/portalhost/desktop/components/Sidebar.kt`**
- Vertical navigation with icons
- Active state highlighting
- User profile section at bottom
- System status indicator

**`composeApp/src/desktopMain/kotlin/com/portalhost/desktop/components/ServerCard.kt`**
- Server list item component
- Server icon, name, version, status
- Player count display
- Context menu button

**`composeApp/src/desktopMain/kotlin/com/portalhost/desktop/components/StatCard.kt`**
- Stat display with label, value, sparkline
- Used for CPU, RAM, TPS, etc.

**`composeApp/src/desktopMain/kotlin/com/portalhost/desktop/components/Sparkline.kt`**
- Mini line chart component
- Smooth path rendering
- Configurable colors

**`composeApp/src/desktopMain/kotlin/com/portalhost/desktop/components/StatusBadge.kt`**
- Status indicator with dot and label
- Color-coded by status type

**`composeApp/src/desktopMain/kotlin/com/portalhost/desktop/components/TopBar.kt`**
- Search bar
- Notifications icon
- User profile dropdown

**`composeApp/src/desktopMain/kotlin/com/portalhost/desktop/components/ServerHero.kt`**
- Large server info card with image
- Start/Stop/Restart buttons

**`composeApp/src/desktopMain/kotlin/com/portalhost/desktop/components/ServerTools.kt`**
- Grid of tool buttons (Console, Files, Plugins, etc.)

### Files to Modify:
1. **`DesktopMain.kt`** - Replace tab navigation with sidebar, new theme
2. **`DashboardScreen.kt`** - Complete redesign with new layout
3. **`ServersScreen.kt`** - New server list design with cards
4. **`ServerDetailScreen.kt`** - Updated tabs and modernized cards
5. **`ServerConsoleScreen.kt`** - Console with new design, quick actions
6. **`CreateServerScreen.kt`** - Match new theme
7. **`SettingsScreen.kt`** - Match new theme
8. **`ThemeColors.kt`** - Add Portalyx color palette (commonMain)

---

## Step-by-Step Implementation

### Phase 1: Foundation (Theme & Colors)
1. Create `PortalyxColors.kt` with all color definitions
2. Create `PortalyxTheme.kt` with MaterialTheme setup
3. Update `ThemeColors.kt` to reference Portalyx colors

### Phase 2: Core Components
1. Create `Sidebar.kt` - Main navigation component
2. Create `TopBar.kt` - Header with search and profile
3. Create `StatusBadge.kt` - Reusable status indicator
4. Create `ServerCard.kt` - Server list item

### Phase 3: Dashboard Components
1. Create `ServerHero.kt` - Large server info card
2. Create `StatCard.kt` - Stat display with sparkline
3. Create `Sparkline.kt` - Mini chart component
4. Create `ServerTools.kt` - Tool button grid

### Phase 4: Screen Redesigns
1. Redesign `DesktopMain.kt` layout structure
2. Redesign `DashboardScreen.kt` completely
3. Redesign `ServersScreen.kt` with new cards
4. Update `ServerDetailScreen.kt` tabs
5. Update `ServerConsoleScreen.kt` layout

### Phase 5: Polish
1. Add hover effects and animations
2. Ensure consistent spacing
3. Test all navigation flows
4. Verify dark theme consistency

---

## Animation & Interactions

### Hover Effects
- Cards: Slight brightness increase + border glow
- Buttons: Scale 1.02 + brightness
- Sidebar items: Background fill + icon animation

### Transitions
- Screen transitions: Fade + slide (200ms)
- Tab switches: Fade (150ms)
- Status changes: Pulse animation

### Loading States
- Skeleton loaders for cards
- Spinner for async actions
- Progress bars for downloads

---

## Responsive Considerations

### Minimum Window Size: 1024x768
### Optimal: 1280x800+
### Sidebar collapsible at < 1100px width