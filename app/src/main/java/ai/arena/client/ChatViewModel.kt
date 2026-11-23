package ai.arena.client

import ai.arena.client.data.AppSettings
import ai.arena.client.data.ChatMessage
import ai.arena.client.data.Conversation
import ai.arena.client.data.ConversationStore
import ai.arena.client.data.Role
import ai.arena.client.data.SettingsStore
import ai.arena.client.data.ThinkingLevel
import ai.arena.client.data.ThinkingMode
import ai.arena.client.net.ArenaClient
import ai.arena.client.net.ChatTurn
import ai.arena.client.net.StreamEvent
import ai.arena.client.net.StreamRequest
import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class Screen { CHAT, SETTINGS }

sealed interface ConnState {
    data object Unknown : ConnState
    data object Testing : ConnState
    data class Ok(val models: Int, val ms: Long) : ConnState
    data class Fail(val message: String) : ConnState
}

data class UiState(
    val ready: Boolean = false,
    val hasKey: Boolean = false,
    val conversations: List<Conversation> = emptyList(),
    val activeId: String = "",
    val screen: Screen = Screen.CHAT,
    val streaming: Boolean = false,
    val conn: ConnState = ConnState.Unknown,
    val notice: String? = null,
)

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsStore = SettingsStore(app)
    private val convoStore = ConversationStore(app)
    private val client = ArenaClient()

    val settings: StateFlow<AppSettings> = settingsStore.flow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var streamJob: Job? = null

    init {
        viewModelScope.launch {
            // v2: built-in key + thinking defaults applied once, then load.
            settingsStore.migrateV2IfNeeded()
            val stored = settingsStore.flow.first()
            val loaded = convoStore.load()
            _ui.update {
                it.copy(
                    ready = true,
                    hasKey = stored.apiKey.isNotBlank(),
                    conversations = loaded,
                    activeId = loaded.lastOrNull()?.id ?: "",
                )
            }
            if (stored.apiKey.isNotBlank()) {
                testConnection()
            }
        }
        viewModelScope.launch {
            settingsStore.flow.collect { s ->
                _ui.update { it.copy(hasKey = s.apiKey.isNotBlank()) }
            }
        }
    }

    val activeConversation: Conversation?
        get() = _ui.value.conversations.find { it.id == _ui.value.activeId }

    // ---------- navigation ----------

    fun setScreen(screen: Screen) {
        _ui.update { it.copy(screen = screen) }
    }

    // ---------- settings ----------

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsStore.update(transform) }
    }

    // ---------- connection / models ----------

    fun testConnection() {
        val s = settings.value
        if (s.apiKey.isBlank()) {
            _ui.update { it.copy(conn = ConnState.Fail("No API key set")) }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(conn = ConnState.Testing) }
            val t0 = SystemClock.elapsedRealtime()
            try {
                val models = client.listModels(s.apiKey, s.gateway)
                val ms = SystemClock.elapsedRealtime() - t0
                _ui.update {
                    it.copy(conn = ConnState.Ok(models.size, ms))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(conn = ConnState.Fail(e.message ?: "Connection failed")) }
            }
        }
    }

    // ---------- conversations ----------

    fun newChat() {
        val current = activeConversation
        if (current != null && current.isEmpty) {
            _ui.update { it.copy(screen = Screen.CHAT) }
            return
        }
        val conv = Conversation()
        _ui.update {
            it.copy(
                conversations = it.conversations + conv,
                activeId = conv.id,
                screen = Screen.CHAT,
            )
        }
        persist()
    }

    fun selectChat(id: String) {
        _ui.update { it.copy(activeId = id, screen = Screen.CHAT) }
    }

    fun deleteChat(id: String) {
        _ui.update { st ->
            val remaining = st.conversations.filterNot { it.id == id }
            val nextActive = when {
                st.activeId != id -> st.activeId
                else -> remaining.lastOrNull()?.id ?: ""
            }
            st.copy(conversations = remaining, activeId = nextActive)
        }
        persist()
    }

    private fun replaceConversation(conv: Conversation) {
        _ui.update { st ->
            st.copy(
                conversations = st.conversations.map { if (it.id == conv.id) conv else it },
                activeId = conv.id,
            )
        }
    }

    private fun persist() {
        convoStore.save(_ui.value.conversations)
    }

    // ---------- chat ----------

    fun send(raw: String) {
        val text = raw.trim()
        if (text.isEmpty() || _ui.value.streaming) return
        val s = settings.value
        if (s.apiKey.isBlank()) {
            _ui.update { it.copy(screen = Screen.CHAT) }
            return
        }
        var conv = activeConversation
        if (conv == null) {
            conv = Conversation()
            _ui.update { it.copy(conversations = it.conversations + conv, activeId = conv.id) }
        }
        val userMsg = ChatMessage(role = Role.USER, content = text)
        conv = conv.copy(
            title = conv.title.ifBlank { text.take(48) },
            messages = conv.messages + userMsg,
            updatedAt = System.currentTimeMillis(),
        )
        replaceConversation(conv)
        runAssistant(conv, s)
    }

    fun stop() {
        streamJob?.cancel()
    }

    fun regenerate() {
        if (_ui.value.streaming) return
        val conv = activeConversation ?: return
        var msgs = conv.messages
        while (msgs.isNotEmpty() && msgs.last().role == Role.ASSISTANT) {
            msgs = msgs.dropLast(1)
        }
        if (msgs.isEmpty() || msgs.last().role != Role.USER) return
        val updated = conv.copy(messages = msgs, updatedAt = System.currentTimeMillis())
        replaceConversation(updated)
        runAssistant(updated, settings.value)
    }

    fun toggleThinking(messageId: String) {
        val convId = _ui.value.activeId
        val mode = settings.value.thinkingMode
        mutateMessage(convId, messageId) { m ->
            val current = m.thinkingExpanded ?: (mode == ThinkingMode.EXPANDED)
            m.copy(thinkingExpanded = !current)
        }
        persist()
    }

    /**
     * Always tells the model today's date so "what's today's date?" and
     * anything time-sensitive works without the user having to say it.
     */
    private fun buildSystemPrompt(s: AppSettings): String {
        val today = java.time.LocalDate.now().format(
            java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", java.util.Locale.ENGLISH),
        )
        return listOfNotNull(
            "Today's date is $today.",
            if (s.webSearch) {
                "You have live web search. Use it whenever the user asks about " +
                    "anything current — news, release dates, prices, scores, " +
                    "recent events — and cite your sources."
            } else {
                null
            },
            s.systemPrompt.trim().takeIf { it.isNotBlank() },
        ).joinToString("\n\n")
    }

    fun clearNotice() {
        _ui.update { it.copy(notice = null) }
    }

    private fun runAssistant(
        conv: Conversation,
        s: AppSettings,
        level: ThinkingLevel = s.thinkingLevel,
        retryStep: Int = 0,
    ) {
        val assistant = ChatMessage(
            role = Role.ASSISTANT,
            streaming = true,
            model = s.model,
        )
        val withAssistant = conv.copy(
            messages = conv.messages + assistant,
            updatedAt = System.currentTimeMillis(),
        )
        replaceConversation(withAssistant)

        val turns = normalizeTurns(withAssistant.messages)
        if (turns.isEmpty()) {
            mutateMessage(withAssistant.id, assistant.id) {
                it.copy(
                    streaming = false,
                    elapsedMs = 0,
                    error = true,
                    errorMessage = "Nothing to send",
                )
            }
            _ui.update { it.copy(streaming = false) }
            return
        }

        val request = StreamRequest(
            gateway = s.gateway,
            apiKey = s.apiKey,
            format = s.format,
            model = s.model,
            system = buildSystemPrompt(s),
            temperature = s.temperature,
            thinkingLevel = level,
            webSearch = s.webSearch,
            turns = turns,
        )

        _ui.update { it.copy(streaming = true) }
        val t0 = SystemClock.elapsedRealtime()
        var failure: String? = null
        var failureCode: Int? = null
        var tokensIn: Int? = null
        var tokensOut: Int? = null

        streamJob = viewModelScope.launch {
            try {
                client.streamChat(request).collect { event ->
                    when (event) {
                        is StreamEvent.Thinking -> mutateMessage(withAssistant.id, assistant.id) {
                            it.copy(thinking = it.thinking + event.text)
                        }
                        is StreamEvent.Content -> mutateMessage(withAssistant.id, assistant.id) {
                            it.copy(content = it.content + event.text)
                        }
                        is StreamEvent.Usage -> {
                            event.input?.let { tokensIn = it }
                            event.output?.let { tokensOut = it }
                        }
                        is StreamEvent.Error -> {
                            failure = event.message
                            failureCode = event.code
                        }
                        StreamEvent.Done -> Unit
                    }
                }
            } catch (e: CancellationException) {
                // Stop button pressed — finish quietly as a normal completion.
            } catch (e: Exception) {
                if (failure == null) failure = e.message ?: "Connection failed"
            } finally {
                val elapsed = SystemClock.elapsedRealtime() - t0
                mutateMessage(withAssistant.id, assistant.id) { m ->
                    // A stream that ended with nothing is a failure the user can retry,
                    // not a silent empty bubble.
                    val noReply = failure == null && m.content.isBlank() && m.thinking.isBlank()
                    m.copy(
                        streaming = false,
                        elapsedMs = elapsed,
                        tokensIn = tokensIn,
                        tokensOut = tokensOut,
                        error = failure != null || noReply,
                        errorMessage = failure
                            ?: if (noReply) "No response received — tap Retry" else null,
                    )
                }
                _ui.update { it.copy(streaming = false) }
                persist()

                // A 400 means the model/gateway rejected a request feature.
                // Strip ONE feature per retry (keyword-aware when the error
                // says which), max two attempts — so a thinking rejection
                // never silently kills web search, and vice versa.
                val fail400 = failure != null && failureCode == 400
                if (fail400 && retryStep < 2) {
                    val msgL = (failure ?: "").lowercase()
                    val toolsBad = msgL.contains("tool") || msgL.contains("web_search")
                    val thinkingBad = msgL.contains("thinking") ||
                        msgL.contains("reasoning") || msgL.contains("effort") ||
                        msgL.contains("budget")
                    var stripThinking = false
                    var stripTools = false
                    when {
                        retryStep == 0 && toolsBad && s.webSearch -> stripTools = true
                        retryStep == 0 && thinkingBad && level != ThinkingLevel.OFF -> stripThinking = true
                        retryStep == 0 && level != ThinkingLevel.OFF -> stripThinking = true
                        retryStep == 0 && s.webSearch -> stripTools = true
                        retryStep == 1 && level != ThinkingLevel.OFF -> stripThinking = true
                        retryStep == 1 && s.webSearch -> stripTools = true
                    }
                    val shouldRetry = stripThinking || stripTools
                    if (shouldRetry) {
                        // Tell the user when a feature had to be dropped — never
                        // silently claim thinking/search was active.
                        _ui.update {
                            it.copy(
                                notice = when {
                                    stripThinking -> "This model doesn't support thinking — sent without it."
                                    else -> "Web search isn't available for this model — answered without it."
                                },
                            )
                        }
                    }
                    if (shouldRetry) {
                    val convNow = _ui.value.conversations.find { it.id == withAssistant.id }
                    if (convNow != null) {
                        var msgs = convNow.messages
                        while (msgs.isNotEmpty() && msgs.last().role == Role.ASSISTANT) {
                            msgs = msgs.dropLast(1)
                        }
                        val cleaned = convNow.copy(
                            messages = msgs,
                            updatedAt = System.currentTimeMillis(),
                        )
                        replaceConversation(cleaned)
                        runAssistant(
                            cleaned,
                            s.copy(webSearch = if (stripTools) false else s.webSearch),
                            level = if (stripThinking) ThinkingLevel.OFF else level,
                            retryStep = retryStep + 1,
                        )
                    }
                }
                }
            }
        }
    }

    /** Merge adjacent same-role turns (safe for Anthropic) and drop empty ones. */
    private fun normalizeTurns(messages: List<ChatMessage>): List<ChatTurn> {
        val raw = messages
            .filter { it.content.isNotBlank() }
            .map { ChatTurn(if (it.role == Role.USER) "user" else "assistant", it.content) }
        val merged = mutableListOf<ChatTurn>()
        raw.forEach { t ->
            val last = merged.lastOrNull()
            if (last != null && last.role == t.role) {
                merged[merged.size - 1] = ChatTurn(t.role, last.content + "\n\n" + t.content)
            } else {
                merged.add(t)
            }
        }
        while (merged.isNotEmpty() && merged.first().role == "assistant") {
            merged.removeAt(0)
        }
        return merged
    }

    private fun mutateMessage(convId: String, messageId: String, fn: (ChatMessage) -> ChatMessage) {
        _ui.update { st ->
            st.copy(
                conversations = st.conversations.map { c ->
                    if (c.id != convId) {
                        c
                    } else {
                        c.copy(
                            updatedAt = System.currentTimeMillis(),
                            messages = c.messages.map { m ->
                                if (m.id == messageId) fn(m) else m
                            },
                        )
                    }
                },
            )
        }
    }
}
