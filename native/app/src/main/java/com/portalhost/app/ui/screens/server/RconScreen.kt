package com.portalhost.app.ui.screens.server

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.app.server.RconClient
import kotlinx.coroutines.launch

@Composable
fun RconDialog(
    host: String,
    port: Int,
    password: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var connected by remember { mutableStateOf(false) }
    var connecting by remember { mutableStateOf(false) }
    var client by remember { mutableStateOf<RconClient?>(null) }
    var commandText by remember { mutableStateOf("") }
    val responses = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    val maxResponses = 500

    AlertDialog(
        onDismissRequest = {
            client?.disconnect()
            onDismiss()
        },
        title = { Text("RCON — $host:$port") },
        text = {
            Column(modifier = Modifier.height(400.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (connected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            if (connected) "Connected" else "Disconnected",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (!connected) {
                        Button(onClick = {
                            connecting = true
                            scope.launch {
                                val c = RconClient(host, port, password)
                                val result = c.connect()
                                if (result.isSuccess) {
                                    client = c
                                    connected = true
                                    responses.add("Connected to $host:$port")
                                    if (responses.size > 500) while (responses.size > 400) responses.removeAt(0)
                                } else {
                                    Toast.makeText(context, "RCON failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                }
                                connecting = false
                            }
                        }, enabled = !connecting) {
                            if (connecting) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            else Text("Connect")
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(responses.toList()) { line ->
                        Text(line, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                }

                if (connected) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = commandText,
                            onValueChange = { commandText = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("Command") },
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = {
                            val cmd = commandText.trim()
                                if (cmd.isNotBlank()) {
                                    responses.add("> $cmd")
                                    if (responses.size > 500) while (responses.size > 400) responses.removeAt(0)
                                    commandText = ""
                                    scope.launch {
                                        client?.command(cmd)?.onSuccess { result ->
                                            responses.add(result)
                                            if (responses.size > 500) while (responses.size > 400) responses.removeAt(0)
                                        }?.onFailure { e ->
                                            responses.add("Error: ${e.message}")
                                            if (responses.size > 500) while (responses.size > 400) responses.removeAt(0)
                                        }
                                    }
                            }
                        }) {
                            Icon(Icons.Default.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { client?.disconnect(); onDismiss() }) { Text("Close") }
        }
    )
}

