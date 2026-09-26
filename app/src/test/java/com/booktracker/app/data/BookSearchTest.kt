package com.booktracker.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BookSearchTest {

    private val openLibraryJson = """
        {"numFound": 2, "docs": [
          {"title": "Project Hail Mary", "author_name": ["Andy Weir"], "first_publish_year": 2021,
           "number_of_pages_median": 496, "cover_i": 10389354,
           "publish_date": ["2022", "May 04, 2021", "Oct 04, 2022", "4 May 2021"]},
          {"title": "Untitled Notes"}
        ]}
    """.trimIndent()

    private val googleJson = """
        {"items": [
          {"volumeInfo": {"title": "Project Hail Mary", "authors": ["Andy Weir"], "publishedDate": "2021-05-04",
            "pageCount": 480, "imageLinks": {"thumbnail": "http://books.google.com/books/content?id=abc&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api"}}},
          {"volumeInfo": {"title": "The Martian", "authors": ["Andy Weir", "Someone Else"], "publishedDate": "2014-02"}},
          {"volumeInfo": {"title": "Artemis", "publishedDate": "2017"}}
        ]}
    """.trimIndent()

    @Test
    fun parsesOpenLibrary() {
        val results = BookSearch.parseOpenLibrary(openLibraryJson)
        assertEquals(2, results.size)
        val phm = results[0]
        assertEquals("Project Hail Mary", phm.title)
        assertEquals("Andy Weir", phm.author)
        assertEquals(LocalDate.of(2021, 5, 4), phm.releaseDate)
        assertEquals(496, phm.pages)
        assertEquals("https://covers.openlibrary.org/b/id/10389354-M.jpg?default=false", phm.coverUrl)

        val bare = results[1]
        assertEquals("", bare.author)
        assertNull(bare.releaseDate)
        assertNull(bare.pages)
        assertNull(bare.coverUrl)
    }

    @Test
    fun parsesGoogleBooks() {
        val results = BookSearch.parseGoogle(googleJson)
        assertEquals(3, results.size)
        assertEquals(LocalDate.of(2021, 5, 4), results[0].releaseDate)
        assertEquals(
            "https://books.google.com/books/content?id=abc&printsec=frontcover&img=1&zoom=1&source=gbs_api",
            results[0].coverUrl,
        )
        assertEquals("Andy Weir, Someone Else", results[1].author)
        assertEquals(LocalDate.of(2014, 2, 1), results[1].releaseDate)
        assertNull(results[1].coverUrl)
        assertEquals(LocalDate.of(2017, 1, 1), results[2].releaseDate)
    }

    @Test
    fun mergesSameBookFromBothSources() {
        val openLibrary = listOf(BookSuggestion("Dune", "Frank Herbert", LocalDate.of(1965, 1, 1), null, "ol-cover"))
        val google = listOf(
            BookSuggestion("DUNE", "Frank Herbert", LocalDate.of(1965, 8, 1), 412, "g-cover"),
            BookSuggestion("Dune Messiah", "Frank Herbert", null, 256, null),
        )
        val merged = BookSearch.merge(openLibrary, google)
        assertEquals(2, merged.size)
        assertEquals(LocalDate.of(1965, 8, 1), merged[0].releaseDate) // precise date wins over year placeholder
        assertEquals(412, merged[0].pages)
        assertEquals("ol-cover", merged[0].coverUrl)
        assertEquals("Dune Messiah", merged[1].title)
    }
}
