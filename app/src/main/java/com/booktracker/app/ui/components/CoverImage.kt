package com.booktracker.app.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Loads book covers from the network, keeping them in memory and on disk so
 * they still show offline once seen.
 */
object CoverCache {
    private val memory = LruCache<String, ImageBitmap>(80)

    fun peek(url: String): ImageBitmap? = memory.get(url)

    /**
     * Returns the cover at [url]. With [persist], the image is kept in app storage
     * for good (a saved book's cover); otherwise only in the clearable cache
     * (search suggestions).
     */
    suspend fun load(context: Context, url: String, persist: Boolean): ImageBitmap? = withContext(Dispatchers.IO) {
        val name = hash(url)
        val saved = File(File(context.filesDir, "covers").apply { mkdirs() }, name)
        val cached = File(File(context.cacheDir, "covers").apply { mkdirs() }, name)
        memory.get(url)?.let { if (!persist || saved.exists()) return@withContext it }
        val bytes = when {
            saved.exists() -> saved.readBytes()
            cached.exists() -> cached.readBytes()
            else -> download(url)
        } ?: return@withContext null
        val target = if (persist) saved else cached
        if (!target.exists()) runCatching { target.writeBytes(bytes) }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()?.also { memory.put(url, it) }
    }

    private fun download(url: String): ByteArray? = runCatching {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "BookTracker/1.0 (Android)")
            if (conn.responseCode !in 200..299) return@runCatching null
            conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }.getOrNull()?.takeIf { it.size > 100 } // skip 1x1 "no cover" placeholders

    private fun hash(url: String): String =
        MessageDigest.getInstance("SHA-1").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
}

/**
 * Shows the cover at [url], cross-fading in once loaded. [placeholder] is shown
 * while loading and when there is no cover.
 */
@Composable
fun CoverImage(
    url: String?,
    modifier: Modifier = Modifier,
    persist: Boolean = true,
    placeholder: @Composable () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    var bitmap by remember(url) { mutableStateOf(url?.let { CoverCache.peek(it) }) }
    if (url != null) {
        LaunchedEffect(url, persist) { CoverCache.load(context, url, persist)?.let { bitmap = it } }
    }
    Box(modifier) {
        Crossfade(targetState = bitmap, label = "cover") { image ->
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                placeholder()
            }
        }
    }
}
