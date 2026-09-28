package com.booktracker.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.booktracker.app.ai.GenreViewModel
import com.booktracker.app.settings.AppearanceSettings
import com.booktracker.app.ui.screens.AiSettingsPage
import com.booktracker.app.ui.screens.GenresScreen
import com.booktracker.app.ui.screens.AppearancePage
import com.booktracker.app.ui.screens.BackupScreen
import com.booktracker.app.ui.screens.BookDetailScreen
import com.booktracker.app.ui.screens.BookEditorScreen
import com.booktracker.app.ui.screens.LibraryScreen
import com.booktracker.app.ui.screens.SettingsPage
import com.booktracker.app.ui.screens.SettingsScreen
import com.booktracker.app.ui.screens.StatsScreen
import com.booktracker.app.ui.screens.StylePage
import com.booktracker.app.ui.screens.TextPage
import com.booktracker.app.ui.theme.AppIcons
import com.booktracker.app.ui.theme.BookTrackerTheme
import com.booktracker.app.ui.theme.isDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val appearanceSettings = AppearanceSettings(applicationContext)
        setContent {
            val appearance by remember { appearanceSettings.changes() }.collectAsState(initial = appearanceSettings.read())
            val dark = appearance.isDark()
            // Status/navigation bar icons follow the app's theme, not just the system's.
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            BookTrackerTheme(appearance) {
                BookTrackerApp(appearanceSettings)
            }
        }
    }
}

// Tabs: "library", "genres", "stats", "settings".
// Pages: "settings/<page>", "add", "add:<title>", "detail:<id>", "edit:<id>".
private const val LIBRARY = "library"
private const val GENRES_TAB = "genres"
private const val STATS = "stats"
private const val SETTINGS = "settings"
private const val SETTINGS_PAGE = "settings/"
private const val ADD = "add"
private const val ADD_TITLED = "add:"
private const val DETAIL = "detail:"
private const val EDIT = "edit:"

private enum class Tab(val route: String, val label: String) {
    BOOKS(LIBRARY, "Books"), GENRES(GENRES_TAB, "Genres"), STATS_TAB(STATS, "Stats"), SETTINGS_TAB(SETTINGS, "Settings");

    val icon: ImageVector
        @Composable get() = when (this) {
            BOOKS -> AppIcons.MenuBook
            GENRES -> AppIcons.Category
            STATS_TAB -> AppIcons.BarChart
            SETTINGS_TAB -> AppIcons.Settings
        }
}

private fun isTab(route: String) = Tab.entries.any { it.route == route }

private fun depth(route: String) = when {
    isTab(route) -> 0
    route.startsWith(EDIT) -> 2
    else -> 1
}

private fun parent(route: String) = when {
    route.startsWith(EDIT) -> DETAIL + route.removePrefix(EDIT)
    route.startsWith(ADD_TITLED) -> GENRES_TAB
    route.startsWith(SETTINGS_PAGE) -> SETTINGS
    else -> LIBRARY
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BookTrackerApp(
    appearanceSettings: AppearanceSettings,
    viewModel: BookViewModel = viewModel(),
    genreViewModel: GenreViewModel = viewModel(),
) {
    val books by viewModel.books.collectAsStateWithLifecycle()
    val aiConfig by genreViewModel.aiConfig.collectAsStateWithLifecycle()
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

    // Back from another tab goes to Books first, then leaves the app.
    BackHandler(enabled = route != LIBRARY) { route = parent(route) }

    Scaffold(
        bottomBar = {
            if (isTab(route)) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { route = tab.route },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                        )
                    }
                }
            }
        },
    ) { outer ->
        // Tab pages sit above the bottom bar; pushed pages use the full screen.
        val tabModifier = Modifier
            .padding(outer)
            .consumeWindowInsets(outer)

        AnimatedContent(
            targetState = route,
            transitionSpec = {
                if (isTab(initialState) && isTab(targetState)) {
                    (fadeIn() + scaleIn(initialScale = 0.96f)) togetherWith fadeOut()
                } else {
                    val forward = depth(targetState) >= depth(initialState)
                    val direction = if (forward) 1 else -1
                    (slideInHorizontally { it / 3 * direction } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 3 * direction } + fadeOut())
                }
            },
            label = "navigation",
        ) { current ->
            when {
                current == LIBRARY -> LibraryScreen(
                    books = books,
                    onAddBook = { route = ADD },
                    onOpenBook = { id -> route = DETAIL + id },
                    modifier = tabModifier,
                )

                current == GENRES_TAB -> GenresScreen(
                    books = books,
                    vm = genreViewModel,
                    onOpenBook = { id -> route = DETAIL + id },
                    onAddBook = { title -> route = ADD_TITLED + title },
                    onSetUpAi = { route = SETTINGS_PAGE + SettingsPage.AI.name },
                    modifier = tabModifier,
                )

                current == STATS -> StatsScreen(
                    books = books,
                    onOpenBook = { id -> route = DETAIL + id },
                    modifier = tabModifier,
                )

                current == SETTINGS -> SettingsScreen(
                    appearanceSettings = appearanceSettings,
                    aiConnected = aiConfig?.displayName,
                    onOpen = { page -> route = SETTINGS_PAGE + page.name },
                    modifier = tabModifier,
                )

                current.startsWith(SETTINGS_PAGE) -> {
                    val back = { route = SETTINGS }
                    when (SettingsPage.entries.firstOrNull { it.name == current.removePrefix(SETTINGS_PAGE) }) {
                        SettingsPage.APPEARANCE -> AppearancePage(appearanceSettings, back)
                        SettingsPage.TEXT -> TextPage(appearanceSettings, back)
                        SettingsPage.STYLE -> StylePage(appearanceSettings, back)
                        SettingsPage.AI -> AiSettingsPage(genreViewModel, back)
                        SettingsPage.BACKUPS -> BackupScreen(onBack = back, onRestore = { viewModel.restore(it) })
                        null -> LaunchedEffect(current) { route = SETTINGS }
                    }
                }

                current == ADD || current.startsWith(ADD_TITLED) -> BookEditorScreen(
                    book = null,
                    onClose = { route = if (current == ADD) LIBRARY else GENRES_TAB },
                    onSave = { details -> viewModel.addBook(details) { id -> route = DETAIL + id } },
                    initialTitle = current.removePrefix(ADD_TITLED).takeIf { current != ADD } ?: "",
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

                else -> LaunchedEffect(current) { route = LIBRARY }
            }
        }
    }
}
