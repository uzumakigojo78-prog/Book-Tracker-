package com.booktracker.app.ai

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.booktracker.app.data.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** The Anthropic API key, kept only in this app's private storage on the phone. */
class AiSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ai", Context.MODE_PRIVATE)

    fun apiKey(): String? = prefs.getString(KEY, null)?.takeIf { it.isNotBlank() }
    fun setApiKey(key: String?) = prefs.edit().apply { if (key.isNullOrBlank()) remove(KEY) else putString(KEY, key.trim()) }.apply()

    fun changes(): Flow<String?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(apiKey()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(apiKey())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private companion object { const val KEY = "anthropic_api_key" }
}

/** Library fingerprint: changes when books are added, removed, renamed or finished. */
fun librarySignature(books: List<Book>): String =
    books.sortedBy { it.id }.joinToString("\n") { "${it.id}|${it.title}|${it.author}|${it.isFinished}" }.hashCode().toString()

class GenreViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = AiSettings(application)
    private val file = File(application.filesDir, "genres.json")

    val apiKey: StateFlow<String?> = settings.changes().stateIn(viewModelScope, SharingStarted.Eagerly, settings.apiKey())

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

    fun setApiKey(key: String?) = settings.setApiKey(key)

    fun analyze(books: List<Book>) {
        if (_running.value || books.isEmpty()) return
        _running.value = true
        _error.value = null
        viewModelScope.launch {
            val signature = librarySignature(books)
            val key = settings.apiKey()
            runCatching {
                if (key != null) ClaudeGenreAi(key).analyze(books, signature) else OpenLibraryAi.analyze(books, signature)
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
