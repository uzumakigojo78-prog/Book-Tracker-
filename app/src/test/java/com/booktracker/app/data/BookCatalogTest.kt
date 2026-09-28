package com.booktracker.app.data

import com.booktracker.app.ai.BookCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BookCatalogTest {

    private val ol = """{"docs":[
        {"key":"/works/OL1W","title":"Pride and Prejudice","author_name":["Jane Austen"],"first_publish_year":1813,
         "number_of_pages_median":432,"cover_i":9,"publisher":["T. Egerton"],"ratings_average":4.2,
         "subject":["Fiction","Love stories","Classic Literature"],"ebook_access":"public","ia":["prideprejudice00aust"],
         "publish_date":["January 28, 1813","1990"]},
        {"key":"/works/OL2W","title":"Project Hail Mary","author_name":["Andy Weir"],"first_publish_year":2021,
         "ebook_access":"no_ebook","ia":["x"],"subject":["Science fiction"]}]}"""

    private val google = """{"items":[
        {"id":"g1","volumeInfo":{"title":"Project Hail Mary","authors":["Andy Weir"],"publishedDate":"2021-05-04","pageCount":496,
          "description":"A <b>lone</b> astronaut.<br>Must save Earth.","previewLink":"http://books.google.com/books?id=g1"},
         "accessInfo":{"publicDomain":false,"pdf":{"isAvailable":true,"downloadLink":"http://example.com/nope.pdf"}}},
        {"id":"g2","volumeInfo":{"title":"Emma","authors":["Jane Austen"],"publishedDate":"1815"},
         "accessInfo":{"publicDomain":true,"pdf":{"isAvailable":true,"downloadLink":"http://books.google.com/books/download/Emma.pdf?id=g2"}}}]}"""

    @Test
    fun parsesAndMergesSources() {
        val merged = BookCatalog.merge(BookCatalog.parseOpenLibrary(ol), BookCatalog.parseGoogle(google))
        assertEquals(listOf("Pride and Prejudice", "Project Hail Mary", "Emma"), merged.map { it.title })

        val pride = merged[0]
        assertEquals(LocalDate.of(1813, 1, 28), pride.releaseDate)
        assertEquals("https://archive.org/download/prideprejudice00aust/prideprejudice00aust.pdf", pride.freePdfUrl)
        assertEquals("T. Egerton", pride.publisher)
        assertTrue(pride.genres.isNotEmpty())

        // Not public domain: no PDF even though Google lists one; details filled from Google.
        val hail = merged[1]
        assertNull(hail.freePdfUrl)
        assertNull(hail.archiveUrl)
        assertEquals(496, hail.pages)
        assertEquals(LocalDate.of(2021, 5, 4), hail.releaseDate)
        assertEquals("A lone astronaut.\nMust save Earth.", hail.description)
        assertEquals("https://books.google.com/books?id=g1", hail.googlePreviewUrl)

        assertEquals("https://books.google.com/books/download/Emma.pdf?id=g2", merged[2].freePdfUrl)
    }

    @Test
    fun readsWorkDescriptions() {
        assertEquals("Plain text.", BookCatalog.parseWorkDescription("""{"description":"Plain text."}"""))
        assertEquals(
            "A classic see Wikipedia.",
            BookCatalog.parseWorkDescription("""{"description":{"type":"/type/text","value":"A classic see [Wikipedia](https://w.org).\n----------\nSource"}}"""),
        )
        assertNull(BookCatalog.parseWorkDescription("""{"title":"x"}"""))
    }
}
