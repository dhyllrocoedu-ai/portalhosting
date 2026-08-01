package com.portalhost.desktop.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.Desktop
import java.net.URI

private val blockTagRegex = Regex("(?i)<(/?)(h[1-6]|p|ul|ol|li|pre|blockquote|hr|br|div|table|thead|tbody|tr|td|th|img)([^>]*)>")
private val inlineTagRegex = Regex("(?i)<(/?)(strong|b|em|i|u|del|code|a|span|br)([^>]*)>")
private val nestedListRegex = Regex("(?is)<(ul|ol)")
private val attributeRegex = Regex("([a-zA-Z_:][a-zA-Z0-9_:.-]*)\\s*=\\s*([\"'])(.*?)\\2")
private val htmlCommentRegex = Regex("(?s)<!--.*?-->")
private val tagOnlyRegex = Regex("<[^>]*>")
private val numericEntityRegex = Regex("&(#x[0-9a-fA-F]+|#\\d+);")

private sealed interface HtmlBlock {
    data class Heading(val level: Int, val inline: List<InlineSegment>) : HtmlBlock
    data class Paragraph(val inline: List<InlineSegment>) : HtmlBlock
    data class ListItem(
        val depth: Int,
        val ordered: Boolean,
        val index: Int,
        val inline: List<InlineSegment>,
        val children: List<HtmlBlock> = emptyList()
    ) : HtmlBlock

    data class CodeBlock(val code: String) : HtmlBlock
    data class Quote(val inline: List<InlineSegment>) : HtmlBlock
    data object Divider : HtmlBlock
    data class Image(val src: String, val alt: String?) : HtmlBlock
    data class Table(val rows: List<List<List<InlineSegment>>>) : HtmlBlock
}

private data class InlineSegment(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false,
    val link: String? = null
)

