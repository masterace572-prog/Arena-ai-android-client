@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ai.arena.client.ui

import ai.arena.client.ChatViewModel
import ai.arena.client.ConnState
import ai.arena.client.Screen
import ai.arena.client.data.AppSettings
import ai.arena.client.data.Conversation
import ai.arena.client.data.ThinkingLevel
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.widget.Toast
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(vm: ChatViewModel) {
    val ui by vm.ui.collectAsState()
    val settings by vm.settings.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val conversations = ui.conversations
    val activeId = ui.activeId
    val conversation: Conversation? =
        conversations.find { it.id == activeId } ?: conversations.lastOrNull()
    val messages = conversation?.messages ?: emptyList()

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    // Honest feature-fallback notices (thinking/search stripped by a 400).
    LaunchedEffect(ui.notice) {
        val notice = ui.notice ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(notice)
        vm.clearNotice()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                conversations = conversations,
                activeId = conversation?.id ?: "",
                onNew = { scope.launch { drawerState.close() }; vm.newChat() },
                onSelect = { id -> scope.launch { drawerState.close() }; vm.selectChat(id) },
                onDelete = vm::deleteChat,
                onSettings = {
                    scope.launch { drawerState.close() }
                    vm.setScreen(Screen.SETTINGS)
                },
            )
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                ChatTopBar(
                    conn = ui.conn,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onNewChat = vm::newChat,
                    onSettings = { vm.setScreen(Screen.SETTINGS) },
                )
            },
            bottomBar = {
                InputBar(
                    streaming = ui.streaming,
                    thinkingLevel = settings.thinkingLevel,
                    onThinkingLevel = { level ->
                        vm.updateSettings { it.copy(thinkingLevel = level) }
                    },
                    onSend = vm::send,
                    onStop = vm::stop,
                )
            },
        ) { padding ->
            MessageList(
                modifier = Modifier.padding(padding),
                messages = messages,
                settings = settings,
                streaming = ui.streaming,
                onToggleThinking = vm::toggleThinking,
                onRegenerate = vm::regenerate,
            )
        }
    }
}

// ---------------------------------------------------------------------------

@Composable
private fun ChatTopBar(
    conn: ConnState,
    onOpenDrawer: () -> Unit,
    onNewChat: () -> Unit,
    onSettings: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Sparkle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(7.dp))
                Text(
                    text = "Arena",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.size(9.dp))
                StatusDot(conn, size = 7.dp)
            }
        },
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Filled.Menu, contentDescription = "Conversations")
            }
        },
        actions = {
            IconButton(onClick = onNewChat) {
                Icon(Icons.Filled.Add, contentDescription = "New chat")
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

// ---------------------------------------------------------------------------
// Drawer — branding, search, recent chats, settings
// ---------------------------------------------------------------------------

@Composable
private fun DrawerContent(
    conversations: List<Conversation>,
    activeId: String,
    onNew: () -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    val version = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrDefault("").orEmpty()
    }
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(conversations, query) {
        if (query.isBlank()) conversations
        else conversations.filter { it.title.contains(query, ignoreCase = true) }
    }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 22.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Sparkle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Arena",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                if (version.isNotBlank()) {
                    Text(
                        text = "v$version",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Button(
                onClick = onNew,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("New chat")
            }

            Spacer(Modifier.size(12.dp))

            // Search conversations
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    1.dp,
                    if (query.isNotBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f).padding(vertical = 9.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        decorationBox = { inner ->
                            Box {
                                if (query.isEmpty()) {
                                    Text(
                                        text = "Search chats",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    )
                                }
                                inner()
                            }
                        },
                    )
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }, modifier = Modifier.size(30.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.size(14.dp))

            if (filtered.isNotEmpty()) {
                Text(
                    text = "RECENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 10.dp, bottom = 6.dp),
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            text = if (query.isBlank()) "No chats yet — start one above"
                            else "No chats match \u201C$query\u201D",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(start = 10.dp, top = 8.dp),
                        )
                    }
                }
                items(
                    filtered.size,
                    key = { filtered[it].id },
                ) { index ->
                    val conv = filtered[index]
                    val selected = conv.id == activeId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelect(conv.id) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) MaterialTheme.colorScheme.surfaceContainerHighest
                        else androidx.compose.ui.graphics.Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = conv.title.ifBlank { "New chat" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.size(2.dp))
                                Text(
                                    text = relativeTime(conv.updatedAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                )
                            }
                            IconButton(
                                onClick = { onDelete(conv.id) },
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onSettings)
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.size(12.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.size(10.dp))
        }
    }
}

// ---------------------------------------------------------------------------

