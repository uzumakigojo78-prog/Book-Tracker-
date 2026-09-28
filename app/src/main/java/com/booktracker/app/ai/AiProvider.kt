package com.booktracker.app.ai

/**
 * AI services the Genres tab can use. Claude goes through Anthropic's SDK; the rest
 * speak the OpenAI-compatible chat completions API, so any provider offering that
 * API works via [CUSTOM].
 */
enum class AiProvider(
    val label: String,
    val baseUrl: String,
    val defaultModel: String,
    val keyUrl: String?,
    val keyHint: String,
) {
    ANTHROPIC("Claude", "https://api.anthropic.com", ClaudeGenreAi.MODEL, "https://console.anthropic.com/settings/keys", "sk-ant-…"),
    GEMINI("Gemini", "https://generativelanguage.googleapis.com/v1beta/openai", "gemini-2.5-flash", "https://aistudio.google.com/apikey", "AIza…"),
    GROK("Grok", "https://api.x.ai/v1", "grok-4", "https://console.x.ai", "xai-…"),
    KIMI("Kimi", "https://api.moonshot.ai/v1", "kimi-k2-0905-preview", "https://platform.moonshot.ai/console/api-keys", "sk-…"),
    OPENAI("ChatGPT", "https://api.openai.com/v1", "gpt-5-mini", "https://platform.openai.com/api-keys", "sk-…"),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat", "https://platform.deepseek.com/api_keys", "sk-…"),
    MISTRAL("Mistral", "https://api.mistral.ai/v1", "mistral-large-latest", "https://console.mistral.ai/api-keys", ""),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1", "openrouter/auto", "https://openrouter.ai/keys", "sk-or-…"),
    GROQ("Groq", "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile", "https://console.groq.com/keys", "gsk_…"),
    CUSTOM("Other", "", "", null, ""),
}

/** Everything needed to call the chosen AI. */
data class AiConfig(val provider: AiProvider, val apiKey: String, val model: String, val baseUrl: String) {
    /** e.g. "Gemini · gemini-2.5-flash" */
    val displayName: String get() = if (provider == AiProvider.ANTHROPIC && model == ClaudeGenreAi.MODEL) ClaudeGenreAi.MODEL_NAME else "${provider.label} · $model"
}
