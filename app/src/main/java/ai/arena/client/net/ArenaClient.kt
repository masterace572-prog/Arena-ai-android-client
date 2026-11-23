package ai.arena.client.net

import ai.arena.client.data.ApiFormat
import ai.arena.client.data.ThinkingLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

sealed interface StreamEvent {
    data class Thinking(val text: String) : StreamEvent
    data class Content(val text: String) : StreamEvent
    data class Usage(val input: Int? = null, val output: Int? = null) : StreamEvent
    data class Error(val message: String, val code: Int? = null) : StreamEvent
    data object Done : StreamEvent
}

data class ChatTurn(
    val role: String,
    val content: String,
)

data class StreamRequest(
    val gateway: String,
    val apiKey: String,
    val format: ApiFormat,
    val model: String,
    val system: String,
    val temperature: Float,
    val thinkingLevel: ThinkingLevel,
    val turns: List<ChatTurn>,
)

class ArenaClient {

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    // Short-lived calls (model list): bounded read.
    private val apiHttp = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Streaming: read must never time out mid-generation.
    private val streamHttp = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /** GET /v1/models — listing costs nothing (0 credits). */
    suspend fun listModels(apiKey: String, gateway: String): List<String> =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            val url = "${normalizeGateway(gateway)}/v1/models"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $apiKey")
                .get()
                .build()
            apiHttp.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw ArenaException(errorMessage(response.code, body, response.header("Retry-After")))
                }
                val data = runCatching { JSONObject(body).optJSONArray("data") }.getOrNull() ?: JSONArray()
                (0 until data.length()).mapNotNull { i ->
                    data.optJSONObject(i)?.optString("id")?.takeIf { it.isNotBlank() }
                }
            }
        }

    fun streamChat(req: StreamRequest): Flow<StreamEvent> {
        val url = when (req.format) {
            ApiFormat.OPENAI -> "${normalizeGateway(req.gateway)}/v1/chat/completions"
            ApiFormat.ANTHROPIC -> "${normalizeGateway(req.gateway)}/v1/messages"
        }
        val payload = when (req.format) {
            ApiFormat.OPENAI -> openAiBody(req)
            ApiFormat.ANTHROPIC -> anthropicBody(req)
        }
        val headers = if (req.format == ApiFormat.ANTHROPIC) {
            mapOf(
                "x-api-key" to req.apiKey,
                "anthropic-version" to "2023-06-01",
            )
        } else {
            mapOf("Authorization" to "Bearer ${req.apiKey}")
        }
        val builder = Request.Builder().url(url).post(payload.toRequestBody(jsonType))
        headers.forEach { (k, v) -> builder.header(k, v) }
        val request = builder.build()
        val parser = when (req.format) {
            ApiFormat.OPENAI -> ::parseOpenAi
            ApiFormat.ANTHROPIC -> ::parseAnthropic
        }
        return executeStream(request, parser)
    }

    private fun openAiBody(req: StreamRequest): String {
        val messages = JSONArray()
        if (req.system.isNotBlank()) {
            messages.put(JSONObject().put("role", "system").put("content", req.system))
        }
        req.turns.forEach { t ->
            messages.put(JSONObject().put("role", t.role).put("content", t.content))
        }
        val body = JSONObject()
            .put("model", req.model)
            .put("messages", messages)
            .put("stream", true)
        if (req.temperature >= 0f) body.put("temperature", req.temperature.toDouble())
        // Thinking level (Off sends nothing — provider default).
        if (req.thinkingLevel != ThinkingLevel.OFF) {
            body.put("reasoning_effort", req.thinkingLevel.name.lowercase())
        }
        return body.toString()
    }

    private fun anthropicBody(req: StreamRequest): String {
        val messages = JSONArray()
        req.turns.forEach { t ->
            // Anthropic requires non-empty blocks.
            if (t.content.isNotBlank()) {
                messages.put(JSONObject().put("role", t.role).put("content", t.content))
            }
        }
        val body = JSONObject()
            .put("model", req.model)
            .put("max_tokens", 8192)
            .put("messages", messages)
            .put("stream", true)
        if (req.system.isNotBlank()) body.put("system", req.system)
        if (req.temperature >= 0f) body.put("temperature", req.temperature.toDouble())
        // Thinking level → Anthropic extended-thinking budget (must stay < max_tokens).
        val budget = when (req.thinkingLevel) {
            ThinkingLevel.OFF -> 0
            ThinkingLevel.LOW -> 1024
            ThinkingLevel.MEDIUM -> 4096
            ThinkingLevel.HIGH -> 6144
        }
        if (budget > 0) {
            body.put(
                "thinking",
                JSONObject().put("type", "enabled").put("budget_tokens", budget),
            )
        }
        return body.toString()
    }

    /**
     * Blocking SSE reader on a child coroutine; [awaitClose] cancels the call the
     * moment the collector goes away, which unblocks the socket read immediately.
     */
    private fun executeStream(
        request: Request,
        parse: (String) -> List<StreamEvent>,
    ): Flow<StreamEvent> = channelFlow {
        val call = streamHttp.newCall(request)
        val lastActivity = java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis())
        launch(Dispatchers.IO) {
            var failed = false
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        failed = true
                        val body = response.body?.string().orEmpty()
                        emitSafely(
                            StreamEvent.Error(
                                message = errorMessage(response.code, body, response.header("Retry-After")),
                                code = response.code,
                            ),
                        )
                        return@use
                    }
                    val source = response.body?.source() ?: return@use
                    val dataLines = mutableListOf<String>()
                    var done = false
                    while (!done) {
                        val line = source.readUtf8Line() ?: break
                        lastActivity.set(System.currentTimeMillis())
                        when {
                            line.startsWith("data:") -> dataLines.add(line.substring(5).trim())
                            line.isEmpty() -> {
                                if (dataLines.isEmpty()) continue
                                val payload = dataLines.joinToString("\n")
                                dataLines.clear()
                                if (payload == "[DONE]") {
                                    done = true
                                } else {
                                    parse(payload).forEach { emitSafely(it) }
                                }
                            }
                            else -> { // event:, id:, retry:, comments
                            }
                        }
                    }
                    // Gateways that skip the trailing blank line would otherwise
                    // drop the final chunk — flush whatever is pending.
                    if (dataLines.isNotEmpty()) {
                        val payload = dataLines.joinToString("\n")
                        dataLines.clear()
                        if (payload != "[DONE]") parse(payload).forEach { emitSafely(it) }
                    }
                    if (!failed) emitSafely(StreamEvent.Done)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                if (!call.isCanceled() && !failed) {
                    emitSafely(StreamEvent.Error("Network error: ${e.message ?: "connection lost"}"))
                }
            } catch (e: Exception) {
                if (!failed) emitSafely(StreamEvent.Error(e.message ?: "Unexpected error"))
            } finally {
                this@channelFlow.close()
            }
        }
        // Never hang forever: if the socket goes quiet for two minutes, surface
        // an error and cancel so the user sees Retry instead of an endless pulse.
        val watchdog = launch {
            while (true) {
                kotlinx.coroutines.delay(15_000)
                if (System.currentTimeMillis() - lastActivity.get() > 120_000) {
                    emitSafely(
                        StreamEvent.Error(
                            "No response for 2 minutes — the model may be busy. Retry.",
                        ),
                    )
                    call.cancel()
                    break
                }
            }
        }
        awaitClose {
            call.cancel()
            watchdog.cancel()
        }
    }

    // ---- OpenAI chat-completions SSE ----
    private fun parseOpenAi(payload: String): List<StreamEvent> {
        val obj = runCatching { JSONObject(payload) }.getOrNull() ?: return emptyList()
        obj.optJSONObject("error")?.let { e ->
            val msg = e.optString("message").ifBlank { e.optString("type") }
            val code = if (e.optString("code").contains("invalid_request")) 400 else null
            return listOf(StreamEvent.Error(msg.ifBlank { "Request failed" }, code))
        }
        val events = mutableListOf<StreamEvent>()
        obj.optJSONObject("usage")?.let { u ->
            events.add(
                StreamEvent.Usage(
                    input = u.optInt("prompt_tokens", -1).takeIf { it >= 0 },
                    output = u.optInt("completion_tokens", -1).takeIf { it >= 0 },
                ),
            )
        }
        val choices = obj.optJSONArray("choices") ?: return events
        if (choices.length() == 0) return events
        val delta = choices.optJSONObject(0)?.optJSONObject("delta") ?: return events
        val reasoning = delta.optString("reasoning_content").ifBlank { delta.optString("reasoning") }
            .ifBlank { delta.optString("thinking") }
        if (reasoning.isNotBlank()) events.add(StreamEvent.Thinking(reasoning))
        val content = delta.optString("content")
        if (content.isNotBlank()) events.add(StreamEvent.Content(content))
        return events
    }

    // ---- Anthropic messages SSE ----
    private fun parseAnthropic(payload: String): List<StreamEvent> {
        val obj = runCatching { JSONObject(payload) }.getOrNull() ?: return emptyList()
        return when (obj.optString("type")) {
            "message_start" -> {
                val input = obj.optJSONObject("message")
                    ?.optJSONObject("usage")?.optInt("input_tokens", -1)?.takeIf { it >= 0 }
                listOfNotNull<StreamEvent>(input?.let { StreamEvent.Usage(input = it) })
            }
            "content_block_start" -> emptyList() // block kind is implied by each delta
            "content_block_delta" -> {
                val delta = obj.optJSONObject("delta") ?: return emptyList()
                when (delta.optString("type")) {
                    "thinking_delta" -> listOf(StreamEvent.Thinking(delta.optString("thinking")))
                    "input_text_delta", "text_delta" -> {
                        val t = delta.optString("text")
                        if (t.isNotBlank()) listOf(StreamEvent.Content(t)) else emptyList()
                    }
                    else -> emptyList()
                }
            }
            "message_delta" -> {
                val output = obj.optJSONObject("usage")?.optInt("output_tokens", -1)?.takeIf { it >= 0 }
                val stop = obj.optJSONObject("delta")?.optString("stop_reason").orEmpty()
                val events = mutableListOf<StreamEvent>()
                output?.let { events.add(StreamEvent.Usage(output = it)) }
                if (stop == "refusal" || stop == "error") {
                    events.add(StreamEvent.Error("Model stopped: $stop"))
                }
                events
            }
            "error" -> {
                val err = obj.optJSONObject("error")
                val msg = err?.optString("message")?.takeIf { it.isNotBlank() } ?: "Request failed"
                val type = err?.optString("type").orEmpty()
                val code = if (type.contains("invalid_request")) 400 else null
                listOf(StreamEvent.Error(msg, code))
            }
            else -> emptyList() // ping, content_block_stop, message_stop
        }
    }
}

