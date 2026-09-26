package com.booktracker.app.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.booktracker.app.data.BookRepository
import com.booktracker.app.data.CsvBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object BackupManager {

    const val WORK_NAME = "auto-backup"
    private const val FILE_PREFIX = "booktracker-backup-"
    /** Older automatic backups beyond this many are deleted so the folder doesn't fill up. */
    const val KEEP_BACKUPS = 50
    private val fileTime = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")

    /** Starts, restarts (with a new interval) or stops the periodic backup to match the settings. */
    fun reschedule(context: Context) {
        val state = BackupSettings(context).read()
        val work = WorkManager.getInstance(context)
        if (state.enabled && state.folderUri != null) {
            val request = PeriodicWorkRequestBuilder<BackupWorker>(state.interval.minutes, TimeUnit.MINUTES).build()
            work.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        } else {
            work.cancelUniqueWork(WORK_NAME)
        }
    }

    /** Writes a CSV of every book and its log into the chosen folder. Returns the file name. */
    suspend fun backUpNow(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val settings = BackupSettings(context)
        runCatching {
            val folder = settings.read().folderUri ?: error("Choose a backup folder first")
            val dir = DocumentFile.fromTreeUri(context, folder)
                ?.takeIf { it.exists() && it.canWrite() }
                ?: error("Can't write to the backup folder. Please choose it again.")

            val repository = BookRepository(context)
            repository.load()
            val books = repository.books.value

            val name = FILE_PREFIX + LocalDateTime.now().format(fileTime) + ".csv"
            val file = dir.createFile("text/csv", name) ?: error("Couldn't create the backup file")
            context.contentResolver.openOutputStream(file.uri, "wt").use { out ->
                requireNotNull(out) { "Couldn't open the backup file" }
                out.write(CsvBackup.toCsv(books).toByteArray(Charsets.UTF_8))
            }
            prune(dir)
            val saved = file.name ?: name
            settings.recordSuccess(System.currentTimeMillis(), saved, books.size)
            saved
        }.onFailure { settings.recordFailure(it.message ?: "Backup failed") }
    }

    private fun prune(dir: DocumentFile) {
        dir.listFiles()
            .filter { it.name?.startsWith(FILE_PREFIX) == true && it.name?.endsWith(".csv") == true }
            .sortedByDescending { it.name }
            .drop(KEEP_BACKUPS)
            .forEach { it.delete() }
    }

    fun folderName(context: Context, uri: Uri?): String? =
        uri?.let { runCatching { DocumentFile.fromTreeUri(context, it)?.name }.getOrNull() }

    /** Whether the folder permission we were granted is still valid. */
    fun hasFolderAccess(context: Context, uri: Uri?): Boolean = uri != null &&
        context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isWritePermission }

    fun isBatteryUnrestricted(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    /** System dialog asking to let the app run in the background without battery restrictions. */
    fun batteryPermissionIntent(context: Context) =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
}

class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    // A failed run is recorded in the settings and shown on the Backups screen;
    // the next scheduled run tries again.
    override suspend fun doWork(): Result =
        if (BackupManager.backUpNow(applicationContext).isSuccess) Result.success() else Result.failure()
}
