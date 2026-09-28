package com.booktracker.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.booktracker.app.data.Book
import com.booktracker.app.data.Library
import com.booktracker.app.data.LibraryFinder
import com.booktracker.app.settings.LibrarySettings
import com.booktracker.app.ui.components.BookBadge
import com.booktracker.app.ui.components.ButtonText
import com.booktracker.app.ui.components.SectionCard
import com.booktracker.app.ui.theme.AppIcons
import com.booktracker.app.ui.theme.BookSection
import com.booktracker.app.ui.theme.LocalAppearance
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/**
 * The Library tab: find your public library (location is used for that and nothing
 * else), then keep its card, account, catalog and digital apps in one place.
 */
@Composable
fun LibraryHubScreen(books: List<Book>, onOpenBook: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val settings = remember { LibrarySettings(context) }
    val library by remember { settings.changes() }.collectAsState(initial = settings.read())
    var changing by rememberSaveable { mutableStateOf(false) }

    val lib = library
    if (lib == null || changing) {
        LibraryFinderContent(
            onPick = { picked ->
                // Keep the reader's card and links when they switch branches.
                val keep = lib?.takeIf { changing }
                settings.save(
                    picked.copy(
                        cardNumber = keep?.cardNumber.orEmpty(),
                        accountUrl = keep?.accountUrl.orEmpty(),
                        catalogUrl = keep?.catalogUrl.orEmpty(),
                    ),
                )
                changing = false
            },
            onCancel = if (lib != null) ({ changing = false }) else null,
            modifier = modifier,
        )
    } else {
        LibraryHubContent(
            lib = lib,
            books = books,
            onOpenBook = onOpenBook,
            onSave = settings::save,
            onChange = { changing = true },
            onUnlink = settings::unlink,
            modifier = modifier,
        )
    }
}

