package com.booktracker.app.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.booktracker.app.data.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Library fingerprint: changes when books are added, removed, renamed or finished. */
fun librarySignature(books: List<Book>): String =
    books.sortedBy { it.id }.joinToString("\n") { "${it.id}|${it.title}|${it.author}|${it.isFinished}" }.hashCode().toString()

class GenreViewModel(application: Application) : AndroidViewModel(application) {

    val aiSettings = AiSettings(application)
    private val file = File(application.filesDir, "genres.json")

    /** The AI in use, or null when none is set up (basic mode). */
    val aiConfig: StateFlow<AiConfig?> = aiSettings.changes().map { aiSettings.config() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, aiSettings.config())

    private val _analysis = MutableStateFlow<GenreAnalysis?>(null)
    val analysis: StateFlow<GenreAnalysis?> = _analysis.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val copiesCache = mutableMapOf<String, OnlineCopies>()

    init {
        viewModelScope.launch {
            _analysis.value = withContext(Dispatchers.IO) {
                runCatching { if (file.exists()) GenreAnalysis.fromJson(file.readText()) else null }.getOrNull()
            }
        }
    }

    fun analyze(books: List<Book>) {
        if (_running.value || books.isEmpty()) return
        _running.value = true
        _error.value = null
        viewModelScope.launch {
            val signature = librarySignature(books)
            val config = aiSettings.config()
            runCatching {
                when {
                    config == null -> OpenLibraryAi.analyze(books, signature)
                    config.provider == AiProvider.ANTHROPIC -> ClaudeGenreAi(config.apiKey, config.model).analyze(books, signature)
                    else -> OpenAiCompatibleAi(config).analyze(books, signature)
                }
            }.onSuccess { result ->
                _analysis.value = result
                withContext(Dispatchers.IO) { runCatching { file.writeText(result.toJson()) } }
            }.onFailure {
                _error.value = it.message ?: "Something went wrong. Try again."
            }
            _running.value = false
        }
    }

    suspend fun copies(title: String, author: String): OnlineCopies {
        val cacheKey = "$title|$author"
        copiesCache[cacheKey]?.let { return it }
        return OpenLibraryAi.findCopies(title, author).also { copiesCache[cacheKey] = it }
    }
}
