package com.portalhost.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.app.server.ServerStatus
import com.portalhost.app.ui.components.MinecraftHeadIcon
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private val PLAYER_TABS = listOf("Online", "Whitelist", "Banned")

data class WhitelistEntry(val name: String, val uuid: String)
data class BannedPlayerEntry(val name: String, val uuid: String, val reason: String, val created: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen(
    serverDir: File?,
    onCommand: (String) -> Unit,
    isOnline: Boolean,
    currentPlayers: List<String>,
    status: ServerStatus,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Player Management") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 4.dp) {
                PLAYER_TABS.forEachIndexed { index, label ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(label, maxLines = 1, fontSize = 13.sp) })
                }
            }

            when (selectedTab) {
                0 -> OnlinePlayersTab(currentPlayers, isOnline, onCommand)
                1 -> WhitelistTab(serverDir)
                2 -> BannedPlayersTab(serverDir, onCommand)
            }
        }
    }
}

@Composable
private fun OnlinePlayersTab(players: List<String>, isOnline: Boolean, onCommand: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Text("Online Players (${players.size})", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
        }

        if (players.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text("No players online", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        items(players, key = { it }) { player ->
            PlayerActionCard(player = player, isOnline = isOnline, onCommand = onCommand)
        }
    }
}

@Composable
private fun PlayerActionCard(player: String, isOnline: Boolean, onCommand: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            MinecraftHeadIcon(player = player, size = 32.dp)
            Spacer(Modifier.width(12.dp))
            Text(player, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)

            if (isOnline) {
                SmallChip("Kick", Color(0xFFFF9800)) { onCommand("/kick $player") }
                Spacer(Modifier.width(4.dp))
                SmallChip("Ban", Color(0xFFF44336)) { onCommand("/ban $player") }
                Spacer(Modifier.width(4.dp))
                SmallChip("OP", Color(0xFF4CAF50)) { onCommand("/op $player") }
            }
        }
    }
}

@Composable
private fun SmallChip(label: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.15f),
        contentColor = color
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
    }
}

@Composable
private fun WhitelistTab(serverDir: File?) {
    val whitelistFile = if (serverDir != null) File(serverDir, "whitelist.json") else null
    var entries by remember(whitelistFile) { mutableStateOf(readWhitelist(whitelistFile)) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newPlayerName by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Whitelist (${entries.size})", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                SmallFloatingActionButton(onClick = { showAddDialog = true; newPlayerName = "" }) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add", modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (entries.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text("No whitelisted players. Add players to control who can join.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        items(entries, key = { it.uuid }) { entry ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    MinecraftHeadIcon(player = entry.name, size = 28.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(entry.uuid, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = {
                        removeFromWhitelist(whitelistFile, entry.name)
                        entries = readWhitelist(whitelistFile)
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add to Whitelist") },
            text = {
                OutlinedTextField(
                    value = newPlayerName,
                    onValueChange = { newPlayerName = it },
                    label = { Text("Player name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPlayerName.isNotBlank()) {
                        val uuid = generateOfflineUuid(newPlayerName)
                        addToWhitelist(whitelistFile, newPlayerName, uuid)
                        entries = readWhitelist(whitelistFile)
                    }
                    showAddDialog = false
                    newPlayerName = ""
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun BannedPlayersTab(serverDir: File?, onCommand: (String) -> Unit) {
    val bannedFile = if (serverDir != null) File(serverDir, "banned-players.json") else null
    var entries by remember(bannedFile) { mutableStateOf(readBannedPlayers(bannedFile)) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Text("Banned Players (${entries.size})", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
        }

        if (entries.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text("No banned players.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        items(entries, key = { it.uuid }) { entry ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    MinecraftHeadIcon(player = entry.name, size = 28.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (entry.reason.isNotBlank()) {
                            Text("Reason: ${entry.reason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    TextButton(onClick = {
                        onCommand("/pardon ${entry.name}")
                        removeFromBanned(bannedFile, entry.name)
                        entries = readBannedPlayers(bannedFile)
                    }) { Text("Pardon", fontSize = 12.sp) }
                }
            }
        }
    }
}

// ─── File helpers ───

private fun readWhitelist(file: File?): List<WhitelistEntry> {
    if (file == null || !file.exists()) return emptyList()
    return try {
        val text = file.readText().trim()
        if (text.isEmpty() || text == "[]") return emptyList()
        val arr = JSONArray(text)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            WhitelistEntry(obj.optString("name", ""), obj.optString("uuid", ""))
        }
    } catch (e: Exception) { emptyList() }
}

private fun addToWhitelist(file: File?, name: String, uuid: String) {
    if (file == null) return
    val entries = readWhitelist(file).toMutableList()
    entries.add(WhitelistEntry(name, uuid))
    val arr = JSONArray()
    entries.forEach { arr.put(JSONObject().apply { put("name", it.name); put("uuid", it.uuid) }) }
    file.parentFile?.mkdirs()
    file.writeText(arr.toString(2))
}

private fun removeFromWhitelist(file: File?, name: String) {
    if (file == null) return
    val entries = readWhitelist(file).filter { !it.name.equals(name, ignoreCase = true) }
    val arr = JSONArray()
    entries.forEach { arr.put(JSONObject().apply { put("name", it.name); put("uuid", it.uuid) }) }
    file.parentFile?.mkdirs()
    file.writeText(arr.toString(2))
}

private fun readBannedPlayers(file: File?): List<BannedPlayerEntry> {
    if (file == null || !file.exists()) return emptyList()
    return try {
        val text = file.readText().trim()
        if (text.isEmpty() || text == "[]") return emptyList()
        val arr = JSONArray(text)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            BannedPlayerEntry(
                obj.optString("name", ""),
                obj.optString("uuid", ""),
                obj.optString("reason", ""),
                obj.optString("created", "")
            )
        }
    } catch (e: Exception) { emptyList() }
}

private fun removeFromBanned(file: File?, name: String) {
    if (file == null) return
    val entries = readBannedPlayers(file).filter { !it.name.equals(name, ignoreCase = true) }
    val arr = JSONArray()
    entries.forEach { arr.put(JSONObject().apply { put("name", it.name); put("uuid", it.uuid); put("reason", it.reason); put("created", it.created) }) }
    file.parentFile?.mkdirs()
    file.writeText(arr.toString(2))
}

private fun generateOfflineUuid(name: String): String {
    val digest = java.security.MessageDigest.getInstance("MD5")
    val bytes = digest.digest(("OfflinePlayer:$name".toByteArray()))
    bytes[6] = (bytes[6].toInt() and 0x0f or 0x30).toByte()
    bytes[8] = (bytes[8].toInt() and 0x3f or 0x80).toByte()
    return "%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x".format(
        *bytes.map { it.toInt() and 0xFF }.toTypedArray()
    )
}
