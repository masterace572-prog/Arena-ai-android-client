@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ai.arena.client.ui

import ai.arena.client.ChatViewModel
import ai.arena.client.ConnState
import ai.arena.client.Screen
import ai.arena.client.data.ApiFormat
import ai.arena.client.data.ThinkingMode
import ai.arena.client.data.normalizeGateway
import androidx.activity.compose.BackHandler
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import java.util.Locale

// ---------------------------------------------------------------------------
// Settings — GENERAL / THINKING / CHAT / CONNECTION / BILLING / ABOUT
// One source of truth per setting; reasoning level lives in the composer.
// ---------------------------------------------------------------------------

@Composable
fun SettingsScreen(vm: ChatViewModel) {
    val ui by vm.ui.collectAsState()
    val settings by vm.settings.collectAsState()

    BackHandler { vm.setScreen(Screen.CHAT) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { vm.setScreen(Screen.CHAT) }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                SectionBlock("General") {
                    GeneralCard(
                        webSearch = settings.webSearch,
                        systemPrompt = settings.systemPrompt,
                        temperature = settings.temperature,
                        onWebSearch = { w -> vm.updateSettings { it.copy(webSearch = w) } },
                        onSystem = { s -> vm.updateSettings { it.copy(systemPrompt = s) } },
                        onTemperature = { t -> vm.updateSettings { it.copy(temperature = t) } },
                    )
                }
            }
            item {
                SectionBlock("Thinking") {
                    ThinkingCard(
                        mode = settings.thinkingMode,
                        onMode = { m -> vm.updateSettings { it.copy(thinkingMode = m) } },
                    )
                }
            }
            item {
                SectionBlock("Chat") {
                    ChatCard(
                        format = settings.format,
                        onFormat = { f -> vm.updateSettings { it.copy(format = f) } },
                    )
                }
            }
            item {
                SectionBlock("Connection") { ConnectionCard(vm, ui.conn) }
            }
            item {
                SectionBlock("Billing") { BillingCard() }
            }
            item {
                SectionBlock("About") { AboutCard() }
            }
        }
    }
}

@Composable
private fun SectionBlock(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        content()
    }
}

// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// GENERAL — web search, system prompt, temperature
// ---------------------------------------------------------------------------

@Composable
private fun GeneralCard(
    webSearch: Boolean,
    systemPrompt: String,
    temperature: Float,
    onWebSearch: (Boolean) -> Unit,
    onSystem: (String) -> Unit,
    onTemperature: (Float) -> Unit,
) {
    var promptDraft by remember(systemPrompt) { mutableStateOf(systemPrompt) }
    val sliderValue = if (temperature < 0f) 0.7f else temperature

    SectionCard(title = "General") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Web search",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.size(2.dp))
                Text(
                    text = "Lets the model look things up online. Falls back automatically if a model can't.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.size(12.dp))
            Switch(
                checked = webSearch,
                onCheckedChange = onWebSearch,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }

        Spacer(Modifier.size(16.dp))

        OutlinedTextField(
            value = promptDraft,
            onValueChange = { v ->
                promptDraft = v
                onSystem(v)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("System prompt (optional)") },
            minLines = 2,
            maxLines = 5,
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors(),
        )

        Spacer(Modifier.size(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Temperature",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (temperature < 0f) "Auto"
                else String.format(Locale.US, "%.2f", temperature),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (temperature >= 0f) {
                TextButton(onClick = { onTemperature(-1f) }) {
                    Text("Reset", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Slider(
            value = sliderValue,
            onValueChange = onTemperature,
            valueRange = 0f..2f,
        )
    }
}

// ---------------------------------------------------------------------------
// THINKING — display mode only (reasoning level lives in the composer)
// ---------------------------------------------------------------------------

@Composable
private fun ThinkingCard(
    mode: ThinkingMode,
    onMode: (ThinkingMode) -> Unit,
) {
    SectionCard(
        title = "Thinking display",
        caption = "How reasoning is shown. The reasoning level (Off / Low / Medium / High) is selected in the composer, above the message field.",
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThinkingMode.entries.forEach { m ->
                SelectPill(
                    text = m.label,
                    selected = mode == m,
                    onClick = { onMode(m) },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// CHAT — API format + fixed router model
// ---------------------------------------------------------------------------

@Composable
private fun ChatCard(
    format: ApiFormat,
    onFormat: (ApiFormat) -> Unit,
) {
    SectionCard(title = "Chat") {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Sparkle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Arena Router",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.size(2.dp))
                    Text(
                        text = "coding-router-preview \u00B7 auto-picks the best model",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.size(16.dp))

        Text(
            text = "API format",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.size(4.dp))
        Text(
            text = "Anthropic returns thinking blocks natively.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectPill(
                text = "OpenAI",
                selected = format == ApiFormat.OPENAI,
                onClick = { onFormat(ApiFormat.OPENAI) },
            )
            SelectPill(
                text = "Anthropic",
                selected = format == ApiFormat.ANTHROPIC,
                onClick = { onFormat(ApiFormat.ANTHROPIC) },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// CONNECTION
// ---------------------------------------------------------------------------

@Composable
private fun ConnectionCard(vm: ChatViewModel, conn: ConnState) {
    val settings by vm.settings.collectAsState()
    var gatewayDraft by remember(settings.gateway) { mutableStateOf(settings.gateway) }
    val testing = conn is ConnState.Testing

    SectionCard(
        title = "Connection",
        caption = "GET /v1/models only — free, 0 credits spent.",
    ) {
        OutlinedTextField(
            value = gatewayDraft,
            onValueChange = { v ->
                gatewayDraft = v
                vm.updateSettings { it.copy(gateway = normalizeGateway(v)) }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Gateway URL") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
            colors = fieldColors(),
        )

        Spacer(Modifier.size(14.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Button(
                onClick = vm::testConnection,
                enabled = !testing,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                if (testing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Test")
                }
            }

            when (conn) {
                is ConnState.Ok -> HintRow {
                    StatusDot(conn)
                    Text(
                        text = "Connected \u00B7 ${conn.models} models \u00B7 ${conn.ms} ms",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is ConnState.Fail -> HintRow {
                    StatusDot(conn)
                    Text(
                        text = conn.message,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                    )
                }
                is ConnState.Testing -> Text(
                    text = "Testing\u2026",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is ConnState.Unknown -> Text(
                    text = "Not tested yet",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// BILLING — live balance lives in the Arena dashboard (no public API)
// ---------------------------------------------------------------------------

@Composable
private fun BillingCard() {
    val context = LocalContext.current

    SectionCard(
        title = "Balance",
        caption = "Your live balance and spend are shown in the Arena dashboard, so the numbers are always accurate.",
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://platform.arena.ai/dashboard")),
                        )
                    }
                }
                .padding(vertical = 8.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Open Arena dashboard",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "platform.arena.ai",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// ABOUT
// ---------------------------------------------------------------------------

@Composable
private fun AboutCard() {
    val context = LocalContext.current
    val version = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrDefault("").orEmpty()
    }

    SectionCard(title = "About") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Arena for Android",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (version.isBlank()) "" else "v$version",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.size(6.dp))
        Text(
            text = "ai.arena.client \u00B7 API key built in \u2014 nothing to enter.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(10.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/libAkAudioVisiual")),
                        )
                    }
                }
                .padding(vertical = 4.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Telegram",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "@libAkAudioVisiual",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ---------------------------------------------------------------------------

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    cursorColor = MaterialTheme.colorScheme.primary,
)
