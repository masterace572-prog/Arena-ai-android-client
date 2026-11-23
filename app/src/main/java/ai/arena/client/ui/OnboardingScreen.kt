package ai.arena.client.ui

import ai.arena.client.ChatViewModel
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ai.arena.client.R

@Composable
fun OnboardingScreen(vm: ChatViewModel) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current

    var draft by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    val testing = ui.onboardingTesting

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(56.dp))

        Surface(
            modifier = Modifier.size(76.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                )
            }
        }

        Spacer(Modifier.height(26.dp))

        Text(
            text = "Connect to Arena",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Paste your API key to start chatting. It never leaves this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(30.dp))

        OutlinedTextField(
            value = draft,
            onValueChange = {
                draft = it
                vm.dismissOnboardingError()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API key") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            visualTransformation = if (show) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            trailingIcon = {
                TextButton(onClick = { show = !show }) {
                    Text(
                        text = if (show) "Hide" else "Show",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        )

        val error = ui.onboardingError
        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = { vm.connectWithKey(draft) },
            enabled = draft.isNotBlank() && !testing,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            if (testing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Test connection & continue", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = {
            runCatching {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://platform.arena.ai/dashboard/keys"),
                    ),
                )
            }
        }) {
            Text(
                text = "Get a key from the dashboard",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(26.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Text(
                text = "The connection test only calls GET /v1/models — free, 0 credits spent.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}