@Composable
fun HtmlBody(html: String) {
    val blocks = remember(html) { parseBlocks(html) }

    if (blocks.isEmpty()) {
        Text(
            text = html,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is HtmlBlock.Heading -> {
                    Text(
                        text = buildInlineAnnotated(block.inline),
                        style = headingStyle(block.level),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                is HtmlBlock.Paragraph -> InlineContentText(
                    block.inline,
                    style = MaterialTheme.typography.bodyMedium
                )
                is HtmlBlock.ListItem -> {
                    Row(modifier = Modifier.padding(start = (block.depth * 16).dp)) {
                        Text(
                            text = if (block.ordered) "${block.index}." else "\u2022",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(22.dp)
                        )
                        InlineContentText(
                            block.inline,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    block.children.forEach { child ->
                        when (child) {
                            is HtmlBlock.ListItem -> {
                                Row(modifier = Modifier.padding(start = ((child.depth + 1) * 16).dp)) {
                                    Text(
                                        text = if (child.ordered) "${child.index}." else "\u2022",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(22.dp)
                                    )
                                    InlineContentText(
                                        child.inline,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                child.children.forEach { grandChild ->
                                    if (grandChild is HtmlBlock.ListItem) {
                                        Row(modifier = Modifier.padding(start = ((grandChild.depth + 1) * 16).dp)) {
                                            Text(
                                                text = if (grandChild.ordered) "${grandChild.index}." else "\u2022",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.width(22.dp)
                                            )
                                            InlineContentText(
                                                grandChild.inline,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
                is HtmlBlock.CodeBlock -> {
                    Text(
                        text = block.code,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHighest,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    )
                }
                is HtmlBlock.Quote -> InlineContentText(
                    block.inline,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(10.dp)
                )
                is HtmlBlock.Divider -> HorizontalDivider()
                is HtmlBlock.Image -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        SimpleAsyncImage(
                            url = block.src,
                            contentDescription = block.alt,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
                is HtmlBlock.Table -> {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        block.rows.forEachIndexed { index, cells ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (index == 0)
                                            MaterialTheme.colorScheme.surfaceContainerHighest
                                        else
                                            MaterialTheme.colorScheme.surfaceContainerHigh,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                cells.forEach { cell ->
                                    InlineContentText(
                                        cell,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (index == 0) FontWeight.SemiBold else null
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineContentText(
    segments: List<InlineSegment>,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    if (segments.isEmpty()) return
    val linkColor = MaterialTheme.colorScheme.primary
    val codeColor = MaterialTheme.colorScheme.onSurface
    val annotated = remember(segments, linkColor, codeColor) { buildInlineAnnotated(segments, linkColor, codeColor) }
    Text(
        text = annotated,
        modifier = modifier,
        style = style,
        color = MaterialTheme.colorScheme.onSurface
    )
}

private fun buildInlineAnnotated(
    segments: List<InlineSegment>,
    linkColor: Color = Color(0xFF1565C0),
    codeColor: Color = Color.Unspecified
): AnnotatedString {
    val builder = AnnotatedString.Builder()
    segments.forEach { seg ->
        val start = builder.length
        builder.append(seg.text)
        val span = SpanStyle(
            fontWeight = if (seg.bold) FontWeight.Bold else null,
            fontStyle = if (seg.italic) FontStyle.Italic else null,
            fontFamily = if (seg.code) FontFamily.Monospace else null,
            color = if (seg.code) codeColor else Color.Unspecified,
            background = if (seg.code) Color(0x14000000) else Color.Unspecified
        )
        if (seg.link != null) {
            val annotation = LinkAnnotation.Clickable(
                tag = seg.link,
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    )
                )
            ) {
                openInBrowser(seg.link)
            }
            builder.addLink(annotation, start, builder.length)
        } else {
            builder.addStyle(span, start, builder.length)
        }
    }
    return builder.toAnnotatedString()
}

private fun openInBrowser(url: String) {
    try {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        } else {
            Runtime.getRuntime().exec(arrayOf("xdg-open", url))
        }
    } catch (_: Exception) {
        try { Runtime.getRuntime().exec(arrayOf("cmd", "/c", "start", url)) } catch (_: Exception) { }
    }
}

private fun headingStyle(level: Int): TextStyle = when (level) {
    1 -> TextStyle(fontSize = 22.sp)
    2 -> TextStyle(fontSize = 19.sp)
    3 -> TextStyle(fontSize = 16.sp)
    4 -> TextStyle(fontSize = 15.sp)
    else -> TextStyle(fontSize = 14.sp)
}

private fun parseBlocks(html: String, depth: Int = 0): List<HtmlBlock> {
    val clean = htmlCommentRegex.replace(html, "")
    val blocks = mutableListOf<HtmlBlock>()
    val text = StringBuilder()
    val listStack = ArrayDeque<Pair<Boolean, Int>>()
    var pos = 0
    val matcher = blockTagRegex.toPattern().matcher(clean)

    while (matcher.find()) {
        if (matcher.start() > pos) text.append(clean, pos, matcher.start())
        val closing = matcher.group(1) == "/"
        val tag = matcher.group(2).lowercase()
        val attrs = matcher.group(3)
        pos = matcher.end()

        when (tag) {
            "br" -> text.append('\n')
            "hr" -> {
                flushText(text, blocks)
                blocks += HtmlBlock.Divider
            }
            "img" -> {
                flushText(text, blocks)
                val src = attrValue(attrs, "src")
                if (src != null) blocks += HtmlBlock.Image(src, attrValue(attrs, "alt"))
            }
            "h1", "h2", "h3", "h4", "h5", "h6" -> if (!closing) {
                flushText(text, blocks)
                val end = clean.indexOf("</$tag>", pos)
                val content = if (end != -1) clean.substring(pos, end) else clean.substring(pos)
                blocks += HtmlBlock.Heading(tag[1].digitToInt(), parseInline(content))
                if (end != -1) pos = end + tag.length + 3
            }
            "p" -> if (!closing) {
                flushText(text, blocks)
                val end = clean.indexOf("</p>", pos)
                val content = if (end != -1) clean.substring(pos, end) else clean.substring(pos)
                blocks += HtmlBlock.Paragraph(parseInline(content))
                if (end != -1) pos = end + 4
            }
            "pre" -> if (!closing) {
                flushText(text, blocks)
                val end = clean.indexOf("</pre>", pos)
                val content = if (end != -1) clean.substring(pos, end) else clean.substring(pos)
                blocks += HtmlBlock.CodeBlock(decodeEntities(tagOnlyRegex.replace(content, "")).trim())
                if (end != -1) pos = end + 6
            }
            "blockquote" -> if (!closing) {
                flushText(text, blocks)
                val end = clean.indexOf("</blockquote>", pos)
                val content = if (end != -1) clean.substring(pos, end) else clean.substring(pos)
                blocks += HtmlBlock.Quote(parseInline(content))
                if (end != -1) pos = end + 13
            }
            "table" -> if (!closing) {
                flushText(text, blocks)
                val end = clean.indexOf("</table>", pos)
                val content = if (end != -1) clean.substring(pos, end) else clean.substring(pos)
                blocks += parseTable(content)
                if (end != -1) pos = end + 8
            }
            "ul", "ol" -> {
                if (!closing) {
                    listStack.addLast((tag == "ol") to 0)
                } else if (listStack.isNotEmpty()) {
                    listStack.removeLast()
                }
            }
            "li" -> if (!closing) {
                val depthNow = listStack.size - 1
                val (ordered, count) = listStack.lastOrNull() ?: (false to 0)
                if (listStack.isNotEmpty()) {
                    listStack.removeLast()
                    listStack.addLast(ordered to count + 1)
                }
                flushText(text, blocks)
                val end = clean.indexOf("</li>", pos)
                val content = if (end != -1) clean.substring(pos, end) else clean.substring(pos)
                val nestedStart = nestedListRegex.find(content)?.range?.first ?: -1
                val textPart = if (nestedStart != -1) content.substring(0, nestedStart) else content
                val nestedPart = if (nestedStart != -1) content.substring(nestedStart) else ""
                val inline = parseInline(textPart)
                val children = if (nestedPart.isNotBlank()) parseBlocks(nestedPart, depthNow + 1) else emptyList()
                blocks += HtmlBlock.ListItem(depthNow, ordered, count + 1, inline, children)
                if (end != -1) pos = end + 5
            }
            else -> {
                if (closing) text.append('\n')
            }
        }
    }
    if (pos < clean.length) text.append(clean, pos, clean.length)
    flushText(text, blocks)

    if (blocks.isEmpty() && clean.isNotBlank()) {
        clean.split(Regex("\\n\\s*\\n")).filter { it.isNotBlank() }.forEach { para ->
            blocks += HtmlBlock.Paragraph(parseInline(para.trim()))
        }
    }
    return blocks
}

private fun parseTable(content: String): HtmlBlock.Table {
    val rows = mutableListOf<List<List<InlineSegment>>>()
    val rowRegex = Regex("(?is)<tr[^>]*>(.*?)</tr>")
    val cellRegex = Regex("(?is)<(td|th)[^>]*>(.*?)</\\1>")
    for (rm in rowRegex.findAll(content)) {
        val cells = cellRegex.findAll(rm.groupValues[1])
            .map { parseInline(it.groupValues[2]) }
            .filter { it.isNotEmpty() }
            .toList()
        if (cells.isNotEmpty()) rows += cells
    }
    return HtmlBlock.Table(rows)
}

private fun parseInline(html: String): List<InlineSegment> {
    val segments = mutableListOf<InlineSegment>()
    val sb = StringBuilder()
    var bold = false
    var italic = false
    var code = false
    var link: String? = null
    var pos = 0

    fun flush() {
        if (sb.isNotBlank()) {
            segments += InlineSegment(decodeEntities(sb.toString()), bold, italic, code, link)
            sb.clear()
        }
    }

    val matcher = inlineTagRegex.toPattern().matcher(html)
    while (matcher.find()) {
        sb.append(html, pos, matcher.start())
        val closing = matcher.group(1) == "/"
        val tag = matcher.group(2).lowercase()
        val attrs = matcher.group(3)
        pos = matcher.end()

        when (tag) {
            "b", "strong" -> { flush(); bold = !closing }
            "i", "em" -> { flush(); italic = !closing }
            "code" -> { flush(); code = !closing }
            "u", "del", "span" -> { }
            "br" -> sb.append('\n')
            "a" -> {
                flush()
                if (!closing) {
                    link = attrValue(attrs, "href")
                } else {
                    link = null
                }
            }
        }
    }
    sb.append(html, pos, html.length)
    flush()
    return segments
}

private fun flushText(text: StringBuilder, blocks: MutableList<HtmlBlock>) {
    val s = text.toString().trim()
    text.clear()
    if (s.isNotBlank()) {
        blocks += HtmlBlock.Paragraph(parseInline(s))
    }
}

private fun attrValue(attrs: String, name: String): String? {
    val regex = Regex("(?i)${Regex.escape(name)}\\s*=\\s*([\"'])(.*?)\\1")
    return regex.find(attrs)?.groupValues?.get(2)
}

private fun decodeEntities(s: String): String {
    val decoded = numericEntityRegex.replace(s) { match ->
        val code = match.groupValues[1].removePrefix("#")
        val value = if (code.startsWith("x", true)) {
            code.substring(1).toIntOrNull(16)
        } else {
            code.toIntOrNull(10)
        }
        if (value != null) String(Character.toChars(value)) else match.value
    }
    return decoded
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&#39;", "'")
        .replace("&nbsp;", "\u00A0")
}
