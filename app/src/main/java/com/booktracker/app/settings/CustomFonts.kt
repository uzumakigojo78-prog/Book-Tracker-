package com.booktracker.app.settings

import android.content.Context
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Any Google Font as the app's main font. Fonts are downloaded once from Google
 * Fonts (regular and bold) and kept in app storage so they work offline.
 */
object CustomFonts {

    /** Popular Google Fonts offered as suggestions; any other family name can be searched too. */
    val POPULAR = listOf(
        "Roboto", "Open Sans", "Lato", "Montserrat", "Poppins", "Inter", "Nunito", "Raleway", "Oswald", "Merriweather",
        "Playfair Display", "Lora", "PT Serif", "Noto Serif", "Source Serif 4", "Libre Baskerville", "EB Garamond",
        "Cormorant Garamond", "Crimson Text", "Bitter", "Rubik", "Work Sans", "Quicksand", "Mulish", "Karla", "Manrope",
        "DM Sans", "DM Serif Display", "Josefin Sans", "Fira Sans", "Barlow", "Cabin", "Hind", "Heebo", "Outfit",
        "Space Grotesk", "Sora", "Urbanist", "Lexend", "Plus Jakarta Sans", "Figtree", "Red Hat Display", "Archivo",
        "Bebas Neue", "Anton", "Abril Fatface", "Alfa Slab One", "Righteous", "Lobster", "Pacifico", "Dancing Script",
        "Caveat", "Satisfy", "Great Vibes", "Shadows Into Light", "Indie Flower", "Permanent Marker", "Amatic SC",
        "Kalam", "Patrick Hand", "Comfortaa", "Fredoka", "Baloo 2", "Varela Round", "Nunito Sans", "Titillium Web",
        "Exo 2", "Orbitron", "Audiowide", "Press Start 2P", "VT323", "Special Elite", "Courier Prime", "Space Mono",
        "IBM Plex Sans", "IBM Plex Serif", "IBM Plex Mono", "JetBrains Mono", "Fira Code", "Source Code Pro",
        "Roboto Slab", "Roboto Mono", "Roboto Condensed", "Zilla Slab", "Arvo", "Vollkorn", "Cardo", "Spectral",
        "Alegreya", "Old Standard TT", "Cinzel", "Marcellus", "Philosopher", "Kaushan Script", "Sacramento",
        "Atkinson Hyperlegible", "Lexend Deca", "Noto Sans", "Ubuntu", "Oxygen", "Asap", "Signika", "Chivo", "Syne",
    ).sorted()

    private fun dir(context: Context) = File(context.filesDir, "fonts").apply { mkdirs() }
    private fun slug(family: String) = family.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

    private fun files(context: Context, family: String): List<Pair<Int, File>> =
        dir(context).listFiles().orEmpty()
            .filter { it.name.startsWith(slug(family) + "__") && it.name.endsWith(".ttf") && it.length() > 0 }
            .mapNotNull { f -> f.name.substringAfter("__").removeSuffix(".ttf").toIntOrNull()?.let { it to f } }

    fun isDownloaded(context: Context, family: String) = files(context, family).isNotEmpty()

    /** Fonts already on the phone, by family name. */
    fun downloaded(context: Context): List<String> =
        dir(context).listFiles().orEmpty().filter { it.name.endsWith(".ttf") }.map { it.name.substringBefore("__") }.distinct()
            .mapNotNull { slug -> readName(context, slug) }.sorted()

    private fun nameFile(context: Context, slug: String) = File(dir(context), "$slug.name")
    private fun readName(context: Context, slug: String) = nameFile(context, slug).takeIf { it.exists() }?.readText()?.trim()

    /** The downloaded family, or null if it isn't on the phone. */
    fun family(context: Context, family: String): FontFamily? = runCatching {
        val f = files(context, family)
        if (f.isEmpty()) null else FontFamily(f.map { (weight, file) -> Font(file, FontWeight(weight)) })
    }.getOrNull()

    fun delete(context: Context, family: String) {
        files(context, family).forEach { it.second.delete() }
        nameFile(context, slug(family)).delete()
    }

    /** Downloads a Google Font. Fails with a readable message if the name isn't a Google Font. */
    suspend fun download(context: Context, family: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val name = family.trim().replace(Regex("\\s+"), " ")
            require(name.isNotEmpty()) { "Type a font name" }
            val enc = URLEncoder.encode(name, "UTF-8")
            // Ask for regular + bold; fonts with only one weight need the plain request.
            val css = fetchText("https://fonts.googleapis.com/css2?family=$enc:wght@400;700")
                ?: fetchText("https://fonts.googleapis.com/css2?family=$enc")
                ?: error("\"$name\" isn't on Google Fonts. Check the spelling.")
            val faces = parseCss(css)
            if (faces.isEmpty()) error("Couldn't read \"$name\" from Google Fonts")
            val slug = slug(name)
            faces.forEach { (weight, url) ->
                val bytes = fetchBytes(url) ?: error("Couldn't download \"$name\". Check your connection.")
                File(dir(context), "${slug}__$weight.ttf").writeBytes(bytes)
            }
            nameFile(context, slug).writeText(name)
            name
        }
    }

    /** (weight, ttf url) pairs for the normal (non-italic) faces in a Google Fonts CSS file. */
    internal fun parseCss(css: String): List<Pair<Int, String>> =
        css.split("@font-face").mapNotNull { block ->
            if (block.contains("font-style: italic")) return@mapNotNull null
            val weight = Regex("font-weight:\\s*(\\d+)").find(block)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
            val url = Regex("url\\((https://[^)]+?\\.ttf)\\)").find(block)?.groupValues?.get(1) ?: return@mapNotNull null
            weight to url
        }.distinctBy { it.first }

    private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 10000
        readTimeout = 20000
        // A non-browser user agent makes Google Fonts serve plain TTF files.
        setRequestProperty("User-Agent", "BookTracker/1.0 (Android)")
    }

    private fun fetchText(url: String): String? {
        val conn = open(url)
        return try {
            if (conn.responseCode in 200..299) conn.inputStream.bufferedReader().use { it.readText() } else null
        } finally { conn.disconnect() }
    }

    private fun fetchBytes(url: String): ByteArray? {
        val conn = open(url)
        return try {
            if (conn.responseCode in 200..299) conn.inputStream.use { it.readBytes() } else null
        } finally { conn.disconnect() }
    }
}
