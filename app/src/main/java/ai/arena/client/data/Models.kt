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

/**
 * Direct-model suggestions, ordered roughly by leaderboard rank (Nov 2026).
 * The live list from GET /v1/models is merged with these at runtime, so the
 * app always reflects what the key can actually call.
 */
val PRESET_MODELS: List<String> = listOf(
    "coding-router-preview",
    "claude-fable-5.2-max",
    "gpt-6.2-astra-max",
    "claude-fable-5.1-max",
    "gpt-6-astra-max",
    "claude-opus-5.5-high",
    "gpt-6.2-sol-max",
    "claude-opus-5-high",
    "gpt-6.1-sol-max",
    "gpt-6-luna-max",
    "kimi-k3.1-pro",
    "qwen3.8-max",
)

private val TIERS = setOf("max", "xhigh", "high", "medium", "low")

private val WORD_MAP = mapOf(
    "gpt" to "GPT",
    "glm" to "GLM",
    "qwen" to "Qwen",
    "kimi" to "Kimi",
    "claude" to "Claude",
    "sonnet" to "Sonnet",
    "opus" to "Opus",
    "fable" to "Fable",
    "astra" to "Astra",
    "sol" to "Sol",
    "luna" to "Luna",
    "flash" to "Flash",
    "pro" to "Pro",
    "router" to "Router",
    "preview" to "Preview",
    "coding" to "Coding",
)

data class ModelDisplay(
    val id: String,
    val label: String,
    val tier: String?,
    val isRouter: Boolean,
)

fun modelDisplay(id: String): ModelDisplay {
    if (id == "coding-router-preview") {
        return ModelDisplay(id, "Arena Router", "Router", true)
    }
    val tier = id.substringAfterLast('-', "").takeIf { it in TIERS }
    val base = if (tier != null) id.substringBeforeLast('-') else id
    val label = base.split('-', '.', '_', ' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            WORD_MAP[token.lowercase()]
                ?: token.replaceFirstChar { c -> c.uppercaseChar() }
        }
        .ifBlank { id }
    return ModelDisplay(id, label, tier?.uppercase(), false)
}

/** Presets first (deduped), then whatever the key can call. */
fun mergeModels(fetched: List<String>): List<String> {
    val seen = LinkedHashSet<String>()
    PRESET_MODELS.forEach { seen.add(it) }
    fetched.forEach { seen.add(it) }
    return seen.toList()
}
