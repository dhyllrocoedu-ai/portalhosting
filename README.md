# PortalHost

> Minecraft server manager — run, configure, and share a server directly from your phone. No Termux, no command line, no port forwarding.

## What It Is

PortalHost turns your Android device (or a PC via the desktop agent) into a Minecraft server host. It's a **user-friendly alternative to Termux** — instead of typing commands, you tap through a wizard to pick your server software, configure RAM, accept the EULA, and start playing.

### Core Philosophy

- **Zero cloud backend** — everything runs locally on your device or PC
- **Free APIs only** — Modrinth, PaperMC, Fabric, Mojang, playit.gg — no API keys required
- **Portable** — a server in your pocket. Pull out your phone, start the server, share the address
- **No port forwarding** — built-in playit.gg integration for public access behind NAT

## Features

### Server Setup Wizard
1. **Pick server software** — PaperMC (recommended), Fabric, or Vanilla
2. **Pick version** — only shows modern versions (1.21+) fetched from official APIs
3. **Pick build** — PaperMC builds fetched live from PaperMC API v2
4. **Configure** — server name, RAM allocation, online-mode toggle
5. **Download & install** — server jar downloaded directly from the provider
6. **Done** — start the server from the dashboard

### Server Manager
- **Dashboard** — one-tap Start/Stop/Restart, live status, player count, uptime
- **Console** — real-time server log viewer with search and command input
- **Players** — view online, whitelisted, and banned players; kick, ban, op from the app
- **Performance** — RAM, CPU, and TPS monitoring

### Content Browser
- Browse and search plugins and datapacks from **Modrinth** (free, no API key)
- Filter by server version and loader type
- One-tap "install" tracking
- Real search, real data — no mock items

### playit.gg Tunnel
- **On Android** — runs the playit.gg agent natively, pipes output to a terminal view
- **On PC** — desktop agent handles the tunnel; just enter the address in settings
- **Tunnel Terminal** — live playit.gg agent output, start/stop controls
- Public address displayed on the dashboard with tap-to-copy

## Architecture

```
┌──────────────────────────────────────────────────┐
│               PortalHost Mobile                   │
│  ┌──────────┬──────────┬──────────┬──────────┐   │
│  │Dashboard │ Console  │ Players  │ Content  │   │
│  └──────────┴──────────┴──────────┴──────────┘   │
│  ┌───────────────────────────────────────────┐    │
│  │ Settings (server mgmt, tunnel, config)    │    │
│  └───────────────────────────────────────────┘    │
│  ┌───────────────────────────────────────────┐    │
│  │ Server Runner │ Tunnel Agent │ Modrinth   │    │
│  └───────────────┴──────────────┴────────────┘    │
└──────────────────────────────────────────────────┘
         │ Android                │ iOS/Web
         ▼                        ▼
  ┌──────────────┐       ┌──────────────┐
  │ Java Process │       │ Desktop Agent│
  │ (on-device)  │       │ (PC/SocketIO)│
  │ playit agent │       │ playit agent │
  └──────────────┘       └──────────────┘
```

### Platform Support

| Feature | Android | iOS | Web |
|---------|---------|-----|-----|
| Local server (Java) | ✅ (native module) | ❌ (Apple blocks JIT) | ❌ |
| Desktop agent remote | ✅ | ✅ | ✅ |
| playit.gg agent | ✅ (native binary) | ❌ | ❌ |
| playit.gg API tunnel | ✅ | ✅ | ✅ |
| Modrinth browser | ✅ | ✅ | ✅ |
| Server setup wizard | ✅ | ✅ (downloads only) | ✅ (downloads only) |

## Tech Stack

- **Framework**: React Native / Expo 56
- **Navigation**: expo-router (file-based routing)
- **State**: Zustand
- **Icons**: expo-symbols (iOS), text/emoji fallbacks (Android/Web)
- **Server APIs**: PaperMC API v2, Fabric meta API, Mojang launcher meta
- **Content**: Modrinth API v2
- **Desktop Agent**: Express + Socket.IO
- **Tunnel**: playit.gg agent or API

## Getting Started

### Prerequisites
- Node.js 18+
- npm
- Expo CLI (`npm install -g expo-cli`) or use `npx expo`

### Install

```bash
# from repo root
npm install
cd mobile
npx expo start
```

### Desktop Agent (for iOS/Web or PC hosting)

```bash
cd desktop-agent
npm install
npm start
```

The agent runs on port 3000 and accepts Socket.IO connections from the mobile app.

## API Endpoints Used

| Service | Endpoint | Purpose |
|---------|----------|---------|
| PaperMC | `GET /v2/projects/paper` | List versions |
| PaperMC | `GET /v2/projects/paper/versions/{v}/builds` | List builds with download URLs |
| Fabric | `GET /v2/versions/game` | List game versions |
| Fabric | `GET /v2/versions/loader/{v}` | Get loader + server jar URL |
| Mojang | `GET /mc/game/version_manifest_v2.json` | Version manifest with server download URLs |
| Modrinth | `GET /v2/search` | Search plugins/datapacks by facets |
| playit.gg | `POST /v1/account/tunnels` | Create tunnel (optional API key) |

## Development

### Project Structure
```
PortalHost/
├── mobile/                    # React Native / Expo app
│   ├── src/
│   │   ├── app/              # expo-router pages
│   │   │   ├── index.tsx     # Splash → redirects to dashboard
│   │   │   ├── setup.tsx     # Server setup wizard (7 steps)
│   │   │   ├── tunnel.tsx    # playit.gg tunnel terminal
│   │   │   └── (tabs)/       # Tab screens
│   │   │       ├── dashboard.tsx
│   │   │       ├── console.tsx
│   │   │       ├── players.tsx
│   │   │       ├── content.tsx
│   │   │       └── settings.tsx
│   │   ├── api/              # External API clients
│   │   │   └── serverSources.ts  # PaperMC, Fabric, Vanilla, playit.gg
│   │   ├── stores/           # Zustand stores
│   │   │   ├── serverStore.ts
│   │   │   ├── setupStore.ts
│   │   │   ├── tunnelStore.ts
│   │   │   ├── contentStore.ts
│   │   │   └── ...
│   │   ├── lib/              # Utilities
│   │   │   ├── serverRunner.ts
│   │   │   ├── playit.ts
│   │   │   └── socket.ts
│   │   ├── components/       # Shared components
│   │   │   └── Icon.tsx
│   │   └── constants/
│   │       └── theme.ts
│   ├── package.json
│   └── tsconfig.json
├── desktop-agent/            # PC server agent
│   ├── src/index.js          # Express + Socket.IO server
│   └── package.json
└── shared/                   # Shared TypeScript types (reference only)
    └── types.ts
```

### Key Scripts

```bash
cd mobile
npx expo start          # Start dev server
npx expo start --web    # Web version
npx expo start --android
npx tsc --noEmit        # Type check
```

## Why Not Termux?

Termux is powerful but requires:
- Knowing Linux commands
- Manually installing Java
- Configuring server files via CLI
- Setting up tunnels manually with ngrok/playit CLI
- No graphical server management

PortalHost wraps all of that in a native UI:
- Step-by-step wizard instead of `curl | bash`
- Visual server configuration
- One-tap start/stop
- Built-in tunnel management
- Real-time console and metrics
- Plugin/datapack browser with search