class ArenaException(message: String) : Exception(message)

/**
 * Suspends when the collector is slow (real backpressure instead of dropping),
 * and quietly ignores events after the channel has closed.
 */
private suspend fun kotlinx.coroutines.channels.ProducerScope<StreamEvent>.emitSafely(event: StreamEvent) {
    try {
        send(event)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        // Channel closed — nothing left to deliver.
    }
}

internal fun normalizeGateway(raw: String): String {
    var g = raw.trim().trimEnd('/')
    if (g.endsWith("/v1")) g = g.dropLast(3).trimEnd('/')
    return g.ifBlank { "https://api.arena.ai" }
}

internal fun errorMessage(code: Int, body: String, retryAfter: String?): String {
    val serverMsg = runCatching {
        val o = JSONObject(body)
        o.optJSONObject("error")?.let { e ->
            e.optString("message").ifBlank { e.optString("type") }
        }?.takeIf { it.isNotBlank() }
            ?: o.optString("message").takeIf { it.isNotBlank() }
            ?: o.optString("detail").takeIf { it.isNotBlank() }
    }.getOrNull()

    return when (code) {
        401 -> "Invalid API key — check it on the Keys page."
        402 -> "Not enough credits on this workspace."
        403 -> serverMsg ?: "Model not allowed for this key."
        404 -> serverMsg ?: "Endpoint not found."
        429 -> buildString {
            append("Rate limited")
            if (!retryAfter.isNullOrBlank()) append(" — retry in ${retryAfter}s")
            else append(" — try again shortly")
        }
        in 500..599 -> "Arena server error ($code). Try again."
        else -> serverMsg ?: "Request failed ($code)."
    }
}
