package com.booktracker.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuyLinksTest {
    @Test
    fun storesSearchForTitleAndAuthor() {
        val stores = BuyLinks.forBook("Dune", "Frank Herbert")
        val byId = stores.associateBy { it.id }
        assertEquals("https://www.amazon.com/s?k=Dune%20Frank%20Herbert&i=stripbooks", byId.getValue("amazon").url)
        assertEquals("https://www.barnesandnoble.com/s/Dune%20Frank%20Herbert", byId.getValue("bn").url)
        assertTrue(byId.getValue("bookshop").url.startsWith("https://bookshop.org/search?keywords=Dune"))
        assertEquals(stores.size, stores.map { it.id }.toSet().size)
        assertTrue(stores.all { it.url.startsWith("https://") && !it.url.contains(' ') && !it.url.contains('+') })
    }

    @Test
    fun encodesAwkwardTitles() {
        val url = BuyLinks.forBook("Harry Potter & the Philosopher's Stone", "").first { it.id == "amazon" }.url
        assertTrue(url, url.contains("Harry%20Potter%20%26%20the%20Philosopher%27s%20Stone&"))
    }
}
