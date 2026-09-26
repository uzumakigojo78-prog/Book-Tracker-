package com.booktracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.booktracker.app.ui.screens.BackupScreen
import com.booktracker.app.ui.screens.BookDetailScreen
import com.booktracker.app.ui.screens.BookEditorScreen
import com.booktracker.app.ui.screens.LibraryScreen
import com.booktracker.app.ui.theme.BookTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            BookTrackerTheme {
                BookTrackerApp()
            }
        }
    }
}

// Routes: "library", "add", "detail:<id>", "edit:<id>"
private const val LIBRARY = "library"
private const val ADD = "add"
private const val BACKUPS = "backups"
private const val DETAIL = "detail:"
private const val EDIT = "edit:"

private fun depth(route: String) = when {
    route.startsWith(EDIT) -> 2
    route == LIBRARY -> 0
    else -> 1
}

private fun parent(route: String) = when {
    route.startsWith(EDIT) -> DETAIL + route.removePrefix(EDIT)
    else -> LIBRARY
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BookTrackerApp(viewModel: BookViewModel = viewModel()) {
    val books by viewModel.books.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    var route by rememberSaveable { mutableStateOf(LIBRARY) }

    if (!loaded) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) { LoadingIndicator() }
        return
    }

    BackHandler(enabled = route != LIBRARY) { route = parent(route) }

    AnimatedContent(
        targetState = route,
        transitionSpec = {
            val forward = depth(targetState) >= depth(initialState)
            val direction = if (forward) 1 else -1
            (slideInHorizontally { it / 3 * direction } + fadeIn()) togetherWith
                (slideOutHorizontally { -it / 3 * direction } + fadeOut())
        },
        label = "navigation",
    ) { current ->
        when {
            current == BACKUPS -> BackupScreen(
                onBack = { route = LIBRARY },
                onRestore = { restored -> viewModel.restore(restored) },
            )

            current == ADD -> BookEditorScreen(
                book = null,
                onClose = { route = LIBRARY },
                onSave = { details -> viewModel.addBook(details) { id -> route = DETAIL + id } },
            )

            current.startsWith(DETAIL) || current.startsWith(EDIT) -> {
                val id = current.substringAfter(':')
                val index = books.indexOfFirst { it.id == id }
                val book = books.getOrNull(index)
                if (book == null) {
                    // The book was deleted; fall back to the library.
                    LaunchedEffect(id) { route = LIBRARY }
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                } else if (current.startsWith(EDIT)) {
                    BookEditorScreen(
                        book = book,
                        onClose = { route = DETAIL + id },
                        onSave = { details ->
                            viewModel.updateBook(id, details)
                            route = DETAIL + id
                        },
                    )
                } else {
                    BookDetailScreen(
                        book = book,
                        colorIndex = index,
                        onBack = { route = LIBRARY },
                        onEdit = { route = EDIT + id },
                        onDelete = {
                            route = LIBRARY
                            viewModel.deleteBook(id)
                        },
                        onLogPage = { date, page -> viewModel.logPage(id, date, page) },
                        onDeleteEntry = { date -> viewModel.deleteEntry(id, date) },
                    )
                }
            }

            else -> LibraryScreen(
                books = books,
                onAddBook = { route = ADD },
                onOpenBook = { id -> route = DETAIL + id },
                onOpenBackups = { route = BACKUPS },
            )
        }
    }
}
