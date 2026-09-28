package com.booktracker.app.settings

import android.content.Context
import android.content.SharedPreferences
import com.booktracker.app.ui.theme.Appearance
import com.booktracker.app.ui.theme.ColorPalette
import com.booktracker.app.ui.theme.CornerStyle
import com.booktracker.app.ui.theme.FontChoice
import com.booktracker.app.ui.theme.IconStyle
import com.booktracker.app.ui.theme.TextSize
import com.booktracker.app.ui.theme.ThemeMode
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Look-and-feel choices from the Settings pages, stored in SharedPreferences. */
class AppearanceSettings(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)

    fun read(): Appearance {
        val d = Appearance()
        return Appearance(
            themeMode = enumOr(THEME, d.themeMode),
            palette = enumOr(PALETTE, d.palette),
            textSize = enumOr(TEXT_SIZE, d.textSize),
            font = enumOr(FONT, d.font),
            boldText = prefs.getBoolean(BOLD, d.boldText),
            corners = enumOr(CORNERS, d.corners),
            iconStyle = enumOr(ICONS, d.iconStyle),
        )
    }

    fun changes(): Flow<Appearance> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(read()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(read())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun setThemeMode(v: ThemeMode) = put(THEME, v.name)
    fun setPalette(v: ColorPalette) = put(PALETTE, v.name)
    fun setTextSize(v: TextSize) = put(TEXT_SIZE, v.name)
    fun setFont(v: FontChoice) = put(FONT, v.name)
    fun setBoldText(v: Boolean) = prefs.edit().putBoolean(BOLD, v).apply()
    fun setCorners(v: CornerStyle) = put(CORNERS, v.name)
    fun setIconStyle(v: IconStyle) = put(ICONS, v.name)
    fun reset() = prefs.edit().clear().apply()

    private fun put(key: String, value: String) = prefs.edit().putString(key, value).apply()

    private inline fun <reified T : Enum<T>> enumOr(key: String, default: T): T =
        prefs.getString(key, null)?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default

    private companion object {
        const val THEME = "theme"
        const val PALETTE = "palette"
        const val TEXT_SIZE = "text_size"
        const val FONT = "font"
        const val BOLD = "bold"
        const val CORNERS = "corners"
        const val ICONS = "icons"
    }
}