@Composable
private fun Header(subtitle: String) {
    Column(Modifier.padding(start = 4.dp, bottom = 4.dp)) {
        Text("Library", style = MaterialTheme.typography.displaySmall, fontWeight = LocalAppearance.current.heavyWeight)
        Text(subtitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------- Finding a library

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LibraryFinderContent(onPick: (Library) -> Unit, onCancel: (() -> Unit)?, modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val c = MaterialTheme.colorScheme
    var results by remember { mutableStateOf<List<Library>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var manual by remember { mutableStateOf(false) }

    fun searchNearby() {
        loading = true
        message = null
        scope.launch {
            val loc = currentLocation(context)
            if (loc == null) {
                loading = false
                message = "Couldn't get your location. Check that location is turned on, or search by name, city or ZIP instead."
                return@launch
            }
            runCatching { LibraryFinder.near(loc.latitude, loc.longitude) }
                .onSuccess { results = it; if (it.isEmpty()) message = "No libraries found nearby. Try searching by city or ZIP." }
                .onFailure { message = "Library search failed. Check your connection and try again." }
            loading = false
        }
    }

    fun searchByName() {
        val q = query.trim()
        if (q.isEmpty()) return
        loading = true
        message = null
        scope.launch {
            runCatching { LibraryFinder.search(q) }
                .onSuccess { results = it; if (it.isEmpty()) message = "No libraries found for \"$q\". Try a city, ZIP code or the library's name." }
                .onFailure { message = "Library search failed. Check your connection and try again." }
            loading = false
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) searchNearby()
        else message = "No problem. Search by library name, city or ZIP code instead."
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Header("Link your public library and keep everything in one place") }
        item {
            SectionCard(color = c.primaryContainer) {
                Icon(AppIcons.LocalLibrary, null, tint = c.onPrimaryContainer, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(10.dp))
                Text("Find your library", style = MaterialTheme.typography.headlineSmall, color = c.onPrimaryContainer)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Your library becomes a hub: your card, your account, catalog search, your want-to-read list and free ebook apps.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.onPrimaryContainer,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED
                        if (granted) searchNearby() else permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                    enabled = !loading,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Icon(AppIcons.MyLocation, null)
                    Spacer(Modifier.width(8.dp))
                    ButtonText("Find libraries near me", MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(AppIcons.Lock, null, tint = c.onPrimaryContainer, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Location is only used to look up libraries near you, once. It's rounded to about 1 km, sent only to " +
                            "OpenStreetMap's library search and never saved or used for anything else.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onPrimaryContainer,
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Or search by name, city or ZIP") },
                leadingIcon = { Icon(AppIcons.Search, null) },
                trailingIcon = {
                    if (query.isNotBlank()) IconButton(onClick = { searchByName() }) { Icon(AppIcons.KeyboardArrowRight, "Search") }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { searchByName() }),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (loading) {
            item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { LoadingIndicator() } }
        }
        message?.let { m ->
            item {
                Text(m, style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
        val list = results.orEmpty()
        if (!loading && list.isNotEmpty()) {
            item {
                Text(
                    "${list.size} ${if (list.size == 1) "library" else "libraries"}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )
            }
            items(list, key = { "${it.name}|${it.address}|${it.lat}" }) { lib -> LibraryResultCard(lib) { onPick(lib) } }
        }
        item {
            TextButton(onClick = { manual = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(AppIcons.Edit, null)
                Spacer(Modifier.width(8.dp))
                ButtonText("Not listed? Add your library yourself")
            }
        }
        if (onCancel != null) {
            item {
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(52.dp)) { ButtonText("Keep my current library") }
            }
        }
    }

    if (manual) {
        LibraryDetailsDialog(
            initial = Library(name = query.trim()),
            title = "Add your library",
            onDismiss = { manual = false },
            onSave = { manual = false; onPick(it) },
        )
    }
}

@Composable
private fun LibraryResultCard(lib: Library, onPick: () -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.Top) {
            Icon(AppIcons.LocalLibrary, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(lib.name, style = MaterialTheme.typography.titleLarge)
                lib.address?.let { InfoLine(AppIcons.Place, it) }
                lib.distanceKm?.let { InfoLine(AppIcons.MyLocation, formatDistance(it)) }
                lib.hours?.let { InfoLine(AppIcons.Schedule, it) }
            }
        }
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(onClick = onPick, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Icon(AppIcons.CheckCircle, null)
            Spacer(Modifier.width(8.dp))
            ButtonText("This is my library")
        }
    }
}

// ---------------------------------------------------------------- The hub

@Composable
private fun LibraryHubContent(
    lib: Library,
    books: List<Book>,
    onOpenBook: (String) -> Unit,
    onSave: (Library) -> Unit,
    onChange: () -> Unit,
    onUnlink: () -> Unit,
    modifier: Modifier,
) {
    val uri = LocalUriHandler.current
    val context = LocalContext.current
    val c = MaterialTheme.colorScheme
    var editing by remember { mutableStateOf(false) }
    var showCard by rememberSaveable { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    var confirmUnlink by remember { mutableStateOf(false) }
    val wanted = books.filter { it.section() == BookSection.WANT }

    fun searchCatalog() {
        if (search.isNotBlank()) uri.openUri(lib.catalogSearch(search.trim()))
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Header("Your library hub") }

        // Which library, and how to get there.
        item {
            SectionCard(color = c.primary) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.LocalLibrary, null, tint = c.onPrimary, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(lib.name, style = MaterialTheme.typography.headlineSmall, color = c.onPrimary, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                lib.address?.let { InfoLine(AppIcons.Place, it, c.onPrimary) }
                lib.hours?.let { InfoLine(AppIcons.Schedule, it, c.onPrimary) }
                lib.phone?.let { InfoLine(AppIcons.Phone, it, c.onPrimary) }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    lib.websiteLink?.let { url ->
                        HubChip(AppIcons.Language, "Website", c.onPrimary, c.primary, Modifier.weight(1f)) { uri.openUri(url) }
                    }
                    HubChip(AppIcons.Map, "Directions", c.onPrimary, c.primary, Modifier.weight(1f)) { uri.openUri(lib.mapUrl) }
                    lib.phone?.let { phone ->
                        HubChip(AppIcons.Phone, "Call", c.onPrimary, c.primary, Modifier.weight(1f)) {
                            uri.openUri("tel:" + phone.filter { it.isDigit() || it == '+' })
                        }
                    }
                }
            }
        }

        // Library card and account.
        item {
            SectionCard(title = "Library card") {
                if (lib.cardNumber.isBlank()) {
                    Text(
                        "Add your card number to keep it handy at the desk and when signing in.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = c.onSurfaceVariant,
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().background(c.surfaceContainerHighest, MaterialTheme.shapes.large).padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                    ) {
                        Icon(AppIcons.CreditCard, null, tint = c.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (showCard) lib.cardNumber else "•••• " + lib.cardNumber.takeLast(4),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            maxLines = if (showCard) 2 else 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { showCard = !showCard }) {
                            Icon(if (showCard) AppIcons.VisibilityOff else AppIcons.Visibility, if (showCard) "Hide" else "Show")
                        }
                        IconButton(onClick = {
                            context.getSystemService(android.content.ClipboardManager::class.java)
                                ?.setPrimaryClip(ClipData.newPlainText("Library card", lib.cardNumber))
                        }) { Icon(AppIcons.ContentCopy, "Copy card number") }
                    }
                }
                Spacer(Modifier.height(12.dp))
                lib.accountLink?.let { url ->
                    Button(onClick = { uri.openUri(url) }, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                        Icon(AppIcons.Person, null)
                        Spacer(Modifier.width(8.dp))
                        ButtonText("Sign in to my library account")
                    }
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedButton(onClick = { editing = true }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Icon(AppIcons.Edit, null)
                    Spacer(Modifier.width(8.dp))
                    ButtonText(if (lib.cardNumber.isBlank()) "Add card & account links" else "Edit card & account links")
                }
            }
        }

        // Catalog search.
        item {
            SectionCard(title = "Search the catalog") {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Title, author or topic") },
                    leadingIcon = { Icon(AppIcons.Search, null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { searchCatalog() }),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                FilledTonalButton(onClick = { searchCatalog() }, enabled = search.isNotBlank(), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Icon(AppIcons.Search, null)
                    Spacer(Modifier.width(8.dp))
                    ButtonText(if (lib.hasCatalog) "Search ${lib.name}" else "Search libraries (WorldCat)")
                }
                if (!lib.hasCatalog) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tip: add your library's catalog in \"Edit card & account links\" to search it directly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onSurfaceVariant,
                    )
                }
            }
        }

        // The reader's want-to-read list, one tap to check each at the library.
        item {
            SectionCard(title = "Want to read") {
                if (wanted.isEmpty()) {
                    Text(
                        "Books you add but haven't started show up here, so you can check if your library has them.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = c.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        wanted.forEachIndexed { i, book ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.padding(end = 12.dp)) {
                                    BookBadge(book.title, books.indexOf(book).coerceAtLeast(i), book.coverUrl, width = 44.dp)
                                }
                                Column(Modifier.weight(1f)) {
                                    TextButton(onClick = { onOpenBook(book.id) }, contentPadding = PaddingValues(0.dp)) {
                                        Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    if (book.author.isNotBlank()) {
                                        Text(book.author, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                FilledTonalButton(onClick = {
                                    uri.openUri(lib.catalogSearch(listOf(book.title, book.author).filter { it.isNotBlank() }.joinToString(" ")))
                                }) { Text("Check") }
                            }
                        }
                    }
                }
            }
        }

        // Free digital borrowing with a library card.
        item {
            SectionCard(title = "Borrow ebooks & audiobooks") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DigitalRow("Libby", "Ebooks & audiobooks from your library", c.secondaryContainer, c.onSecondaryContainer) { uri.openUri("https://libbyapp.com") }
                    DigitalRow("Hoopla", "Borrow instantly, no waitlists", c.tertiaryContainer, c.onTertiaryContainer) { uri.openUri("https://www.hoopladigital.com") }
                    DigitalRow("Open Library", "Free digital lending", c.surfaceContainerHighest, c.onSurface) { uri.openUri("https://openlibrary.org") }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Sign in to these with your library card number.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant,
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onChange, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(AppIcons.SwapVert, null)
                    Spacer(Modifier.width(6.dp))
                    ButtonText("Change library")
                }
                TextButton(
                    onClick = { confirmUnlink = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = c.error),
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Icon(AppIcons.LinkOff, null)
                    Spacer(Modifier.width(6.dp))
                    ButtonText("Unlink")
                }
            }
        }
        item {
            Text(
                "Your library, card number and links are stored only on this phone.",
                style = MaterialTheme.typography.bodySmall,
                color = c.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }

    if (editing) {
        LibraryDetailsDialog(
            initial = lib,
            title = "Card & account",
            onDismiss = { editing = false },
            onSave = { onSave(it); editing = false },
        )
    }
    if (confirmUnlink) {
        AlertDialog(
            onDismissRequest = { confirmUnlink = false },
            icon = { Icon(AppIcons.LinkOff, null) },
            title = { Text("Unlink ${lib.name}?") },
            text = { Text("Your saved card number and links for this library will be removed from this phone.") },
            confirmButton = {
                Button(
                    onClick = { confirmUnlink = false; onUnlink() },
                    colors = ButtonDefaults.buttonColors(containerColor = c.error, contentColor = c.onError),
                ) { Text("Unlink") }
            },
            dismissButton = { TextButton(onClick = { confirmUnlink = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun LibraryDetailsDialog(initial: Library, title: String, onDismiss: () -> Unit, onSave: (Library) -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var card by remember { mutableStateOf(initial.cardNumber) }
    var account by remember { mutableStateOf(initial.accountUrl) }
    var catalog by remember { mutableStateOf(initial.catalogUrl) }
    var website by remember { mutableStateOf(initial.website.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(AppIcons.LocalLibrary, null) },
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Library name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    card, { card = it.trim() }, label = { Text("Library card number") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    website, { website = it.trim() }, label = { Text("Library website") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    account, { account = it.trim() }, label = { Text("Account sign-in page") }, singleLine = true,
                    placeholder = { Text("https://…") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    catalog, { catalog = it.trim() }, label = { Text("Catalog search link") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "To link the catalog: on your library's website, search for the word \"${Library.PLACEHOLDER}\", then copy that " +
                        "page's address here. Book Tracker swaps in whatever you search for.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            name = name.trim().ifBlank { initial.name.ifBlank { "My library" } },
                            cardNumber = card,
                            accountUrl = account,
                            catalogUrl = catalog,
                            website = website.ifBlank { null },
                        ),
                    )
                },
                enabled = name.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun InfoLine(icon: ImageVector, text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp).padding(top = 1.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HubChip(icon: ImageVector, label: String, bg: Color, fg: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        modifier = modifier.height(52.dp),
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        ButtonText(label, MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun DigitalRow(title: String, subtitle: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg),
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(AppIcons.AutoStories, null)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.8f))
        }
        Icon(AppIcons.OpenInNew, null, modifier = Modifier.size(18.dp))
    }
}

private fun formatDistance(km: Double): String {
    val miles = Locale.getDefault().country in setOf("US", "GB", "LR", "MM")
    return if (miles) String.format(Locale.US, "%.1f mi away", km * 0.621371) else String.format(Locale.US, "%.1f km away", km)
}

/**
 * One rough location fix, only to search for libraries. Uses a recent fix if the phone
 * already has one; otherwise asks the network provider once.
 */
@SuppressLint("MissingPermission")
private suspend fun currentLocation(context: Context): Location? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
    val lm = context.getSystemService(LocationManager::class.java) ?: return null
    val known = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }
    if (known != null && System.currentTimeMillis() - known.time < 30 * 60_000L) return known

    val provider = when {
        runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false) -> LocationManager.NETWORK_PROVIDER
        Build.VERSION.SDK_INT >= 31 && runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false) -> LocationManager.GPS_PROVIDER
        else -> return known
    }
    val fresh = withTimeoutOrNull(20_000) {
        suspendCancellableCoroutine<Location?> { cont ->
            runCatching {
                if (Build.VERSION.SDK_INT >= 30) {
                    val signal = CancellationSignal()
                    cont.invokeOnCancellation { signal.cancel() }
                    lm.getCurrentLocation(provider, signal, ContextCompat.getMainExecutor(context)) { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }
                } else {
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            lm.removeUpdates(this)
                            if (cont.isActive) cont.resume(location)
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
                        override fun onProviderEnabled(provider: String) = Unit
                        override fun onProviderDisabled(provider: String) = Unit
                    }
                    cont.invokeOnCancellation { lm.removeUpdates(listener) }
                    @Suppress("DEPRECATION")
                    lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                }
            }.onFailure { if (cont.isActive) cont.resume(null) }
        }
    }
    return fresh ?: known
}
