package com.portalhost.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MC_COLORS = listOf(
    '0' to Color(0xFF000000) to "Black",
    '1' to Color(0xFF0000AA) to "Dark Blue",
    '2' to Color(0xFF00AA00) to "Dark Green",
    '3' to Color(0xFF00AAAA) to "Dark Aqua",
    '4' to Color(0xFFAA0000) to "Dark Red",
    '5' to Color(0xFFAA00AA) to "Dark Purple",
    '6' to Color(0xFFFFAA00) to "Gold",
    '7' to Color(0xFFAAAAAA) to "Gray",
    '8' to Color(0xFF555555) to "Dark Gray",
    '9' to Color(0xFF5555FF) to "Blue",
    'a' to Color(0xFF55FF55) to "Green",
    'b' to Color(0xFF55FFFF) to "Aqua",
    'c' to Color(0xFFFF5555) to "Red",
    'd' to Color(0xFFFF55FF) to "Light Purple",
    'e' to Color(0xFFFFFF55) to "Yellow",
    'f' to Color(0xFFFFFFFF) to "White"
)

private data class FormatCodeDef(val label: String, val code: String, val icon: @Composable () -> Unit = {})

private val FORMAT_CODES = listOf(
    FormatCodeDef("Bold", "§l"),
    FormatCodeDef("Italic", "§o"),
    FormatCodeDef("Underline", "§n"),
    FormatCodeDef("Strike", "§m"),
    FormatCodeDef("Obfuscated", "§k"),
    FormatCodeDef("Reset", "§r")
)

private val MOTD_PRESETS = listOf(
    "A Minecraft Server",
    "§aWelcome §6to §cthe §bserver§r!",
    "§c❤ §6Minecraft §aServer §c❤",
    "§l§nSERVER NAME§r §7| §e1.20 §7| §aOnline!",
    "§b✸ §fNew §aSurvival §b✸ §7join.us"
)

