package com.booktracker.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/** A public library, plus (once it's the reader's) their card and links. */
data class Library(
    val name: String,
    val address: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val website: String? = null,
    val phone: String? = null,
    val hours: String? = null,
    /** Only set on search results; never saved. */
    val distanceKm: Double? = null,
    val cardNumber: String = "",
    /** Where the reader signs in to their account (holds, loans, fines). */
    val accountUrl: String = "",
    /**
     * The library's catalog search address. "{q}" (or the word "booktracker") is replaced
     * with what to search for.
     */
    val catalogUrl: String = "",
) {
    /** A search of this library's own catalog, once the reader has linked it. */
    fun catalogSearch(query: String): String? {
        val template = catalogUrl.trim()
        val e = BuyLinks.enc(query)
        return when {
            template.contains("{q}") -> template.replace("{q}", e)
            template.contains(PLACEHOLDER, ignoreCase = true) -> template.replace(Regex(PLACEHOLDER, RegexOption.IGNORE_CASE), e)
            else -> null
        }
    }

    val hasCatalog: Boolean get() = catalogUrl.contains("{q}") || catalogUrl.contains(PLACEHOLDER, ignoreCase = true)

    val accountLink: String? get() = accountUrl.ifBlank { website.orEmpty() }.takeIf { it.isNotBlank() }?.let(::withScheme)

    val websiteLink: String? get() = website?.takeIf { it.isNotBlank() }?.let(::withScheme)

    val mapUrl: String
        get() = "https://www.google.com/maps/search/?api=1&query=${BuyLinks.enc(listOfNotNull(name, address).joinToString(", "))}"

    fun toJson(): JSONObject = JSONObject()
        .put("name", name).put("address", address).put("lat", lat).put("lon", lon)
        .put("website", website).put("phone", phone).put("hours", hours)
        .put("cardNumber", cardNumber).put("accountUrl", accountUrl).put("catalogUrl", catalogUrl)

    companion object {
        const val PLACEHOLDER = "booktracker"

        fun fromJson(o: JSONObject): Library = Library(
            name = o.optString("name"),
            address = o.str("address"),
            lat = o.optDouble("lat").takeUnless { it.isNaN() },
            lon = o.optDouble("lon").takeUnless { it.isNaN() },
            website = o.str("website"),
            phone = o.str("phone"),
            hours = o.str("hours"),
            cardNumber = o.optString("cardNumber"),
            accountUrl = o.optString("accountUrl"),
            catalogUrl = o.optString("catalogUrl"),
        )

        private fun withScheme(url: String) = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
    }
}

/**
 * Finds public libraries with OpenStreetMap. A location is only used for this one
 * search: it is rounded to about 1 km first and never saved.
 */
object LibraryFinder {

    suspend fun near(lat: Double, lon: Double, radiusKm: Int = 15): List<Library> {
        val (rLat, rLon) = roundForPrivacy(lat, lon)
        val body = "data=" + URLEncoder.encode(overpassQuery(rLat, rLon, radiusKm), "UTF-8")
        val json = http("https://overpass-api.de/api/interpreter", body)
        return parseOverpass(JSONObject(json), rLat, rLon)
    }

    /** By library name, city or ZIP code: no location needed. */
    suspend fun search(text: String): List<Library> {
        val q = text.trim()
        if (q.isEmpty()) return emptyList()
        val phrase = if (q.contains("librar", ignoreCase = true)) q else "library in $q"
        val url = "https://nominatim.openstreetmap.org/search?format=jsonv2&addressdetails=1&extratags=1&limit=20&q=" +
            URLEncoder.encode(phrase, "UTF-8")
        return parseNominatim(JSONArray(http(url, null)))
    }

    /** Two decimal places is roughly 1 km: plenty to find libraries, not enough to find a home. */
    fun roundForPrivacy(lat: Double, lon: Double): Pair<Double, Double> =
        round(lat * 100) / 100 to round(lon * 100) / 100

