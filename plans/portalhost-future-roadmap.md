# PortalHost — Future Implementation Roadmap

> **Status:** Planning document — not committed to main branch.
> **Date:** 2026-08-15
> **Current release:** v5.0.71-mobilev2

---

## Current State Summary

- **Android (native)**: ~71 KT files, 35+ screens — feature-complete for core hosting
- **Desktop (composeApp)**: ~105 KT files, 27+ screens — includes World Map, Player Detail, full Marketplace, WelcomeScreen
- **v5.0.71 released**: 0 B/s network-rate fix, Storage Card, Live IP display on server card, CPU-% rounding
- **Two major unimplemented plans exist**: Desktop UI Redesign (789-line detailed spec) and Auth/Subscription (269-line spec)
- **Known minor gaps**: error toasts on desktops, two TODOs in `ServerDetailScreen.kt`, unused imports in `HomeServerCard.kt`

---

## Phase 1: v5.0.72 — Polish & Quick Wins (~1–2 days)

| # | Item | Effort |
|---|------|--------|
| 1 | **Recent Activity transitional states** — add `STARTING`/`STOPPING` events to `ActivityLog` so the dashboard shows intermediate states instead of jumping straight ONLINE→STOPPED | ~2 h |
| 2 | **Desktop error toasts on server delete** — two `// TODO: Show error toast` at `ServerDetailScreen.kt:221,226` | ~30 m |
| 3 | **Android unused imports cleanup** — `HomeServerCard.kt` has unused `combinedClickable`, `DpOffset`, redundant `LazyRow` | ~10 m |
| 4 | **Android WelcomeScreen** — port `desktop/screens/WelcomeScreen.kt` to native as first-launch onboarding | ~2 h |
| 5 | **Android sparkline charts** — `PerformanceChartsScreen` is static; add rolling 60-point history like desktop | ~4 h |

**Decision point:** Should the CANNOT LINK EXECUTABLE fix be revisited? It shipped in the initial v5.0.71 commit (7326d11) but was later reverted in the working tree. If users hit "CANNOT LINK" on JDK start, a different approach (e.g., post-install verification + auto-repair) may be needed.

---

## Phase 2: Desktop UI Redesign — "Portalyx" (~2–3 weeks)

**Source spec:** `.kilo/plans/portalhost-redesign-plan.md` (789 lines, fully detailed)

