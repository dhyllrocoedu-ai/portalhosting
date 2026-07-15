package com.portalhost.desktop.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private val playerJson = Json { prettyPrint = true }

data class WhitelistEntry(val uuid: String, val name: String)
data class OpEntry(val uuid: String, val name: String, val level: Int = 4, val bypassesPlayerLimit: Boolean = false)
data class BannedPlayerEntry(val uuid: String, val name: String, val created: String? = null, val source: String? = null, val expires: String? = null, val reason: String? = null)
data class BannedIpEntry(val ip: String, val created: String? = null, val source: String? = null, val expires: String? = null, val reason: String? = null)

@Composable
fun PlayerManagementScreen(serverId: String, onBack: () -> Unit = {}) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Online", "Whitelist", "Operators", "Banned Players", "Banned IPs")

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Player Management",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
        )

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 12.sp) },
                )
            }
        }

        when (selectedTab) {
            0 -> OnlinePlayersTab(serverId)
            1 -> WhitelistTab(serverId)
            2 -> OperatorsTab(serverId)
            3 -> BannedPlayersTab(serverId)
            4 -> BannedIpsTab(serverId)
        }
    }
}

@Composable
private fun OnlinePlayersTab(serverId: String) {
    val serverDir = File("servers/$serverId")
    val usercacheFile = File(serverDir, "usercache.json")

    var players by remember { mutableStateOf<List<WhitelistEntry>>(emptyList()) }
    var newPlayerName by remember { mutableStateOf("") }
    var newPlayerUuid by remember { mutableStateOf("") }

    LaunchedEffect(usercacheFile) {
        if (usercacheFile.exists()) {
            val json = Json { ignoreUnknownKeys = true }
            val content = usercacheFile.readText()
            val list = json.decodeFromString<List<WhitelistEntry>>(content)
            players = list
        } else {
            players = emptyList()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Online Players (from usercache.json)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
        }

        Spacer(Modifier.height(12.dp))

        if (players.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material.Icon(androidx.compose.material.icons.Icons.Filled.PersonOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(8.dp))
                    Text("No players have joined yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn {
                items(players) { player ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.material.Icon(androidx.compose.material.icons.Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(player.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("UUID: ${player.uuid}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WhitelistTab(serverId: String) {
    val file = File("servers/$serverId/whitelist.json")
    var players by remember(file) { mutableStateOf<List<WhitelistEntry>>(emptyList()) }
    var newName by remember { mutableStateOf("") }
    var newUuid by remember { mutableStateOf("") }

    LaunchedEffect(file) {
        players = if (file.exists()) {
            val json = Json { ignoreUnknownKeys = true }
            json.decodeFromString<List<WhitelistEntry>>(file.readText())
        } else emptyList()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Add Player", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Player Name") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = newUuid,
                onValueChange = { newUuid = it },
                label = { Text("UUID (optional)") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newName.isNotBlank()) {
                        val uuid = newUuid.ifBlank { java.util.UUID.randomUUID().toString() }
                        val entry = WhitelistEntry(uuid, newName.trim())
                        players = players + entry
                        saveWhitelist(file, players)
                        newName = ""
                        newUuid = ""
                    }
                },
                enabled = newName.isNotBlank(),
            ) {
                androidx.compose.material.Icon(androidx.compose.material.icons.Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }
        }

        Spacer(Modifier.height(12.dp))

        if (players.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material.Icon(androidx.compose.material.icons.Icons.Filled.PersonOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(8.dp))
                    Text("Whitelist is empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn {
                items(players) { player ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.material.Icon(androidx.compose.material.icons.Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(player.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("UUID: ${player.uuid}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = {
                                players = players.filter { it.uuid != player.uuid }
                                saveWhitelist(file, players)
                            }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OperatorsTab(serverId: String) {
    val file = File("servers/$serverId/ops.json")
    var players by remember(file) { mutableStateOf<List<OpEntry>>(emptyList()) }
    var newName by remember { mutableStateOf("") }
    var newUuid by remember { mutableStateOf("") }
    var newLevel by remember { mutableStateOf(4) }

    LaunchedEffect(file) {
        players = if (file.exists()) {
            val json = Json { ignoreUnknownKeys = true }
            json.decodeFromString<List<OpEntry>>(file.readText())
        } else emptyList()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Add Operator", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Player Name") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = newUuid,
                onValueChange = { newUuid = it },
                label = { Text("UUID (optional)") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = newLevel.toString(),
                onValueChange = { newLevel = it.toIntOrNull() ?: 4 },
                label = { Text("Level") },
                singleLine = true,
                modifier = Modifier.width(80.dp),
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newName.isNotBlank()) {
                        val uuid = newUuid.ifBlank { java.util.UUID.randomUUID().toString() }
                        val entry = OpEntry(uuid, newName.trim(), newLevel)
                        players = players + entry
                        saveOps(file, players)
                        newName = ""
                        newUuid = ""
                    }
                },
                enabled = newName.isNotBlank(),
            ) {
                androidx.compose.material.Icon(androidx.compose.material.icons.Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }
        }

        Spacer(Modifier.height(12.dp))

        if (players.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material.Icon(androidx.compose.material.icons.Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(8.dp))
                    Text("No operators", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn {
                items(players) { player ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.material.Icon(androidx.compose.material.icons.Icons.Filled.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(player.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("UUID: ${player.uuid} | Level: ${player.level}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = {
                                players = players.filter { it.uuid != player.uuid }
                                saveOps(file, players)
                            }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BannedPlayersTab(serverId: String) {
    val file = File("servers/$serverId/banned-players.json")
    var players by remember(file) { mutableStateOf<List<BannedPlayerEntry>>(emptyList()) }
    var newName by remember { mutableStateOf("") }
    var newUuid by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    LaunchedEffect(file) {
        players = if (file.exists()) {
            val json = Json { ignoreUnknownKeys = true }
            json.decodeFromString<List<BannedPlayerEntry>>(file.readText())
        } else emptyList()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Ban Player", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Player Name") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = newUuid,
                onValueChange = { newUuid = it },
                label = { Text("UUID (optional)") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Reason") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newName.isNotBlank()) {
                        val uuid = newUuid.ifBlank { java.util.UUID.randomUUID().toString() }
                        val entry = BannedPlayerEntry(
                            uuid = uuid,
                            name = newName.trim(),
                            created = java.time.Instant.now().toString(),
                            source = "PortalHost",
                            expires = null,
                            reason = reason.ifBlank { "Banned by admin" }
                        )
                        players = players + entry
                        saveBannedPlayers(file, players)
                        newName = ""
                        newUuid = ""
                        reason = ""
                    }
                },
                enabled = newName.isNotBlank(),
            ) {
                androidx.compose.material.Icon(androidx.compose.material.icons.Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Ban")
            }
        }

        Spacer(Modifier.height(12.dp))

        if (players.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material.Icon(androidx.compose.material.icons.Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(8.dp))
                    Text("No banned players", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn {
                items(players) { player ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.material.Icon(androidx.compose.material.icons.Icons.Filled.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(player.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("UUID: ${player.uuid} | Reason: ${player.reason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = {
                                players = players.filter { it.uuid != player.uuid }
                                saveBannedPlayers(file, players)
                            }) {
                                Text("Unban", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BannedIpsTab(serverId: String) {
    val file = File("servers/$serverId/banned-ips.json")
    var entries by remember(file) { mutableStateOf<List<BannedIpEntry>>(emptyList()) }
    var newIp by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    LaunchedEffect(file) {
        entries = if (file.exists()) {
            val json = Json { ignoreUnknownKeys = true }
            json.decodeFromString<List<BannedIpEntry>>(file.readText())
        } else emptyList()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Ban IP", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = newIp,
                onValueChange = { newIp = it },
                label = { Text("IP Address") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Reason") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newIp.isNotBlank()) {
                        val entry = BannedIpEntry(
                            ip = newIp.trim(),
                            created = java.time.Instant.now().toString(),
                            source = "PortalHost",
                            expires = null,
                            reason = reason.ifBlank { "Banned by admin" }
                        )
                        entries = entries + entry
                        saveBannedIps(file, entries)
                        newIp = ""
                        reason = ""
                    }
                },
                enabled = newIp.isNotBlank(),
            ) {
                androidx.compose.material.Icon(androidx.compose.material.icons.Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Ban")
            }
        }

        Spacer(Modifier.height(12.dp))

        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material.Icon(androidx.compose.material.icons.Icons.Filled.GppBad, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(8.dp))
                    Text("No banned IPs", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn {
                items(entries) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.material.Icon(androidx.compose.material.icons.Icons.Filled.GppBad, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.ip, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("Reason: ${entry.reason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = {
                                entries = entries.filter { it.ip != entry.ip }
                                saveBannedIps(file, entries)
                            }) {
                                Text("Unban", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun saveWhitelist(file: File, players: List<WhitelistEntry>) {
    file.parentFile?.mkdirs()
    file.writeText(playerJson.encodeToString(players))
}

private fun saveOps(file: File, players: List<OpEntry>) {
    file.parentFile?.mkdirs()
    file.writeText(playerJson.encodeToString(players))
}

private fun saveBannedPlayers(file: File, players: List<BannedPlayerEntry>) {
    file.parentFile?.mkdirs()
    file.writeText(playerJson.encodeToString(players))
}

private fun saveBannedIps(file: File, entries: List<BannedIpEntry>) {
    file.parentFile?.mkdirs()
    file.writeText(playerJson.encodeToString(entries))
}