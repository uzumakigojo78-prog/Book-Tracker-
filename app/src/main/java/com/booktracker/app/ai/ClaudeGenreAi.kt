package com.booktracker.app.ai

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.StopReason
import com.booktracker.app.data.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Sorts the library into genres and recommends books using Claude, with the reader's own API key. */
class ClaudeGenreAi(
    private val apiKey: String,
    private val model: String = MODEL,
    private val baseUrl: String? = null,
) {

    suspend fun analyze(books: List<Book>, signature: String): GenreAnalysis = withContext(Dispatchers.IO) {
        val client = AnthropicOkHttpClient.builder().apiKey(apiKey)
            .apply { if (baseUrl != null) baseUrl(baseUrl) }
            .build()
        try {
            val params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(16000L)
                .system(GenrePrompt.SYSTEM_PROMPT)
                .addUserMessage(GenrePrompt.libraryJson(books))
                // If a request is declined, let the API retry it on a suitable fallback model.
                .putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                .build()
            val response = client.messages().create(params)
            if (response.stopReason().orElse(null) == StopReason.REFUSAL) {
                throw AiException("Claude declined to answer this time. Try again later.")
            }
            val text = response.content().mapNotNull { it.text().orElse(null)?.text() }.joinToString("")
            GenrePrompt.parseAnalysis(
                text, books, signature, System.currentTimeMillis(),
                if (model == MODEL) MODEL_NAME else "Claude · $model",
            )
        } catch (e: UnauthorizedException) {
            throw AiException("Your Anthropic API key was rejected. Check it in Settings → AI.", e)
        } catch (e: PermissionDeniedException) {
            throw AiException("This API key doesn't have access to Claude. Check it in Settings → AI.", e)
        } catch (e: RateLimitException) {
            throw AiException("Claude is busy right now (rate limit). Try again in a minute.", e)
        } catch (e: AnthropicServiceException) {
            throw AiException("Claude returned an error (${e.statusCode()}). Try again later.", e)
        } catch (e: AnthropicIoException) {
            throw AiException("Couldn't reach Claude. Check your internet connection.", e)
        } finally {
            client.close()
        }
    }

    companion object {
        const val MODEL = "claude-opus-5"
        const val MODEL_NAME = "Claude Opus 5"

    }
}

class AiException(message: String, cause: Throwable? = null) : Exception(message, cause)
