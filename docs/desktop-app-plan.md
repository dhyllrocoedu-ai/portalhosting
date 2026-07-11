# PortalHost Desktop App Implementation Plan

## Recommended Tech Stack: Tauri + SvelteKit

### Project Structure
```
portals-host-desktop/
├── src-tauri/           # Rust backend
│   ├── Cargo.toml       # Dependencies
│   ├── src/
│   │   ├── main.rs      # App entry point
│   │   ├── server.rs    # Server management commands
│   │   └── config.rs    # Configuration handling
│   └── tauri.conf.json  # Build configuration
├── src/                 # SvelteKit frontend
│   ├── routes/
│   │   ├── +layout.svelte
│   │   ├── servers/
│   │   └── settings/
│   ├── lib/
│   │   ├── components/
│   │   └── stores/
│   └── static/
├── package.json
└── vite.config.js
```

### Rust Backend Implementation

#### Server Management (src-tauri/src/server.rs)
```rust
use tauri::command;
use std::process::Command;

#[command]
pub async fn start_server(port: String) -> Result<String, String> {
    // Execute server start command
    // Return status
}

#[command]
pub async fn stop_server() -> Result<String, String> {
    // Kill server process
}

#[command]
pub async fn read_log_lines(path: String, count: usize) -> Result<Vec<String>, String> {
    // Read last N lines from server log
}
```

### SvelteKit Frontend Architecture

#### State Management (lib/stores)
- `servers.ts` - Server list and status
- `config.ts` - Application settings
- `console.ts` - Real-time console output stream

#### Component Structure
- `ServerCard.svelte` - Individual server display
- `ConsoleOutput.svelte` - Live log streaming
- `ConfigEditor.svelte` - Properties file editor
- `FileBrowser.svelte` - World/plugin file management

### Database Schema (SQLite)

```sql
CREATE TABLE servers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    jar_path TEXT NOT NULL,
    port INTEGER DEFAULT 25565,
    max_players INTEGER DEFAULT 20,
    status TEXT DEFAULT 'stopped',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE server_properties (
    server_id INTEGER,
    key TEXT,
    value TEXT,
    FOREIGN KEY(server_id) REFERENCES servers(id)
);
```

### Key Technical Challenges

1. **Process Management**
   - Cross-platform process spawning
   - Graceful shutdown handling
   - PID file management

2. **Real-time Console**
   - WebSocket streaming from backend
   - Output buffering and parsing
   - ANSI color code handling

3. **File System Security**
   - Whitelist allowed directories
   - Validate file paths
   - Handle permission errors gracefully

4. **Cross-platform Compatibility**
   - Path separator handling (Windows vs Unix)
   - Process spawning differences
   - Notification APIs

### Development Timeline

**Phase 1 (Week 1-2): Foundation**
- Project setup with Tauri + SvelteKit
- Basic UI shell
- SQLite database integration
- Server CRUD operations

**Phase 2 (Week 3-4): Core Features**
- Server start/stop functionality
- Console output streaming
- File browser implementation
- Server properties editor

**Phase 3 (Week 5-6): Polish**
- Real-time status monitoring
- Settings/preferences UI
- Cross-platform testing
- Performance optimization

### Deployment & Distribution

1. **Build Targets**
   - Windows: NSIS installer
   - macOS: DMG package
   - Linux: AppImage/DEB/RPM

2. **Auto-update System**
   - Tauri updater plugin
   - GitHub releases integration
   - Delta updates for smaller downloads