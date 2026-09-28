package com.booktracker.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/** A book found by searching, with everything we could find out about it. */
data class CatalogBook(
    val title: String,
    val author: String,
    val year: Int?,
    val releaseDate: LocalDate?,
    val pages: Int?,
    val coverUrl: String?,
    val publisher: String?,
    val rating: Double?,
    val subjects: List<String>,
    val description: String?,
    /** Open Library work key like "/works/OL27448W". */
    val workKey: String?,
    /** Open Library ebook status: "public", "borrowable", "printdisabled" or "no_ebook". */
    val ebookAccess: String?,
    /** Internet Archive identifier of a scanned copy. */
    val iaId: String?,
    /** A free, legal PDF (public-domain books only). */
    val freePdfUrl: String?,
    val googlePreviewUrl: String?,
) {
    /** Main genres, most specific first. */
    val genres: List<String> get() = OpenLibraryAi.genresFromSubjects(subjects).filter { it != "Other" }
    val openLibraryUrl: String get() = workKey?.let { "https://openlibrary.org$it" } ?: "https://openlibrary.org/search?q=${enc("$title $author")}"
    val archiveUrl: String? get() = iaId?.takeIf { ebookAccess == "public" || ebookAccess == "borrowable" }?.let { "https://archive.org/details/$it" }
    val isBorrowable: Boolean get() = ebookAccess == "borrowable"
}

/** Searches Open Library and Google Books together for the Genres tab's search bar. */
object BookCatalog {

    suspend fun search(query: String): List<CatalogBook> = coroutineScope {
        val q = query.trim()
        if (q.length < 2) return@coroutineScope emptyList()
        val ol = async { runCatching { parseOpenLibrary(get(OL_SEARCH + enc(q))) }.getOrDefault(emptyList()) }
        val google = async { runCatching { parseGoogle(get(GOOGLE_SEARCH + enc(q))) }.getOrDefault(emptyList()) }
        merge(ol.await(), google.await()).take(20)
    }

    /** Fills in the description from Open Library when the search didn't include one. */
    suspend fun withDetails(book: CatalogBook): CatalogBook {
        if (book.description != null || book.workKey == null) return book
        val description = runCatching { parseWorkDescription(get("https://openlibrary.org${book.workKey}.json")) }.getOrNull()
        return book.copy(description = description)
    }

    private const val OL_SEARCH = "https://openlibrary.org/search.json?limit=15&fields=key,title,author_name,first_publish_year," +
        "number_of_pages_median,cover_i,publisher,ratings_average,subject,ebook_access,ia,publish_date&q="
    private const val GOOGLE_SEARCH = "https://www.googleapis.com/books/v1/volumes?maxResults=15&printType=books" +
        "&fields=items(id,volumeInfo(title,authors,publishedDate,pageCount,publisher,averageRating,categories,description,imageLinks/thumbnail,previewLink)," +
        "accessInfo(publicDomain,pdf(isAvailable,downloadLink)))&q="

    internal fun parseOpenLibrary(json: String): List<CatalogBook> {
        val docs = JSONObject(json).optJSONArray("docs") ?: return emptyList()
        return (0 until docs.length()).mapNotNull { i ->
            val d = docs.optJSONObject(i) ?: return@mapNotNull null
            val title = d.optString("title").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val year = d.optInt("first_publish_year", 0).takeIf { it > 0 }
            val access = d.optString("ebook_access").takeIf { it.isNotBlank() }
            val ia = d.optJSONArray("ia")?.optString(0)?.takeIf { it.isNotBlank() }
            CatalogBook(
                title = title,
                author = strings(d.optJSONArray("author_name")).joinToString(", "),
                year = year,
                releaseDate = year?.let { y -> strings(d.optJSONArray("publish_date")).mapNotNull(::looseDate).filter { it.year == y }.minOrNull() ?: LocalDate.of(y, 1, 1) },
                pages = d.optInt("number_of_pages_median", 0).takeIf { it > 0 },
                coverUrl = d.optLong("cover_i", 0L).takeIf { it > 0 }?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg?default=false" },
                publisher = strings(d.optJSONArray("publisher")).firstOrNull(),
                rating = d.optDouble("ratings_average", Double.NaN).takeIf { !it.isNaN() && it > 0 },
                subjects = strings(d.optJSONArray("subject")).take(40),
                description = null,
                workKey = d.optString("key").takeIf { it.startsWith("/works/") },
                ebookAccess = access,
                iaId = ia,
                // Public-domain scans on the Internet Archive can be downloaded as PDF.
                freePdfUrl = if (access == "public" && ia != null) "https://archive.org/download/$ia/$ia.pdf" else null,
                googlePreviewUrl = null,
            )
        }
    }

