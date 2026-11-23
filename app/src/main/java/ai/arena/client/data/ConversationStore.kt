package ai.arena.client.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Persists conversations as a small JSON file — no database needed. */
class ConversationStore(private val context: Context) {

    private val file: File get() = File(context.filesDir, "conversations.json")

    fun load(): List<Conversation> = runCatching {
        if (!file.exists()) return emptyList()
        val arr = JSONArray(file.readText())
        (0 until arr.length()).mapNotNull { i ->
            val c = arr.optJSONObject(i) ?: return@mapNotNull null
            val msgsArr = c.optJSONArray("messages") ?: JSONArray()
            val messages = (0 until msgsArr.length()).mapNotNull { j ->
                val m = msgsArr.optJSONObject(j) ?: return@mapNotNull null
                val role = if (m.optString("role") == "user") Role.USER else Role.ASSISTANT
                ChatMessage(
                    id = m.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                    role = role,
                    content = m.optString("content"),
                    thinking = m.optString("thinking"),
                    thinkingExpanded = if (m.has("thinkingExpanded")) m.optBoolean("thinkingExpanded") else null,
                    model = m.optString("model").takeIf { it.isNotBlank() },
                    ts = m.optLong("ts", System.currentTimeMillis()),
                    elapsedMs = if (m.has("elapsedMs")) m.optLong("elapsedMs") else null,
                    tokensIn = if (m.has("tokensIn")) m.optInt("tokensIn") else null,
                    tokensOut = if (m.has("tokensOut")) m.optInt("tokensOut") else null,
                    error = m.optBoolean("error"),
                    errorMessage = m.optString("errorMessage").takeIf { it.isNotBlank() },
                    streaming = false, // never restore a live stream
                )
            }
            Conversation(
                id = c.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                title = c.optString("title"),
                createdAt = c.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = c.optLong("updatedAt", System.currentTimeMillis()),
                messages = messages,
            )
        }
    }.getOrDefault(emptyList())

    fun save(conversations: List<Conversation>) {
        runCatching {
            val arr = JSONArray()
            conversations.forEach { c ->
                val co = JSONObject()
                co.put("id", c.id)
                co.put("title", c.title)
                co.put("createdAt", c.createdAt)
                co.put("updatedAt", c.updatedAt)
                val msgs = JSONArray()
                c.messages.forEach { m ->
                    val mo = JSONObject()
                    mo.put("id", m.id)
                    mo.put("role", if (m.role == Role.USER) "user" else "assistant")
                    mo.put("content", m.content)
                    mo.put("thinking", m.thinking)
                    if (m.thinkingExpanded != null) mo.put("thinkingExpanded", m.thinkingExpanded)
                    m.model?.let { mo.put("model", it) }
                    mo.put("ts", m.ts)
                    m.elapsedMs?.let { mo.put("elapsedMs", it) }
                    m.tokensIn?.let { mo.put("tokensIn", it) }
                    m.tokensOut?.let { mo.put("tokensOut", it) }
                    mo.put("error", m.error)
                    m.errorMessage?.let { mo.put("errorMessage", it) }
                    msgs.put(mo)
                }
                co.put("messages", msgs)
                arr.put(co)
            }
            file.writeText(arr.toString())
        }
    }
}