| Sub-phase | Key Deliverables |
|-----------|------------------|
| **2.1 Critical Bug Fixes** | Fix desktop `TunnelManager` stream crash (stderr stream closed before read, causing `IOException: Stream closed`), add back buttons to all screens, `hasSeenWelcome` flag + first-launch WelcomeScreen, WiX uninstaller entry |
| **2.2 Theme System** | `PortalyxColors.kt` + `PortalyxTheme.kt` — deep-navy palette (#0D1117), Inter typography, 6dp–24dp shape tokens |
| **2.3 Core Components** | `Sidebar.kt` (240 px, purple active state), `TopBar.kt` (56 px, server selector), `StatusBadge.kt`, `ServerCard.kt`, `Sparkline.kt` + `StatCard.kt`, `ServerHero.kt`, `ServerTools.kt`, `ActivityItem.kt` |
| **2.4 Screen Redesigns** | Dashboard (hero + 6 sparkline stat cards + activity + tools), Servers (card grid), ServerDetail (modern tabs), ServerConsole (3-panel), CreateServer (wizard), Settings (categorized) |
| **2.5 Data Models** | `ServerState` + rolling 60-point history fields, `ActivityEvent` enum + SQLDelight `activity_events` table, enhanced `TunnelStatus` enum with `CLAIM_REQUIRED` |
| **2.6 Polish** | Animations (200 ms screen, 150 ms tab, hover states), responsive breakpoints (1024 / 1280 / 1600), accessibility (AA contrast, focus rings) |

**Tradeoff:** This is a full UI rewrite. Desktop will be in flux for 2–3 weeks. Mobile parity features should either be parallel-tracked or deferred until desktop stabilizes.

---

## Phase 3: Authentication & Subscription (~2–3 weeks)

**Source spec:** `docs/auth-subscription-plan.md` (269 lines, 6 phases)

| Sub-phase | Key Deliverables |
|-----------|------------------|
| **3.0 Foundation** | Firebase BOM, Google Play Billing (`billing-ktx`), `EncryptedSharedPreferences`, DataStore, (optional) Hilt DI |
| **3.1 Auth Core** | `AuthManager` (StateFlow), `AuthApi` (OkHttp + REST), `TokenStore`, `User` / `AuthState` models |
| **3.2 Google Play Billing** | `BillingManager` wrapper (query products, launch purchase flow, acknowledge), server-side `/subscription/verify` endpoint |
| **3.3 Auth UI** | Login / Signup / ForgotPassword screens with validation, loading/error states |
| **3.4 Route Guards** | `AuthGate` composable wrapping `NavHost`, feature gating (tunnel = premium ≥ 1 server for free) |
| **3.5 Account Screen** | Profile card, subscription status + countdown, "Manage Subscription" → Play Store, "Upgrade to Premium" → billing flow, logout |

**Backend required:** Supabase or Firebase Functions (~6 endpoints: signup, login, refresh, me, verify, status).

**Monetization tiers:**

| Tier | Server cap | Max RAM | Tunnel | Backups | Priority support |
|------|-----------|---------|--------|---------|-----------------|
| Free | 1 | 1 GB | ❌ | ❌ | ❌ |
| Premium (monthly / yearly / lifetime) | ∞ | 8 GB | ✅ | ✅ | ✅ |

**Tradeoff:** This touches Android + Desktop + Backend simultaneously. Requires a backend dev or Firebase Functions developer. Should it run in parallel with the Desktop Redesign, or sequentially after it?

**Question:** Do you want monetization now, or after the desktop redesign ships?

---

## Phase 4: Mobile Parity (ongoing, ~1–2 weeks)

Port desktop features that are already implemented on desktop but missing from Android.

| Feature | Desktop | Android | Effort |
|---------|---------|---------|--------|
| World Map | ✅ `WorldMapScreen.kt` | ❌ Missing | ~1 week |
| Player Detail | ✅ `PlayerDetailScreen.kt` | ❌ Missing | ~3 days |
| Full RCON Screen | ✅ `RconScreen.kt` | ⚠️ Basic Console only | ~2 days |
| WelcomeScreen | ✅ | ❌ Missing | ~2 h |
| Sparkline charts | ✅ Dashboard | ❌ Static `PerformanceChartsScreen` | ~4 h |
| Marketplace filters | ✅ Full `MarketplaceFilters.kt` | ⚠️ Basic only | ~3 days |

**Android-specific QoL:**
- Auto-backup scheduling (e.g., every 4 hours, rotation)
- Crash reporting (Firebase Crashlytics or Sentry)
- Tactile feedback feedback (haptics on long-press, button actions)

---

## Phase 5: Platform Expansion (future, unscheduled)

| Target | Notes |
|--------|-------|
| **iOS** | Compose Multiplatform supports iOS; add `iosMain` + `iosApp`, handle foreground-service equivalent, test App Store requirements |
| **Linux** | Compose Desktop already supports it; add AppImage / deb / rpm packages, test file associations + icons |
| **macOS** | Compose Desktop supports it; add dmg package, notarize for Gatekeeper, handle sandbox permissions |
| **Web (WASM)** | Experimental; could offer a read-only dashboard or server status view |

---

## Phase 6: QoL / Stability / Marketing (continuous)

| Area | Ideas |
|------|-------|
| **Crash reporting** | Firebase Crashlytics (Android), Sentry (Desktop) |
| **Auto-update desktop** | Delta/patch MSI updates, background download, silent install; Android updater exists |
| **Website updates** | Roadmap page, refresh "Key Features" for v5.0.71 fixes, consider CDN for APK (CF Pages 25 MB limit) |
| **Accessibility** | Screen-reader labels, focus order, AA contrast (covered in redesign plan §5.5) |
| **Performance** | Desktop startup profiling, memory profiling, lazy tab loading, smaller APK (current: 10 MB) |

---

## Recommended Execution Sequence

```
v5.0.72 (polish ~1 day)
    → Desktop Redesign (~3 weeks, parallel: mobile parity)
        → Auth / Subscription (~3 weeks, needs backend)
            → Platform Expansion (iOS / Linux / macOS)
                → Continuous QoL / Stability
```

Why this order:
1. **Polish first** — establishes a clean baseline before major surgery.
2. **Desktop Redesign** — biggest user-facing win; spec is already detailed and ready to execute.
3. **Auth/Subscription** — revenue unlock; runs in parallel with mobile parity to save time.
4. **Mobile Parity** — ports the redesigned desktop features to Android naturally.
5. **Platform Expansion** — apples-to-apples ports once Kotlin Multiplatform is stable.

---

## Open Questions (blockers)

1. **Priority**: Is the Desktop Redesign the top priority, or Auth/Subscription (revenue) first?
2. **CANNOT LINK fix**: Revisit with a different approach, or accept the reverted state?
3. **Team bandwidth**: How many devs? Redesign can be parallelized (components in parallel tracks); Auth needs backend.
4. **Backend choice**: Supabase (open-source REST + Realtime), Firebase (standard), or custom server?
5. **iOS**: iOS target for 2026, or desktop+Android only for now?
6. **Release cadence**: Monthly? Quarterly? Affects how phases are batched.