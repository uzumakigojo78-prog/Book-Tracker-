package com.booktracker.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.util.UUID

/**
 * Stores books as a JSON file in the app's private storage.
 */
class BookRepository(context: Context) {

    private val file = File(context.filesDir, FILE_NAME)
    private val writeLock = Mutex()
    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        _books.value = if (file.exists()) {
            runCatching { decode(file.readText()) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
    }

    suspend fun addBook(details: BookDetails): String {
        val id = UUID.randomUUID().toString()
        mutate { it + Book(id, details.title, details.author, details.releaseDate, details.totalPages, details.coverUrl) }
        return id
    }

    suspend fun updateBook(id: String, details: BookDetails) = mutate { list ->
        list.map {
            if (it.id != id) it else it.copy(
                title = details.title,
                author = details.author,
                releaseDate = details.releaseDate,
                totalPages = details.totalPages,
                coverUrl = details.coverUrl,
            )
        }
    }

    suspend fun deleteBook(id: String) = mutate { list -> list.filterNot { it.id == id } }

    /** Records the page reached on [date], replacing any entry already on that day. */
    suspend fun logPage(id: String, date: LocalDate, page: Int) = mutate { list ->
        list.map { book ->
            if (book.id != id) return@map book
            val clamped = page.coerceIn(0, book.totalPages)
            book.copy(entries = book.entries.filterNot { it.date == date } + ReadingEntry(date, clamped))
        }
    }

    suspend fun deleteEntry(id: String, date: LocalDate) = mutate { list ->
        list.map { book ->
            if (book.id == id) book.copy(entries = book.entries.filterNot { it.date == date }) else book
        }
    }

    private suspend fun mutate(transform: (List<Book>) -> List<Book>) {
        writeLock.withLock {
            _books.update(transform)
            val json = encode(_books.value)
            withContext(Dispatchers.IO) {
                val tmp = File(file.parentFile, "$FILE_NAME.tmp")
                tmp.writeText(json)
                tmp.renameTo(file)
            }
        }
    }

    private fun encode(books: List<Book>): String {
        val array = JSONArray()
        books.forEach { book ->
            array.put(
                JSONObject()
                    .put("id", book.id)
                    .put("title", book.title)
                    .put("author", book.author)
                    .put("releaseDate", book.releaseDate?.toString() ?: JSONObject.NULL)
                    .put("totalPages", book.totalPages)
                    .put("coverUrl", book.coverUrl ?: JSONObject.NULL)
                    .put("createdAt", book.createdAt)
                    .put("entries", JSONArray().apply {
                        book.entries.forEach { entry ->
                            put(JSONObject().put("date", entry.date.toString()).put("page", entry.page))
                        }
                    })
            )
        }
        return JSONObject().put("version", 1).put("books", array).toString()
    }

    private fun decode(text: String): List<Book> {
        val array = JSONObject(text).getJSONArray("books")
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            val entries = o.optJSONArray("entries") ?: JSONArray()
            Book(
                id = o.getString("id"),
                title = o.getString("title"),
                author = o.optString("author"),
                releaseDate = if (o.isNull("releaseDate")) null else LocalDate.parse(o.getString("releaseDate")),
                totalPages = o.getInt("totalPages"),
                coverUrl = if (o.isNull("coverUrl")) null else o.getString("coverUrl"),
                createdAt = o.optLong("createdAt", 0L),
                entries = (0 until entries.length()).map { j ->
                    val e = entries.getJSONObject(j)
                    ReadingEntry(LocalDate.parse(e.getString("date")), e.getInt("page"))
                },
            )
        }
    }

    private companion object {
        const val FILE_NAME = "books.json"
    }
}
