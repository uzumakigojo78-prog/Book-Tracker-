package com.booktracker.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

data class BookSuggestion(
    val title: String,
    val author: String,
    val releaseDate: LocalDate?,
    val pages: Int?,
    val coverUrl: String?,
)

/**
 * Looks up books by title using Open Library and Google Books (both free, no
 * API key). Both are queried in parallel so either one can fill in for the other.
 */
object BookSearch {

    suspend fun search(query: String): List<BookSuggestion> = coroutineScope {
        val q = query.trim()
        if (q.length < 2) return@coroutineScope emptyList()
        val openLibrary = async { runCatching { searchOpenLibrary(q) }.getOrDefault(emptyList()) }
        val google = async { runCatching { searchGoogle(q) }.getOrDefault(emptyList()) }
        merge(openLibrary.await(), google.await()).take(MAX_RESULTS)
    }

    /** Combines results for the same book, keeping the best detail from each source. */
    internal fun merge(primary: List<BookSuggestion>, secondary: List<BookSuggestion>): List<BookSuggestion> {
        val merged = LinkedHashMap<String, BookSuggestion>()
        for (s in primary + secondary) {
            val key = normalize(s.title) + "|" + normalize(s.author.substringBefore(','))
            val existing = merged[key]
            merged[key] = if (existing == null) s else existing.copy(
                releaseDate = pickDate(existing.releaseDate, s.releaseDate),
                pages = existing.pages ?: s.pages,
                coverUrl = existing.coverUrl ?: s.coverUrl,
            )
        }
        return merged.values.toList()
    }

    // Prefer a precise date over a bare "January 1st" year placeholder.
    private fun pickDate(a: LocalDate?, b: LocalDate?): LocalDate? = when {
        a == null -> b
        b == null -> a
        a.dayOfYear == 1 && b.year == a.year && b.dayOfYear != 1 -> b
        else -> a
    }

    private fun normalize(s: String) = s.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }

    private suspend fun searchOpenLibrary(q: String): List<BookSuggestion> {
        val url = "https://openlibrary.org/search.json?q=${enc(q)}&limit=8" +
            "&fields=title,author_name,first_publish_year,number_of_pages_median,cover_i,publish_date"
        return parseOpenLibrary(get(url))
    }

    internal fun parseOpenLibrary(json: String): List<BookSuggestion> {
        val docs = JSONObject(json).optJSONArray("docs") ?: return emptyList()
        return (0 until docs.length()).mapNotNull { i ->
            val d = docs.getJSONObject(i)
            val title = d.optString("title").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val authors = d.optJSONArray("author_name")
            val author = (0 until (authors?.length() ?: 0)).joinToString(", ") { authors!!.getString(it) }
            val year = d.optInt("first_publish_year", 0).takeIf { it > 0 }
            val publishDates = d.optJSONArray("publish_date")
            val exact = (0 until (publishDates?.length() ?: 0))
                .mapNotNull { parseLooseDate(publishDates!!.getString(it)) }
                .filter { it.year == year }
                .minOrNull()
            BookSuggestion(
                title = title,
                author = author,
                releaseDate = exact ?: year?.let { LocalDate.of(it, 1, 1) },
                pages = d.optInt("number_of_pages_median", 0).takeIf { it > 0 },
                coverUrl = d.optLong("cover_i", 0L).takeIf { it > 0 }
                    ?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg?default=false" },
            )
        }
    }

    private suspend fun searchGoogle(q: String): List<BookSuggestion> {
        val url = "https://www.googleapis.com/books/v1/volumes?q=${enc(q)}&maxResults=10&printType=books" +
            "&fields=items(volumeInfo(title,subtitle,authors,publishedDate,pageCount,imageLinks/thumbnail))"
        return parseGoogle(get(url))
    }

    internal fun parseGoogle(json: String): List<BookSuggestion> {
        val items = JSONObject(json).optJSONArray("items") ?: return emptyList()
        return (0 until items.length()).mapNotNull { i ->
            val v = items.getJSONObject(i).optJSONObject("volumeInfo") ?: return@mapNotNull null
            val title = v.optString("title").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val authors = v.optJSONArray("authors")
            BookSuggestion(
                title = title,
                author = (0 until (authors?.length() ?: 0)).joinToString(", ") { authors!!.getString(it) },
                releaseDate = parseIsoPartial(v.optString("publishedDate")),
                pages = v.optInt("pageCount", 0).takeIf { it > 0 },
                coverUrl = v.optJSONObject("imageLinks")?.optString("thumbnail")
                    ?.takeIf { it.isNotBlank() }
                    ?.replace("http://", "https://")
                    ?.replace("&edge=curl", ""),
            )
        }
    }

    /** Parses Google's "2021", "2021-05" or "2021-05-04". */
    private fun parseIsoPartial(s: String): LocalDate? = runCatching {
        when (s.length) {
            4 -> LocalDate.of(s.toInt(), 1, 1)
            7 -> YearMonth.parse(s).atDay(1)
            else -> LocalDate.parse(s.take(10))
        }
    }.getOrNull()

    private val looseFormats: List<DateTimeFormatter> =
        listOf("MMMM d, yyyy", "MMM d, yyyy", "d MMMM yyyy", "d MMM yyyy", "yyyy-MM-dd").map {
            DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(it).toFormatter(Locale.ENGLISH)
        }

    /** Parses Open Library's free-form dates such as "May 04, 2021". */
    private fun parseLooseDate(s: String): LocalDate? = looseFormats.firstNotNullOfOrNull { f ->
        runCatching { LocalDate.parse(s.trim(), f) }.getOrNull()
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "BookTracker/1.0 (Android)")
            conn.setRequestProperty("Accept", "application/json")
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private const val MAX_RESULTS = 8
}
