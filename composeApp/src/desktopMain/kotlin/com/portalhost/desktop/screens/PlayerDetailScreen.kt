package com.portalhost.desktop.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.filesystem.defaultDataDir
import com.portalhost.player.BannedPlayerEntry
import com.portalhost.player.MojangSkinService
import com.portalhost.player.NameChange
import com.portalhost.player.OpEntry
import com.portalhost.player.PlayerProfile
import com.portalhost.player.PlayerProfileRepository
import com.portalhost.player.PlayerStatus
import com.portalhost.player.PlayerStatusResolver
import com.portalhost.player.SkinRenderCache
import com.portalhost.player.WhitelistEntry
import com.portalhost.server.ServerManager
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val playerJson = Json { prettyPrint = true; ignoreUnknownKeys = true }

@Composable
fun PlayerDetailScreen(serverId: String, uuid: String, onBack: () -> Unit) {
    val serverManager = koinInject<ServerManager>()
    val profileRepo = remember { PlayerProfileRepository(serverManager) }
    val statusResolver = remember { PlayerStatusResolver() }
    val skinService = remember { MojangSkinService() }
    val skinCache = remember {
        SkinRenderCache(File(defaultDataDir(), "skins"))
    }
    val snackbarHost = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val serverDir = serverManager.getServerDir(serverId)

    var profile by remember { mutableStateOf<PlayerProfile?>(null) }
    var status by remember { mutableStateOf<PlayerStatus?>(null) }
    var skinBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showReasonDialog by remember { mutableStateOf<String?>(null) }
    var onlineNow by remember { mutableStateOf(false) }

    val serverStates by serverManager.serverStates.collectAsState()
    val currentState = serverStates[serverId]
    onlineNow = currentState?.players?.any { it.equals(profile?.currentName, ignoreCase = true) } == true

    suspend fun refresh() {
        loading = true
        val p = profileRepo.findByUuid(serverDir, uuid, serverId)
        profile = p
        if (p != null) {
            status = statusResolver.resolve(serverDir, p.uuid)
            if (p.skinUrl == null) {
                val url = skinService.fetchSkinUrl(p.uuid)
                if (url != null) {
                    profile = p.copy(skinUrl = url, skinUrlCachedAt = skinService.now())
                }
            }
            val cached = profile?.skinUrl
            if (cached != null) {
                skinBitmap = skinCache.load(p.uuid, cached)
            }
        }
        loading = false
    }

    LaunchedEffect(uuid) { refresh() }
    DisposableEffect(Unit) {
        onDispose { skinService.close() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlayerDetailTopBar(profile = profile, serverName = serverDir.name, onBack = onBack)
            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (profile == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Player not found", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "usercache.json does not contain this UUID",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                PlayerDetailBody(
                    profile = profile!!,
                    status = status,
                    onlineNow = onlineNow,
                    skinBitmap = skinBitmap,
                    onCopyUuid = {
                        clipboard.setText(AnnotatedString(profile!!.uuid))
                        scope.launch { snackbarHost.showSnackbar("UUID copied") }
                    },
                    onCopyName = {
                        clipboard.setText(AnnotatedString(profile!!.currentName))
                        scope.launch { snackbarHost.showSnackbar("Name copied") }
                    },
                    onAction = { cmd -> runServerCommand(serverManager, serverId, cmd) },
                    onRequestBan = { showReasonDialog = "ban" },
                    onRequestKick = { showReasonDialog = "kick" },
                    onWhitelistToggle = {
                        val p = profile!!
                        val current = status?.isWhitelisted == true
                        toggleWhitelist(serverDir, p, !current)
                        status = statusResolver.resolve(serverDir, p.uuid)
                        scope.launch {
                            snackbarHost.showSnackbar(
                                if (!current) "Added to whitelist" else "Removed from whitelist"
                            )
                        }
                    },
                    onOpToggle = {
                        val p = profile!!
                        val current = status?.isOp == true
                        toggleOp(serverDir, p, !current, 4)
                        status = statusResolver.resolve(serverDir, p.uuid)
                        scope.launch {
                            snackbarHost.showSnackbar(
                                if (!current) "Granted operator" else "Revoked operator"
                            )
                        }
                    },
                    onUnban = {
                        val p = profile!!
                        unbanPlayer(serverDir, p.uuid)
                        status = statusResolver.resolve(serverDir, p.uuid)
                        scope.launch { snackbarHost.showSnackbar("Unbanned") }
                    },
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }

    showReasonDialog?.let { mode ->
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReasonDialog = null; reason = "" },
            title = { Text(if (mode == "ban") "Ban ${profile?.currentName ?: "player"}" else "Kick ${profile?.currentName ?: "player"}") },
            text = {
                Column {
                    Text(
                        if (mode == "ban")
                            "The ban will be added to banned-players.json and applied on next login."
                        else
                            "Kick the player immediately. They can rejoin unless banned.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val finalReason = reason.ifBlank { "Banned by admin" }
                    val p = profile ?: return@TextButton
                    when (mode) {
                        "ban" -> {
                            banPlayer(serverDir, p, finalReason)
                            status = statusResolver.resolve(serverDir, p.uuid)
                            runServerCommand(serverManager, serverId, "kick ${p.currentName} Banned: $finalReason")
                            scope.launch { snackbarHost.showSnackbar("Banned") }
                        }
                        "kick" -> {
                            val cmd = if (reason.isBlank()) "kick ${p.currentName}"
                            else "kick ${p.currentName} ${reason}"
                            runServerCommand(serverManager, serverId, cmd)
                            scope.launch { snackbarHost.showSnackbar("Kicked") }
                        }
                    }
                    showReasonDialog = null
                    reason = ""
                }) {
                    Text(if (mode == "ban") "Ban" else "Kick")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReasonDialog = null; reason = "" }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun PlayerDetailTopBar(profile: PlayerProfile?, serverName: String, onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    profile?.currentName ?: "Player",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(serverName, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PlayerDetailBody(
    profile: PlayerProfile,
    status: PlayerStatus?,
    onlineNow: Boolean,
    skinBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    onCopyUuid: () -> Unit,
    onCopyName: () -> Unit,
    onAction: (String) -> Unit,
    onRequestBan: () -> Unit,
    onRequestKick: () -> Unit,
    onWhitelistToggle: () -> Unit,
    onOpToggle: () -> Unit,
    onUnban: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (skinBitmap != null) {
                Image(
                    bitmap = skinBitmap,
                    contentDescription = profile.currentName,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                )
            } else {
                MinecraftHeadIcon(player = profile.currentName, size = 64.dp)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(profile.currentName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    "UUID: ${profile.uuid}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        IdentitySection(profile = profile, onCopyUuid = onCopyUuid, onCopyName = onCopyName)

        Spacer(Modifier.height(16.dp))
        StatusSection(status = status, onlineNow = onlineNow)

        Spacer(Modifier.height(16.dp))
        ActionsSection(
            status = status,
            onRequestKick = onRequestKick,
            onRequestBan = onRequestBan,
            onWhitelistToggle = onWhitelistToggle,
            onOpToggle = onOpToggle,
            onUnban = onUnban,
            onAction = onAction,
        )

        Spacer(Modifier.height(16.dp))
        NameHistorySection(profile = profile)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun IdentitySection(profile: PlayerProfile, onCopyUuid: () -> Unit, onCopyName: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle("Identity")
            Spacer(Modifier.height(8.dp))
            KeyValueRow("UUID", profile.uuid, copyable = true, onCopy = onCopyUuid)
            Spacer(Modifier.height(4.dp))
            KeyValueRow("Name", profile.currentName, copyable = true, onCopy = onCopyName)
            Spacer(Modifier.height(4.dp))
            KeyValueRow(
                "First seen",
                profile.firstSeen?.let { formatTimestamp(it) } ?: "unknown",
            )
            Spacer(Modifier.height(4.dp))
            KeyValueRow(
                "Last seen",
                profile.lastSeen?.let { formatTimestamp(it) } ?: "unknown",
            )
        }
    }
}

@Composable
private fun StatusSection(status: PlayerStatus?, onlineNow: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle("Status")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(label = if (onlineNow) "Online" else "Offline", color = if (onlineNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                if (status?.isWhitelisted == true) {
                    StatusChip(label = "Whitelisted", color = MaterialTheme.colorScheme.tertiary)
                }
                if (status?.isOp == true) {
                    StatusChip(label = "Operator (${status.opLevel})", color = MaterialTheme.colorScheme.secondary)
                }
                if (status?.isBanned == true) {
                    StatusChip(label = "Banned", color = MaterialTheme.colorScheme.error)
                }
                if (status == null || (status.isWhitelisted.not() && status.isOp.not() && status.isBanned.not())) {
                    StatusChip(label = "No special status", color = MaterialTheme.colorScheme.outline)
                }
            }
            if (status?.isBanned == true && !status.banReason.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("Ban reason: ${status.banReason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (status?.banExpires != null) {
                Text("Ban expires: ${formatTimestamp(status.banExpires!!)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ActionsSection(
    status: PlayerStatus?,
    onRequestKick: () -> Unit,
    onRequestBan: () -> Unit,
    onWhitelistToggle: () -> Unit,
    onOpToggle: () -> Unit,
    onUnban: () -> Unit,
    onAction: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle("Quick actions")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ActionPill(
                    label = "Kick",
                    icon = Icons.Default.Block,
                    container = MaterialTheme.colorScheme.errorContainer,
                    content = MaterialTheme.colorScheme.onErrorContainer,
                    onClick = onRequestKick,
                )
                ActionPill(
                    label = if (status?.isBanned == true) "Unban" else "Ban",
                    icon = if (status?.isBanned == true) Icons.Default.Check else Icons.Default.GppBad,
                    container = if (status?.isBanned == true) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
                    content = if (status?.isBanned == true) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    onClick = if (status?.isBanned == true) onUnban else onRequestBan,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ActionPill(
                    label = if (status?.isOp == true) "De-OP" else "OP",
                    icon = Icons.Default.Shield,
                    container = if (status?.isOp == true) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                    content = if (status?.isOp == true) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onOpToggle,
                )
                ActionPill(
                    label = if (status?.isWhitelisted == true) "Remove WL" else "Add WL",
                    icon = if (status?.isWhitelisted == true) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                    container = if (status?.isWhitelisted == true) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.tertiaryContainer,
                    content = if (status?.isWhitelisted == true) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = onWhitelistToggle,
                )
            }
        }
    }
}

@Composable
private fun NameHistorySection(profile: PlayerProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle("Name history")
            Spacer(Modifier.height(8.dp))
            if (profile.nameHistory.isEmpty()) {
                Text("No name changes recorded", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                profile.nameHistory.forEachIndexed { index, change ->
                    NameHistoryRow(change = change, isCurrent = index == 0)
                    if (index < profile.nameHistory.lastIndex) HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun NameHistoryRow(change: NameChange, isCurrent: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            change.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (isCurrent) {
            Text("current", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun KeyValueRow(label: String, value: String, copyable: Boolean = false, onCopy: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
        if (copyable && onCopy != null) {
            IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy $label", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, color: Color) {
    AssistChip(
        onClick = {},
        label = { Text(label, fontSize = 12.sp) },
        colors = AssistChipDefaults.assistChipColors(containerColor = color.copy(alpha = 0.15f), labelColor = color),
    )
}

@Composable
private fun ActionPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    container: Color,
    content: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(36.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        shape = RoundedCornerShape(10.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 13.sp)
    }
}

private fun runServerCommand(serverManager: ServerManager, serverId: String, command: String) {
    val process = serverManager.getProcessForServer(serverId)
    if (process != null) {
        try {
            process.outputStream.bufferedWriter().use { it.write("$command\n"); it.flush() }
        } catch (_: Exception) {
        }
    }
}

private fun toggleWhitelist(serverDir: File, profile: PlayerProfile, add: Boolean) {
    val file = File(serverDir, "whitelist.json")
    val list: MutableList<WhitelistEntry> = if (file.exists()) {
        runCatching { playerJson.decodeFromString<List<WhitelistEntry>>(file.readText()).toMutableList() }
            .getOrElse { mutableListOf() }
    } else mutableListOf()
    list.removeAll { it.uuid.replace("-", "").equals(profile.uuid.replace("-", ""), ignoreCase = true) }
    if (add) list.add(WhitelistEntry(uuid = profile.uuid, name = profile.currentName))
    file.parentFile?.mkdirs()
    file.writeText(playerJson.encodeToString(list))
}

private fun toggleOp(serverDir: File, profile: PlayerProfile, add: Boolean, level: Int) {
    val file = File(serverDir, "ops.json")
    val list: MutableList<OpEntry> = if (file.exists()) {
        runCatching { playerJson.decodeFromString<List<OpEntry>>(file.readText()).toMutableList() }
            .getOrElse { mutableListOf() }
    } else mutableListOf()
    list.removeAll { it.uuid.replace("-", "").equals(profile.uuid.replace("-", ""), ignoreCase = true) }
    if (add) list.add(OpEntry(uuid = profile.uuid, name = profile.currentName, level = level))
    file.parentFile?.mkdirs()
    file.writeText(playerJson.encodeToString(list))
}

private fun banPlayer(serverDir: File, profile: PlayerProfile, reason: String) {
    val file = File(serverDir, "banned-players.json")
    val list: MutableList<BannedPlayerEntry> = if (file.exists()) {
        runCatching { playerJson.decodeFromString<List<BannedPlayerEntry>>(file.readText()).toMutableList() }
            .getOrElse { mutableListOf() }
    } else mutableListOf()
    list.removeAll { it.uuid.replace("-", "").equals(profile.uuid.replace("-", ""), ignoreCase = true) }
    list.add(
        BannedPlayerEntry(
            uuid = profile.uuid,
            name = profile.currentName,
            created = java.time.Instant.now().toString(),
            source = "PortalHost",
            expires = null,
            reason = reason,
        )
    )
    file.parentFile?.mkdirs()
    file.writeText(playerJson.encodeToString(list))
}

private fun unbanPlayer(serverDir: File, uuid: String) {
    val file = File(serverDir, "banned-players.json")
    if (!file.exists()) return
    val normalized = uuid.replace("-", "").lowercase()
    val list: MutableList<BannedPlayerEntry> = runCatching {
        playerJson.decodeFromString<List<BannedPlayerEntry>>(file.readText()).toMutableList()
    }.getOrElse { mutableListOf() }
    list.removeAll { it.uuid.replace("-", "").lowercase() == normalized }
    file.writeText(playerJson.encodeToString(list))
}

private fun formatTimestamp(millis: Long): String =
    SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date(millis))