    internal fun parseGoogle(json: String): List<CatalogBook> {
        val items = JSONObject(json).optJSONArray("items") ?: return emptyList()
        return (0 until items.length()).mapNotNull { i ->
            val item = items.optJSONObject(i) ?: return@mapNotNull null
            val v = item.optJSONObject("volumeInfo") ?: return@mapNotNull null
            val title = v.optString("title").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val access = item.optJSONObject("accessInfo")
            val pdf = access?.optJSONObject("pdf")
            val date = isoPartial(v.optString("publishedDate"))
            CatalogBook(
                title = title,
                author = strings(v.optJSONArray("authors")).joinToString(", "),
                year = date?.year,
                releaseDate = date,
                pages = v.optInt("pageCount", 0).takeIf { it > 0 },
                coverUrl = v.optJSONObject("imageLinks")?.optString("thumbnail")?.takeIf { it.isNotBlank() }
                    ?.replace("http://", "https://")?.replace("&edge=curl", ""),
                publisher = v.optString("publisher").takeIf { it.isNotBlank() },
                rating = v.optDouble("averageRating", Double.NaN).takeIf { !it.isNaN() && it > 0 },
                subjects = strings(v.optJSONArray("categories")),
                description = v.optString("description").takeIf { it.isNotBlank() }?.let(::stripHtml),
                workKey = null,
                ebookAccess = null,
                iaId = null,
                // Google only offers a PDF download link for free, public-domain books.
                freePdfUrl = if (access?.optBoolean("publicDomain") == true && pdf?.optBoolean("isAvailable") == true)
                    pdf.optString("downloadLink").takeIf { it.startsWith("http") }?.replace("http://", "https://") else null,
                googlePreviewUrl = v.optString("previewLink").takeIf { it.startsWith("http") }?.replace("http://", "https://"),
            )
        }
    }

    /** Combines the two sources, keeping Open Library's order and filling gaps from Google. */
    internal fun merge(primary: List<CatalogBook>, secondary: List<CatalogBook>): List<CatalogBook> {
        val out = LinkedHashMap<String, CatalogBook>()
        for (b in primary + secondary) {
            val key = norm(b.title) + "|" + norm(b.author.substringBefore(','))
            val e = out[key]
            out[key] = if (e == null) b else e.copy(
                releaseDate = if (e.releaseDate == null || (e.releaseDate.dayOfYear == 1 && b.releaseDate?.year == e.releaseDate.year && b.releaseDate.dayOfYear != 1)) b.releaseDate ?: e.releaseDate else e.releaseDate,
                pages = e.pages ?: b.pages,
                coverUrl = e.coverUrl ?: b.coverUrl,
                publisher = e.publisher ?: b.publisher,
                rating = e.rating ?: b.rating,
                subjects = (e.subjects + b.subjects).distinct(),
                description = e.description ?: b.description,
                freePdfUrl = e.freePdfUrl ?: b.freePdfUrl,
                googlePreviewUrl = e.googlePreviewUrl ?: b.googlePreviewUrl,
            )
        }
        return out.values.toList()
    }

    internal fun parseWorkDescription(json: String): String? {
        val o = JSONObject(json)
        val d = o.opt("description")
        val text = when (d) {
            is String -> d
            is JSONObject -> d.optString("value")
            else -> null
        }
        // Open Library descriptions often end with a source line or markdown links.
        return text?.substringBefore("\n----------")?.replace(Regex("""\[([^\]]+)]\([^)]*\)"""), "$1")?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun strings(a: JSONArray?): List<String> = (0 until (a?.length() ?: 0)).mapNotNull { a!!.optString(it).takeIf { s -> s.isNotBlank() } }

    private fun stripHtml(s: String) = s.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n").replace(Regex("<[^>]+>"), "").trim()

    private fun isoPartial(s: String): LocalDate? = runCatching {
        when (s.length) {
            4 -> LocalDate.of(s.toInt(), 1, 1)
            7 -> YearMonth.parse(s).atDay(1)
            else -> LocalDate.parse(s.take(10))
        }
    }.getOrNull()

    private val MONTHS = mapOf("jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6, "jul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12)
    private fun looseDate(s: String): LocalDate? = runCatching {
        val t = s.trim()
        Regex("""^([A-Za-z]+)\.? (\d{1,2}),? (\d{4})$""").find(t)?.let { m ->
            val month = MONTHS[m.groupValues[1].take(3).lowercase(Locale.ROOT)] ?: return@runCatching null
            return@runCatching LocalDate.of(m.groupValues[3].toInt(), month, m.groupValues[2].toInt())
        }
        Regex("""^(\d{1,2}) ([A-Za-z]+)\.? (\d{4})$""").find(t)?.let { m ->
            val month = MONTHS[m.groupValues[2].take(3).lowercase(Locale.ROOT)] ?: return@runCatching null
            return@runCatching LocalDate.of(m.groupValues[3].toInt(), month, m.groupValues[1].toInt())
        }
        null
    }.getOrNull()

    private fun norm(s: String) = s.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }
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

private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