@Composable
fun MotdEditor(
    motd: String,
    onMotdChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showColors by remember { mutableStateOf(false) }
    var showPresets by remember { mutableStateOf(false) }

    var tfv by remember { mutableStateOf(TextFieldValue(motd, TextRange(motd.length))) }

    LaunchedEffect(motd) {
        if (motd != tfv.text) {
            val clamped = tfv.selection.start.coerceAtMost(motd.length)
            tfv = TextFieldValue(motd, TextRange(clamped))
        }
    }

    Column(modifier = modifier) {
        // MOTD Input
        OutlinedTextField(
            value = tfv,
            onValueChange = { newVal ->
                tfv = newVal
                onMotdChange(newVal.text)
            },
            label = { Text("MOTD") },
            placeholder = { Text("A Minecraft Server") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null) },
            supportingText = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Use § color codes (e.g. §cRed §aGreen)")
                    Text("${motd.length}/256", style = MaterialTheme.typography.labelSmall, color = if (motd.length > 256) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            isError = motd.length > 256
        )

        Spacer(Modifier.height(8.dp))

        // Toolbar
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { showColors = !showColors }) {
                Icon(
                    if (showColors) Icons.Default.ExpandLess else Icons.Default.Palette,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(if (showColors) "Hide Codes" else "Color Codes", style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = { showPresets = !showPresets }) {
                Icon(Icons.Default.FormatListBulleted, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Presets", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.weight(1f))
            if (motd.isNotEmpty()) {
                TextButton(onClick = { onMotdChange(""); tfv = TextFieldValue("") }) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("Clear", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Presets section
        if (showPresets) {
            Spacer(Modifier.height(4.dp))
            Text("Presets", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MOTD_PRESETS.forEach { preset ->
                    SuggestionChip(
                        onClick = {
                            val parsed = preset.replace("§", "\u00A7")
                            tfv = TextFieldValue(parsed, TextRange(parsed.length))
                            onMotdChange(parsed)
                            showPresets = false
                        },
                        label = {
                            Text(
                                text = preset.replace("§", "").take(20),
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        },
                        shape = MaterialTheme.shapes.small
                    )
                }
            }
        }

        // Color codes section
        if (showColors) {
            val insertAtCursor: (String) -> Unit = { code ->
                val pos = tfv.selection.start
                val newText = tfv.text.substring(0, pos) + code + tfv.text.substring(pos)
                val newPos = pos + code.length
                val newTfv = TextFieldValue(newText, TextRange(newPos))
                tfv = newTfv
                onMotdChange(newText)
            }

            Spacer(Modifier.height(4.dp))
            Text("Colors", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MC_COLORS.forEach { ((code, color), label) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val borderColor = if (code == 'f') Color(0xFF888888) else Color.Transparent
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(0.5.dp, borderColor, CircleShape)
                                .clickable { insertAtCursor("§$code") },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                code.toString(),
                                fontSize = 10.sp,
                                color = if (code in listOf('0', '8', '4')) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            label.take(6),
                            fontSize = 7.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Formatting", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FORMAT_CODES.forEach { fmt ->
                    SuggestionChip(
                        onClick = { insertAtCursor(fmt.code) },
                        label = { Text(fmt.label, fontSize = 11.sp) },
                        shape = MaterialTheme.shapes.small
                    )
                }
                SuggestionChip(
                    onClick = { insertAtCursor("§r") },
                    label = { Text("Reset", fontSize = 11.sp, color = MaterialTheme.colorScheme.error) },
                    shape = MaterialTheme.shapes.small
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Preview
        Text("Preview", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B2B))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                val parsed = remember(motd) { parseMotd(motd) }
                if (parsed.isEmpty()) {
                    Text(
                        text = "MOTD preview will appear here",
                        color = Color(0xFFAAAAAA),
                        fontSize = 14.sp
                    )
                } else {
                    Text(
                        text = parsed,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // Minecraft color reference
        if (!showColors) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Tip: Colors use § codes (e.g. §c = red). Tap \"Color Codes\" to insert them.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

fun parseMotd(motd: String): AnnotatedString {
    val colorMap = mapOf(
        '0' to Color(0xFF000000),
        '1' to Color(0xFF0000AA),
        '2' to Color(0xFF00AA00),
        '3' to Color(0xFF00AAAA),
        '4' to Color(0xFFAA0000),
        '5' to Color(0xFFAA00AA),
        '6' to Color(0xFFFFAA00),
        '7' to Color(0xFFAAAAAA),
        '8' to Color(0xFF555555),
        '9' to Color(0xFF5555FF),
        'a' to Color(0xFF55FF55),
        'b' to Color(0xFF55FFFF),
        'c' to Color(0xFFFF5555),
        'd' to Color(0xFFFF55FF),
        'e' to Color(0xFFFFFF55),
        'f' to Color(0xFFFFFFFF)
    )

    return buildAnnotatedString {
        var i = 0
        var currentColor: Color? = null
        var bold = false
        var italic = false
        var strikethrough = false
        var underline = false

        while (i < motd.length) {
            if ((motd[i] == '§' || motd[i] == '\u00A7') && i + 1 < motd.length) {
                val code = motd[i + 1].lowercaseChar()
                when (code) {
                    in '0'..'9', in 'a'..'f' -> currentColor = colorMap[code]
                    'l' -> bold = true
                    'm' -> strikethrough = true
                    'n' -> underline = true
                    'o' -> italic = true
                    'r' -> {
                        currentColor = null; bold = false; italic = false
                        strikethrough = false; underline = false
                    }
                }
                i += 2
            } else {
                val start = i
                while (i < motd.length && !((motd[i] == '§' || motd[i] == '\u00A7') && i + 1 < motd.length)) {
                    i++
                }
                val segment = motd.substring(start, i)
                if (segment.isNotEmpty()) {
                    val textDecoration = when {
                        strikethrough && underline -> TextDecoration.combine(listOf(TextDecoration.LineThrough, TextDecoration.Underline))
                        strikethrough -> TextDecoration.LineThrough
                        underline -> TextDecoration.Underline
                        else -> null
                    }
                    withStyle(
                        SpanStyle(
                            color = currentColor ?: Color(0xFFAAAAAA),
                            fontWeight = if (bold) FontWeight.Bold else null,
                            fontStyle = if (italic) FontStyle.Italic else null,
                            textDecoration = textDecoration
                        )
                    ) {
                        append(segment)
                    }
                }
            }
        }
    }
}
