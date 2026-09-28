package com.booktracker.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/**
 * Library catalogs (what's on the shelf at the reader's library). Recognises the catalog
 * systems most public libraries use and turns any page of one into a search address with
 * "{q}" where the search words go.
 */
object Catalogs {

    /**
     * A search template for [input]: a catalog page from a known system, or a results page for
     * the word "booktracker". Null if we can't tell how to search it.
     */
    fun template(input: String): String? {
        val raw = input.trim()
        if (raw.isEmpty()) return null
        val url = if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) raw else "https://$raw"
        if (url.contains("{q}") || url.contains(Library.PLACEHOLDER, ignoreCase = true)) return url
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        val host = uri.host?.lowercase() ?: return null
        val base = "${uri.scheme.lowercase()}://${uri.rawAuthority}"
        val path = uri.rawPath.orEmpty()
        val sirsi = Regex("/client/([a-z]{2}_[A-Za-z]{2})/([^/?#]+)").find(path)
        return when {
            host.endsWith("bibliocommons.com") -> "$base/v2/search?query={q}&searchType=smart"
            host.contains("iiivega.com") -> "$base/search?query={q}&searchType=everything"
            path.contains("/polaris", true) -> "$base/polaris/search/searchresults.aspx?ctx=1.1033.0.0.1&type=Keyword&term={q}"
            path.contains("/cgi-bin/koha", true) -> "$base/cgi-bin/koha/opac-search.pl?q={q}"
            path.contains("/eg/opac", true) -> "$base/eg/opac/results?query={q}&qtype=keyword"
            path.contains("/iii/encore", true) -> "$base/iii/encore/search/C__S{q}__Orightresult"
            sirsi != null -> "$base/client/${sirsi.groupValues[1]}/${sirsi.groupValues[2]}/search/results?qu={q}"
            host.contains("aspendiscovery") || path.contains("/Search/Results", true) || path.contains("/GroupedWork/", true) ->
                "$base/Search/Results?lookfor={q}&searchIndex=Keyword"
            else -> null
        }
    }

    /** Looks through a library website's links for its catalog. */
    fun findInHtml(html: String, pageUrl: String): String? {
        val page = runCatching { URI(pageUrl) }.getOrNull()
        return HREF.findAll(html)
            .map { it.groupValues[1].replace("&amp;", "&").trim() }
            .filter { it.startsWith("http", true) || it.startsWith("/") }
            .mapNotNull { href -> runCatching { page?.resolve(href)?.toString() ?: href }.getOrNull() }
            // A link that already contains the placeholder word would be a coincidence.
            .filterNot { it.contains(Library.PLACEHOLDER, ignoreCase = true) || it.contains("{q}") }
            .firstNotNullOfOrNull(::template)
    }

    /** Finds the catalog from the library's website: the website itself, or a link on it. */
    suspend fun detect(website: String): String? = withContext(Dispatchers.IO) {
        template(website)?.let { return@withContext it }
        val url = if (website.startsWith("http", true)) website else "https://$website"
        runCatching {
            val conn = URL(url).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 10_000
                conn.readTimeout = 15_000
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) BookTracker/1.0")
                if (conn.responseCode !in 200..299) return@runCatching null
                val finalUrl = conn.url.toString()
                template(finalUrl) ?: findInHtml(conn.inputStream.bufferedReader().use { it.readText().take(2_000_000) }, finalUrl)
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private val HREF = Regex("""href\s*=\s*["']([^"'#][^"']*)["']""", RegexOption.IGNORE_CASE)
}
