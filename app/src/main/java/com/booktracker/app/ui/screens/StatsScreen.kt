package com.booktracker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.booktracker.app.data.Book
import com.booktracker.app.ui.components.BookBadge
import com.booktracker.app.ui.components.DailyBarChart
import com.booktracker.app.ui.components.SectionCard
import com.booktracker.app.ui.components.StatTile
import com.booktracker.app.ui.components.bookAccent
import com.booktracker.app.ui.theme.AppIcons
import com.booktracker.app.ui.theme.LocalAppearance
import java.time.LocalDate

/** Reading totals across every book. */
@Composable
fun StatsScreen(books: List<Book>, onOpenBook: (String) -> Unit, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    // Pages read per day, summed across books.
    val perDay: Map<LocalDate, Int> = books.flatMap { it.dailyPages }.groupBy({ it.date }, { it.pagesRead }).mapValues { it.value.sum() }
    val days = (13 downTo 0).map { today.minusDays(it.toLong()) }
    val values = days.map { perDay[it] ?: 0 }
    val week = (0..6).sumOf { perDay[today.minusDays(it.toLong())] ?: 0 }
    val allTime = books.sumOf { it.currentPage }
    val finished = books.count { it.isFinished }
    val streak = run {
        var day = if ((perDay[today] ?: 0) > 0) today else today.minusDays(1)
        var n = 0
        while ((perDay[day] ?: 0) > 0) { n++; day = day.minusDays(1) }
        n
    }
    // Books read most in the last 7 days.
    val weekly = books.withIndex()
        .map { (i, b) -> Triple(i, b, b.dailyPages.filter { it.date > today.minusDays(7) }.sumOf { it.pagesRead }) }
        .filter { it.third > 0 }
        .sortedByDescending { it.third }
    val c = MaterialTheme.colorScheme

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                "Stats",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = LocalAppearance.current.heavyWeight,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                StatTile("Pages today", (perDay[today] ?: 0).toString(), c.primary, c.onPrimary, Modifier.weight(1f))
                StatTile("This week", week.toString(), c.secondary, c.onSecondary, Modifier.weight(1f))
                StatTile("Day streak", streak.toString(), c.tertiary, c.onTertiary, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                StatTile("Pages read in total", allTime.toString(), c.primaryContainer, c.onPrimaryContainer, Modifier.weight(1f))
                StatTile("Books finished", "$finished / ${books.size}", c.tertiaryContainer, c.onTertiaryContainer, Modifier.weight(1f))
            }
        }
        item {
            SectionCard {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Last 14 days", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    Text("${values.sum()} pages", style = MaterialTheme.typography.titleMedium, color = c.primary)
                }
                Spacer(Modifier.height(20.dp))
                DailyBarChart(days, values, c.primary)
            }
        }
        item {
            SectionCard(title = "Most read this week") {
                if (weekly.isEmpty()) {
                    Text(
                        "Log some pages and your top books will show up here.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = c.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        weekly.take(5).forEach { (index, book, pages) ->
                            WeeklyRow(book, index, pages, max = weekly.first().third, onClick = { onOpenBook(book.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyRow(book: Book, index: Int, pages: Int, max: Int, onClick: () -> Unit) {
    val (accent, _) = bookAccent(index)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        BookBadge(book.title, index % 3, book.coverUrl, width = 40.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(10.dp).background(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraSmall)) {
                Box(Modifier.fillMaxWidth(pages.toFloat() / max).height(10.dp).background(accent, MaterialTheme.shapes.extraSmall))
            }
        }
        Spacer(Modifier.width(14.dp))
        Text("$pages", style = MaterialTheme.typography.titleLarge, fontWeight = LocalAppearance.current.heavyWeight)
        Icon(AppIcons.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}
