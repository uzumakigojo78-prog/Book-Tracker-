package com.booktracker.app.ai

import com.booktracker.app.data.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/** Where a book can legally be read or downloaded online. */
data class OnlineCopies(
    val openLibraryUrl: String,
    /** Open Library's ebook status: "public" (free download), "borrowable", "printdisabled" or "no_ebook". */
    val access: String?,
    /** Internet Archive page with the downloadable/borrowable scan, when there is one. */
    val archiveUrl: String?,
    val gutenbergUrl: String,
    val googleBooksUrl: String,
    val coverUrl: String?,
    val pages: Int?,
) {
    val isFreeDownload: Boolean get() = access == "public"
    val isBorrowable: Boolean get() = access == "borrowable"
}

/**
 * The no-API-key mode: genres come from Open Library's subject tags and
 * recommendations are popular books in the reader's top genres.
 */
object OpenLibraryAi {

    suspend fun analyze(books: List<Book>, signature: String): GenreAnalysis = coroutineScope {
        val limit = Semaphore(4) // be polite to Open Library
        val genres = books.map { b ->
            async { b.id to limit.withPermit { runCatching { genresFor(b) }.getOrDefault(listOf("Other")) } }
        }.awaitAll().toMap()

        // Top genres by how many pages were read in them.
        val weight = mutableMapOf<String, Int>()
        books.forEach { b -> genres[b.id]?.firstOrNull()?.let { g -> weight[g] = (weight[g] ?: 0) + b.currentPage.coerceAtLeast(1) } }
        val top = weight.entries.filter { it.key != "Other" }.sortedByDescending { it.value }.map { it.key }.take(2)
            .ifEmpty { listOf("Literary Fiction") }

        val owned = books.map { key(it.title) }.toMutableSet()
        val recs = top.map { g -> async { g to runCatching { popularIn(g) }.getOrDefault(emptyList()) } }.awaitAll()
            .flatMap { (genre, candidates) ->
                val count = genres.values.count { it.firstOrNull() == genre }
                candidates.filter { owned.add(key(it.first)) }.take(4).map { (title, author, year) ->
                    Recommendation(
                        title, author, year, genre,
                        if (count > 0) "Popular with $genre readers, and you have $count $genre book${if (count == 1) "" else "s"} in your library."
                        else "A popular $genre pick.",
                    )
                }
            }
        GenreAnalysis(AnalysisSource.BASIC, genres, null, recs, System.currentTimeMillis(), signature)
    }

    /** Finds a book on Open Library and where to read or download it legally. */
    suspend fun findCopies(title: String, author: String): OnlineCopies {
        val q = listOf(title, author).filter { it.isNotBlank() }.joinToString(" ")
        val doc = runCatching {
            JSONObject(get("https://openlibrary.org/search.json?q=${enc(q)}&limit=1&fields=key,ebook_access,ia,cover_i,number_of_pages_median"))
                .optJSONArray("docs")?.optJSONObject(0)
        }.getOrNull()
        return copiesFrom(doc, q)
    }

    internal fun copiesFrom(doc: JSONObject?, query: String): OnlineCopies {
        val workKey = doc?.optString("key")?.takeIf { it.startsWith("/works/") }
        val access = doc?.optString("ebook_access")?.takeIf { it.isNotBlank() }
        val ia = doc?.optJSONArray("ia")?.optString(0)?.takeIf { it.isNotBlank() }
        return OnlineCopies(
            openLibraryUrl = if (workKey != null) "https://openlibrary.org$workKey" else "https://openlibrary.org/search?q=${enc(query)}",
            access = access,
            archiveUrl = if (ia != null && (access == "public" || access == "borrowable")) "https://archive.org/details/$ia" else null,
            gutenbergUrl = "https://www.gutenberg.org/ebooks/search/?query=${enc(query)}",
            googleBooksUrl = "https://www.google.com/books?q=${enc(query)}",
            coverUrl = doc?.optLong("cover_i", 0L)?.takeIf { it > 0 }?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg?default=false" },
            pages = doc?.optInt("number_of_pages_median", 0)?.takeIf { it > 0 },
        )
    }

    private suspend fun genresFor(book: Book): List<String> {
        val url = "https://openlibrary.org/search.json?title=${enc(book.title)}" +
            (if (book.author.isNotBlank()) "&author=${enc(book.author)}" else "") + "&limit=1&fields=subject"
        val subjects = JSONObject(get(url)).optJSONArray("docs")?.optJSONObject(0)?.optJSONArray("subject")
            ?: return listOf("Other")
        return genresFromSubjects((0 until subjects.length()).map { subjects.optString(it) })
    }

    /** Picks the one or two genres that the most subject tags point to. */
    internal fun genresFromSubjects(subjects: List<String>): List<String> {
        val counts = mutableMapOf<String, Int>()
        subjects.take(40).forEach { raw ->
            val s = raw.lowercase(Locale.ROOT)
            SUBJECT_KEYWORDS.firstOrNull { (keywords, _) -> keywords.any { s.contains(it) } }
                ?.let { (_, genre) -> counts[genre] = (counts[genre] ?: 0) + 1 }
        }
        // "Literary Fiction" is a catch-all for any fiction tag; prefer a more specific genre.
        val ranked = counts.entries.sortedWith(
            compareByDescending<Map.Entry<String, Int>> { if (it.key == "Literary Fiction") 0 else it.value }.thenByDescending { it.value }
        ).map { it.key }
        return ranked.take(2).ifEmpty { listOf("Other") }
    }

    private suspend fun popularIn(genre: String): List<Triple<String, String, Int?>> {
        val subject = GENRE_SUBJECTS[genre] ?: return emptyList()
        val docs = JSONObject(
            get("https://openlibrary.org/search.json?subject=${enc(subject)}&sort=readinglog&limit=20&fields=title,author_name,first_publish_year")
        ).optJSONArray("docs") ?: return emptyList()
        return (0 until docs.length()).mapNotNull { i ->
            val d = docs.optJSONObject(i) ?: return@mapNotNull null
            val title = d.optString("title").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Triple(title, d.optJSONArray("author_name")?.optString(0) ?: "", d.optInt("first_publish_year", 0).takeIf { it > 0 })
        }
    }

    private val GENRE_SUBJECTS = mapOf(
        "Fantasy" to "fantasy", "Science Fiction" to "science_fiction", "Mystery & Thriller" to "mystery_and_detective_stories",
        "Horror" to "horror", "Romance" to "romance", "Historical Fiction" to "historical_fiction",
        "Literary Fiction" to "literary_fiction", "Classics" to "classic_literature", "Young Adult" to "young_adult_fiction",
        "Children's" to "juvenile_fiction", "Adventure" to "adventure_stories", "Comics & Graphic Novels" to "comics_&_graphic_novels",
        "Poetry" to "poetry", "Biography & Memoir" to "biography", "History" to "history", "Science & Nature" to "science",
        "Self-Help" to "self-help", "Business & Money" to "business", "Philosophy & Religion" to "philosophy", "Travel" to "travel",
    )

    private fun key(title: String) = title.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.setRequestProperty("User-Agent", "BookTracker/1.0 (Android)")
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
