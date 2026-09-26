package com.booktracker.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.booktracker.app.data.Book
import com.booktracker.app.data.DailyPages
import com.booktracker.app.ui.components.BookBadge
import com.booktracker.app.ui.components.DatePickerModal
import com.booktracker.app.ui.components.bookAccent
import com.booktracker.app.ui.components.bookColors
import com.booktracker.app.ui.components.pretty
import com.booktracker.app.ui.components.relative
import com.booktracker.app.ui.theme.HeroNumber
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    book: Book,
    colorIndex: Int,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLogPage: (LocalDate, Int) -> Unit,
    onDeleteEntry: (LocalDate) -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Edit book") }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Delete book") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { HeroCard(book, colorIndex) }
            item {
                LogReadingCard(book) { date, page ->
                    onLogPage(date, page)
                    scope.launch { snackbar.showSnackbar("Saved page $page · ${date.relative()}") }
                }
            }
            item { ChartCard(book, colorIndex) }
            item {
                Text(
                    "Daily log",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                )
            }
            val days = book.dailyPages
            if (days.isEmpty()) {
                item {
                    Text(
                        "Nothing logged yet. Save the page you reached today to start your log.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            } else {
                items(days, key = { it.date.toString() }) { day ->
                    HistoryRow(day, colorIndex, onDelete = { onDeleteEntry(day.date) }, modifier = Modifier.animateItem())
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Rounded.Delete, null) },
            title = { Text("Delete this book?") },
            text = { Text("\"${book.title}\" and its whole reading log will be removed.") },
            confirmButton = {
                Button(
                    onClick = { confirmDelete = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeroCard(book: Book, colorIndex: Int) {
    val (container, onContainer) = bookColors(colorIndex)
    val (accent, _) = bookAccent(colorIndex)
    val animated by animateFloatAsState(book.progress, spring(dampingRatio = 0.6f, stiffness = 200f), label = "progress")
    val strokePx = with(LocalDensity.current) { 14.dp.toPx() }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = onContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            Row {
                if (book.coverUrl != null) {
                    BookBadge(book.title, colorIndex, book.coverUrl, width = 88.dp)
                    Spacer(Modifier.width(18.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        book.title,
                        style = if (book.coverUrl != null) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displaySmall,
                    )
                    Spacer(Modifier.height(12.dp))
                    if (book.author.isNotBlank()) InfoLine(Icons.Rounded.Person, book.author)
                    InfoLine(Icons.Rounded.Event, book.releaseDate?.let { "Released ${it.pretty()}" } ?: "Release date unknown")
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(170.dp)) {
                    CircularWavyProgressIndicator(
                        progress = { animated },
                        color = accent,
                        trackColor = onContainer.copy(alpha = 0.15f),
                        stroke = Stroke(width = strokePx, cap = StrokeCap.Round),
                        trackStroke = Stroke(width = strokePx, cap = StrokeCap.Round),
                        modifier = Modifier.size(170.dp),
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${(book.progress * 100).toInt()}%", style = HeroNumber)
                        if (book.isFinished) Text("Finished!", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.width(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    BigStat("${book.currentPage}", "of ${book.totalPages} pages")
                    BigStat("${(book.totalPages - book.currentPage).coerceAtLeast(0)}", "pages left")
                    val streak = streak(book)
                    if (streak > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocalFireDepartment, null, tint = accent)
                            Spacer(Modifier.width(4.dp))
                            Text("$streak day streak", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun BigStat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/** Consecutive days (ending today or yesterday) on which pages were read. */
private fun streak(book: Book): Int {
    val readDays = book.dailyPages.filter { it.pagesRead > 0 }.map { it.date }.toSet()
    var day = LocalDate.now()
    if (day !in readDays) day = day.minusDays(1)
    var count = 0
    while (day in readDays) {
        count++
        day = day.minusDays(1)
    }
    return count
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LogReadingCard(book: Book, onSave: (LocalDate, Int) -> Unit) {
    var date by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var pickDate by remember { mutableStateOf(false) }
    val existing = book.entries.firstOrNull { it.date == date }?.page
    var pageText by rememberSaveable(book.id, date) { mutableStateOf((existing ?: book.currentPage).toString()) }
    val focus = LocalFocusManager.current

    val page = pageText.toIntOrNull()
    val valid = page != null && page in 0..book.totalPages
    // Page reached on the last logged day before the selected date.
    val previous = book.sortedEntries.lastOrNull { it.date < date }?.page ?: 0
    val readThatDay = if (page != null) page - previous else 0

    fun bump(by: Int) {
        pageText = ((pageText.toIntOrNull() ?: book.currentPage) + by).coerceIn(0, book.totalPages).toString()
    }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            Text("Log your reading", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = { pickDate = true }, modifier = Modifier.height(48.dp)) {
                Icon(Icons.Rounded.CalendarMonth, null)
                Spacer(Modifier.width(8.dp))
                Text(date.relative())
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = pageText,
                onValueChange = { new -> pageText = new.filter { it.isDigit() }.take(6) },
                label = { Text("Page I'm on") },
                suffix = { Text("/ ${book.totalPages}") },
                isError = pageText.isNotEmpty() && !valid,
                supportingText = {
                    when {
                        pageText.isNotEmpty() && !valid -> Text("Enter a page between 0 and ${book.totalPages}")
                        readThatDay > 0 -> Text("That's $readThatDay pages read on this day")
                        else -> Text("The page number you reached")
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(1, 5, 10, 25).forEach { step ->
                    FilledTonalButton(
                        onClick = { bump(step) },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) { Text("+$step") }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        focus.clearFocus()
                        onSave(date, page!!)
                    },
                    enabled = valid,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f).height(64.dp),
                ) {
                    Text("Save page", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.width(12.dp))
                FilledTonalIconButton(
                    onClick = { pageText = book.totalPages.toString() },
                    modifier = Modifier.size(64.dp),
                ) {
                    Text("END", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    if (pickDate) {
        DatePickerModal(
            initial = date,
            allowFuture = false,
            onDismiss = { pickDate = false },
            onPicked = { date = it },
        )
    }
}

@Composable
private fun ChartCard(book: Book, colorIndex: Int) {
    val (accent, _) = bookAccent(colorIndex)
    val today = LocalDate.now()
    val byDate = book.dailyPages.associate { it.date to it.pagesRead }
    val days = (13 downTo 0).map { today.minusDays(it.toLong()) }
    val values = days.map { byDate[it] ?: 0 }
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val total = values.sum()

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Last 14 days", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                Text("$total pages", style = MaterialTheme.typography.titleMedium, color = accent)
            }
            Spacer(Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth().height(140.dp),
            ) {
                values.forEachIndexed { i, v ->
                    val target = if (v == 0) 0.04f else (v.toFloat() / max).coerceAtLeast(0.08f)
                    val fraction by animateFloatAsState(target, spring(dampingRatio = 0.55f, stiffness = 220f), label = "bar")
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(fraction)
                            .background(
                                if (v == 0) MaterialTheme.colorScheme.outlineVariant else if (days[i] == today) MaterialTheme.colorScheme.secondary else accent,
                                RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                            )
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                days.forEach { d ->
                    Text(
                        d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = if (d == today) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(day: DailyPages, colorIndex: Int, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val (container, onContainer) = bookColors(colorIndex)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large)
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
    ) {
        Box(
            Modifier
                .size(width = 72.dp, height = 56.dp)
                .background(container, MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "+${day.pagesRead}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = onContainer,
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(day.date.relative(), style = MaterialTheme.typography.titleMedium)
            Text(
                "Reached page ${day.pageReached}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Close, "Remove entry", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
