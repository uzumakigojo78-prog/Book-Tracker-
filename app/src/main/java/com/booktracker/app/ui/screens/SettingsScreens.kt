package com.booktracker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import com.booktracker.app.settings.CustomFonts
import com.booktracker.app.ui.components.ButtonText
import com.booktracker.app.ui.components.ColorWheelPicker
import com.booktracker.app.ui.theme.seedSwatch
import kotlinx.coroutines.launch
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.booktracker.app.BuildConfig
import com.booktracker.app.backup.BackupSettings
import com.booktracker.app.data.Book
import com.booktracker.app.data.ReadingEntry
import com.booktracker.app.settings.AppearanceSettings
import com.booktracker.app.ui.components.ButtonText
import com.booktracker.app.ui.components.ChoiceButton
import com.booktracker.app.ui.components.NavRow
import com.booktracker.app.ui.components.SectionCard
import com.booktracker.app.ui.components.SubPage
import com.booktracker.app.ui.theme.AppIcons
import com.booktracker.app.ui.theme.ColorPalette
import com.booktracker.app.ui.theme.CornerStyle
import com.booktracker.app.ui.theme.FontChoice
import com.booktracker.app.ui.theme.IconStyle
import com.booktracker.app.ui.theme.LocalAppearance
import com.booktracker.app.ui.theme.TextSize
import com.booktracker.app.ui.theme.ThemeMode
import com.booktracker.app.ui.theme.dynamicColorAvailable
import com.booktracker.app.ui.theme.isDark
import com.booktracker.app.ui.theme.paletteSwatch
import java.time.LocalDate

enum class SettingsPage { APPEARANCE, TEXT, STYLE, AI, BACKUPS, DEVELOPER }

