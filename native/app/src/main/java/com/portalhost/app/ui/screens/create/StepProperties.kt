package com.portalhost.app.ui.screens.create

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepProperties(
    port: String, gamemode: String, difficulty: String, motd: String,
    gamemodes: List<String>, difficulties: List<String>,
    onPortChange: (String) -> Unit, onGamemodeChange: (String) -> Unit,
    onDifficultyChange: (String) -> Unit, onMotdChange: (String) -> Unit,
    iconUri: Uri? = null, onIconChange: (Uri?) -> Unit = {}
) {
    val context = LocalContext.current
    var previewBitmap by remember(iconUri) {
        mutableStateOf(
            if (iconUri != null) {
                try {
                    context.contentResolver.openInputStream(iconUri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                } catch (e: Exception) { null }
            } else null
        )
    }

    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onIconChange(uri)
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    previewBitmap = BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (e: Exception) { previewBitmap = null }
        }
    }

    Column {
        Text("Server Properties", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Configure basic server settings", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        // Server Icon
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap!!,
                        contentDescription = "Server Icon",
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Server Icon", style = MaterialTheme.typography.titleSmall)
                    Text(if (previewBitmap != null) "Tap to change" else "Select a picture", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = { iconPickerLauncher.launch("image/*") }) {
                    Text(if (previewBitmap != null) "Change" else "Select")
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = port,
            onValueChange = { onPortChange(it.filter { c -> c.isDigit() }.take(5)) },
            label = { Text("Port") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = { Icon(Icons.Default.Lan, contentDescription = null) }
        )
        Spacer(Modifier.height(12.dp))
        Text("Gamemode", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            gamemodes.forEach { gm ->
                FilterChip(
                    selected = gamemode == gm,
                    onClick = { onGamemodeChange(gm) },
                    label = { Text(gm.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Difficulty", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            difficulties.forEach { diff ->
                FilterChip(
                    selected = difficulty == diff,
                    onClick = { onDifficultyChange(diff) },
                    label = { Text(diff.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = motd,
            onValueChange = onMotdChange,
            label = { Text("MOTD") },
            placeholder = { Text("A Minecraft Server") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null) }
        )
    }
}
