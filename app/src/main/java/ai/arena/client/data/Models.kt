package ai.arena.client.data

import java.util.UUID

enum class Role { USER, ASSISTANT }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val content: String = "",
    val thinking: String = "",
    /** null = follow the global thinking mode; true/false = per-message override. */
    val thinkingExpanded: Boolean? = null,
    val model: String? = null,
    val ts: Long = System.currentTimeMillis(),
    val elapsedMs: Long? = null,
    val tokensIn: Int? = null,
    val tokensOut: Int? = null,
    val error: Boolean = false,
    val errorMessage: String? = null,
    val streaming: Boolean = false,
)

data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messages: List<ChatMessage> = emptyList(),
) {
    val isEmpty: Boolean get() = messages.isEmpty()
}
