package com.booktracker.app.ai

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** AI choices, kept only in this app's private storage on the phone. Each provider keeps its own key and model. */
class AiSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ai", Context.MODE_PRIVATE)

    fun provider(): AiProvider = AiProvider.entries.firstOrNull { it.name == prefs.getString(PROVIDER, null) } ?: AiProvider.ANTHROPIC
    fun apiKey(p: AiProvider): String? = prefs.getString(keyOf(p), null)?.takeIf { it.isNotBlank() }
    fun model(p: AiProvider): String = prefs.getString("model_${p.name}", null)?.takeIf { it.isNotBlank() } ?: p.defaultModel
    fun baseUrl(p: AiProvider): String = if (p == AiProvider.CUSTOM) prefs.getString(CUSTOM_URL, "") ?: "" else p.baseUrl

    /** The ready-to-use configuration, or null if the chosen provider isn't set up yet. */
    fun config(): AiConfig? {
        val p = provider()
        val key = apiKey(p) ?: return null
        val model = model(p).takeIf { it.isNotBlank() } ?: return null
        val url = baseUrl(p).takeIf { it.isNotBlank() } ?: return null
        return AiConfig(p, key, model, url)
    }

    fun setProvider(p: AiProvider) = prefs.edit().putString(PROVIDER, p.name).apply()
    fun setApiKey(p: AiProvider, key: String?) =
        prefs.edit().apply { if (key.isNullOrBlank()) remove(keyOf(p)) else putString(keyOf(p), key.trim()) }.apply()
    fun setModel(p: AiProvider, model: String) = prefs.edit().putString("model_${p.name}", model.trim()).apply()
    fun setCustomBaseUrl(url: String) = prefs.edit().putString(CUSTOM_URL, url.trim().trimEnd('/')).apply()

    fun changes(): Flow<Unit> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(Unit) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(Unit)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // The Claude key keeps the name used before other providers were added.
    private fun keyOf(p: AiProvider) = if (p == AiProvider.ANTHROPIC) "anthropic_api_key" else "key_${p.name}"

    private companion object {
        const val PROVIDER = "provider"
        const val CUSTOM_URL = "custom_base_url"
    }
}
