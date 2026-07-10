package com.portalhost.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhost.app.ui.screens.consoleLineColor

@Composable
fun ConsolePreview(
    consoleLines: List<String>,
    onOpenConsole: () -> Unit,
    onCommand: (String) -> Unit,
    onClearConsole: () -> Unit,
    isOnline: Boolean
) {
    var commandInput by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Console", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onClearConsole,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear console", modifier = Modifier.size(18.dp))
                        }
                        TextButton(onClick = onOpenConsole) {
                            Text("Open Console →")
                        }
                    }
                }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0D0D0D))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (consoleLines.isEmpty()) {
                    Text(
                        text = "Console output will appear here...",
                        color = Color(0xFF555555),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                } else {
                    LazyColumn {
                        items(consoleLines.takeLast(5)) { line ->
                            Text(
                                text = line,
                                color = consoleLineColor(line),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }

            if (isOnline) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter command...", fontSize = 13.sp) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (commandInput.isNotBlank()) {
                                onCommand(commandInput)
                                commandInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}
