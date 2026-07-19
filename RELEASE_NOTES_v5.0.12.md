### Changes in v5.0.12

**Tunnel stability improvements**
- Fixed playit.gg tunnel process crash on Windows by ensuring proper binary name (`playitd.exe`) and adding a 1‑second health check after process start before marking it as connected.

**Desktop UI enhancements**
- Added native‑style back button in `ServerDetailScreen` using a `Scaffold` + `TopAppBar` with a back‑arrow that calls the supplied `onBack` callback.
- Removed the leftover custom window chrome; the window now uses the native OS title bar with the custom back button inside the content area.

**Version bump**
- Updated package version to **5.0.12** (MSI and EXE now named `PortalHost-5.0.12.msi` / `PortalHost-5.0.12.exe`).

**Artifacts**
- `PortalHost-5.0.12.msi`
- `PortalHost-5.0.12.exe`