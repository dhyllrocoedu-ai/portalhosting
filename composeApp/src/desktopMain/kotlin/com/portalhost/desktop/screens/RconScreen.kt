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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.model.ServerConfig
import com.portalhost.server.RconClient
import com.portalhost.server.ServerManager
import com.portalhost.theme.ThemeColors
import com.portalhost.desktop.util.rememberResourcePainter
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

data class RconMessage(
    val text: String,
    val isSent: Boolean,
    val isError: Boolean = false,
)

@Composable
fun RconScreen(serverId: String, onBack: () -> Unit = {}) {
    val serverManager = koinInject<ServerManager>()
    val servers by serverManager.servers.collectAsState()
    val serverStates by serverManager.serverStates.collectAsState()
    val config = servers[serverId]
    val state = serverStates[serverId]
    val scope = rememberCoroutineScope()
    val sendIcon = rememberResourcePainter("/icons/Arrow_Right_Curved_Highlighted.png")
    val messages = remember { mutableStateListOf<RconMessage>() }
    var commandInput by remember { mutableStateOf("") }
    var isConnected by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var client by remember { mutableStateOf<RconClient?>(null) }
    val scrollState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(messages.size - 1)
        }
    }

    val connectionStatusColor = when {
        isConnecting -> ThemeColors.RconColors.Connecting
        isConnected -> ThemeColors.RconColors.Connected
        else -> ThemeColors.RconColors.Disconnected
    }
    val connectionStatusText = when {
        isConnecting -> "Connecting..."
        isConnected -> "Connected"
        else -> "Disconnected"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("RCON Client", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (config != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${config.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(8.dp))
                            // Connection status indicator
                            Box(
                                modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "\u25CF",
                                    color = connectionStatusColor,
                                    fontSize = 12.sp,
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(":$connectionStatusText", style = MaterialTheme.typography.bodySmall, color = connectionStatusColor)
                        }
                    }
                }
                Button(
                    onClick = {
                        if (isConnected) {
                            client?.disconnect()
                            isConnected = false
                            client = null
                            messages.add(RconMessage("Disconnected from RCON", isSent = false))
                        } else if (config != null && config.rconPassword != null) {
                            isConnecting = true
                            scope.launch {
                                val c = RconClient("localhost", config.rconPort, config.rconPassword)
                                val result = c.connect()
                                result.onSuccess {
                                    client = c
                                    isConnected = true
                                    isConnecting = false
                                    messages.add(RconMessage("Connected to RCON on port ${config.rconPort}", isSent = false))
                                }
                                result.onFailure {
                                    isConnecting = false
                                    messages.add(RconMessage("Connection failed: ${it.message}", isSent = false, isError = true))
                                }
                            }
                        }
                    },
                    enabled = !isConnecting && config?.rconPassword != null,
                ) {
                    Text(if (isConnecting) "Connecting..." else if (isConnected) "Disconnect" else "Connect")
                }
            }
        }

        if (config?.rconPassword == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Error, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(8.dp))
                    Text("RCON is not enabled for this server", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("Enable RCON in Server Details > Properties tab", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (!isConnected) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.WifiOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(8.dp))
                    Text("Click Connect to establish RCON connection", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("Host: localhost  Port: ${config?.rconPort ?: 25575}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                color = Color(0xFF1E1E1E),
            ) {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(messages) { msg ->
                        Text(
                            msg.text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
color = when {
                            msg.isError -> ThemeColors.RconColors.Error
                            msg.isSent -> ThemeColors.RconColors.SentMessage
                            else -> ThemeColors.RconColors.Received
                        },
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF252526),
            ) {
                Row(
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter command...", color = Color(0xFF888888), fontSize = 13.sp) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = Color(0xFFD4D4D4),
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                val cmd = commandInput.trim()
                                if (cmd.isNotBlank()) {
                                    messages.add(RconMessage("> $cmd", isSent = true))
                                    scope.launch {
                                        client?.command(cmd)?.fold(
                                            onSuccess = { response -> messages.add(RconMessage(response, isSent = false)) },
                                            onFailure = { e -> messages.add(RconMessage("Error: ${e.message}", isSent = false, isError = true)) },
                                        )
                                    }
                                    commandInput = ""
                                }
                            }
                        ),
                        enabled = isConnected,
                    )
                    IconButton(
                        onClick = {
                            val cmd = commandInput.trim()
                            if (cmd.isNotBlank()) {
                                messages.add(RconMessage("> $cmd", isSent = true))
                                scope.launch {
                                    client?.command(cmd)?.fold(
                                        onSuccess = { response -> messages.add(RconMessage(response, isSent = false)) },
                                        onFailure = { e -> messages.add(RconMessage("Error: ${e.message}", isSent = false, isError = true)) },
                                    )
                                }
                                commandInput = ""
                            }
                        },
                        enabled = isConnected && commandInput.isNotBlank(),
                    ) {
                        Icon(painter = sendIcon, contentDescription = "Send", tint = Color.Unspecified)
                    }
                }
            }
        }
    }
}