@Composable
private fun MessageList(
    modifier: Modifier,
    messages: List<ai.arena.client.data.ChatMessage>,
    settings: AppSettings,
    streaming: Boolean,
    onToggleThinking: (String) -> Unit,
    onRegenerate: () -> Unit,
) {
    val scroll = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val reversed = remember(messages) { messages.asReversed() }

    val lastContent = messages.lastOrNull()?.content
    val lastThinking = messages.lastOrNull()?.thinking
    val lastStreaming = messages.lastOrNull()?.streaming
    var previousSize by remember { mutableIntStateOf(messages.size) }

    LaunchedEffect(messages.size, lastContent, lastThinking, lastStreaming) {
        if (messages.isEmpty()) return@LaunchedEffect
        val info = scroll.layoutInfo
        val atBottom = info.visibleItemsInfo.firstOrNull()?.index == 0 ||
            info.visibleItemsInfo.isEmpty()
        val grew = messages.size > previousSize
        previousSize = messages.size
        if (grew || atBottom) scroll.scrollToItem(0)
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState()
            }
            return@Box
        }

        LazyColumn(
            state = scroll,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 780.dp),
            reverseLayout = true,
            contentPadding = PaddingValues(top = 10.dp, bottom = 14.dp),
        ) {
            items(
                count = reversed.size,
                key = { reversed[it].id },
            ) { index ->
                val message = reversed[index]
                Box(modifier = Modifier.animateItem()) {
                    if (message.role == ai.arena.client.data.Role.USER) {
                        UserBubble(message)
                    } else {
                        AssistantMessage(
                            message = message,
                            mode = settings.thinkingMode,
                            isLast = index == 0,
                            onToggleThinking = { onToggleThinking(message.id) },
                            onRegenerate = onRegenerate,
                        )
                    }
                }
            }
        }

        val atBottom by remember {
            derivedStateOf {
                scroll.firstVisibleItemIndex <= 0 && scroll.firstVisibleItemScrollOffset < 80
            }
        }
        if (!atBottom && messages.isNotEmpty()) {
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable {
                            scope.launch { scroll.animateScrollToItem(0) }
                        },
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Jump to latest",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Composer — thinking selector, attachment, send/stop transitions
// ---------------------------------------------------------------------------

@Composable
private fun InputBar(
    streaming: Boolean,
    thinkingLevel: ThinkingLevel,
    onThinkingLevel: (ThinkingLevel) -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val attachLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("unreadable")
            val content = String(bytes, Charsets.UTF_8)
            val bad = content.count { it == '\uFFFD' }
            require(content.isNotBlank() && bad < content.length / 50 + 2) { "binary" }
            val name = uri.lastPathSegment?.substringAfterLast('/').orEmpty().ifBlank { "file" }
            val truncated = content.length > 8000
            val block = buildString {
                append("Attached file: ").append(name).append('\n')
                append(content.take(8000))
                if (truncated) append("\n\u2026 (truncated)")
            }
            text = if (text.isBlank()) block else text + "\n\n" + block
        }.onFailure {
            Toast.makeText(context, "Only text files can be attached", Toast.LENGTH_SHORT).show()
        }
    }

    val borderColor by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(150),
        label = "composer-border",
    )

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 12.dp)) {
            // Reasoning-level selector — drives the next request directly.
            Row(modifier = Modifier.fillMaxWidth().padding(start = 2.dp, bottom = 8.dp)) {
                ThinkingSelector(level = thinkingLevel, onLevel = onThinkingLevel)
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, borderColor),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { attachLauncher.launch(arrayOf("text/*")) },
                            modifier = Modifier.size(38.dp).padding(start = 4.dp),
                        ) {
                            Icon(
                                imageVector = AttachGlyph,
                                contentDescription = "Attach text file",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        BasicTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { focused = it.isFocused },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            maxLines = 12,
                            keyboardOptions = KeyboardOptions(
                                imeAction = if (streaming) ImeAction.None else ImeAction.Send,
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (text.isNotBlank() && !streaming) {
                                        onSend(text)
                                        text = ""
                                    }
                                },
                            ),
                            decorationBox = { inner ->
                                Box(modifier = Modifier.padding(start = 4.dp, end = 16.dp, top = 15.dp, bottom = 15.dp)) {
                                    if (text.isEmpty()) {
                                        Text(
                                            text = "Message Arena",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                        )
                                    }
                                    inner()
                                }
                            },
                        )
                    }
                }

                val canSend = text.isNotBlank() && !streaming
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            color = when {
                                streaming -> MaterialTheme.colorScheme.surfaceContainerHighest
                                canSend -> MaterialTheme.colorScheme.primary
                                else -> androidx.compose.ui.graphics.Color.Transparent
                            },
                        )
                        .then(
                            if (!canSend && !streaming) {
                                Modifier.border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    CircleShape,
                                )
                            } else {
                                Modifier
                            },
                        )
                        .clickable(enabled = canSend || streaming) {
                            if (streaming) {
                                onStop()
                            } else {
                                onSend(text)
                                text = ""
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = streaming,
                        transitionSpec = {
                            fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                        },
                        label = "send-stop",
                    ) { isStreaming ->
                        Icon(
                            imageVector = if (isStreaming) StopSquare else Icons.Filled.KeyboardArrowUp,
                            contentDescription = if (isStreaming) "Stop" else "Send",
                            tint = when {
                                isStreaming -> MaterialTheme.colorScheme.onSurface
                                canSend -> MaterialTheme.colorScheme.onPrimary
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            },
                            modifier = Modifier.size(if (isStreaming) 18.dp else 22.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Compact "Thinking · Medium ▼" chip with a polished selection menu. */
@Composable
private fun ThinkingSelector(
    level: ThinkingLevel,
    onLevel: (ThinkingLevel) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { expanded = true },
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Sparkle,
                    contentDescription = null,
                    tint = if (level == ThinkingLevel.OFF) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "Thinking \u00B7 ${level.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(3.dp))
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Change reasoning level",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp),
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ThinkingLevel.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    trailingIcon = {
                        if (option == level) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    },
                    onClick = {
                        onLevel(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
