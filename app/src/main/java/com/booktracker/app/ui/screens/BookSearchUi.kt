package com.booktracker.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.booktracker.app.ai.BookCatalog
import com.booktracker.app.ai.CatalogBook
import com.booktracker.app.ai.GENRES
import com.booktracker.app.data.Book
import com.booktracker.app.data.BookDetails
import com.booktracker.app.ui.components.BuySection
import com.booktracker.app.ui.components.ButtonText
import com.booktracker.app.ui.components.BookBadge
import com.booktracker.app.ui.components.bookAccent
import com.booktracker.app.ui.components.pretty
import com.booktracker.app.ui.theme.AppIcons
import java.util.Locale

/** The search field at the top of the Genres tab. */
@Composable
fun BookSearchField(query: String, onQuery: (String) -> Unit, searching: Boolean) {
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        placeholder = { Text("Search any book, author or topic", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(AppIcons.Search, null) },
        trailingIcon = {
            if (query.isNotEmpty()) IconButton(onClick = { onQuery(""); focus.clearFocus() }) { Icon(AppIcons.Close, "Clear search") }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        textStyle = MaterialTheme.typography.titleMedium,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        supportingText = if (searching) { { Text("Searching…") } } else null,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** One search result row. */
@Composable
fun CatalogRow(book: CatalogBook, index: Int, inLibrary: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surfaceContainerHigh, MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        BookBadge(book.title, index, book.coverUrl, width = 56.dp, persist = false)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (book.author.isNotBlank()) {
                Text(book.author, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                listOfNotNull(book.year?.toString(), book.pages?.let { "$it pages" }, book.genres.firstOrNull()).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = c.primary,
            )
            val badges = listOfNotNull(
                "In your books".takeIf { inLibrary },
                "Free PDF".takeIf { book.freePdfUrl != null },
                "Borrow free".takeIf { book.freePdfUrl == null && book.isBorrowable },
            )
            if (badges.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    badges.forEach { Tag(it, if (it == "In your books") c.secondary else c.tertiary, if (it == "In your books") c.onSecondary else c.onTertiary) }
                }
            }
        }
        Icon(AppIcons.KeyboardArrowRight, null, tint = c.onSurfaceVariant)
    }
}

@Composable
private fun Tag(text: String, bg: Color, fg: Color) {
    Box(Modifier.background(bg, MaterialTheme.shapes.small).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
    }
}

/**
 * Everything about a book from search, with one-tap add, and links to a free PDF
 * (public-domain books only) or other legal ways to read it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun BookInfoSheet(
    initial: CatalogBook,
    library: List<Book>,
    onAdd: (BookDetails, (String) -> Unit) -> Unit,
    onAddWithForm: (String) -> Unit,
    onOpenBook: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var book by remember(initial) { mutableStateOf(initial) }
    LaunchedEffect(initial) { book = BookCatalog.withDetails(initial) }
    var addedId by remember(initial) { mutableStateOf<String?>(null) }
    var adding by remember(initial) { mutableStateOf(false) }
    var expanded by remember(initial) { mutableStateOf(false) }
    val existing = library.firstOrNull { it.title.normKey() == book.title.normKey() }
    val uri = LocalUriHandler.current
    val c = MaterialTheme.colorScheme
    val genreIndex = GENRES.indexOf(book.genres.firstOrNull()).coerceAtLeast(0)
    val (accent, onAccent) = bookAccent(genreIndex)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Row {
                BookBadge(book.title, genreIndex, book.coverUrl, width = 104.dp, persist = false)
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(book.title, style = MaterialTheme.typography.headlineSmall)
                    if (book.author.isNotBlank()) Text("by ${book.author}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    book.releaseDate?.let { InfoText(AppIcons.Event, if (it.dayOfYear == 1) "First published ${it.year}" else "Released ${it.pretty()}") }
                    book.pages?.let { InfoText(AppIcons.MenuBook, "$it pages") }
                    book.publisher?.let { InfoText(AppIcons.Category, it) }
                    book.rating?.let { InfoText(AppIcons.Insights, String.format(Locale.US, "%.1f / 5 rating", it)) }
                }
            }
            if (book.genres.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    book.genres.forEach { Tag(it, accent, onAccent) }
                }
            }

            Spacer(Modifier.height(18.dp))
            when {
                existing != null || addedId != null -> Button(
                    onClick = { onOpenBook(addedId ?: existing!!.id) },
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(containerColor = c.secondary, contentColor = c.onSecondary),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Icon(AppIcons.CheckCircle, null)
                    Spacer(Modifier.width(8.dp))
                    ButtonText(if (addedId != null) "Added · Open book" else "In your books · Open", MaterialTheme.typography.titleSmall)
                }

                book.pages != null -> Button(
                    onClick = {
                        adding = true
                        onAdd(BookDetails(book.title, book.author, book.releaseDate, book.pages!!, book.coverUrl)) { id -> addedId = id; adding = false }
                    },
                    enabled = !adding,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Icon(AppIcons.LibraryAdd, null)
                    Spacer(Modifier.width(8.dp))
                    ButtonText("Add to my books", MaterialTheme.typography.titleSmall)
                }

                // Page count unknown: let the reader fill it in.
                else -> Button(
                    onClick = { onAddWithForm(book.title) },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Icon(AppIcons.LibraryAdd, null)
                    Spacer(Modifier.width(8.dp))
                    ButtonText("Add to my books…", MaterialTheme.typography.titleSmall)
                }
            }

            Spacer(Modifier.height(10.dp))
            val pdf = book.freePdfUrl
            if (pdf != null) {
                FilledTonalButton(onClick = { uri.openUri(pdf) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(AppIcons.Download, null)
                    Spacer(Modifier.width(8.dp))
                    ButtonText("Download free PDF", MaterialTheme.typography.titleSmall)
                }
                Text(
                    "Public domain, free and legal to download.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 6.dp),
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().background(c.surfaceContainerHighest, MaterialTheme.shapes.large).padding(14.dp),
                ) {
                    Icon(AppIcons.Lock, null, tint = c.onSurfaceVariant)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "No free PDF: this book is still under copyright. You can borrow it or buy it below.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                book.archiveUrl?.takeIf { book.isBorrowable }?.let { LinkButton(AppIcons.MenuBook, "Borrow free on Open Library") { uri.openUri(book.openLibraryUrl) } }
                book.archiveUrl?.takeIf { pdf != null }?.let { url -> LinkButton(AppIcons.Download, "Other formats (EPUB, text)") { uri.openUri(url) } }
                book.googlePreviewUrl?.let { url -> LinkButton(AppIcons.OpenInNew, "Preview on Google Books") { uri.openUri(url) } }
                LinkButton(AppIcons.Link, "Open Library page") { uri.openUri(book.openLibraryUrl) }
            }

            Spacer(Modifier.height(18.dp))
            BuySection(book.title, book.author)

            book.description?.let { text ->
                Spacer(Modifier.height(18.dp))
                Text("About this book", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    text,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = if (expanded) Int.MAX_VALUE else 6,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.animateContentSize(),
                )
                if (text.length > 280) TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Show less" else "Read more") }
            }
            if (book.description == null && book.workKey != null) {
                Spacer(Modifier.height(18.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { LoadingIndicator(Modifier.size(40.dp)) }
            }
        }
    }
}

@Composable
private fun InfoText(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LinkButton(icon: ImageVector, text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(50.dp)) {
        Icon(icon, null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        ButtonText(text, MaterialTheme.typography.titleSmall, Modifier.weight(1f))
        Icon(AppIcons.OpenInNew, null, modifier = Modifier.size(16.dp))
    }
}

private fun String.normKey() = lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }
