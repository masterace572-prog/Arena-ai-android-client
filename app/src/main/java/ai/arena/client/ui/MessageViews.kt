package ai.arena.client.ui

import ai.arena.client.ChatViewModel
import ai.arena.client.data.ChatMessage
import ai.arena.client.data.Role
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Markdown (lightweight: code fences, headings, bullets, bold, inline code)
// ---------------------------------------------------------------------------

private sealed interface MdBlock {
    data class Text(val body: String) : MdBlock
    data class Code(val lang: String, val code: String) : MdBlock
    data class Heading(val text: String) : MdBlock
    data class Bullets(val items: List<String>) : MdBlock
}

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
            line.isBlank() -> i++
            else -> {
                val buf = StringBuilder(line)
                i++
                while (i < lines.size) {
                    val next = lines[i]
                    val nt = next.trimStart()
                    if (next.isBlank() || nt.startsWith("```") || nt.startsWith("#") ||
                        nt.startsWith("- ") || nt.startsWith("* ")
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

private val INLINE_REGEX = Regex("(\\*\\*[^*\\n]+\\*\\*|`[^`\\n]+`)")

private fun buildInline(text: String, codeBg: Color, codeFg: Color): AnnotatedString =
    buildAnnotatedString {
        var idx = 0
        INLINE_REGEX.findAll(text).forEach { m ->
            if (m.range.first > idx) append(text.substring(idx, m.range.first))
            val token = m.value
            when {
                token.startsWith("**") -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                    append(token.removeSurround("**"))
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
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val codeFg = MaterialTheme.colorScheme.onSurface
    val bodyStyle = MaterialTheme.typography.bodyMedium
    val onBackground = MaterialTheme.colorScheme.onBackground

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Text -> Text(
                    text = remember(block.body, codeBg) { buildInline(block.body, codeBg, codeFg) },
                    style = bodyStyle,
                    color = onBackground,
                )
                is MdBlock.Heading -> Text(
                    text = remember(block.text, codeBg) { buildInline(block.text, codeBg, codeFg) },
                    style = MaterialTheme.typography.titleMedium,
                    color = onBackground,
                )
                is MdBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    block.items.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "•",
                                style = bodyStyle,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Text(
                                text = remember(item, codeBg) { buildInline(item, codeBg, codeFg) },
                                style = bodyStyle,
                                color = onBackground,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                is MdBlock.Code -> CodeBlock(lang = block.lang, code = block.code)
            }
        }
    }
}

@Composable
private fun CodeBlock(lang: String, code: String) {
    val clipboard = LocalClipboardManager.current
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
                ActionPill(text = "Copy") {
                    clipboard.setText(AnnotatedString(code))
                }
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

    val expanded = when (mode) {
        ThinkingMode.HIDDEN -> return
        ThinkingMode.COLLAPSED -> message.thinkingExpanded ?: false
        ThinkingMode.EXPANDED -> message.thinkingExpanded ?: true
    }
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
                    text = if (streaming) "Thinking…"
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
    Box(
        modifier = Modifier
            .size(7.dp)
            .alpha(if (streaming) alpha else 1f)
            .background(
                color = if (streaming) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = CircleShape,
            ),
    )
}

// ---------------------------------------------------------------------------
// Typing indicator
// ---------------------------------------------------------------------------

@Composable
private fun TypingDots() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(520),
                    repeatMode = RepeatMode.Reverse,
                    initialDelay = index * 160L,
                ),
                label = "dot$index",
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .alpha(alpha)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = CircleShape,
                    ),
            )
        }
    }
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
            modifier = Modifier.widthIn(max = 300.dp),
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
    val clipboard = LocalClipboardManager.current
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

            message.streaming && message.thinking.isBlank() -> TypingDots()

            message.streaming -> TypingDots()
        }

        if (!message.streaming && !message.error && message.content.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionPill(text = "Copy") {
                    clipboard.setText(AnnotatedString(message.content))
                }
                if (isLast) {
                    ActionPill(text = "Regenerate", onClick = onRegenerate)
                }
                Spacer(Modifier.weight(1f, fill = true))
                val meta = buildString {
                    message.elapsedMs?.let {
                        val t = formatElapsed(it)
                        if (t.isNotBlank()) append(t)
                    }
                    val tokens = message.tokensOut ?: message.tokensIn
                    if (tokens != null) {
                        if (isNotEmpty()) append(" · ")
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
                if (isLast) ActionPill(text = "Retry", onClick = onRegenerate)
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
                    ActionPill(text = "Retry", onClick = onRetry)
                }
            }
        }
    }
}

@Composable
fun EmptyState(modelLabel: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Image(
                painter = androidx.compose.ui.res.painterResource(
                    ai.arena.client.R.drawable.ic_launcher_foreground,
                ),
                contentDescription = null,
                modifier = Modifier.padding(16.dp),
            )
        }
        Spacer(Modifier.size(20.dp))
        Text(
            text = "Start a conversation",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "Messages stream live. Reasoning, when the model shares it, collapses into a tidy card.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.size(18.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Text(
                text = modelLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}
