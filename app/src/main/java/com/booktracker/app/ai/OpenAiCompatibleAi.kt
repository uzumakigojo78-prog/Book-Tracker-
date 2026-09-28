package com.booktracker.app.ai

import com.booktracker.app.data.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Genres and recommendations from any provider with an OpenAI-compatible
 * chat completions API (Gemini, Grok, Kimi, ChatGPT, DeepSeek, Mistral,
 * OpenRouter, Groq, or a custom endpoint), using the reader's own key.
 */
class OpenAiCompatibleAi(private val config: AiConfig) {

    suspend fun analyze(books: List<Book>, signature: String): GenreAnalysis = withContext(Dispatchers.IO) {
        val name = config.provider.label.takeIf { config.provider != AiProvider.CUSTOM } ?: "The AI service"
        val body = requestBody(config.model, books)
        val conn = try {
            URL(config.baseUrl.trimEnd('/') + "/chat/completions").openConnection() as HttpURLConnection
        } catch (e: Exception) {
            throw AiException("That service address doesn't look right. Check it in Settings → AI.", e)
        }
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 20_000
            conn.readTimeout = 180_000 // some models think for a while
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            if (config.provider == AiProvider.OPENROUTER) conn.setRequestProperty("X-Title", "Book Tracker")
            conn.outputStream.use { it.write(body.toByteArray()) }

            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw AiException(errorMessage(name, code, text))
            GenrePrompt.parseAnalysis(replyText(text), books, signature, System.currentTimeMillis(), config.displayName)
        } catch (e: IOException) {
            throw AiException("Couldn't reach $name. Check your internet connection.", e)
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        fun requestBody(model: String, books: List<Book>): String = JSONObject()
            .put("model", model)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", GenrePrompt.SYSTEM_PROMPT))
                .put(JSONObject().put("role", "user").put("content", GenrePrompt.libraryJson(books))))
            .toString()

        /** The assistant's text from a chat completions response. */
        fun replyText(json: String): String {
            val message = runCatching { JSONObject(json).getJSONArray("choices").getJSONObject(0).getJSONObject("message") }
                .getOrElse { throw AiException("The AI's answer couldn't be read. Try again.", it) }
            val content = message.opt("content")
            return when {
                content is String && content.isNotBlank() -> content
                // Some providers return content as a list of text parts.
                content is JSONArray -> (0 until content.length()).joinToString("") { content.optJSONObject(it)?.optString("text").orEmpty() }
                message.optString("refusal").isNotBlank() -> throw AiException("The AI declined to answer this time. Try again later.")
                else -> throw AiException("The AI sent an empty answer. Try again.")
            }
        }

        fun errorMessage(name: String, code: Int, body: String): String {
            val detail = runCatching {
                JSONObject(body).let { o -> o.optJSONObject("error")?.optString("message") ?: o.optString("message") }
            }.getOrNull()?.takeIf { it.isNotBlank() }?.take(160)
            return when (code) {
                401, 403 -> "$name rejected the API key. Check it in Settings → AI."
                404 -> "$name couldn't find that model. Check the model name in Settings → AI."
                429 -> "$name is busy or your quota ran out (rate limit). Try again later."
                else -> "$name returned an error ($code)${if (detail != null) ": $detail" else ". Try again later."}"
            }
        }
    }
}
