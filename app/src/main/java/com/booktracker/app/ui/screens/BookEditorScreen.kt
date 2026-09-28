package com.booktracker.app.ui.screens

import com.booktracker.app.ui.theme.AppIcons
import com.booktracker.app.ui.theme.LocalAppearance
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.booktracker.app.data.Book
import com.booktracker.app.data.BookDetails
import com.booktracker.app.data.BookSearch
import com.booktracker.app.data.BookSuggestion
import com.booktracker.app.ui.components.BookBadge
import com.booktracker.app.ui.components.ClickableField
import com.booktracker.app.ui.components.DatePickerModal
import com.booktracker.app.ui.components.pretty
import kotlinx.coroutines.delay

private const val SEARCH_DELAY_MS = 350L
private const val MIN_QUERY = 3

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BookEditorScreen(
    book: Book?,
    onClose: () -> Unit,
    onSave: (BookDetails) -> Unit,
    initialTitle: String = "",
) {
    // A prefilled title (e.g. from a recommendation) starts the autofill search straight away.
    var title by rememberSaveable { mutableStateOf(book?.title ?: initialTitle) }
    var author by rememberSaveable { mutableStateOf(book?.author ?: "") }
    var releaseDate by rememberSaveable { mutableStateOf(book?.releaseDate) }
    var pagesText by rememberSaveable { mutableStateOf(book?.totalPages?.toString() ?: "") }
    var coverUrl by rememberSaveable { mutableStateOf(book?.coverUrl) }
    var pickDate by remember { mutableStateOf(false) }
    var triedSave by rememberSaveable { mutableStateOf(false) }

    // Title we last filled in from a suggestion; typing it again doesn't re-search.
    var filledTitle by rememberSaveable { mutableStateOf(book?.title) }
    var autoFilled by rememberSaveable { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<BookSuggestion>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchedFor by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val titleFocus = remember { FocusRequester() }
    val pagesFocus = remember { FocusRequester() }

    val pages = pagesText.toIntOrNull()
    val titleError = triedSave && title.isBlank()
    val pagesError = triedSave && (pages == null || pages <= 0)

    // Search as you type, waiting for a short pause so we don't query on every key.
    LaunchedEffect(title) {
        val query = title.trim()
        if (query.length < MIN_QUERY || title == filledTitle) {
            suggestions = emptyList()
            searching = false
            searchedFor = null
            return@LaunchedEffect
        }
        searching = true
        delay(SEARCH_DELAY_MS)
        suggestions = BookSearch.search(query)
        searchedFor = query
        searching = false
    }

    // Jump straight into typing when adding a new book.
    LaunchedEffect(Unit) { if (book == null) titleFocus.requestFocus() }

    fun fillFrom(s: BookSuggestion) {
        filledTitle = s.title
        title = s.title
        author = s.author
        releaseDate = s.releaseDate
        s.pages?.let { pagesText = it.toString() }
        coverUrl = s.coverUrl
        autoFilled = true
        suggestions = emptyList()
        if (s.pages == null) pagesFocus.requestFocus() else focusManager.clearFocus()
    }

    fun save() {
        triedSave = true
        if (title.isNotBlank() && pages != null && pages > 0) {
            onSave(BookDetails(title.trim(), author.trim(), releaseDate, pages, coverUrl))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (book == null) "Add a book" else "Edit book",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = LocalAppearance.current.heavyWeight,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(AppIcons.Close, "Close") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (book == null) {
                Text(
                    "Start typing the title and pick your book. We'll fill in the rest.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    autoFilled = false
                },
                label = { Text("Book name") },
                leadingIcon = { Icon(AppIcons.Search, null) },
                trailingIcon = {
                    AnimatedContent(
                        targetState = searching,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "searching",
                    ) { busy ->
                        when {
                            busy -> LoadingIndicator(Modifier.size(36.dp))
                            title.isNotEmpty() -> IconButton(onClick = {
                                title = ""
                                autoFilled = false
                                titleFocus.requestFocus()
                            }) { Icon(AppIcons.Clear, "Clear title") }
                        }
                    }
                },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text("Give your book a name") }
                } else null,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { suggestions.firstOrNull()?.let { fillFrom(it) } }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocus),
            )

            AnimatedVisibility(
                visible = suggestions.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                SuggestionList(suggestions, onPick = { fillFrom(it) })
            }

            val noMatches = !searching && searchedFor != null && suggestions.isEmpty() && title.trim() == searchedFor
            AnimatedVisibility(visible = noMatches) {
                Text(
                    "No matches found (or you're offline). You can fill in the details yourself.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            AnimatedVisibility(
                visible = autoFilled || coverUrl != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                FilledBanner(
                    title = title,
                    coverUrl = coverUrl,
                    autoFilled = autoFilled,
                    onRemoveCover = { coverUrl = null },
                )
            }

            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text("Author") },
                leadingIcon = { Icon(AppIcons.Person, null) },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            ClickableField(
                value = releaseDate?.pretty() ?: "",
                label = "Release date",
                onClick = { pickDate = true },
                leadingIcon = { Icon(AppIcons.Event, null) },
                trailingIcon = if (releaseDate != null) {
                    { IconButton(onClick = { releaseDate = null }) { Icon(AppIcons.Clear, "Clear date") } }
                } else null,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = pagesText,
                onValueChange = { new -> pagesText = new.filter { it.isDigit() }.take(6) },
                label = { Text("Total pages") },
                leadingIcon = { Icon(AppIcons.MenuBook, null) },
                isError = pagesError,
                supportingText = if (pagesError) {
                    { Text("How many pages does it have?") }
                } else null,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(pagesFocus),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { save() },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            ) {
                Text(if (book == null) "Add book" else "Save changes", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (pickDate) {
        DatePickerModal(
            initial = releaseDate,
            onDismiss = { pickDate = false },
            onPicked = { releaseDate = it },
        )
    }
}

@Composable
private fun SuggestionList(suggestions: List<BookSuggestion>, onPick: (BookSuggestion) -> Unit) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            suggestions.forEachIndexed { i, s ->
                if (i > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(s) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    BookBadge(s.title, i, s.coverUrl, width = 44.dp, persist = false)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            s.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (s.author.isNotBlank()) {
                            Text(
                                s.author,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        val meta = listOfNotNull(
                            s.releaseDate?.year?.toString(),
                            s.pages?.let { "$it pages" },
                        ).joinToString(" · ")
                        if (meta.isNotEmpty()) {
                            Text(
                                meta,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilledBanner(title: String, coverUrl: String?, autoFilled: Boolean, onRemoveCover: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        if (coverUrl != null) {
            BookBadge(title, 0, coverUrl, width = 72.dp)
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            if (autoFilled) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        AppIcons.AutoAwesome,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Details filled in", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    "Double-check them below. Edition page counts can vary.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (coverUrl != null) {
                TextButton(onClick = onRemoveCover) { Text("Remove cover") }
            }
        }
    }
}
