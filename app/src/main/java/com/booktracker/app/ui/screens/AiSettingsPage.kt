package com.booktracker.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.booktracker.app.ai.AiProvider
import com.booktracker.app.ai.GenreViewModel
import com.booktracker.app.ui.components.ChoiceButton
import com.booktracker.app.ui.components.SectionCard
import com.booktracker.app.ui.components.SubPage
import com.booktracker.app.ui.theme.AppIcons
import kotlinx.coroutines.launch

/** Settings → AI: pick an AI service (Claude, Gemini, Grok, Kimi, …) and add your own API key. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun AiSettingsPage(vm: GenreViewModel, onBack: () -> Unit) {
    val settings = vm.aiSettings
    val active by vm.aiConfig.collectAsStateWithLifecycle()
    // Bumped after each save so values re-read from settings.
    var version by remember { mutableIntStateOf(0) }
    var provider by rememberSaveable { mutableStateOf(settings.provider()) }
    val savedKey = remember(provider, version) { settings.apiKey(provider) }
    var keyInput by rememberSaveable(provider) { mutableStateOf("") }
    var model by rememberSaveable(provider, version) { mutableStateOf(settings.model(provider)) }
    var baseUrl by rememberSaveable(provider, version) { mutableStateOf(settings.baseUrl(provider)) }
    var show by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current
    val c = MaterialTheme.colorScheme

    fun notify(msg: String) = scope.launch { snackbar.showSnackbar(msg) }

    SubPage("AI", onBack, snackbar) {
        SectionCard(color = c.secondaryContainer) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Psychology, null, tint = c.onSecondaryContainer)
                Spacer(Modifier.width(10.dp))
                Text(
                    active?.let { "Using ${it.displayName}" } ?: "Not set up",
                    style = MaterialTheme.typography.headlineSmall,
                    color = c.onSecondaryContainer,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "The Genres tab can use an AI of your choice to sort your library into genres, describe your reading taste and recommend books. " +
                    "It sends your books' titles, authors and reading progress to that service when you tap Analyze.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.onSecondaryContainer,
            )
        }

        SectionCard(title = "AI service") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AiProvider.entries.forEach { p ->
                    ChoiceButton(
                        label = p.label,
                        selected = p == provider,
                        onClick = {
                            provider = p
                            show = false
                            // Switch straight away if this service is already set up.
                            if (settings.apiKey(p) != null) settings.setProvider(p)
                        },
                        icon = if (settings.apiKey(p) != null) AppIcons.CheckCircle else null,
                    )
                }
            }
            if (provider == AiProvider.OPENROUTER) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "OpenRouter gives one key for hundreds of models from many companies.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSurfaceVariant,
                )
            }
        }

        SectionCard(title = "${provider.label} settings") {
            if (savedKey != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.CheckCircle, null, tint = c.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Key saved · ${mask(savedKey)}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        settings.setApiKey(provider, null)
                        version++
                        notify("${provider.label} key removed")
                    }) { Text("Remove") }
                }
                Spacer(Modifier.height(12.dp))
            }
            if (provider == AiProvider.CUSTOM) {
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it.trim() },
                    label = { Text("API base URL") },
                    placeholder = { Text("https://example.com/v1") },
                    supportingText = { Text("Any service with an OpenAI-compatible /chat/completions API") },
                    leadingIcon = { Icon(AppIcons.Link, null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next, autoCorrectEnabled = false),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it.trim() },
                label = { Text(if (savedKey != null) "Replace API key" else "Paste your ${provider.label} API key") },
                placeholder = { if (provider.keyHint.isNotEmpty()) Text(provider.keyHint) },
                leadingIcon = { Icon(AppIcons.Key, null) },
                trailingIcon = {
                    IconButton(onClick = { show = !show }) {
                        Icon(if (show) AppIcons.VisibilityOff else AppIcons.Visibility, if (show) "Hide key" else "Show key")
                    }
                },
                visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next, autoCorrectEnabled = false),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = model,
                onValueChange = { model = it.trim() },
                label = { Text("Model") },
                placeholder = { if (provider.defaultModel.isNotEmpty()) Text(provider.defaultModel) },
                supportingText = {
                    Text(
                        if (provider.defaultModel.isNotEmpty()) "Default: ${provider.defaultModel}. Any model name from ${provider.label} works."
                        else "The model name your service uses"
                    )
                },
                leadingIcon = { Icon(AppIcons.Psychology, null) },
                trailingIcon = if (provider.defaultModel.isNotEmpty() && model != provider.defaultModel) {
                    { IconButton(onClick = { model = provider.defaultModel }) { Icon(AppIcons.RestartAlt, "Use default model") } }
                } else null,
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, autoCorrectEnabled = false),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            val hasKey = keyInput.length >= 8 || savedKey != null
            val ready = hasKey && model.isNotBlank() && (provider != AiProvider.CUSTOM || baseUrl.startsWith("http"))
            Button(
                onClick = {
                    if (keyInput.isNotEmpty()) settings.setApiKey(provider, keyInput)
                    settings.setModel(provider, model.ifBlank { provider.defaultModel })
                    if (provider == AiProvider.CUSTOM) settings.setCustomBaseUrl(baseUrl)
                    settings.setProvider(provider)
                    keyInput = ""
                    show = false
                    version++
                    notify("Saved. Open Genres and tap Analyze.")
                },
                enabled = ready,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text("Save and use ${provider.label}", style = MaterialTheme.typography.titleSmall) }
            provider.keyUrl?.let { url ->
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { uri.openUri(url) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(AppIcons.OpenInNew, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Get a ${provider.label} API key", style = MaterialTheme.typography.titleSmall)
                }
            }
        }

        SectionCard(title = "Good to know") {
            Row {
                Icon(AppIcons.Lock, null, tint = c.onSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Keys are stored only on this phone, in the app's private storage, and each is sent only to its own service.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row {
                Icon(AppIcons.Insights, null, tint = c.onSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Usage is billed by the service you choose (some, like Gemini and Groq, have free tiers). Nothing runs in the background; it only runs when you tap Analyze.",
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

private fun mask(key: String) = if (key.length > 12) key.take(6) + "…" + key.takeLast(4) else "saved"
