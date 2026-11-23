package ai.arena.client.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ApiFormat(val label: String) {
    OPENAI("OpenAI"),
    ANTHROPIC("Anthropic"),
}

enum class ThinkingMode(val label: String) {
    COLLAPSED("Collapsed"),
    EXPANDED("Expanded"),
    HIDDEN("Hidden"),
}

/** How hard the model reasons. Off = provider default (nothing extra sent). */
enum class ThinkingLevel(val label: String) {
    OFF("Off"),
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
}

data class AppSettings(
    val apiKey: String = DEFAULT_API_KEY,
    val gateway: String = DEFAULT_GATEWAY,
    val model: String = DEFAULT_MODEL,
    val format: ApiFormat = ApiFormat.ANTHROPIC,
    val thinkingMode: ThinkingMode = ThinkingMode.EXPANDED,
    val thinkingLevel: ThinkingLevel = ThinkingLevel.MEDIUM,
    val systemPrompt: String = "",
    val temperature: Float = -1f, // -1 = provider default
) {
    companion object {
        const val DEFAULT_GATEWAY = "https://api.arena.ai"
        const val DEFAULT_MODEL = "coding-router-preview"

        /** Pre-installed key — the app never asks for one. */
        const val DEFAULT_API_KEY =
            "sk-arena-cb484d19ca6b7742f4a7a8c6913c901c64bc9251f0fb4816"
    }
}

private val Context.dataStore by preferencesDataStore(name = "arena_settings")

class SettingsStore(private val context: Context) {

    private object Keys {
        val API_KEY = stringPreferencesKey("api_key")
        val GATEWAY = stringPreferencesKey("gateway")
        val MODEL = stringPreferencesKey("model")
        val FORMAT = stringPreferencesKey("format")
        val THINKING_MODE = stringPreferencesKey("thinking_mode")
        val THINKING_LEVEL = stringPreferencesKey("thinking_level")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val MIGRATED_V2 = booleanPreferencesKey("migrated_v2")
    }

    /** One-time v2 defaults: thinking shown, medium level, Anthropic format, built-in key. */
    suspend fun migrateV2IfNeeded() {
        context.dataStore.edit { p ->
            if (p[Keys.MIGRATED_V2] != true) {
                val blank = (p[Keys.API_KEY] ?: "").isBlank()
                if (blank) p[Keys.API_KEY] = AppSettings.DEFAULT_API_KEY
                p[Keys.THINKING_MODE] = ThinkingMode.EXPANDED.name
                p[Keys.THINKING_LEVEL] = ThinkingLevel.MEDIUM.name
                p[Keys.FORMAT] = ApiFormat.ANTHROPIC.name
                p[Keys.MIGRATED_V2] = true
            }
        }
    }

    val flow: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            apiKey = (p[Keys.API_KEY] ?: "").ifBlank { AppSettings.DEFAULT_API_KEY },
            gateway = p[Keys.GATEWAY] ?: AppSettings.DEFAULT_GATEWAY,
            model = p[Keys.MODEL] ?: AppSettings.DEFAULT_MODEL,
            format = p[Keys.FORMAT]?.let { runCatching { ApiFormat.valueOf(it) }.getOrNull() } ?: ApiFormat.OPENAI,
            thinkingMode = p[Keys.THINKING_MODE]?.let { runCatching { ThinkingMode.valueOf(it) }.getOrNull() }
                ?: ThinkingMode.COLLAPSED,
            thinkingLevel = p[Keys.THINKING_LEVEL]?.let { runCatching { ThinkingLevel.valueOf(it) }.getOrNull() }
                ?: ThinkingLevel.OFF,
            systemPrompt = p[Keys.SYSTEM_PROMPT] ?: "",
            temperature = p[Keys.TEMPERATURE] ?: -1f,
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { p ->
            val current = AppSettings(
                apiKey = (p[Keys.API_KEY] ?: "").ifBlank { AppSettings.DEFAULT_API_KEY },
                gateway = p[Keys.GATEWAY] ?: AppSettings.DEFAULT_GATEWAY,
                model = p[Keys.MODEL] ?: AppSettings.DEFAULT_MODEL,
                format = p[Keys.FORMAT]?.let { runCatching { ApiFormat.valueOf(it) }.getOrNull() } ?: ApiFormat.OPENAI,
                thinkingMode = p[Keys.THINKING_MODE]?.let { runCatching { ThinkingMode.valueOf(it) }.getOrNull() }
                    ?: ThinkingMode.COLLAPSED,
                thinkingLevel = p[Keys.THINKING_LEVEL]?.let { runCatching { ThinkingLevel.valueOf(it) }.getOrNull() }
                    ?: ThinkingLevel.OFF,
                systemPrompt = p[Keys.SYSTEM_PROMPT] ?: "",
                temperature = p[Keys.TEMPERATURE] ?: -1f,
            )
            val next = transform(current)
            p[Keys.API_KEY] = next.apiKey.ifBlank { AppSettings.DEFAULT_API_KEY }
            p[Keys.GATEWAY] = next.gateway.ifBlank { AppSettings.DEFAULT_GATEWAY }
            p[Keys.MODEL] = next.model.ifBlank { AppSettings.DEFAULT_MODEL }
            p[Keys.FORMAT] = next.format.name
            p[Keys.THINKING_MODE] = next.thinkingMode.name
            p[Keys.THINKING_LEVEL] = next.thinkingLevel.name
            p[Keys.SYSTEM_PROMPT] = next.systemPrompt
            p[Keys.TEMPERATURE] = next.temperature
        }
    }
}

/** Trailing "/v1" or slashes are trimmed so paths can be appended safely. */
fun normalizeGateway(raw: String): String {
    var g = raw.trim().trimEnd('/')
    if (g.endsWith("/v1")) g = g.dropLast(3).trimEnd('/')
    return g.ifBlank { AppSettings.DEFAULT_GATEWAY }
}
