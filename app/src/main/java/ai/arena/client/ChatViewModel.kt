package ai.arena.client

import ai.arena.client.data.ApiFormat
import ai.arena.client.data.AppSettings
import ai.arena.client.data.ChatMessage
import ai.arena.client.data.Conversation
import ai.arena.client.data.ConversationStore
import ai.arena.client.data.Role
import ai.arena.client.data.SettingsStore
import ai.arena.client.data.ThinkingMode
import ai.arena.client.data.mergeModels
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
    val models: List<String> = emptyList(),
    val modelsFetched: Boolean = false,
    val onboardingTesting: Boolean = false,
    val onboardingError: String? = null,
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
            // Wait for the real stored settings before revealing the UI —
            // otherwise a default-settings flash could show the onboarding screen.
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
                    it.copy(
                        conn = ConnState.Ok(models.size, ms),
                        models = mergeModels(models),
                        modelsFetched = true,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(conn = ConnState.Fail(e.message ?: "Connection failed")) }
            }
        }
    }

    /** Onboarding: save the key, verify it with a free GET /v1/models call. */
    fun connectWithKey(rawKey: String) {
        val key = rawKey.trim()
        if (key.isBlank()) {
            _ui.update { it.copy(onboardingError = "Enter your API key") }
            return
        }
        val gateway = settings.value.gateway
        viewModelScope.launch {
            _ui.update { it.copy(onboardingTesting = true, onboardingError = null) }
            settingsStore.update { s -> s.copy(apiKey = key) }
            val t0 = SystemClock.elapsedRealtime()
            try {
                val models = client.listModels(key, gateway)
                val ms = SystemClock.elapsedRealtime() - t0
                _ui.update {
                    it.copy(
                        onboardingTesting = false,
                        onboardingError = null,
                        conn = ConnState.Ok(models.size, ms),
                        models = mergeModels(models),
                        modelsFetched = true,
                        screen = Screen.CHAT,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update {
                    it.copy(onboardingTesting = false, onboardingError = e.message ?: "Connection failed")
                }
            }
        }
    }

    fun dismissOnboardingError() {
        _ui.update { it.copy(onboardingError = null) }
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

    private fun runAssistant(conv: Conversation, s: AppSettings) {
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
        if (turns.isEmpty()) return

        val request = StreamRequest(
            gateway = s.gateway,
            apiKey = s.apiKey,
            format = s.format,
            model = s.model,
            system = s.systemPrompt.trim(),
            temperature = s.temperature,
            anthropicThinking = s.anthropicThinking && s.format == ApiFormat.ANTHROPIC,
            turns = turns,
        )

        _ui.update { it.copy(streaming = true) }
        val t0 = SystemClock.elapsedRealtime()
        var failure: String? = null
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
                        is StreamEvent.Error -> failure = event.message
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
                    m.copy(
                        streaming = false,
                        elapsedMs = elapsed,
                        tokensIn = tokensIn,
                        tokensOut = tokensOut,
                        error = failure != null,
                        errorMessage = failure,
                    )
                }
                _ui.update { it.copy(streaming = false) }
                persist()
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
