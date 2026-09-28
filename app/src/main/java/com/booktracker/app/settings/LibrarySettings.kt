package com.booktracker.app.settings

import android.content.Context
import android.content.SharedPreferences
import com.booktracker.app.data.Library
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONObject

/** The reader's own library, kept only on this device. */
class LibrarySettings(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("library", Context.MODE_PRIVATE)

    fun read(): Library? = prefs.getString(KEY, null)?.let { runCatching { Library.fromJson(JSONObject(it)) }.getOrNull() }

    fun changes(): Flow<Library?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(read()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(read())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    /** Saves the library, without the search distance (which came from the reader's location). */
    fun save(library: Library) = prefs.edit().putString(KEY, library.copy(distanceKm = null).toJson().toString()).apply()

    fun unlink() = prefs.edit().remove(KEY).apply()

    private companion object {
        const val KEY = "my_library"
    }
}
