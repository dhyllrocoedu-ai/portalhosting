### Changes in v5.0.11

**Version bump to 5.0.11** – MSI and EXE now ship as `PortalHost-5.0.11.msi` / `PortalHost-5.0.11.exe`.

**Enhanced uninstaller support**
- **WiX (MSI)**: custom actions clean `%APPDATA%\PortalHost`, `%LOCALAPPDATA%\PortalHost` and registry `HKCU\Software\PortalHost` on uninstall; adds "Uninstall PortalHost" shortcut to Start Menu; ARP metadata (comments, help link, about URL).
- **jpackage (EXE)**: config file added (`src/windows/jpackage/jpackage.cfg`) with proper app name/version, menu & shortcut flags, per‑machine install.

**Build artifacts**
- MSI: `PortalHost-5.0.11.msi`
- EXE: `PortalHost-5.0.11.exe`