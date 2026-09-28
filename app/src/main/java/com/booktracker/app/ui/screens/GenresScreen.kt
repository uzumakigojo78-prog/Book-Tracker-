package com.booktracker.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.booktracker.app.ai.AnalysisSource
import com.booktracker.app.ai.BookCatalog
import com.booktracker.app.ai.CatalogBook
import com.booktracker.app.ai.AiConfig
import com.booktracker.app.ai.AiProvider
import com.booktracker.app.ai.GENRES
import com.booktracker.app.ai.GenreViewModel
import com.booktracker.app.ai.OnlineCopies
import com.booktracker.app.ai.Recommendation
import com.booktracker.app.ai.librarySignature
import com.booktracker.app.data.Book
import com.booktracker.app.data.BookDetails
import com.booktracker.app.ui.components.BuySection
import com.booktracker.app.ui.components.ButtonText
import com.booktracker.app.ui.components.BookBadge
import com.booktracker.app.ui.components.SectionCard
import com.booktracker.app.ui.components.bookAccent
import com.booktracker.app.ui.theme.AppIcons
import com.booktracker.app.ui.theme.LocalAppearance
import kotlinx.coroutines.delay

/** Books grouped by genre, plus recommendations, from Claude (with an API key) or Open Library. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GenresScreen(
    books: List<Book>,
    vm: GenreViewModel,
    onOpenBook: (String) -> Unit,
    onAddBook: (String) -> Unit,
    onAddDirect: (BookDetails, (String) -> Unit) -> Unit,
    onSetUpAi: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Search any book (Open Library + Google Books), shown instead of the genres while typing.
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<CatalogBook>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchedFor by remember { mutableStateOf("") }
    var infoFor by remember { mutableStateOf<CatalogBook?>(null) }
    val searchActive = query.trim().length >= 2
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) { results = emptyList(); searching = false; searchedFor = ""; return@LaunchedEffect }
        searching = true
        delay(400)
        results = BookCatalog.search(q)
        searchedFor = q
        searching = false
    }

    val analysis by vm.analysis.collectAsStateWithLifecycle()
    val running by vm.running.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val aiConfig by vm.aiConfig.collectAsStateWithLifecycle()
    val hasAi = aiConfig != null
    val outdated = analysis != null && analysis?.librarySignature != librarySignature(books)
    var sheetFor by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Basic mode is free, so keep it up to date automatically. AI runs only when asked.
    LaunchedEffect(books.isNotEmpty(), outdated, analysis == null, hasAi) {
        if (books.isNotEmpty() && !hasAi && (analysis == null || outdated)) vm.analyze(books)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(Modifier.padding(start = 4.dp, bottom = 4.dp)) {
                Text("Genres", style = MaterialTheme.typography.displaySmall, fontWeight = LocalAppearance.current.heavyWeight)
                Text(
                    "Search any book, see your genres and what to read next",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item { BookSearchField(query, { query = it }, searching) }

        if (searchActive) {
            if (!searching && searchedFor == query.trim() && results.isEmpty()) {
                item {
                    Text(
                        "No books found (or you're offline). Try another title, author or topic.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            items(results.withIndex().toList(), key = { (i, b) -> "search-$i-${b.title}-${b.author}" }) { (i, b) ->
                CatalogRow(b, i % 3, inLibrary = books.any { it.title.equals(b.title, ignoreCase = true) }, onClick = { infoFor = b })
            }
            return@LazyColumn
        }

        if (books.isEmpty()) {
            item {
                SectionCard {
                    Text("Add some books first", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Once you have books in your library, they'll be sorted into genres here with recommendations.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return@LazyColumn
        }

        item {
            ModeCard(
                ai = aiConfig,
                running = running,
                hasResult = analysis != null,
                outdated = outdated,
                source = analysis?.source,
                madeBy = analysis?.madeBy,
                onAnalyze = { vm.analyze(books) },
                onSetUpAi = onSetUpAi,
            )
        }

        error?.let { message ->
            item {
                SectionCard(color = MaterialTheme.colorScheme.errorContainer) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(12.dp))
                        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        val result = analysis
        if (result != null) {
            result.summary?.let { summary ->
                item {
                    SectionCard(color = MaterialTheme.colorScheme.primaryContainer) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(AppIcons.AutoAwesome, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(8.dp))
                            Text("Your reading taste", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(summary, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // Group books by main genre, in the fixed genre order; new books go last.
            val byGenre = books.withIndex().groupBy { (_, b) -> result.bookGenres[b.id]?.firstOrNull() ?: UNSORTED }
            val order = GENRES + UNSORTED
            byGenre.entries.sortedWith(compareBy({ order.indexOf(it.key) }, { -it.value.size })).forEach { (genre, entries) ->
                item(key = "genre-$genre") {
                    GenreSection(genre, entries.map { it.index to it.value }, result.bookGenres, onOpenBook)
                }
            }

            if (result.recommendations.isNotEmpty()) {
                item {
                    Text(
                        "Recommended for you",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                    )
                }
                items(result.recommendations, key = { "rec-${it.title}-${it.author}" }) { rec ->
                    RecommendationCard(
                        rec = rec,
                        vm = vm,
                        onGetCopy = { sheetFor = rec.title to rec.author },
                        onAdd = { onAddBook(rec.title) },
                    )
                }
            }
        }
    }

    infoFor?.let { b ->
        BookInfoSheet(
            initial = b,
            library = books,
            onAdd = onAddDirect,
            onAddWithForm = { title -> infoFor = null; onAddBook(title) },
            onOpenBook = { id -> infoFor = null; onOpenBook(id) },
            onDismiss = { infoFor = null },
        )
    }

    sheetFor?.let { (title, author) ->
        OnlineCopiesSheet(title, author, load = { vm.copies(title, author) }, onDismiss = { sheetFor = null })
    }
}

private const val UNSORTED = "Not sorted yet"

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ModeCard(
    ai: AiConfig?,
    running: Boolean,
    hasResult: Boolean,
    outdated: Boolean,
    source: AnalysisSource?,
    madeBy: String?,
    onAnalyze: () -> Unit,
    onSetUpAi: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val hasAi = ai != null
    val name = if (ai != null && ai.provider != AiProvider.CUSTOM) ai.provider.label else "The AI"
    SectionCard(color = if (hasAi) c.secondaryContainer else c.surfaceContainerHigh) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp).background(if (hasAi) c.secondary else c.outline, MaterialTheme.shapes.medium),
            ) { Icon(if (hasAi) AppIcons.Psychology else AppIcons.Category, null, tint = if (hasAi) c.onSecondary else c.surface) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(if (ai != null) "AI: ${ai.displayName}" else "Basic mode", style = MaterialTheme.typography.titleLarge)
                Text(
                    when {
                        running && hasAi -> "$name is reading your library…"
                        running -> "Sorting your books…"
                        hasAi && source == AnalysisSource.BASIC -> "Showing basic results. Tap Analyze for $name's take."
                        hasAi && !hasResult -> "Tap Analyze to let $name sort your books and pick what to read next."
                        outdated && hasAi -> "Your library changed since the last analysis."
                        hasAi && madeBy != null && madeBy != ai?.displayName -> "Last analysis by $madeBy. Tap Re-analyze to use $name."
                        hasAi -> "Genres and picks by $name."
                        else -> "Genres from Open Library. Add an AI API key (Claude, Gemini, Grok, Kimi and more) for AI genres and personal picks."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        if (running) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                LoadingIndicator(Modifier.size(56.dp))
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onAnalyze,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Icon(if (hasResult) AppIcons.Refresh else AppIcons.AutoAwesome, null)
                    Spacer(Modifier.width(8.dp))
                    ButtonText(if (!hasResult) "Analyze" else if (outdated) "Update" else "Re-analyze", MaterialTheme.typography.titleSmall)
                }
                if (!hasAi) {
                    FilledTonalButton(onClick = onSetUpAi, modifier = Modifier.weight(1f).height(52.dp)) {
                        Icon(AppIcons.Key, null)
                        Spacer(Modifier.width(8.dp))
                        ButtonText("Set up AI", MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreSection(
    genre: String,
    books: List<Pair<Int, Book>>,
    allGenres: Map<String, List<String>>,
    onOpenBook: (String) -> Unit,
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(genre, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small).padding(horizontal = 10.dp, vertical = 2.dp),
            ) { Text("${books.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary) }
        }
        Spacer(Modifier.height(14.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(books, key = { it.second.id }) { (index, book) ->
                Column(
                    modifier = Modifier.width(88.dp).clickable { onOpenBook(book.id) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BookBadge(book.title, index, book.coverUrl, width = 80.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(book.title, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    allGenres[book.id]?.getOrNull(1)?.let { second ->
                        Text("+ $second", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(rec: Recommendation, vm: GenreViewModel, onGetCopy: () -> Unit, onAdd: () -> Unit) {
    var copies by remember(rec) { mutableStateOf<OnlineCopies?>(null) }
    LaunchedEffect(rec) { copies = runCatching { vm.copies(rec.title, rec.author) }.getOrNull() }
    val c = MaterialTheme.colorScheme
    val index = GENRES.indexOf(rec.genre).coerceAtLeast(0)
    val (accent, onAccent) = bookAccent(index)

    SectionCard {
        Row {
            BookBadge(rec.title, index, copies?.coverUrl, width = 72.dp, persist = false)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(rec.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(rec.author.ifBlank { null }, rec.year?.toString()).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(rec.genre, accent, onAccent)
                    AnimatedVisibility(copies?.isFreeDownload == true || copies?.isBorrowable == true) {
                        Pill(if (copies?.isFreeDownload == true) "Free download" else "Borrow free", c.tertiary, c.onTertiary)
                    }
                }
            }
        }
        if (rec.reason.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(rec.reason, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(onClick = onGetCopy, modifier = Modifier.weight(1f).height(48.dp)) {
                Icon(AppIcons.Download, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                ButtonText("Get a copy", MaterialTheme.typography.titleSmall)
            }
            OutlinedButton(onClick = onAdd, modifier = Modifier.weight(1f).height(48.dp)) {
                Icon(AppIcons.LibraryAdd, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                ButtonText("Add to books", MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun Pill(text: String, bg: Color, fg: Color) {
    Box(Modifier.background(bg, MaterialTheme.shapes.small).padding(horizontal = 10.dp, vertical = 3.dp)) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
    }
}

/** Bottom sheet listing free, legal places to read, borrow or download a book. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnlineCopiesSheet(title: String, author: String, load: suspend () -> OnlineCopies, onDismiss: () -> Unit) {
    var copies by remember { mutableStateOf<OnlineCopies?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(title, author) {
        runCatching { load() }.onSuccess { copies = it }.onFailure { failed = true }
    }
    val uri = LocalUriHandler.current
    val c = MaterialTheme.colorScheme

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("Get a copy", style = MaterialTheme.typography.headlineSmall)
            Text(
                listOf(title, author).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.titleMedium,
                color = c.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            val result = copies
            when {
                result == null && !failed -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }

                else -> {
                    // Even if the lookup failed, the search links still work.
                    val q = listOf(title, author).filter { it.isNotBlank() }.joinToString(" ")
                    val links = result ?: com.booktracker.app.ai.OpenLibraryAi.copiesFrom(null, q)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (links.isFreeDownload && links.archiveUrl != null) {
                            LinkRow(AppIcons.Download, "Download free", "Public domain · PDF & EPUB on the Internet Archive", c.primary, c.onPrimary) {
                                uri.openUri(links.archiveUrl)
                            }
                        }
                        if (links.isBorrowable) {
                            LinkRow(AppIcons.MenuBook, "Borrow free", "Read online with a free Open Library account", c.secondary, c.onSecondary) {
                                uri.openUri(links.openLibraryUrl)
                            }
                        }
                        LinkRow(AppIcons.Link, "Open Library", "Editions, ebooks and libraries near you", c.surfaceContainerHighest, c.onSurface) {
                            uri.openUri(links.openLibraryUrl)
                        }
                        LinkRow(AppIcons.Download, "Project Gutenberg", "Free ebooks of classic, public-domain books", c.surfaceContainerHighest, c.onSurface) {
                            uri.openUri(links.gutenbergUrl)
                        }
                        LinkRow(AppIcons.OpenInNew, "Google Books", "Preview or buy the ebook", c.surfaceContainerHighest, c.onSurface) {
                            uri.openUri(links.googleBooksUrl)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    BuySection(title, author)
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Free downloads are only for books in the public domain. Newer books can be borrowed from your library or bought from the stores above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkRow(icon: ImageVector, title: String, subtitle: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Icon(icon, null, tint = fg)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = fg)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.8f))
        }
        Icon(AppIcons.OpenInNew, null, tint = fg, modifier = Modifier.size(18.dp))
    }
}
