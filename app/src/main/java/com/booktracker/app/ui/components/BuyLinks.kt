package com.booktracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.booktracker.app.data.BuyLinks
import com.booktracker.app.settings.LibrarySettings
import com.booktracker.app.ui.theme.AppIcons

/**
 * "Get it from your library" plus a grid of stores to buy the book from. Shown with
 * every book description.
 */
@Composable
fun BuySection(title: String, author: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val settings = remember { LibrarySettings(context) }
    val library by remember { settings.changes() }.collectAsState(initial = settings.read())
    val uri = LocalUriHandler.current
    val c = MaterialTheme.colorScheme
    val stores = remember(title, author) { BuyLinks.forBook(title, author) }
    val query = listOf(title, author).filter { it.isNotBlank() }.joinToString(" ")

    Column(modifier.fillMaxWidth()) {
        Text("Get this book", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        val lib = library
        if (lib != null) {
            StoreTile(
                AppIcons.LocalLibrary, "Check ${lib.name}",
                if (lib.hasCatalog) "Search your library's catalog" else "Find it in libraries near you",
                c.tertiary, c.onTertiary, Modifier.fillMaxWidth(),
            ) { uri.openUri(lib.catalogSearch(query)) }
        } else {
            StoreTile(
                AppIcons.LocalLibrary, "Find it at a library", "Borrow it free · WorldCat",
                c.tertiaryContainer, c.onTertiaryContainer, Modifier.fillMaxWidth(),
            ) { uri.openUri(BuyLinks.worldCat(title, author)) }
        }
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            stores.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    pair.forEach { store ->
                        val featured = store.id == "amazon" || store.id == "bn"
                        StoreTile(
                            if (featured) AppIcons.ShoppingCart else AppIcons.Storefront,
                            store.name, store.note,
                            if (featured) c.secondaryContainer else c.surfaceContainerHighest,
                            if (featured) c.onSecondaryContainer else c.onSurface,
                            Modifier.weight(1f).fillMaxHeight(),
                        ) { uri.openUri(store.url) }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Store links open a search for this book. Prices and formats are on each store's site.",
            style = MaterialTheme.typography.bodySmall,
            color = c.onSurfaceVariant,
        )
    }
}

@Composable
private fun StoreTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(bg, MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = fg, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.8f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
