package com.booktracker.app.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.booktracker.app.ai.ClaudeGenreAi
import com.booktracker.app.ai.GenreViewModel
import com.booktracker.app.ui.components.SectionCard
import com.booktracker.app.ui.components.SubPage
import com.booktracker.app.ui.theme.AppIcons
import kotlinx.coroutines.launch

/** Settings → AI: connect Claude with the reader's own Anthropic API key. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AiSettingsPage(vm: GenreViewModel, onBack: () -> Unit) {
    val savedKey by vm.apiKey.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    var show by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current
    val c = MaterialTheme.colorScheme

    SubPage("AI", onBack, snackbar) {
        SectionCard(color = c.secondaryContainer) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Psychology, null, tint = c.onSecondaryContainer)
                Spacer(Modifier.width(10.dp))
                Text(ClaudeGenreAi.MODEL_NAME, style = MaterialTheme.typography.headlineSmall, color = c.onSecondaryContainer)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "The Genres tab can use Claude, Anthropic's AI, to sort your library into genres, describe your reading taste and recommend books you'll like. " +
                    "It sends your books' titles, authors and reading progress to Anthropic when you tap Analyze.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.onSecondaryContainer,
            )
        }

        SectionCard(title = "Anthropic API key") {
            if (savedKey != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.CheckCircle, null, tint = c.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Connected · ${mask(savedKey!!)}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        vm.setApiKey(null)
                        scope.launch { snackbar.showSnackbar("API key removed") }
                    }) { Text("Remove") }
                }
                Spacer(Modifier.height(12.dp))
            }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.trim() },
                label = { Text(if (savedKey != null) "Replace key" else "Paste your API key") },
                placeholder = { Text("sk-ant-…") },
                leadingIcon = { Icon(AppIcons.Key, null) },
                trailingIcon = {
                    IconButton(onClick = { show = !show }) {
                        Icon(if (show) AppIcons.VisibilityOff else AppIcons.Visibility, if (show) "Hide key" else "Show key")
                    }
                },
                visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, autoCorrectEnabled = false),
                supportingText = {
                    if (input.isNotEmpty() && !input.startsWith("sk-ant-")) Text("Anthropic keys start with sk-ant-")
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    vm.setApiKey(input)
                    input = ""
                    show = false
                    scope.launch { snackbar.showSnackbar("Saved. Open Genres and tap Analyze.") }
                },
                enabled = input.length > 20,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text("Save key", style = MaterialTheme.typography.titleSmall) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { uri.openUri("https://console.anthropic.com/settings/keys") },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(AppIcons.OpenInNew, null)
                Spacer(Modifier.width(8.dp))
                Text("Get a key from the Anthropic Console", style = MaterialTheme.typography.titleSmall)
            }
        }

        SectionCard(title = "Good to know") {
            Row {
                Icon(AppIcons.Lock, null, tint = c.onSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Your key is stored only on this phone, in the app's private storage, and is sent only to Anthropic.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row {
                Icon(AppIcons.Insights, null, tint = c.onSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Each analysis is billed to your Anthropic account, usually a few cents to about 25¢ depending on how many books you have. " +
                        "Nothing runs in the background; it only runs when you tap Analyze.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row {
                Icon(AppIcons.Category, null, tint = c.onSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Without a key, Genres still works in basic mode using Open Library's subject tags.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun mask(key: String) = if (key.length > 12) key.take(7) + "…" + key.takeLast(4) else "saved"
