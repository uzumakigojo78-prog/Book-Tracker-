package com.booktracker.app.data

import com.booktracker.app.ai.AiException
import com.booktracker.app.ai.AnalysisSource
import com.booktracker.app.ai.ClaudeGenreAi
import com.booktracker.app.ai.GenreAnalysis
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress
import java.time.LocalDate

class ClaudeGenreAiTest {

    private val books = listOf(
        Book("b1", "The Hobbit", "J.R.R. Tolkien", LocalDate.of(1937, 9, 21), 310,
            entries = listOf(ReadingEntry(LocalDate.now(), 310))),
        Book("b2", "Dune", "Frank Herbert", null, 600),
    )

    private val reply = """
        Here you go:
        {"books":[{"id":"b1","genres":["fantasy","Adventure"]},{"id":"b2","genres":["Sci-Fi"]}],
         "summary":"You love epic journeys.",
         "recommendations":[
           {"title":"The Name of the Wind","author":"Patrick Rothfuss","year":2007,"genre":"Fantasy","reason":"Like The Hobbit."},
           {"title":"the hobbit","author":"J.R.R. Tolkien","year":1937,"genre":"Fantasy","reason":"Already owned"},
           {"title":"Hyperion","author":"Dan Simmons","year":null,"genre":"Space opera","reason":"Like Dune."}]}
    """.trimIndent()

    @Test
    fun parsesReplyAndNormalizesGenres() {
        val a = ClaudeGenreAi.parseAnalysis(reply, books, "sig", 42L)
        assertEquals(AnalysisSource.AI, a.source)
        assertEquals(listOf("Fantasy", "Adventure"), a.bookGenres["b1"])
        assertEquals(listOf("Science Fiction"), a.bookGenres["b2"])
        assertEquals("You love epic journeys.", a.summary)
        // Books already in the library are dropped.
        assertEquals(listOf("The Name of the Wind", "Hyperion"), a.recommendations.map { it.title })
        assertEquals("Science Fiction", a.recommendations[1].genre)
        assertEquals(null, a.recommendations[1].year)
        // Survives a save/load round trip.
        assertEquals(a, GenreAnalysis.fromJson(a.toJson()))
    }

    @Test(expected = AiException::class)
    fun rejectsNonJson() {
        ClaudeGenreAi.parseAnalysis("Sorry, I can't help.", books, "sig", 0L)
    }

    @Test
    fun sendsExpectedRequestThroughSdk() {
        var body = ""
        var beta = ""
        var apiKey = ""
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/v1/messages") { ex ->
            body = ex.requestBody.readBytes().decodeToString()
            beta = ex.requestHeaders.getFirst("anthropic-beta") ?: ""
            apiKey = ex.requestHeaders.getFirst("x-api-key") ?: ""
            val message = JSONObject()
                .put("id", "msg_test").put("type", "message").put("role", "assistant").put("model", "claude-opus-5")
                .put("content", org.json.JSONArray().put(JSONObject().put("type", "text").put("text", reply)))
                .put("stop_reason", "end_turn").put("stop_sequence", JSONObject.NULL)
                .put("usage", JSONObject().put("input_tokens", 10).put("output_tokens", 20))
                .toString().toByteArray()
            ex.responseHeaders.add("content-type", "application/json")
            ex.sendResponseHeaders(200, message.size.toLong())
            ex.responseBody.use { it.write(message) }
        }
        server.start()
        try {
            val result = runBlocking {
                ClaudeGenreAi("sk-test", "http://127.0.0.1:${server.address.port}").analyze(books, "sig")
            }
            val sent = JSONObject(body)
            assertEquals("claude-opus-5", sent.getString("model"))
            assertEquals("default", sent.getString("fallbacks"))
            assertTrue(beta.contains("server-side-fallback-2026-07-01"))
            assertEquals("sk-test", apiKey)
            assertTrue(sent.getJSONArray("messages").getJSONObject(0).toString().contains("The Hobbit"))
            assertEquals(2, result.recommendations.size)
        } finally {
            server.stop(0)
        }
    }
}

class OpenLibraryAiTest {
    @Test
    fun picksSpecificGenresFromSubjects() {
        val g = com.booktracker.app.ai.OpenLibraryAi.genresFromSubjects(
            listOf("Fiction", "Fantasy fiction", "Dragons", "Fiction, fantasy, epic", "Adventure and adventurers", "English literature")
        )
        assertEquals("Fantasy", g[0])
        assertEquals(2, g.size)
    }

    @Test
    fun buildsLegalCopyLinks() {
        val doc = JSONObject("""{"key":"/works/OL27448W","ebook_access":"public","ia":["hobbit00tolk"],"cover_i":123,"number_of_pages_median":310}""")
        val c = com.booktracker.app.ai.OpenLibraryAi.copiesFrom(doc, "The Hobbit Tolkien")
        assertEquals("https://openlibrary.org/works/OL27448W", c.openLibraryUrl)
        assertEquals("https://archive.org/details/hobbit00tolk", c.archiveUrl)
        assertTrue(c.isFreeDownload)
        assertEquals(310, c.pages)
        assertTrue(c.gutenbergUrl.startsWith("https://www.gutenberg.org/ebooks/search/?query=The+Hobbit"))

        val none = com.booktracker.app.ai.OpenLibraryAi.copiesFrom(JSONObject("""{"key":"/works/X","ebook_access":"no_ebook","ia":["x"]}"""), "q")
        assertEquals(null, none.archiveUrl)
    }
}
