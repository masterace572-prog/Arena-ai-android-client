@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ai.arena.client.ui

import ai.arena.client.ChatViewModel
import ai.arena.client.ConnState
import ai.arena.client.data.ApiFormat
import ai.arena.client.data.ThinkingLevel
import ai.arena.client.data.ThinkingMode
import ai.arena.client.data.normalizeGateway
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun SettingsScreen(vm: ChatViewModel) {
    val ui by vm.ui.collectAsState()
    val settings by vm.settings.collectAsState()

    BackHandler { vm.setScreen(ai.arena.client.Screen.CHAT) }

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
                    IconButton(onClick = { vm.setScreen(ai.arena.client.Screen.CHAT) }) {
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ConnectionCard(vm, ui.conn) }
            item { ApiKeyCard(vm, settings.apiKey) }
            item { FormatCard(settings.format) { f -> vm.updateSettings { it.copy(format = f) } } }
            item {
                ThinkingCard(
                    mode = settings.thinkingMode,
                    level = settings.thinkingLevel,
                    onMode = { m -> vm.updateSettings { it.copy(thinkingMode = m) } },
                    onLevel = { l -> vm.updateSettings { it.copy(thinkingLevel = l) } },
                )
            }
            item {
                GenerationCard(
                    systemPrompt = settings.systemPrompt,
                    temperature = settings.temperature,
                    onSystem = { s -> vm.updateSettings { it.copy(systemPrompt = s) } },
                    onTemperature = { t -> vm.updateSettings { it.copy(temperature = t) } },
                )
            }
            item { AboutCard() }
        }
    }

}

// ---------------------------------------------------------------------------

@Composable
private fun ConnectionCard(vm: ChatViewModel, conn: ConnState) {
    val settings by vm.settings.collectAsState()
    var gatewayDraft by remember(settings.gateway) { mutableStateOf(settings.gateway) }
    val testing = conn is ConnState.Testing
    val hasKey = settings.apiKey.isNotBlank()

    SectionCard(title = "Connection", caption = "Test uses GET /v1/models only — free, 0 credits spent.") {
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
                enabled = hasKey && !testing,
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
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                is ConnState.Testing -> Text(
                    text = "Testing…",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is ConnState.Unknown -> Text(
                    text = if (hasKey) "Not tested yet" else "Add your API key first",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------

@Composable
private fun ApiKeyCard(vm: ChatViewModel, apiKey: String) {
    var show by remember { mutableStateOf(false) }
    var draft by remember(apiKey) { mutableStateOf(apiKey) }

    SectionCard(title = "API key", caption = "Stored on this device only. Create keys in the Arena dashboard.") {
        OutlinedTextField(
            value = draft,
            onValueChange = { v ->
                draft = v
                vm.updateSettings { it.copy(apiKey = v.trim()) }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Arena API key") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (show) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
            trailingIcon = {
                TextButton(onClick = { show = !show }) {
                    Text(
                        text = if (show) "Hide" else "Show",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            },
            colors = fieldColors(),
        )
        if (apiKey.isNotBlank()) {
            Spacer(Modifier.size(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {
                    vm.updateSettings { it.copy(apiKey = "") }
                }) {
                    Text(
                        text = "Remove key",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------

@Composable
private fun FormatCard(format: ApiFormat, onChange: (ApiFormat) -> Unit) {
    SectionCard(
        title = "API format",
        caption = "Both are supported by the Arena gateway. Switch if a model behaves oddly.",
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectPill(
                text = "OpenAI",
                selected = format == ApiFormat.OPENAI,
                onClick = { onChange(ApiFormat.OPENAI) },
            )
            SelectPill(
                text = "Anthropic",
                selected = format == ApiFormat.ANTHROPIC,
                onClick = { onChange(ApiFormat.ANTHROPIC) },
            )
        }
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
        caption = "Reasoning appears only when the API returns it. Collapsed keeps chat clean.",
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

        Spacer(Modifier.size(16.dp))

        Text(
            text = "Thinking level",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.size(4.dp))
        Text(
            text = "Off = default. Higher levels reason longer and use more tokens.",
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
private fun GenerationCard(
    systemPrompt: String,
    temperature: Float,
    onSystem: (String) -> Unit,
    onTemperature: (Float) -> Unit,
) {
    var promptDraft by remember(systemPrompt) { mutableStateOf(systemPrompt) }
    val sliderValue = if (temperature < 0f) 0.7f else temperature

    SectionCard(title = "Generation") {
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
private fun AboutCard() {
    SectionCard(title = "About") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Arena for Android",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "v1.0.0",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.size(6.dp))
        Text(
            text = "ai.arena.client · OpenAI & Anthropic formats · Key and chats stay on this device.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
