package com.portalhost.app.ui.screens.create

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StepStorageCheck(
    availableBytes: Long,
    requiredBytes: Long,
    maxRam: Float,
    onCheck: () -> Unit
) {
    LaunchedEffect(Unit) { onCheck() }
    Column {
        Text("Storage Check", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Ensure enough space is available before creating the server", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        if (availableBytes == 0L) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Checking storage...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            val sufficient = availableBytes >= requiredBytes
            val availableFormatted = com.portalhost.app.storage.StorageInfo.formatBytes(availableBytes)
            val requiredFormatted = com.portalhost.app.storage.StorageInfo.formatBytes(requiredBytes)

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (sufficient) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (sufficient) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (sufficient) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (sufficient) "Sufficient storage" else "Insufficient storage",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (sufficient) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    StorageRow("Available", availableFormatted)
                    StorageRow("Required (JAR + world + swap)", requiredFormatted)
                }
            }

            if (!sufficient) {
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Free up space or reduce the RAM allocation. The server needs at least 500 MB plus the allocated RAM size.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("RAM: ${com.portalhost.app.storage.StorageInfo.formatBytes((maxRam * 1024 * 1024 * 1024).toLong())}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(16.dp))
                Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("JAR + overhead: 500 MB", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun StorageRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
