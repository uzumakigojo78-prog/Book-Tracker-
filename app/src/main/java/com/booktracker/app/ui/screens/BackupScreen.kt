package com.booktracker.app.ui.screens

import com.booktracker.app.ui.components.ButtonText
import com.booktracker.app.ui.theme.AppIcons
import com.booktracker.app.ui.theme.LocalAppearance
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.work.WorkManager
import com.booktracker.app.backup.BackupInterval
import com.booktracker.app.backup.BackupManager
import com.booktracker.app.backup.BackupSettings
import com.booktracker.app.data.Book
import com.booktracker.app.data.CsvBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    onRestore: (List<Book>) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val settings = remember { BackupSettings(context) }
    val state by remember { settings.changes() }.collectAsState(initial = settings.read())
    val workInfos by remember { WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(BackupManager.WORK_NAME) }
        .collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var folderOk by remember { mutableStateOf(BackupManager.hasFolderAccess(context, state.folderUri)) }
    var batteryOk by remember { mutableStateOf(BackupManager.isBatteryUnrestricted(context)) }
    var busy by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<List<Book>?>(null) }
    // When turning backups on, we walk through the permissions one after another.
    var setupInProgress by remember { mutableStateOf(false) }

    // Re-check permissions whenever we come back to the screen (e.g. from system settings).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        folderOk = BackupManager.hasFolderAccess(context, settings.read().folderUri)
        batteryOk = BackupManager.isBatteryUnrestricted(context)
    }

    val batteryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        batteryOk = BackupManager.isBatteryUnrestricted(context)
        if (setupInProgress) {
            setupInProgress = false
            settings.setEnabled(true)
            BackupManager.reschedule(context)
        }
    }

    fun askBattery() {
        runCatching { batteryLauncher.launch(BackupManager.batteryPermissionIntent(context)) }
            .onFailure {
                // Some phones hide this dialog; fall back to the battery settings list.
                runCatching { batteryLauncher.launch(Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
            }
    }

    fun finishSetup() {
        if (!BackupManager.isBatteryUnrestricted(context)) {
            askBattery()
        } else {
            setupInProgress = false
            settings.setEnabled(true)
            BackupManager.reschedule(context)
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            // Drop the old folder's permission so we don't collect stale grants.
            settings.read().folderUri?.takeIf { it != uri }?.let { old ->
                runCatching { context.contentResolver.releasePersistableUriPermission(old, flags) }
            }
            context.contentResolver.takePersistableUriPermission(uri, flags)
            settings.setFolder(uri)
            folderOk = true
            BackupManager.reschedule(context)
            if (setupInProgress) finishSetup()
        } else {
            setupInProgress = false
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val text = context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
                    CsvBackup.fromCsv(text)
                }
            }
            result.onSuccess { pendingRestore = it }
                .onFailure { snackbar.showSnackbar("That file isn't a Book Tracker CSV backup") }
        }
    }

    fun setEnabled(on: Boolean) {
        if (!on) {
            settings.setEnabled(false)
            BackupManager.reschedule(context)
        } else if (!folderOk) {
            setupInProgress = true
            folderLauncher.launch(null)
        } else {
            setupInProgress = true
            finishSetup()
        }
    }

    val nextRun = workInfos.firstOrNull()?.nextScheduleTimeMillis?.takeIf { state.enabled && it in 1 until Long.MAX_VALUE }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("CSV backups", style = MaterialTheme.typography.headlineSmall, fontWeight = LocalAppearance.current.heavyWeight) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(AppIcons.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // On/off + status
            val heroColor by animateColorAsState(
                if (state.enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                label = "hero",
            )
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = heroColor),
            ) {
                Column(Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Automatic CSV backups", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                if (state.enabled) "On · every ${state.interval.label}" else "Off",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Switch(checked = state.enabled, onCheckedChange = { setEnabled(it) })
                    }
                    Spacer(Modifier.height(16.dp))
                    StatusLine(state.lastBackupAt, state.lastBackupFile, state.lastBackupBooks, state.lastError, nextRun)
                }
            }

            // Interval
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("Back up every", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(16.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BackupInterval.entries.forEach { option ->
                            IntervalOption(option.label, selected = option == state.interval) {
                                settings.setInterval(option)
                                BackupManager.reschedule(context)
                            }
                        }
                    }
                }
            }

            // Permissions
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text("Permissions", style = MaterialTheme.typography.headlineSmall)
                    PermissionRow(
                        icon = AppIcons.Folder,
                        title = "Backup folder",
                        detail = if (folderOk) "Saving to “${BackupManager.folderName(context, state.folderUri) ?: "chosen folder"}” on this phone"
                        else "Pick a folder on your phone where backups are saved",
                        granted = folderOk,
                        action = if (folderOk) "Change" else "Choose",
                        onClick = { folderLauncher.launch(null) },
                    )
                    PermissionRow(
                        icon = AppIcons.BatteryChargingFull,
                        title = "Battery",
                        detail = if (batteryOk) "Unrestricted: backups run on time"
                        else "Battery saving can delay or skip backups",
                        granted = batteryOk,
                        action = "Allow",
                        onClick = { askBattery() },
                    )
                }
            }

            // Manual actions
            Button(
                onClick = {
                    busy = true
                    scope.launch {
                        val result = BackupManager.backUpNow(context)
                        busy = false
                        snackbar.showSnackbar(result.fold({ "Saved $it" }, { it.message ?: "Backup failed" }))
                    }
                },
                enabled = folderOk && !busy,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().height(64.dp),
            ) {
                Icon(AppIcons.Save, null)
                Spacer(Modifier.width(10.dp))
                ButtonText(if (busy) "Backing up…" else "Back up now", MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(
                onClick = { restoreLauncher.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream")) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(AppIcons.Restore, null)
                Spacer(Modifier.width(10.dp))
                ButtonText("Restore from a CSV backup", MaterialTheme.typography.titleSmall)
            }
            Text(
                "Each backup is a new CSV file (open it in any spreadsheet app). The newest ${BackupManager.KEEP_BACKUPS} are kept.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }

    pendingRestore?.let { restored ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            icon = { Icon(AppIcons.Restore, null) },
            title = { Text("Restore this backup?") },
            text = { Text("Your current books will be replaced with the ${restored.size} book${if (restored.size == 1) "" else "s"} in the backup.") },
            confirmButton = {
                Button(onClick = {
                    pendingRestore = null
                    onRestore(restored)
                    scope.launch { snackbar.showSnackbar("Restored ${restored.size} books") }
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancel") } },
        )
    }
}

private val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
private val dateTimeFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

private fun friendlyTime(millis: Long): String {
    val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    return when (time.toLocalDate()) {
        LocalDate.now() -> "today at ${time.format(timeFormat)}"
        LocalDate.now().plusDays(1) -> "tomorrow at ${time.format(timeFormat)}"
        LocalDate.now().minusDays(1) -> "yesterday at ${time.format(timeFormat)}"
        else -> time.format(dateTimeFormat)
    }
}

@Composable
private fun StatusLine(lastAt: Long?, file: String?, books: Int, error: String?, nextRun: Long?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (error != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.CloudDone, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (lastAt != null) "Last backup ${friendlyTime(lastAt)} · $books book${if (books == 1) "" else "s"}" else "No backups yet",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (file != null && lastAt != null) {
            Text(file, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 28.dp))
        }
        if (nextRun != null) {
            Text("Next backup around ${friendlyTime(nextRun)}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 28.dp))
        }
    }
}

@Composable
private fun IntervalOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "option",
    )
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(48.dp)
            .background(bg, if (selected) MaterialTheme.shapes.small else CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    detail: String,
    granted: Boolean,
    action: String,
    onClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.shapes.medium,
                ),
        ) {
            Icon(
                if (granted) AppIcons.CheckCircle else icon,
                null,
                tint = if (granted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onErrorContainer,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedVisibility(visible = !granted || action == "Change") {
            FilledTonalButton(onClick = onClick, modifier = Modifier.padding(start = 8.dp)) { ButtonText(action, MaterialTheme.typography.labelLarge) }
        }
    }
}
