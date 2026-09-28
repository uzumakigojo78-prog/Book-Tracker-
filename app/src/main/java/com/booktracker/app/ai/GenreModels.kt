package com.booktracker.app.ai

import org.json.JSONArray
import org.json.JSONObject

/** The fixed set of genres the app groups books into, so every mode sorts the same way. */
val GENRES = listOf(
    "Fantasy", "Science Fiction", "Mystery & Thriller", "Horror", "Romance", "Historical Fiction",
    "Literary Fiction", "Classics", "Young Adult", "Children's", "Adventure", "Comics & Graphic Novels",
    "Poetry", "Biography & Memoir", "History", "Science & Nature", "Self-Help", "Business & Money",
    "Philosophy & Religion", "Travel", "Other",
)

data class Recommendation(
    val title: String,
    val author: String,
    val year: Int?,
    val genre: String,
    val reason: String,
)

enum class AnalysisSource { AI, BASIC }

/** Result of sorting the library into genres and suggesting what to read next. */
data class GenreAnalysis(
    val source: AnalysisSource,
    /** Book id to its genres (first is the main one). */
    val bookGenres: Map<String, List<String>>,
    /** A short description of the reader's taste (AI mode only). */
    val summary: String?,
    val recommendations: List<Recommendation>,
    val createdAt: Long,
    /** Fingerprint of the library when this was made, to tell when it's out of date. */
    val librarySignature: String,
) {
    fun toJson(): String = JSONObject()
        .put("source", source.name)
        .put("summary", summary ?: JSONObject.NULL)
        .put("createdAt", createdAt)
        .put("librarySignature", librarySignature)
        .put("bookGenres", JSONObject().apply { bookGenres.forEach { (id, g) -> put(id, JSONArray(g)) } })
        .put("recommendations", JSONArray().apply {
            recommendations.forEach { r ->
                put(
                    JSONObject().put("title", r.title).put("author", r.author).put("year", r.year ?: JSONObject.NULL)
                        .put("genre", r.genre).put("reason", r.reason)
                )
            }
        })
        .toString()

    companion object {
        fun fromJson(text: String): GenreAnalysis {
            val o = JSONObject(text)
            val genres = o.getJSONObject("bookGenres")
            val recs = o.getJSONArray("recommendations")
            return GenreAnalysis(
                source = AnalysisSource.valueOf(o.getString("source")),
                bookGenres = genres.keys().asSequence().associateWith { id ->
                    genres.getJSONArray(id).let { a -> (0 until a.length()).map { a.getString(it) } }
                },
                summary = if (o.isNull("summary")) null else o.getString("summary"),
                recommendations = (0 until recs.length()).map { i ->
                    val r = recs.getJSONObject(i)
                    Recommendation(
                        r.getString("title"), r.optString("author"),
                        if (r.isNull("year")) null else r.optInt("year"),
                        r.optString("genre", "Other"), r.optString("reason"),
                    )
                },
                createdAt = o.optLong("createdAt"),
                librarySignature = o.optString("librarySignature"),
            )
        }
    }
}

/** Maps any genre label onto [GENRES] (case-insensitive, with a few common synonyms). */
fun normalizeGenre(raw: String): String {
    val s = raw.trim().lowercase()
    GENRES.firstOrNull { it.lowercase() == s }?.let { return it }
    return SUBJECT_KEYWORDS.firstOrNull { (keywords, _) -> keywords.any { s.contains(it) } }?.second ?: "Other"
}

/** Keywords in Open Library subjects (or loose labels) for each genre, most specific first. */
val SUBJECT_KEYWORDS: List<Pair<List<String>, String>> = listOf(
    listOf("graphic novel", "comic", "manga") to "Comics & Graphic Novels",
    listOf("young adult", "teen", "juvenile fiction") to "Young Adult",
    listOf("children", "picture book", "juvenile") to "Children's",
    listOf("science fiction", "sci-fi", "dystopia", "space opera", "cyberpunk") to "Science Fiction",
    listOf("fantasy", "magic", "dragons", "wizards") to "Fantasy",
    listOf("horror", "ghost", "vampire", "zombie") to "Horror",
    listOf("mystery", "thriller", "detective", "crime", "suspense", "espionage") to "Mystery & Thriller",
    listOf("romance", "love stories") to "Romance",
    listOf("historical fiction") to "Historical Fiction",
    listOf("adventure", "survival", "sea stories") to "Adventure",
    listOf("poetry", "poems") to "Poetry",
    listOf("biography", "autobiography", "memoir") to "Biography & Memoir",
    listOf("self-help", "self help", "personal development", "success", "habits", "happiness") to "Self-Help",
    listOf("business", "economics", "finance", "management", "investing", "money") to "Business & Money",
    listOf("philosophy", "religion", "spirituality", "theology", "christianity", "buddhism") to "Philosophy & Religion",
    listOf("travel", "voyages") to "Travel",
    listOf("science", "nature", "physics", "biology", "astronomy", "mathematics", "psychology") to "Science & Nature",
    listOf("history", "war", "civilization") to "History",
    listOf("classic", "classical literature") to "Classics",
    listOf("fiction", "novel", "literature") to "Literary Fiction",
)
