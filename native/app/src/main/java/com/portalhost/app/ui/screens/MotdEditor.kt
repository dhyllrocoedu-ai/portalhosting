package com.portalhost.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MC_COLORS = listOf(
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

private data class FormatCode(val label: String, val code: String, val color: Color? = null)

private val FORMAT_CODES = listOf(
    FormatCode("Bold", "§l"),
    FormatCode("Italic", "§o"),
    FormatCode("Underline", "§n"),
    FormatCode("Strike", "§m"),
    FormatCode("Obfuscate", "§k"),
    FormatCode("Reset", "§r")
)

@Composable
fun MotdEditor(
    motd: String,
    onMotdChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showColors by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = motd,
            onValueChange = onMotdChange,
            label = { Text("MOTD") },
            placeholder = { Text("A Minecraft Server") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null) },
            supportingText = { Text("Use § followed by a color code (e.g. §cRed §aGreen). Use §r to reset.") }
        )

        Spacer(Modifier.height(8.dp))

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
                Text(if (showColors) "Hide Colors" else "Color Codes", style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = { onMotdChange(motd + "§") }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(2.dp))
                Text("Add §", style = MaterialTheme.typography.labelSmall)
            }
        }

        if (showColors) {
            ColorCodeGrid(onInsert = { code -> onMotdChange(motd + code) })
            Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.height(8.dp))

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
                Text(
                    text = if (parsed.isEmpty()) buildAnnotatedString { withStyle(SpanStyle(color = Color(0xFFAAAAAA))) { append("MOTD preview will appear here") } } else parsed,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
private fun ColorCodeGrid(onInsert: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Colors", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MC_COLORS.chunked(8).forEach { row ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { (code, color) ->
                        val borderColor = if (code == 'f') Color(0xFF888888) else Color.Transparent
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(0.5.dp, borderColor, CircleShape)
                                .clickable { onInsert("§$code") },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                code.toString(),
                                fontSize = 9.sp,
                                color = if (code in listOf('0', '8', '4')) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text("Formatting", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FORMAT_CODES.forEach { fmt ->
                SuggestionChip(
                    onClick = { onInsert(fmt.code) },
                    label = { Text(fmt.label, fontSize = 10.sp) }
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text("Codes are inserted at the end of your MOTD text.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            if (motd[i] == '§' && i + 1 < motd.length) {
                val code = motd[i + 1].lowercaseChar()
                when (code) {
                    in '0'..'9', in 'a'..'f' -> currentColor = colorMap[code]
                    'l' -> bold = true
                    'm' -> strikethrough = true
                    'n' -> underline = true
                    'o' -> italic = true
                    'r' -> {
                        currentColor = null
                        bold = false
                        italic = false
                        strikethrough = false
                        underline = false
                    }
                }
                i += 2
            } else {
                val start = i
                while (i < motd.length && !(motd[i] == '§' && i + 1 < motd.length)) {
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
