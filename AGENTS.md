# Expo HAS CHANGED

Read the exact versioned docs at https://docs.expo.dev/versions/v56.0.0/ before writing any code.

# PortalHost — Project Context

## Architecture
- **Monorepo** with npm workspaces: `mobile/`, `desktop-agent/`, `shared/`
- **Mobile** (React Native / Expo 56 / React 19) — primary app
- **Desktop agent** (Express + Socket.IO) — spawns Java process on PC, optional

## Key Decisions
- **No cloud backend needed**: Modrinth API (free, no key) for content data, playit.gg (free API or agent) for tunnel, local filesystem for server files
- **Android** runs servers locally (same architecture as desktop agent) — app spawns Java process via native module; also handles playit.gg agent
- **iOS/Web** connect to a remote desktop agent (Apple blocks JIT, can't run Java)
- **No QR/pairing flow**: App opens directly to dashboard (splash → redirect)
- **5 tabs**: Dashboard, Console, Players, Content (Modrinth), Settings
- **Cross-platform icons**: Custom `Icon` component (uses expo-symbols/SFSymbols on iOS, text fallback on web/Android)
- **SafeAreaView** on both iOS and Android to avoid phone nav overlap
- **Version filtering**: Only shows 1.21+ in server setup picker

## Server Setup Flow
1. User taps "Create Server" (dashboard CTA or settings)
2. 7-step wizard: Welcome → Pick Type (Paper/Fabric/Vanilla) → Pick Version (1.21+ only) → Pick Build (Paper only) → Configure (name, RAM, online-mode, EULA) → Download → Done
3. Server jar downloaded from official API (PaperMC API v2, Fabric meta, Mojang launcher meta)
4. Server stored in app document directory

## Server Runner
- **Android**: Native module (Kotlin `ProcessBuilder`) spawns Java, pipes I/O, monitors process — matches desktop agent pattern exactly
- **Remote**: Socket.IO connection to desktop agent

## playit.gg Integration
- **Android**: App can download and run the playit.gg agent binary natively
- **PC**: Desktop agent runs the playit agent
- **Manual**: User enters tunnel address in settings, displayed on dashboard
- **Tunnel Terminal**: Dedicated screen showing agent output, start/stop, address display
- Address displayed on dashboard with tap-to-copy

## Content (Modrinth)
- `contentStore.ts` fetches from `api.modrinth.com/v2/search` with facets for version+loader compatibility
- Search by query, filter by server version/type, sort by downloads
- Loading/error states in content screen

## Relevant API Endpoints
- **PaperMC**: `GET https://api.papermc.io/v2/projects/paper` → versions list; `versions/{v}/builds` → builds with download URLs
- **Fabric**: `GET https://meta.fabricmc.net/v2/versions/game` + `/versions/loader/{v}` → server jar URL (redirect)
- **Vanilla**: `GET https://launchermeta.mojang.com/mc/game/version_manifest_v2.json` → version manifest with download URLs
- **Modrinth**: `GET https://api.modrinth.com/v2/search?facets=[["project_type:mod"],["categories:paper"],["versions:1.21.4"]]&limit=30&index=downloads`
- **playit.gg API**: `POST/GET/DELETE https://api.playit.gg/v1/account/tunnels` (Bearer token auth)

## Web CORS Proxy
- **File**: `mobile/src/api/serverSources.ts`
- **CORS proxies**: `corsproxy.io` (only maintained proxy; `allorigins.win` dead — 522, `corsproxy.org` became VPN site)
- **On web**: Build/version detail requests are staggered with 600ms delay to avoid proxy rate limiting
- **Fabric/Mojang APIs work directly** (both have `Access-Control-Allow-Origin: *`); only PaperMC needs the proxy
- **PaperMC** API lacks CORS headers — web requires a CORS proxy

## File Map
- `mobile/src/app/index.tsx` — Splash → redirect to dashboard (no QR/pairing)
- `mobile/src/app/setup.tsx` — 7-step server setup wizard
- `mobile/src/app/tunnel.tsx` — playit.gg tunnel terminal
- `mobile/src/app/(tabs)/dashboard.tsx` — Server manager (status, IP, controls, tunnel link)
- `mobile/src/app/(tabs)/settings.tsx` — Server management, tunnel config, desktop agent pairing
- `mobile/src/api/serverSources.ts` — PaperMC/Fabric/Vanilla API clients (1.21+ filtered)
- `mobile/src/lib/serverRunner.ts` — Local + remote process abstraction
- `mobile/src/lib/playit.ts` — playit.gg agent download + API methods
- `mobile/src/stores/tunnelStore.ts` — Tunnel terminal state (logs, status, address)
