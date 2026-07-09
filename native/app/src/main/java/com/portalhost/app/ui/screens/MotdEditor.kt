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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
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

private data class FormatCodeDef(val label: String, val code: String)

private val FORMAT_CODES = listOf(
    FormatCodeDef("Bold", "§l"),
    FormatCodeDef("Italic", "§o"),
    FormatCodeDef("Underline", "§n"),
    FormatCodeDef("Strike", "§m"),
    FormatCodeDef("Obfuscated", "§k"),
    FormatCodeDef("Reset", "§r")
)

@Composable
fun MotdEditor(
    motd: String,
    onMotdChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showColors by remember { mutableStateOf(false) }

    var tfv by remember { mutableStateOf(TextFieldValue(motd, TextRange(motd.length))) }

    LaunchedEffect(motd) {
        if (motd != tfv.text) {
            val clamped = tfv.selection.start.coerceAtMost(motd.length)
            tfv = TextFieldValue(motd, TextRange(clamped))
        }
    }

    Column(modifier = modifier) {
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
                Text(if (showColors) "Hide Codes" else "Color Codes", style = MaterialTheme.typography.labelSmall)
            }
        }

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
                MC_COLORS.forEach { (code, color) ->
                    val borderColor = if (code == 'f') Color(0xFF888888) else Color.Transparent
                    Box(
                        modifier = Modifier
                            .size(24.dp)
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
                        label = { Text(fmt.label, fontSize = 11.sp) }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

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
