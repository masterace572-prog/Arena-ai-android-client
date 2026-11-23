package ai.arena.client.ui

import ai.arena.client.ChatViewModel
import ai.arena.client.Screen
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AppRoot(vm: ChatViewModel = viewModel()) {
    val ui by vm.ui.collectAsState()

    val onChat = ui.screen == Screen.CHAT
    BackHandler(enabled = !onChat && ui.hasKey) {
        vm.setScreen(Screen.CHAT)
    }

    when {
        !ui.ready -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {}

        ui.screen == Screen.SETTINGS -> SettingsScreen(vm)

        else -> ChatScreen(vm)
    }
}
