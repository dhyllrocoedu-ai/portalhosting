### Changes in v5.0.11

**Installer & Uninstaller Improvements**
- Added WiX custom actions (`template.wxs`) for clean uninstall:
  - Removes `%APPDATA%\PortalHost` and `%LOCALAPPDATA%\PortalHost` data directories
  - Deletes `HKCU\Software\PortalHost` registry keys
  - Properly registers ARP (Add/Remove Programs) entry with support links
- Added jpackage config (`jpackage.cfg`) for polished EXE installer:
  - Custom icon, Start Menu shortcuts, uninstall entry
  - Directory chooser, per-machine install

**Build & Versioning**
- Bumped package version to **5.0.10** (MSI/EXE now show 5.0.10)
- Build system uses standard decorated window (no custom WindowManager hacks)

**Previous Features (v5.0.9)**
- Playit.gg tunnel fix (Windows binary renamed to `playitd.exe`)
- Full-featured Dashboard Console (filters, search, auto-scroll, color coding)
- Tunnel Card with domains, IP:port, copy buttons
- ServerCard icon updates (Layers, Laptop, Settings gear)
- Player Management full-screen with 5 tabs
- Shared `ConsoleUtils.kt` for log classification/coloring

**Artifacts**
- MSI: `PortalHost-5.0.10.msi`
- EXE: `PortalHost-5.0.10.exe`