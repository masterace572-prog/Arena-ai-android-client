package ai.arena.client.ui

import ai.arena.client.data.ChatMessage
import ai.arena.client.data.ThinkingMode
import ai.arena.client.ui.theme.Palette
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Markdown: code fences, headings, bullets, bold, inline code, links, tables
// ---------------------------------------------------------------------------

private sealed interface MdBlock {
    data class Text(val body: String) : MdBlock
    data class Code(val lang: String, val code: String) : MdBlock
    data class Heading(val text: String) : MdBlock
    data class Bullets(val items: List<String>) : MdBlock
    data class Ordered(val items: List<String>) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Table(val header: List<String>, val rows: List<List<String>>) : MdBlock
    data object Divider : MdBlock
}

private val ORDERED_ITEM = Regex("\\d{1,3}\\.\\s+(.+)")

private fun parseCells(line: String): List<String> =
    line.trim().trim('|').split('|').map { it.trim() }

private fun isTableSeparator(line: String): Boolean =
    line.contains('-') && parseCells(line).all { it.isEmpty() || it.all { c -> c == '-' || c == ':' } }

private fun parseMarkdown(src: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = src.replace("\r\n", "\n").split('\n')
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trimStart()
        when {
            trimmed.startsWith("```") -> {
                val lang = trimmed.removePrefix("```").trim()
                val buf = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                    buf.append(lines[i]).append('\n')
                    i++
                }
                if (i < lines.size) i++ // closing fence
                blocks.add(MdBlock.Code(lang, buf.toString().trimEnd('\n')))
            }
            trimmed.startsWith("|") && trimmed.count { it == '|' } >= 2 -> {
                val tableLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trimStart().startsWith("|")) {
                    tableLines.add(lines[i].trim())
                    i++
                }
                if (tableLines.size >= 2) {
                    val header = parseCells(tableLines[0])
                    val bodyStart = if (isTableSeparator(tableLines[1])) 2 else 1
                    val rows = tableLines.drop(bodyStart).map { parseCells(it) }
                    blocks.add(MdBlock.Table(header, rows))
                } else {
                    blocks.add(MdBlock.Text(tableLines.joinToString("\n")))
                }
            }
            trimmed.startsWith("#") -> {
                blocks.add(MdBlock.Heading(trimmed.trimStart('#').trim()))
                i++
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                val items = mutableListOf<String>()
                while (i < lines.size) {
                    val t = lines[i].trimStart()
                    if (t.startsWith("- ") || t.startsWith("* ")) {
                        items.add(t.substring(2))
                        i++
                    } else break
                }
                blocks.add(MdBlock.Bullets(items))
            }
            trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                blocks.add(MdBlock.Divider)
                i++
            }
            trimmed.startsWith(">") -> {
                val buf = StringBuilder()
                while (i < lines.size && lines[i].trimStart().startsWith(">")) {
                    val t = lines[i].trimStart().removePrefix(">").trimStart()
                    if (buf.isNotEmpty()) buf.append('\n')
                    buf.append(t)
                    i++
                }
                blocks.add(MdBlock.Quote(buf.toString()))
            }
            ORDERED_ITEM.matchEntire(trimmed) != null -> {
                val items = mutableListOf<String>()
                while (i < lines.size) {
                    val m = ORDERED_ITEM.matchEntire(lines[i].trimStart())
                    if (m == null) break
                    items.add(m.groupValues[1])
                    i++
                }
                blocks.add(MdBlock.Ordered(items))
            }
            line.isBlank() -> i++
            else -> {
                val buf = StringBuilder(line)
                i++
                while (i < lines.size) {
                    val next = lines[i]
                    val nt = next.trimStart()
                    if (next.isBlank() || nt.startsWith("```") || nt.startsWith("#") ||
                        nt.startsWith("- ") || nt.startsWith("* ") || nt.startsWith("|") ||
                        nt.startsWith(">") || ORDERED_ITEM.matchEntire(nt) != null ||
                        nt == "---"
                    ) break
                    buf.append('\n').append(next)
                    i++
                }
                blocks.add(MdBlock.Text(buf.toString()))
            }
        }
    }
    return blocks
}

private val INLINE_REGEX = Regex(
    "(\\*\\*[^*\\n]+\\*\\*|`[^`\\n]+`|\\[([^\\]\\n]+)]\\((https?://[^)\\s]+)\\)" +
        "|https?://[^\\s)\\]]+|\\*[^*\\n]+\\*)",
)

