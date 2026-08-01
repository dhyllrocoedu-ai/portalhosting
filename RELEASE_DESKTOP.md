## Portal Host 5.0.65 — Desktop

### Player Management (Live Players)
- **Online tab now shows live players** from console join/leave events instead of stale `usercache.json`
- **Minecraft head icons** per player (deterministic pixel-art variants)
- **Action buttons** per player: Kick, Ban, OP, De-OP — sends commands directly to server stdin
- **Player dedup** on join events (matches Android behavior)

### Marketplace Install Fix
- **Datapacks now install to `world/datapacks/`** (where Minecraft actually loads them) instead of `plugins/`
- **Project type checked first**: `datapack` → `world/datapacks`, `resourcepack` → `resourcepacks`, `mod` with fabric/forge/neoforge/quilt → `mods`, `plugin` or paper/spigot/purpur/folia loaders → `plugins`
- **Datapack loader handled**: projects with `project_type="mod"` + `loaders=["datapack"]` now correctly route to `world/datapacks`
- **Client-side types blocked from server install**: shaders, resourcepacks, modpacks show "Client-side only" message with disabled Install button

### Branding
- Green primary + portal violet/cyan accents palette
- Titlebar: surface background (`#1E1E1E` dark) with square logo + "Portal Host" text
- New logo assets: `portalhost_logo.png` (1024×1024), `portalhost_wordmark.png` (1536×1024), `portalhost2.ico` (16/32/48/64/128)

### Installers
- **PortalHost-5.0.65.msi** (132.8 MB) — WiX installer
- **PortalHost-5.0.65.exe** (133.5 MB) — jpackage EXE installer