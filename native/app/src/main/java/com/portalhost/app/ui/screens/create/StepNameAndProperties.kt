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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.portalhost.app.ui.screens.MotdEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepNameAndProperties(
    name: String,
    onNameChange: (String) -> Unit,
    port: String,
    gamemode: String,
    difficulty: String,
    motd: String,
    gamemodes: List<String>,
    difficulties: List<String>,
    onPortChange: (String) -> Unit,
    onGamemodeChange: (String) -> Unit,
    onDifficultyChange: (String) -> Unit,
    onMotdChange: (String) -> Unit,
    iconUri: Uri? = null,
    onIconChange: (Uri?) -> Unit = {}
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
        Text("Server Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Name your server and configure its basic properties",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Server Name") },
            placeholder = { Text("My Survival Server") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
        )

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = port,
            onValueChange = { onPortChange(it.filter { c -> c.isDigit() }.take(5)) },
            label = { Text("Port") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = { Icon(Icons.Default.Lan, contentDescription = null) }
        )

        Spacer(Modifier.height(16.dp))
        Text("Gamemode", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        FlowRowChips(
            options = gamemodes,
            selected = gamemode,
            onSelect = onGamemodeChange
        )

        Spacer(Modifier.height(16.dp))
        Text("Difficulty", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        FlowRowChips(
            options = difficulties,
            selected = difficulty,
            onSelect = onDifficultyChange
        )

        Spacer(Modifier.height(16.dp))
        MotdEditor(motd = motd, onMotdChange = onMotdChange)

        Spacer(Modifier.height(16.dp))

        // Server icon picker
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
                    Text(
                        if (previewBitmap != null) "Tap to change" else "Select a picture",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = { iconPickerLauncher.launch("image/*") }) {
                    Text(if (previewBitmap != null) "Change" else "Select")
                }
            }
            Text(
                "Recommended: 64x64 PNG",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowChips(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { opt ->
            FilterChip(
                selected = selected == opt,
                onClick = { onSelect(opt) },
                label = {
                    Text(opt.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall)
                }
            )
        }
    }
}
