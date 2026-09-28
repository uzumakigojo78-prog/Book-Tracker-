package com.booktracker.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleasesTest {
    @Test
    fun comparesVersions() {
        assertTrue(Releases.isNewer("1.0.31", "1.0.30"))
        assertTrue(Releases.isNewer("1.1.0", "1.0.99"))
        assertFalse(Releases.isNewer("1.0.30", "1.0.30"))
        assertFalse(Releases.isNewer("1.0.9", "1.0.30"))
    }

    @Test
    fun parsesReleases() {
        val json = """[
          {"tag_name":"v1.0.31","name":"Book Tracker 1.0.31","published_at":"2026-09-29T10:00:00Z","body":"Add widgets","html_url":"https://x/r/31",
           "assets":[{"name":"BookTracker-1.0.31.apk","browser_download_url":"https://x/a.apk"}]},
          {"tag_name":"v1.0.30","draft":true,"assets":[]}
        ]"""
        val r = Releases.parse(json)
        assertEquals(1, r.size)
        assertEquals("1.0.31", r[0].version)
        assertEquals("2026-09-29", r[0].date)
        assertEquals("https://x/a.apk", r[0].apkUrl)
    }
}