private fun buildInline(
    text: String,
    codeBg: Color,
    codeFg: Color,
    linkColor: Color,
): AnnotatedString =
    buildAnnotatedString {
        var idx = 0
        INLINE_REGEX.findAll(text).forEach { m ->
            if (m.range.first > idx) append(text.substring(idx, m.range.first))
            val token = m.value
            val mdLabel = m.groupValues.getOrNull(2).orEmpty()
            val mdUrl = m.groupValues.getOrNull(3).orEmpty()
            val linkStyle = SpanStyle(
                color = linkColor,
                textDecoration = androidx.compose.ui.text.style.TextUnderline,
            )
            when {
                token.startsWith("**") -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                    append(token.removePrefix("**").removeSuffix("**"))
                }
                mdUrl.isNotEmpty() -> withLink(LinkAnnotation.Url(mdUrl)) {
                    withStyle(linkStyle) { append(mdLabel) }
                }
                token.startsWith("http") -> {
                    val url = token.trimEnd('.', ',', ';', ':')
                    withLink(LinkAnnotation.Url(url)) {
                        withStyle(linkStyle) { append(token) }
                    }
                }
                token.startsWith("*") -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(token.trim('*'))
                }
                else -> withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = codeFg,
                        background = codeBg,
                    ),
                ) {
                    append(token.trim('`'))
                }
            }
            idx = m.range.last + 1
        }
        if (idx < text.length) append(text.substring(idx))
    }