    internal fun overpassQuery(lat: Double, lon: Double, radiusKm: Int): String {
        val around = "around:${radiusKm * 1000},$lat,$lon"
        return "[out:json][timeout:25];(node[\"amenity\"=\"library\"]($around);way[\"amenity\"=\"library\"]($around);" +
            "relation[\"amenity\"=\"library\"]($around););out center tags 60;"
    }

    internal fun parseOverpass(root: JSONObject, fromLat: Double?, fromLon: Double?): List<Library> {
        val elements = root.optJSONArray("elements") ?: return emptyList()
        val out = (0 until elements.length()).mapNotNull { i ->
            val el = elements.optJSONObject(i) ?: return@mapNotNull null
            val tags = el.optJSONObject("tags") ?: return@mapNotNull null
            val name = tags.str("name") ?: return@mapNotNull null
            // Private and school libraries aren't much use to the public.
            if (tags.str("access") in setOf("private", "no")) return@mapNotNull null
            val center = el.optJSONObject("center")
            val lat = el.optDouble("lat").takeUnless { it.isNaN() } ?: center?.optDouble("lat")?.takeUnless { it.isNaN() }
            val lon = el.optDouble("lon").takeUnless { it.isNaN() } ?: center?.optDouble("lon")?.takeUnless { it.isNaN() }
            val street = listOfNotNull(tags.str("addr:housenumber"), tags.str("addr:street")).joinToString(" ").ifBlank { null }
            val address = listOfNotNull(street, tags.str("addr:city"), tags.str("addr:state") ?: tags.str("addr:postcode"))
                .joinToString(", ").ifBlank { null }
            Library(
                name = name,
                address = address,
                lat = lat,
                lon = lon,
                website = tags.str("website") ?: tags.str("contact:website") ?: tags.str("url"),
                phone = tags.str("phone") ?: tags.str("contact:phone"),
                hours = tags.str("opening_hours"),
                distanceKm = if (fromLat != null && fromLon != null && lat != null && lon != null) distanceKm(fromLat, fromLon, lat, lon) else null,
            )
        }
        return out.distinctBy { it.name.lowercase(Locale.ROOT) to it.address }
            .sortedBy { it.distanceKm ?: Double.MAX_VALUE }
            .take(30)
    }

    internal fun parseNominatim(results: JSONArray): List<Library> = (0 until results.length()).mapNotNull { i ->
        val r = results.optJSONObject(i) ?: return@mapNotNull null
        if (r.optString("type") != "library") return@mapNotNull null
        val a = r.optJSONObject("address")
        val extra = r.optJSONObject("extratags")
        val name = r.str("name") ?: a?.str("amenity") ?: return@mapNotNull null
        val street = listOfNotNull(a?.str("house_number"), a?.str("road")).joinToString(" ").ifBlank { null }
        val city = a?.str("city") ?: a?.str("town") ?: a?.str("village") ?: a?.str("suburb")
        Library(
            name = name,
            address = listOfNotNull(street, city, a?.str("state")).joinToString(", ").ifBlank { null },
            lat = r.optString("lat").toDoubleOrNull(),
            lon = r.optString("lon").toDoubleOrNull(),
            website = extra?.str("website") ?: extra?.str("contact:website"),
            phone = extra?.str("phone") ?: extra?.str("contact:phone"),
            hours = extra?.str("opening_hours"),
        )
    }.distinctBy { it.name.lowercase(Locale.ROOT) to it.address }

    /** Great-circle distance in kilometres. */
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * 6371.0 * asin(sqrt(h))
    }

    private suspend fun http(url: String, postBody: String?): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            // OpenStreetMap asks apps to identify themselves.
            conn.setRequestProperty("User-Agent", "BookTracker/1.0 (https://github.com/${Releases.REPO})")
            conn.setRequestProperty("Accept", "application/json")
            if (postBody != null) {
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                conn.outputStream.use { it.write(postBody.toByteArray()) }
            }
            if (conn.responseCode !in 200..299) error("Library search failed (HTTP ${conn.responseCode})")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}

private fun JSONObject.str(key: String): String? =
    if (isNull(key)) null else optString(key).trim().takeIf { it.isNotEmpty() }
