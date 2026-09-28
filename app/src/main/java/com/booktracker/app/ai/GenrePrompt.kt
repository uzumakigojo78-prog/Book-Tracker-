package com.booktracker.app.ai

import com.booktracker.app.data.Book
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.Locale

/** The instructions, library summary and reply parsing shared by every AI provider. */
object GenrePrompt {
    val SYSTEM_PROMPT = """
        You organize a reader's personal library into genres and suggest what they might enjoy reading next. You'll receive their books as JSON, including how far they've read and how much they read recently.

        Give each book one or two genres from this list, main genre first: ${GENRES.joinToString(", ")}. Use what you know about the actual book; if you don't recognize it, infer from the title and author, and use "Other" only as a last resort.

        Write a two-sentence summary of their reading taste, addressed to them ("You…"). Books they finished or are actively reading say more about their taste than ones they barely started.

        Recommend 8 real, published books that are not already in their library: mostly from the genres they read most, plus one or two stretch picks they'd plausibly enjoy. Give each a one-sentence reason that mentions a book from their library. Only recommend books you are confident exist, with the correct author.

        Reply with only a JSON object and no other text, in this shape:
        {"books":[{"id":"<book id>","genres":["<genre>"]}],"summary":"<two sentences>","recommendations":[{"title":"<title>","author":"<author>","year":<first published year or null>,"genre":"<genre from the list>","reason":"<one sentence>"}]}
    """.trimIndent()

    /** The library as compact JSON for the prompt. */
    fun libraryJson(books: List<Book>): String {
        val since = LocalDate.now().minusDays(30)
        val array = JSONArray()
        books.forEach { b ->
            array.put(
                JSONObject()
                    .put("id", b.id)
                    .put("title", b.title)
                    .put("author", b.author.ifBlank { JSONObject.NULL })
                    .put("release_year", b.releaseDate?.year ?: JSONObject.NULL)
                    .put("total_pages", b.totalPages)
                    .put("current_page", b.currentPage)
                    .put("status", if (b.isFinished) "finished" else if (b.currentPage > 0) "reading" else "not started")
                    .put("pages_read_last_30_days", b.dailyPages.filter { it.date > since }.sumOf { it.pagesRead })
            )
        }
        return JSONObject().put("books", array).toString()
    }

    /** Parses Claude's JSON reply, tolerating stray text around it and unknown genres. */
    fun parseAnalysis(text: String, books: List<Book>, signature: String, now: Long, madeBy: String? = null): GenreAnalysis {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) throw AiException("The AI's answer couldn't be read. Try again.")
        val o = runCatching { JSONObject(text.substring(start, end + 1)) }
            .getOrElse { throw AiException("The AI's answer couldn't be read. Try again.", it) }

        val assigned = mutableMapOf<String, List<String>>()
        o.optJSONArray("books")?.let { arr ->
            for (i in 0 until arr.length()) {
                val b = arr.optJSONObject(i) ?: continue
                val g = b.optJSONArray("genres") ?: continue
                assigned[b.optString("id")] = (0 until g.length()).map { normalizeGenre(g.optString(it)) }.distinct().take(2)
            }
        }
        val bookGenres = books.associate { b -> b.id to (assigned[b.id]?.takeIf { it.isNotEmpty() } ?: listOf("Other")) }

        val owned = books.map { key(it.title) }.toSet()
        val recs = mutableListOf<Recommendation>()
        o.optJSONArray("recommendations")?.let { arr ->
            for (i in 0 until arr.length()) {
                val r = arr.optJSONObject(i) ?: continue
                val title = r.optString("title").trim()
                if (title.isEmpty() || key(title) in owned) continue
                recs += Recommendation(
                    title = title,
                    author = r.optString("author").trim(),
                    year = if (r.isNull("year")) null else r.optInt("year").takeIf { it > 0 },
                    genre = normalizeGenre(r.optString("genre")),
                    reason = r.optString("reason").trim(),
                )
            }
        }
        val summary = o.optString("summary").trim().takeIf { it.isNotEmpty() }
        return GenreAnalysis(AnalysisSource.AI, bookGenres, summary, recs, now, signature, madeBy)
    }

    private fun key(title: String) = title.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }
}
