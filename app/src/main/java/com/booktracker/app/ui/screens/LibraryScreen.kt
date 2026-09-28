package com.booktracker.app.ui.screens

import com.booktracker.app.ui.theme.AppIcons
import com.booktracker.app.ui.theme.LocalAppearance
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.booktracker.app.data.Book
import com.booktracker.app.ui.components.BookBadge
import com.booktracker.app.ui.components.bookAccent
import com.booktracker.app.ui.components.bookColors
import com.booktracker.app.ui.components.pretty

@Composable
fun LibraryScreen(
    books: List<Book>,
    onAddBook: () -> Unit,
    onOpenBook: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // No top app bar: the heading scrolls with the list so the whole screen is content.
    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddBook,
                icon = { Icon(AppIcons.Add, contentDescription = null, modifier = Modifier.size(28.dp)) },
                text = { Text("Add book", style = MaterialTheme.typography.titleMedium) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large,
            )
        },
    ) { padding ->
        if (books.isEmpty()) {
            Column(Modifier.padding(padding)) {
                Header(books, Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp))
                EmptyLibrary()
            }
        } else {
            // Unfinished books first, most recently added at the top.
            val ordered = books.withIndex().sortedWith(
                compareBy<IndexedValue<Book>> { it.value.isFinished }.thenByDescending { it.value.createdAt }
            )
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 12.dp,
                    bottom = padding.calculateBottomPadding() + 104.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { Header(books, Modifier.padding(start = 4.dp, bottom = 4.dp)) }
                itemsIndexed(ordered, key = { _, it -> it.value.id }) { _, (colorIndex, book) ->
                    BookCard(book, colorIndex, onClick = { onOpenBook(book.id) }, modifier = Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun Header(books: List<Book>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text("My Books", style = MaterialTheme.typography.displaySmall, fontWeight = LocalAppearance.current.heavyWeight)
        if (books.isNotEmpty()) {
            val reading = books.count { !it.isFinished }
            Text(
                "$reading reading · ${books.size - reading} finished",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BookCard(book: Book, colorIndex: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (container, onContainer) = bookColors(colorIndex)
    val (accent, _) = bookAccent(colorIndex)
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = onContainer),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BookBadge(book.title, colorIndex, book.coverUrl)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (book.author.isNotBlank()) {
                        Text("by ${book.author}", style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    book.releaseDate?.let {
                        Text("Released ${it.pretty()}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (book.isFinished) {
                    Icon(
                        AppIcons.CheckCircle,
                        contentDescription = "Finished",
                        tint = accent,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            LinearWavyProgressIndicator(
                progress = { book.progress },
                color = accent,
                trackColor = onContainer.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth().height(14.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Page ${book.currentPage} of ${book.totalPages}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${(book.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = LocalAppearance.current.heavyWeight,
                )
            }
        }
    }
}

@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp).padding(bottom = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(140.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                AppIcons.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(72.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
        Text("No books yet", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap \"Add book\" to start tracking what you read, day by day.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
