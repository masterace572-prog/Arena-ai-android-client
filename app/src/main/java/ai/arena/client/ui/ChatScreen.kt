@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ai.arena.client.ui

import ai.arena.client.ChatViewModel
import ai.arena.client.ConnState
import ai.arena.client.Screen
import ai.arena.client.data.AppSettings
import ai.arena.client.data.Conversation
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.onFocusChanged
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(vm: ChatViewModel) {
    val ui by vm.ui.collectAsState()
    val settings by vm.settings.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val conversations = ui.conversations
    val activeId = ui.activeId
    val conversation: Conversation? =
        conversations.find { it.id == activeId } ?: conversations.lastOrNull()
    val messages = conversation?.messages ?: emptyList()

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

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
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Sparkle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Arena",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.size(10.dp))
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

@Composable
private fun DrawerContent(
    conversations: List<Conversation>,
    activeId: String,
    onNew: () -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onSettings: () -> Unit,
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 22.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Arena",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "v1.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

            Spacer(Modifier.size(18.dp))

            if (conversations.isNotEmpty()) {
                Text(
                    text = "RECENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 10.dp, bottom = 8.dp),
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(
                    conversations.size,
                    key = { conversations[it].id },
                ) { index ->
                    val conv = conversations[index]
                    val selected = conv.id == activeId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelect(conv.id) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh
                        else androidx.compose.ui.graphics.Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 9.dp, bottom = 9.dp),
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

    if (messages.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState()
        }
        return
    }

    LazyColumn(
        state = scroll,
        modifier = modifier.fillMaxSize(),
        reverseLayout = true,
        contentPadding = PaddingValues(top = 10.dp, bottom = 14.dp),
    ) {
        items(
            count = reversed.size,
            key = { reversed[it].id },
        ) { index ->
            val message = reversed[index]
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

// ---------------------------------------------------------------------------

@Composable
private fun InputBar(
    streaming: Boolean,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }

    var focused by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
    ) {
        Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 10.dp)) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(
                        1.dp,
                        if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier
                            .fillMaxWidth()
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
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
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

                val canSend = text.isNotBlank() && !streaming
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            color = when {
                                streaming -> MaterialTheme.colorScheme.surfaceContainerHighest
                                canSend -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceContainerHigh
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
                    Icon(
                        imageVector = if (streaming) StopSquare else Icons.Filled.KeyboardArrowUp,
                        contentDescription = if (streaming) "Stop" else "Send",
                        tint = when {
                            streaming -> MaterialTheme.colorScheme.onSurface
                            canSend -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.size(if (streaming) 18.dp else 22.dp),
                    )
                }
            }
        }
    }
}
