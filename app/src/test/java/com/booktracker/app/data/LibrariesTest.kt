package com.booktracker.app.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        assertEquals("https://search.worldcat.org/search?q=Emma%20Jane%20Austen", BuyLinks.worldCat("Emma", "Jane Austen"))
    }
}

class LibrariesTest {
    @Test
    fun catalogSearchUsesTheReadersCatalog() {
        val lib = Library("Springfield Library", catalogUrl = "https://catalog.example.org/search?q=BookTracker&type=all")
        assertTrue(lib.hasCatalog)
        assertEquals("https://catalog.example.org/search?q=The%20Hobbit&type=all", lib.catalogSearch("The Hobbit"))
        val braces = Library("X", catalogUrl = "https://x.org/s/{q}")
        assertEquals("https://x.org/s/Dune", braces.catalogSearch("Dune"))
        val none = Library("Y")
        assertFalse(none.hasCatalog)
        assertEquals("https://search.worldcat.org/search?q=Dune", none.catalogSearch("Dune"))
    }

    @Test
    fun linksGetAScheme() {
        val lib = Library("Z", website = "zlib.org")
        assertEquals("https://zlib.org", lib.websiteLink)
        assertEquals("https://zlib.org", lib.accountLink)
        assertEquals("https://login.zlib.org", lib.copy(accountUrl = "https://login.zlib.org").accountLink)
        assertNull(Library("No site").accountLink)
    }

    @Test
    fun savesAndLoads() {
        val lib = Library("Central", "1 Main St, Town", 1.5, 2.5, "https://c.org", "555", "Mo-Fr 09:00-17:00", 3.2, "1234", "https://c.org/login", "https://c.org/s?q={q}")
        val back = Library.fromJson(JSONObject(lib.toJson().toString()))
        assertEquals(lib.copy(distanceKm = null), back)
        val bare = Library.fromJson(JSONObject(Library("Only name").toJson().toString()))
        assertEquals(Library("Only name"), bare)
    }

    @Test
    fun roundsLocationToAboutOneKilometre() {
        assertEquals(40.71 to -74.01, LibraryFinder.roundForPrivacy(40.712776, -74.005974))
        val q = LibraryFinder.overpassQuery(40.71, -74.01, 15)
        assertTrue(q, q.contains("around:15000,40.71,-74.01") && q.contains("\"amenity\"=\"library\""))
    }

    @Test
    fun parsesOverpassNearestFirst() {
        val json = JSONObject(
            """
            {"elements":[
              {"type":"way","id":2,"center":{"lat":40.80,"lon":-74.01},
               "tags":{"amenity":"library","name":"Far Library","website":"far.org"}},
              {"type":"node","id":1,"lat":40.72,"lon":-74.01,
               "tags":{"amenity":"library","name":"Near Library","addr:housenumber":"5","addr:street":"Main St",
                       "addr:city":"Town","addr:state":"NY","opening_hours":"Mo-Sa 10:00-18:00","phone":"+1 555 0100"}},
              {"type":"node","id":3,"lat":40.72,"lon":-74.0,"tags":{"amenity":"library"}},
              {"type":"node","id":4,"lat":40.72,"lon":-74.0,"tags":{"amenity":"library","name":"Staff Only","access":"private"}}
            ]}
            """.trimIndent(),
        )
        val libs = LibraryFinder.parseOverpass(json, 40.71, -74.01)
        assertEquals(listOf("Near Library", "Far Library"), libs.map { it.name })
        val near = libs[0]
        assertEquals("5 Main St, Town, NY", near.address)
        assertEquals("Mo-Sa 10:00-18:00", near.hours)
        assertEquals("+1 555 0100", near.phone)
        assertEquals(1.1, near.distanceKm!!, 0.05)
        assertEquals("far.org", libs[1].website)
        assertEquals(40.80, libs[1].lat!!, 1e-9)
    }

    @Test
    fun parsesNominatimLibrariesOnly() {
        val json = JSONArray(
            """
            [{"name":"Boston Public Library","type":"library","lat":"42.349","lon":"-71.078",
              "address":{"house_number":"700","road":"Boylston Street","city":"Boston","state":"Massachusetts"},
              "extratags":{"website":"https://www.bpl.org","opening_hours":"Mo-Th 09:00-20:00"}},
             {"name":"Boston","type":"city","lat":"42.3","lon":"-71.0"}]
            """.trimIndent(),
        )
        val libs = LibraryFinder.parseNominatim(json)
        assertEquals(1, libs.size)
        assertEquals("700 Boylston Street, Boston, Massachusetts", libs[0].address)
        assertEquals("https://www.bpl.org", libs[0].website)
        assertEquals(42.349, libs[0].lat!!, 1e-9)
        assertNull(libs[0].distanceKm)
    }
}
