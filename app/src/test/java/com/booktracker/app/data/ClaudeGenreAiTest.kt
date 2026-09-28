package com.booktracker.app.data

import com.booktracker.app.ai.AiException
import com.booktracker.app.ai.AnalysisSource
import com.booktracker.app.ai.ClaudeGenreAi
import com.booktracker.app.ai.GenreAnalysis
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread
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
        val message = JSONObject()
            .put("id", "msg_test").put("type", "message").put("role", "assistant").put("model", "claude-opus-5")
            .put("content", org.json.JSONArray().put(JSONObject().put("type", "text").put("text", reply)))
            .put("stop_reason", "end_turn").put("stop_sequence", JSONObject.NULL)
            .put("usage", JSONObject().put("input_tokens", 10).put("output_tokens", 20))
            .toString()
        val server = OneShotHttpServer(message)
        val result = runBlocking {
            ClaudeGenreAi("sk-test", "http://127.0.0.1:${server.port}").analyze(books, "sig")
        }
        server.join()
        val sent = JSONObject(server.body)
        assertEquals("/v1/messages", server.path)
        assertEquals("claude-opus-5", sent.getString("model"))
        assertEquals("default", sent.getString("fallbacks"))
        assertTrue(server.headers["anthropic-beta"].orEmpty().contains("server-side-fallback-2026-07-01"))
        assertEquals("sk-test", server.headers["x-api-key"])
        assertTrue(sent.getJSONArray("messages").getJSONObject(0).toString().contains("The Hobbit"))
        assertEquals(2, result.recommendations.size)
    }
}

/** Minimal HTTP server that answers a single POST with [response] and records the request. */
private class OneShotHttpServer(response: String) {
    private val socket = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
    val port: Int = socket.localPort
    var path = ""
    var body = ""
    val headers = mutableMapOf<String, String>()
    private val thread = thread {
        socket.use { server ->
            server.accept().use { conn ->
                val input = conn.getInputStream().buffered()
                fun readLine(): String {
                    val sb = StringBuilder()
                    while (true) {
                        val c = input.read()
                        if (c == -1 || c == '\n'.code) break
                        if (c != '\r'.code) sb.append(c.toChar())
                    }
                    return sb.toString()
                }
                path = readLine().split(" ").getOrElse(1) { "" }
                while (true) {
                    val line = readLine()
                    if (line.isEmpty()) break
                    val i = line.indexOf(':')
                    if (i > 0) headers[line.substring(0, i).trim().lowercase()] = line.substring(i + 1).trim()
                }
                val length = headers["content-length"]?.toInt() ?: 0
                val bytes = ByteArray(length)
                var read = 0
                while (read < length) {
                    val n = input.read(bytes, read, length - read)
                    if (n < 0) break
                    read += n
                }
                body = bytes.decodeToString()
                val payload = response.toByteArray()
                val out = conn.getOutputStream()
                out.write(
                    ("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${payload.size}\r\n" +
                        "Connection: close\r\n\r\n").toByteArray()
                )
                out.write(payload)
                out.flush()
            }
        }
    }

    fun join() = thread.join(5000)
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
