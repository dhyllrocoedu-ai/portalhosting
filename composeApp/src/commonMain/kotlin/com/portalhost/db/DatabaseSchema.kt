package com.portalhost.db

object PortalHostDatabase {
    const val CURRENT_VERSION = 1

    val schema = """
        CREATE TABLE IF NOT EXISTS schema_version (
            version INTEGER PRIMARY KEY NOT NULL
        );

        CREATE TABLE IF NOT EXISTS servers (
            id TEXT PRIMARY KEY NOT NULL,
            config_json TEXT NOT NULL,
            state_json TEXT,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL
        );

        CREATE TABLE IF NOT EXISTS backups (
            id TEXT PRIMARY KEY NOT NULL,
            server_id TEXT NOT NULL REFERENCES servers(id),
            path TEXT NOT NULL,
            size INTEGER NOT NULL,
            type TEXT NOT NULL,
            created_at INTEGER NOT NULL
        );

        CREATE TABLE IF NOT EXISTS console_logs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            server_id TEXT NOT NULL REFERENCES servers(id),
            timestamp INTEGER NOT NULL,
            level TEXT NOT NULL,
            message TEXT NOT NULL
        );

        CREATE INDEX IF NOT EXISTS idx_console_logs_server_id ON console_logs(server_id);
        CREATE INDEX IF NOT EXISTS idx_console_logs_timestamp ON console_logs(timestamp);
        CREATE INDEX IF NOT EXISTS idx_backups_server_id ON backups(server_id);
    """

    val migrations = mapOf<Int, List<String>>(
        1 to listOf(
            "CREATE TABLE IF NOT EXISTS schema_version (version INTEGER PRIMARY KEY NOT NULL)"
        )
    )
}
