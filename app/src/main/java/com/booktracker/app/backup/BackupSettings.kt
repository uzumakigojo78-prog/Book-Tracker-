package com.booktracker.app.backup

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** How often automatic backups run. WorkManager's shortest period is 15 minutes. */
enum class BackupInterval(val minutes: Long, val label: String) {
    MIN_15(15, "15 min"),
    MIN_30(30, "30 min"),
    HOUR_1(60, "1 hour"),
    HOUR_3(180, "3 hours"),
    HOUR_6(360, "6 hours"),
    HOUR_12(720, "12 hours"),
    DAY_1(1440, "1 day"),
    WEEK_1(10080, "1 week"),
}

data class BackupState(
    val enabled: Boolean,
    val interval: BackupInterval,
    val folderUri: Uri?,
    val lastBackupAt: Long?,
    val lastBackupFile: String?,
    val lastBackupBooks: Int,
    val lastError: String?,
)

/** Backup preferences, stored in SharedPreferences. */
class BackupSettings(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("backup", Context.MODE_PRIVATE)

    fun read() = BackupState(
        enabled = prefs.getBoolean(ENABLED, false),
        interval = BackupInterval.entries.firstOrNull { it.name == prefs.getString(INTERVAL, null) } ?: BackupInterval.DAY_1,
        folderUri = prefs.getString(FOLDER, null)?.let(Uri::parse),
        lastBackupAt = prefs.getLong(LAST_AT, 0L).takeIf { it > 0 },
        lastBackupFile = prefs.getString(LAST_FILE, null),
        lastBackupBooks = prefs.getInt(LAST_BOOKS, 0),
        lastError = prefs.getString(LAST_ERROR, null),
    )

    /** Emits the current state and again whenever it changes (including from the worker). */
    fun changes(): Flow<BackupState> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(read()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(read())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun setEnabled(enabled: Boolean) = prefs.edit().putBoolean(ENABLED, enabled).apply()
    fun setInterval(interval: BackupInterval) = prefs.edit().putString(INTERVAL, interval.name).apply()
    fun setFolder(uri: Uri?) = prefs.edit().putString(FOLDER, uri?.toString()).remove(LAST_ERROR).apply()

    fun recordSuccess(at: Long, file: String, books: Int) = prefs.edit()
        .putLong(LAST_AT, at).putString(LAST_FILE, file).putInt(LAST_BOOKS, books).remove(LAST_ERROR).apply()

    fun recordFailure(message: String) = prefs.edit().putString(LAST_ERROR, message).apply()

    private companion object {
        const val ENABLED = "enabled"
        const val INTERVAL = "interval"
        const val FOLDER = "folder"
        const val LAST_AT = "last_at"
        const val LAST_FILE = "last_file"
        const val LAST_BOOKS = "last_books"
        const val LAST_ERROR = "last_error"
    }
}