/** The Settings tab: one row per settings page. */
@Composable
fun SettingsScreen(
    appearanceSettings: AppearanceSettings,
    aiConnected: String?,
    onOpen: (SettingsPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val backup by remember { BackupSettings(context).changes() }.collectAsState(initial = BackupSettings(context).read())
    val a = LocalAppearance.current
    val c = MaterialTheme.colorScheme

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Settings",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = a.heavyWeight,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
        }
        item { PreviewCard() }
        item {
            NavRow(AppIcons.Palette, "Theme & colors", "${a.themeMode.label} · ${a.palette.label}", c.primary, c.onPrimary) {
                onOpen(SettingsPage.APPEARANCE)
            }
        }
        item {
            NavRow(
                AppIcons.TextFields, "Text",
                "${a.textSize.label} · ${a.fontLabel}${if (a.boldText) " · Bold" else ""}",
                c.secondary, c.onSecondary,
            ) { onOpen(SettingsPage.TEXT) }
        }
        item {
            NavRow(AppIcons.Style, "Style & layout", "${a.corners.label} corners · ${a.iconStyle.label} icons · tab order", c.tertiary, c.onTertiary) {
                onOpen(SettingsPage.STYLE)
            }
        }
        item {
            NavRow(
                AppIcons.Psychology, "AI",
                aiConnected ?: "Not set up · genres use basic mode",
                c.secondaryContainer, c.onSecondaryContainer,
            ) { onOpen(SettingsPage.AI) }
        }
        item {
            NavRow(
                AppIcons.Backup, "CSV backups",
                if (backup.enabled) "On · every ${backup.interval.label}" else "Off",
                c.primaryContainer, c.onPrimaryContainer,
            ) { onOpen(SettingsPage.BACKUPS) }
        }
        item {
            NavRow(AppIcons.Code, "Developer", "App info, GitHub, updates and what's new", c.surfaceContainerHighest, c.onSurface) {
                onOpen(SettingsPage.DEVELOPER)
            }
        }
        item {
            OutlinedButton(
                onClick = { appearanceSettings.reset() },
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 4.dp),
            ) {
                Icon(AppIcons.RestartAlt, null)
                Spacer(Modifier.width(8.dp))
                ButtonText("Reset look to defaults", MaterialTheme.typography.titleSmall)
            }
        }
        item {
            Text(
                "Book Tracker ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = c.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}

private val previewBook = Book(
    id = "preview",
    title = "The Hobbit",
    author = "J.R.R. Tolkien",
    releaseDate = LocalDate.of(1937, 9, 21),
    totalPages = 310,
    entries = listOf(
        ReadingEntry(LocalDate.now().minusDays(1), 96),
        ReadingEntry(LocalDate.now(), 142),
    ),
)

/** A sample book card so changes can be seen straight away. */
@Composable
private fun PreviewCard() {
    Column {
        Text(
            "Preview",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        BookCard(previewBook, colorIndex = 0, onClick = {})
    }
}

/* ---------- Theme & colors ---------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearancePage(settings: AppearanceSettings, onBack: () -> Unit) {
    val a = LocalAppearance.current
    SubPage("Theme & colors", onBack) {
        PreviewCard()
        SectionCard(title = "Theme") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEach { mode ->
                    val icon = when (mode) {
                        ThemeMode.SYSTEM -> AppIcons.BrightnessAuto
                        ThemeMode.LIGHT -> AppIcons.LightMode
                        ThemeMode.DARK -> AppIcons.DarkMode
                    }
                    BigChoice(mode.label, icon, selected = a.themeMode == mode, modifier = Modifier.weight(1f)) {
                        settings.setThemeMode(mode)
                    }
                }
            }
        }
        SectionCard(title = "Colors") {
            val dark = a.isDark()
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ColorPalette.entries
                    .filter { it != ColorPalette.DYNAMIC || dynamicColorAvailable }
                    .forEach { palette ->
                        Swatch(
                            label = palette.label,
                            colors = if (palette == ColorPalette.CUSTOM) seedSwatch(a.customColor, dark) else if (palette == ColorPalette.DYNAMIC) {
                                // Shows the current wallpaper colours only when selected; otherwise a hint.
                                if (a.palette == palette) listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary)
                                else listOf(Color(0xFF7D8B99), Color(0xFFB39DDB), Color(0xFF80CBC4))
                            } else paletteSwatch(palette, dark),
                            selected = a.palette == palette,
                        ) { settings.setPalette(palette) }
                    }
            }
            if (dynamicColorAvailable) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Wallpaper uses colours from your phone's wallpaper.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SectionCard(title = "Pick your own color") {
            var pick by remember { mutableIntStateOf(a.customColor) }
            ColorWheelPicker(pick, onChange = { pick = it })
            Spacer(Modifier.height(16.dp))
            Text("Your palette", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                seedSwatch(pick, a.isDark()).forEachIndexed { i, color ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().height(44.dp).background(color, MaterialTheme.shapes.medium))
                        Text(listOf("Main", "Second", "Accent")[i], style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { settings.setCustomColor(pick) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(AppIcons.Colorize, null)
                Spacer(Modifier.width(8.dp))
                ButtonText(if (a.palette == ColorPalette.CUSTOM && a.customColor == pick) "Using this color" else "Use this color")
            }
        }
    }
}

@Composable
private fun BigChoice(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(if (selected) MaterialTheme.shapes.medium else MaterialTheme.shapes.extraLarge)
            .background(if (selected) c.primary else c.surfaceContainerHighest)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
    ) {
        Icon(icon, null, tint = if (selected) c.onPrimary else c.onSurface, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) c.onPrimary else c.onSurface)
    }
}

@Composable
private fun Swatch(label: String, colors: List<Color>, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp).clickable(onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .border(if (selected) 3.dp else 0.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                .padding(5.dp)
                .clip(CircleShape),
        ) {
            // Three colour wedges: primary on top, secondary and tertiary below.
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth().background(colors[0]))
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    Box(Modifier.weight(1f).fillMaxSize().background(colors[1]))
                    Box(Modifier.weight(1f).fillMaxSize().background(colors[2]))
                }
            }
            if (selected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.surface, CircleShape),
                ) { Icon(AppIcons.Check, null, modifier = Modifier.size(18.dp)) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

/* ---------- Text ---------- */

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TextPage(settings: AppearanceSettings, onBack: () -> Unit) {
    val a = LocalAppearance.current
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var fontQuery by rememberSaveable { mutableStateOf("") }
    var fontError by remember { mutableStateOf<String?>(null) }
    var downloading by remember { mutableStateOf<String?>(null) }
    var fontsVersion by remember { mutableIntStateOf(0) }
    val myFonts = remember(fontsVersion, a.font) { CustomFonts.downloaded(context) }
    SubPage("Text", onBack) {
        SectionCard(color = MaterialTheme.colorScheme.primaryContainer) {
            Text("The Hobbit", style = MaterialTheme.typography.headlineLarge)
            Text("by J.R.R. Tolkien", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "In a hole in the ground there lived a hobbit.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        SectionCard(title = "Text size") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextSize.entries.forEach { size ->
                    ChoiceButton(size.label, selected = a.textSize == size, onClick = { settings.setTextSize(size) }, icon = AppIcons.FormatSize)
                }
            }
        }
        SectionCard(title = "Font") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FontChoice.entries.forEach { font ->
                    val selected = a.font == font.name
                    ChoiceButton(
                        font.label,
                        selected = selected,
                        onClick = { settings.setFont(font) },
                        modifier = Modifier.weight(1f),
                        labelContent = {
                            Text(
                                font.label,
                                fontFamily = font.family,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            )
                        },
                    )
                }
            }
            if (myFonts.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Your fonts", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    myFonts.forEach { name ->
                        val selected = a.customFontName == name
                        val family = remember(name, fontsVersion) { CustomFonts.family(context, name) }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.large)
                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
                                .clickable { settings.setCustomFont(name) }
                                .padding(start = 18.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                        ) {
                            Text(
                                name,
                                fontFamily = family,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = {
                                if (selected) settings.setFont(FontChoice.SANS)
                                CustomFonts.delete(context, name)
                                fontsVersion++
                            }) {
                                Icon(AppIcons.Delete, "Remove $name", tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        SectionCard(title = "Find a font") {
            Text(
                "Search Google Fonts, which has over 1,500 free fonts. Tap one to download it and use it everywhere in the app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = fontQuery,
                onValueChange = { fontQuery = it; fontError = null },
                placeholder = { Text("e.g. Playfair Display, Lobster", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(AppIcons.Search, null) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            fontError?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(10.dp))
            val q = fontQuery.trim()
            val matches = CustomFonts.POPULAR.filter { q.isEmpty() || it.contains(q, ignoreCase = true) }.take(if (q.isEmpty()) 12 else 20)
            val options = if (q.isNotEmpty() && matches.none { it.equals(q, ignoreCase = true) }) listOf(q) + matches else matches
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { name ->
                    val isTyped = name == q && CustomFonts.POPULAR.none { it.equals(q, ignoreCase = true) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable(enabled = downloading == null) {
                                downloading = name
                                fontError = null
                                scope.launch {
                                    CustomFonts.download(context, name)
                                        .onSuccess { real -> settings.setCustomFont(real); fontsVersion++; fontQuery = "" }
                                        .onFailure { fontError = it.message ?: "Couldn't download that font" }
                                    downloading = null
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Icon(AppIcons.FontDownload, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (isTyped) "Search Google Fonts for \"$name\"" else name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        when {
                            downloading == name -> LoadingIndicator(Modifier.size(28.dp))
                            a.customFontName.equals(name, ignoreCase = true) -> Icon(AppIcons.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            else -> Icon(AppIcons.Download, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Bold text", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Heavy headings and numbers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = a.boldText, onCheckedChange = { settings.setBoldText(it) })
            }
        }
    }
}

/* ---------- Style ---------- */

@Composable
fun StylePage(settings: AppearanceSettings, onBack: () -> Unit) {
    val a = LocalAppearance.current
    val c = MaterialTheme.colorScheme
    SubPage("Style & layout", onBack) {
        SectionCard(title = "Tab order") {
            Text(
                "Put the bottom tabs in any order. The first tab opens when you start the app.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            ReorderList(
                items = a.tabOrder,
                label = { it.label },
                onMove = { settings.setTabOrder(it) },
            )
        }
        PreviewCard()
        SectionCard(title = "Corners") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CornerStyle.entries.forEach { style ->
                    val selected = a.corners == style
                    val radius = when (style) {
                        CornerStyle.EXTRA_ROUND -> 18.dp
                        CornerStyle.ROUNDED -> 10.dp
                        CornerStyle.SQUARE -> 3.dp
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.large)
                            .background(if (selected) c.primary else c.surfaceContainerHighest)
                            .clickable { settings.setCorners(style) }
                            .padding(vertical = 16.dp),
                    ) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .background(if (selected) c.onPrimary else c.primary, RoundedCornerShape(radius))
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(style.label, style = MaterialTheme.typography.labelLarge, color = if (selected) c.onPrimary else c.onSurface)
                    }
                }
            }
        }
        SectionCard(title = "Icons") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IconStyle.entries.forEach { style ->
                    val selected = a.iconStyle == style
                    // Render the sample icons in that style regardless of the current one.
                    CompositionLocalProvider(LocalAppearance provides a.copy(iconStyle = style)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.large)
                                .background(if (selected) c.primary else c.surfaceContainerHighest)
                                .clickable { settings.setIconStyle(style) }
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                        ) {
                            val tint = if (selected) c.onPrimary else c.onSurface
                            Text(style.label, style = MaterialTheme.typography.titleMedium, color = tint, modifier = Modifier.weight(1f))
                            listOf(AppIcons.MenuBook, AppIcons.Settings, AppIcons.Delete, AppIcons.Person).forEach {
                                Icon(it, null, tint = tint, modifier = Modifier.padding(start = 10.dp).size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A list with up/down buttons to reorder items. */
@Composable
fun <T> ReorderList(items: List<T>, label: (T) -> String, onMove: (List<T>) -> Unit) {
    val c = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { i, item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.surfaceContainerHighest, MaterialTheme.shapes.large)
                    .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(28.dp).background(c.primary, CircleShape),
                ) { Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = c.onPrimary) }
                Spacer(Modifier.width(12.dp))
                Text(label(item), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton(onClick = { onMove(items.toMutableList().apply { add(i - 1, removeAt(i)) }) }, enabled = i > 0) {
                    Icon(AppIcons.KeyboardArrowUp, "Move ${label(item)} up")
                }
                IconButton(onClick = { onMove(items.toMutableList().apply { add(i + 1, removeAt(i)) }) }, enabled = i < items.lastIndex) {
                    Icon(AppIcons.KeyboardArrowDown, "Move ${label(item)} down")
                }
            }
        }
    }
}