@Composable
private fun MarkdownText(text: String) {
    val blocks = remember(text) { parseMarkdown(text) }
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val codeFg = MaterialTheme.colorScheme.onSurface
    val linkColor = MaterialTheme.colorScheme.primary
    val bodyStyle = MaterialTheme.typography.bodyMedium
    val onBackground = MaterialTheme.colorScheme.onBackground

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Text -> Text(
                    text = remember(block.body, codeBg, linkColor) {
                        buildInline(block.body, codeBg, codeFg, linkColor)
                    },
                    style = bodyStyle,
                    color = onBackground,
                )
                is MdBlock.Heading -> Text(
                    text = remember(block.text, codeBg, linkColor) {
                        buildInline(block.text, codeBg, codeFg, linkColor)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = onBackground,
                )
                is MdBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    block.items.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "\u2022",
                                style = bodyStyle,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Text(
                                text = remember(item, codeBg, linkColor) {
                                    buildInline(item, codeBg, codeFg, linkColor)
                                },
                                style = bodyStyle,
                                color = onBackground,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                is MdBlock.Ordered -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    block.items.forEachIndexed { idx, item ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "${idx + 1}.",
                                style = bodyStyle,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Text(
                                text = remember(item, codeBg, linkColor) {
                                    buildInline(item, codeBg, codeFg, linkColor)
                                },
                                style = bodyStyle,
                                color = onBackground,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                is MdBlock.Quote -> Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                RoundedCornerShape(2.dp),
                            ),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = remember(block.text, codeBg, linkColor) {
                            buildInline(block.text, codeBg, codeFg, linkColor)
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
                is MdBlock.Divider -> HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                is MdBlock.Table -> TableBlock(header = block.header, rows = block.rows)
                is MdBlock.Code -> CodeBlock(lang = block.lang, code = block.code)
            }
        }
    }
}

@Composable
private fun TableBlock(header: List<String>, rows: List<List<String>>) {
    val cellWidth = 150.dp
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                header.forEach { cell ->
                    Text(
                        text = cell,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        modifier = Modifier.width(cellWidth).padding(horizontal = 10.dp),
                    )
                }
            }
            HorizontalTableDivider()
            rows.forEachIndexed { rowIndex, row ->
                if (rowIndex > 0) HorizontalTableDivider()
                Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                    row.forEach { cell ->
                        Text(
                            text = cell,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 6,
                            modifier = Modifier.width(cellWidth).padding(horizontal = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HorizontalTableDivider() {
    androidx.compose.material3.HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 1.dp,
    )
}

@Composable
private fun CodeBlock(lang: String, code: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = lang.ifBlank { "code" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                CopyButton(textToCopy = code)
            }
            Text(
                text = code,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Copy with quiet confirmation (compact icon action)
// ---------------------------------------------------------------------------

@Composable
private fun CopyButton(textToCopy: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }
    IconAction(
        icon = CopyGlyph,
        label = if (copied) "Copied \u2713" else "Copy",
        onClick = {
            clipboard.setText(AnnotatedString(textToCopy))
            copied = true
        },
    )
}

// ---------------------------------------------------------------------------
// Thinking — collapsible, with global mode + per-message override
// ---------------------------------------------------------------------------

@Composable
fun ThinkingSection(
    message: ChatMessage,
    mode: ThinkingMode,
    streaming: Boolean,
    onToggle: () -> Unit,
) {
    if (mode == ThinkingMode.HIDDEN) return
    val thinking = message.thinking
    if (thinking.isBlank()) return

    val base = when (mode) {
        ThinkingMode.HIDDEN -> return
        ThinkingMode.COLLAPSED -> false
        ThinkingMode.EXPANDED -> true
    }
    // Show reasoning live while the model works; honor per-message override always.
    val expanded = message.thinkingExpanded ?: (streaming || base)
    val chevron by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(220),
        label = "chevron",
    )

    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onToggle),
        shape = RoundedCornerShape(12.dp),
        color = if (MaterialTheme.colorScheme.background.luminanceSafe() < 0.5f) {
            Palette.ThinkingDark
        } else {
            Palette.ThinkingLight
        },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ThinkingDot(streaming)
                Spacer(Modifier.size(8.dp))
                Text(
                    text = if (streaming) "Arena"
                    else "Thought for ${formatElapsed(message.elapsedMs).ifBlank { "a moment" }}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse thinking" else "Expand thinking",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp).rotate(chevron),
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(220)) + fadeIn(tween(180)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(120)),
            ) {
                Text(
                    text = thinking,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

private fun Color.luminanceSafe(): Float =
    (0.299f * red + 0.587f * green + 0.114f * blue)

@Composable
private fun ThinkingDot(streaming: Boolean) {
    val transition = rememberInfiniteTransition(label = "think-dot")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (streaming) 0.3f else 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "think-dot-alpha",
    )
    Icon(
        imageVector = Sparkle,
        contentDescription = null,
        tint = if (streaming) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .size(15.dp)
            .alpha(if (streaming) alpha else 1f),
    )
}

// ---------------------------------------------------------------------------
// Typing indicator
// ---------------------------------------------------------------------------

@Composable
private fun ArenaPulse() {
    val transition = rememberInfiniteTransition(label = "arena-pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "arena-pulse-alpha",
    )
    Text(
        text = "Arena",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.alpha(alpha),
    )
}

// ---------------------------------------------------------------------------
// Messages
// ---------------------------------------------------------------------------

@Composable
fun UserBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 5.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
fun AssistantMessage(
    message: ChatMessage,
    mode: ThinkingMode,
    isLast: Boolean,
    onToggleThinking: () -> Unit,
    onRegenerate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (message.thinking.isNotBlank() && mode != ThinkingMode.HIDDEN) {
            ThinkingSection(
                message = message,
                mode = mode,
                streaming = message.streaming,
                onToggle = onToggleThinking,
            )
        }

        when {
            message.error && message.content.isBlank() -> ErrorCard(
                message = message.errorMessage ?: "Request failed",
                onRetry = if (isLast) onRegenerate else null,
            )

            message.content.isNotBlank() -> MarkdownText(message.content)

            message.streaming && message.thinking.isBlank() -> ArenaPulse()

            else -> Unit // thinking card carries the signal while reasoning
        }

        if (!message.streaming && !message.error && message.content.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CopyButton(textToCopy = message.content)
                if (isLast) {
                    IconAction(
                        icon = Icons.Filled.Refresh,
                        label = "Regenerate",
                        onClick = onRegenerate,
                    )
                }
                Spacer(Modifier.weight(1f, fill = true))
                val meta = buildString {
                    message.elapsedMs?.let {
                        val t = formatElapsed(it)
                        if (t.isNotBlank()) append(t)
                    }
                    val tokens = message.tokensOut ?: message.tokensIn
                    if (tokens != null) {
                        if (isNotEmpty()) append(" \u00B7 ")
                        append("$tokens tokens")
                    }
                }
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    )
                }
            }
        }

        // Partial answer followed by an error — show a quiet retry row.
        if (message.error && message.content.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = message.errorMessage ?: "Stream interrupted",
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.Error,
                    modifier = Modifier.weight(1f),
                )
                if (isLast) {
                    IconAction(
                        icon = Icons.Filled.Refresh,
                        label = "Retry",
                        onClick = onRegenerate,
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: (() -> Unit)?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Palette.Error.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Palette.Error.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = Palette.Error,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Something went wrong",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (onRetry != null) {
                    Spacer(Modifier.size(6.dp))
                    IconAction(
                        icon = Icons.Filled.Refresh,
                        label = "Retry",
                        onClick = onRetry,
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState() {
    val greeting = remember {
        when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(76.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Sparkle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        Spacer(Modifier.size(24.dp))
        Text(
            text = greeting,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "How can I help you today?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.size(20.dp))
        Text(
            text = "A R E N A",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            letterSpacing = 3.sp,
        )
    }
}
