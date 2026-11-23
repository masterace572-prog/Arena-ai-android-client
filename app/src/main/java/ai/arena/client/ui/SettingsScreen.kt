@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ai.arena.client.ui

import ai.arena.client.ChatViewModel
import ai.arena.client.ConnState
import ai.arena.client.Screen
import ai.arena.client.data.ApiFormat
import ai.arena.client.data.ThinkingLevel
import ai.arena.client.data.ThinkingMode
import ai.arena.client.data.normalizeGateway
import androidx.activity.compose.BackHandler
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import java.util.Locale

// ---------------------------------------------------------------------------
// Settings — Claude-clean: section labels, flat cards, no clutter
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
                SectionBlock("Credits") { CreditsCard() }
            }
            item {
                SectionBlock("Connection") { ConnectionCard(vm, ui.conn) }
            }
            item {
                SectionBlock("Chat") {
                    ChatSettingsCard(
                        format = settings.format,
                        systemPrompt = settings.systemPrompt,
                        temperature = settings.temperature,
                        onFormat = { f -> vm.updateSettings { it.copy(format = f) } },
                        onSystem = { s -> vm.updateSettings { it.copy(systemPrompt = s) } },
                        onTemperature = { t -> vm.updateSettings { it.copy(temperature = t) } },
                    )
                }
            }
            item {
                SectionBlock("Thinking") {
                    ThinkingCard(
                        mode = settings.thinkingMode,
                        level = settings.thinkingLevel,
                        onMode = { m -> vm.updateSettings { it.copy(thinkingMode = m) } },
                        onLevel = { l -> vm.updateSettings { it.copy(thinkingLevel = l) } },
                    )
                }
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

@Composable
private fun CreditsCard() {
    SectionCard(title = "Balance") {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "$50.00",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "USD",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Spacer(Modifier.size(4.dp))
        Text(
            text = "Promotional · expires Dec 22 · covers Arena Router",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.size(16.dp))

        BalanceRow(
            color = Color(0xFF4ADE80),
            name = "Arena Router",
            amount = "$50.00",
            sub = "Your current model",
        )
        Spacer(Modifier.size(10.dp))
        BalanceRow(
            color = Color(0xFF4A7EEA),
            name = "All models",
            amount = "$0.00",
            sub = "Add funds on the dashboard for direct models",
        )

        Spacer(Modifier.size(12.dp))
        Text(
            text = "Balance from your Arena dashboard · connection tests stay free",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        )
    }
}

@Composable
private fun BalanceRow(color: Color, name: String, amount: String, sub: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
                .padding(top = 0.dp),
        )
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.size(2.dp))
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

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
                        text = "Connected · ${conn.models} models · ${conn.ms} ms",
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
                    text = "Testing…",
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

@Composable
private fun ChatSettingsCard(
    format: ApiFormat,
    systemPrompt: String,
    temperature: Float,
    onFormat: (ApiFormat) -> Unit,
    onSystem: (String) -> Unit,
    onTemperature: (Float) -> Unit,
) {
    var promptDraft by remember(systemPrompt) { mutableStateOf(systemPrompt) }
    val sliderValue = if (temperature < 0f) 0.7f else temperature

    SectionCard(title = "Chat") {
        // Fixed model — informational only
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
                        text = "coding-router-preview · auto-picks the best model",
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

        Spacer(Modifier.size(18.dp))

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

@Composable
private fun ThinkingCard(
    mode: ThinkingMode,
    level: ThinkingLevel,
    onMode: (ThinkingMode) -> Unit,
    onLevel: (ThinkingLevel) -> Unit,
) {
    SectionCard(
        title = "Thinking",
        caption = "Reasoning streams live while the model works.",
    ) {
        Text(
            text = "Display",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.size(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThinkingMode.entries.forEach { m ->
                SelectPill(
                    text = m.label,
                    selected = mode == m,
                    onClick = { onMode(m) },
                )
            }
        }

        Spacer(Modifier.size(18.dp))

        Text(
            text = "Reasoning level",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.size(4.dp))
        Text(
            text = "Higher levels think longer and use more tokens. Auto-falls back if a model rejects it.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThinkingLevel.entries.forEach { l ->
                SelectPill(
                    text = l.label,
                    selected = level == l,
                    onClick = { onLevel(l) },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------

@Composable
private fun AboutCard() {
    val context = LocalContext.current

    SectionCard(title = "About") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Arena for Android",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "v2.0.0",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.size(6.dp))
        Text(
            text = "ai.arena.client · API key built in — nothing to enter.",
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
