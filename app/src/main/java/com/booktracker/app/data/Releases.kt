package com.booktracker.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class Release(val version: String, val name: String, val date: String, val notes: String, val apkUrl: String?, val pageUrl: String)

/** The app's GitHub releases, for "What's new" and update checks. */
object Releases {
    const val REPO = "uzumakigojo78-prog/Book-Tracker-"
    const val REPO_URL = "https://github.com/$REPO"
    const val RELEASES_URL = "$REPO_URL/releases"
    const val ISSUES_URL = "$REPO_URL/issues/new"
    const val WEB_APP_URL = "https://uzumakigojo78-prog.github.io/Book-Tracker-/"

    suspend fun fetch(count: Int = 8): List<Release> = withContext(Dispatchers.IO) {
        val conn = URL("https://api.github.com/repos/$REPO/releases?per_page=$count").openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "BookTracker-Android")
            if (conn.responseCode !in 200..299) error("GitHub returned ${conn.responseCode}")
            parse(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }

    internal fun parse(json: String): List<Release> {
        val a = JSONArray(json)
        return (0 until a.length()).mapNotNull { i ->
            val r = a.optJSONObject(i) ?: return@mapNotNull null
            if (r.optBoolean("draft")) return@mapNotNull null
            val assets = r.optJSONArray("assets")
            val apk = (0 until (assets?.length() ?: 0)).mapNotNull { assets!!.optJSONObject(it) }
                .firstOrNull { it.optString("name").endsWith(".apk") }?.optString("browser_download_url")
            Release(
                version = r.optString("tag_name").removePrefix("v"),
                name = r.optString("name").ifBlank { r.optString("tag_name") },
                date = r.optString("published_at").take(10),
                notes = r.optString("body").trim(),
                apkUrl = apk,
                pageUrl = r.optString("html_url"),
            )
        }
    }

    /** True if [candidate] (e.g. "1.0.31") is newer than [current] (e.g. "1.0.30"). */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = candidate.split('.', '-').map { it.toIntOrNull() ?: 0 }
        val b = current.split('.', '-').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
