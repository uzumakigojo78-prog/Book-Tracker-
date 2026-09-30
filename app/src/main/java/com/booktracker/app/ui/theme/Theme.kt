package com.booktracker.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ---------- user choices ---------- */

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

enum class TextSize(val label: String, val scale: Float) {
    SMALL("Small", 0.9f), DEFAULT("Default", 1f), LARGE("Large", 1.12f), EXTRA_LARGE("Extra large", 1.25f),
}

enum class FontChoice(val label: String, val family: FontFamily) {
    SANS("Sans", FontFamily.SansSerif), SERIF("Serif", FontFamily.Serif), MONO("Mono", FontFamily.Monospace),
}

enum class CornerStyle(val label: String) { EXTRA_ROUND("Extra round"), ROUNDED("Rounded"), SQUARE("Square") }

enum class IconStyle(val label: String) { OUTLINED("Outlined"), ROUNDED("Rounded"), SHARP("Sharp"), FILLED("Filled") }

enum class ColorPalette(val label: String) {
    VIOLET("Violet"), OCEAN("Ocean"), FOREST("Forest"), SUNSET("Sunset"), ROSE("Rose"), DYNAMIC("Wallpaper"), CUSTOM("Custom"),
}

/** Bottom navigation tabs, in the order the reader chose. */
enum class AppTab(val label: String) { BOOKS("Books"), GENRES("Genres"), STATS("Stats"), SETTINGS("Settings") }

/** Sections of the Books tab. */
enum class BookSection(val label: String) { READING("Currently reading"), WANT("Want to read"), READ("Read") }

data class Appearance(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val palette: ColorPalette = ColorPalette.VIOLET,
    val textSize: TextSize = TextSize.DEFAULT,
    /** A [FontChoice] name, or "gf:<Google Font family>" for a downloaded font. */
    val font: String = FontChoice.SANS.name,
    val boldText: Boolean = true,
    val corners: CornerStyle = CornerStyle.EXTRA_ROUND,
    val iconStyle: IconStyle = IconStyle.OUTLINED,
    /** Seed colour (ARGB) for the [ColorPalette.CUSTOM] palette. */
    val customColor: Int = 0xFF6B2BD9.toInt(),
    val tabOrder: List<AppTab> = AppTab.entries,
    val sectionOrder: List<BookSection> = BookSection.entries,
) {
    val builtInFont: FontChoice? get() = FontChoice.entries.firstOrNull { it.name == font }
    val customFontName: String? get() = font.removePrefix(CUSTOM_FONT_PREFIX).takeIf { font.startsWith(CUSTOM_FONT_PREFIX) }
    val fontLabel: String get() = builtInFont?.label ?: customFontName ?: "Sans"

    /** Weight for big numbers and headings. */
    val heavyWeight: FontWeight get() = if (boldText) FontWeight.Black else FontWeight.SemiBold
}

const val CUSTOM_FONT_PREFIX = "gf:"

val LocalAppearance = staticCompositionLocalOf { Appearance() }

val dynamicColorAvailable: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/* ---------- colours ---------- */

private class Accent(val main: Color, val on: Color, val container: Color, val onContainer: Color)
private class PaletteColors(val primary: Accent, val secondary: Accent, val tertiary: Accent)

private fun a(main: Long, on: Long, container: Long, onContainer: Long) =
    Accent(Color(main), Color(on), Color(container), Color(onContainer))

// Light / dark tones for each palette. Surfaces stay neutral so every palette reads clearly.
private val violet = a(0xFF6B2BD9, 0xFFFFFFFF, 0xFFE9DDFF, 0xFF22005D) to a(0xFFD0BCFF, 0xFF3B0092, 0xFF5516BE, 0xFFE9DDFF)
private val pink = a(0xFFB0235F, 0xFFFFFFFF, 0xFFFFD9E2, 0xFF3E001D) to a(0xFFFFB1C8, 0xFF650033, 0xFF8E0048, 0xFFFFD9E2)
private val amber = a(0xFF8A5100, 0xFFFFFFFF, 0xFFFFDCBE, 0xFF2C1600) to a(0xFFFFB870, 0xFF4A2800, 0xFF693C00, 0xFFFFDCBE)
private val blue = a(0xFF0061A4, 0xFFFFFFFF, 0xFFD1E4FF, 0xFF001D36) to a(0xFF9ECAFF, 0xFF003258, 0xFF00497D, 0xFFD1E4FF)
private val teal = a(0xFF00696E, 0xFFFFFFFF, 0xFFA0EFF3, 0xFF002022) to a(0xFF4CD9E0, 0xFF003739, 0xFF004F52, 0xFFA0EFF3)
private val green = a(0xFF2E6C00, 0xFFFFFFFF, 0xFFADF67A, 0xFF0A2100) to a(0xFF92D961, 0xFF173800, 0xFF225100, 0xFFADF67A)
private val mint = a(0xFF006C4C, 0xFFFFFFFF, 0xFF89F8C7, 0xFF002114) to a(0xFF6CDBAC, 0xFF003826, 0xFF005139, 0xFF89F8C7)
private val orange = a(0xFFA33200, 0xFFFFFFFF, 0xFFFFDBCF, 0xFF3A0B00) to a(0xFFFFB59D, 0xFF5D1800, 0xFF842500, 0xFFFFDBCF)
private val gold = a(0xFF7D5800, 0xFFFFFFFF, 0xFFFFDEA6, 0xFF271900) to a(0xFFF9BC48, 0xFF422C00, 0xFF5F4100, 0xFFFFDEA6)

private fun paletteColors(p: ColorPalette, dark: Boolean): PaletteColors {
    val (primary, secondary, tertiary) = when (p) {
        ColorPalette.VIOLET, ColorPalette.DYNAMIC, ColorPalette.CUSTOM -> Triple(violet, pink, amber)
        ColorPalette.OCEAN -> Triple(blue, teal, violet)
        ColorPalette.FOREST -> Triple(green, mint, amber)
        ColorPalette.SUNSET -> Triple(orange, pink, gold)
        ColorPalette.ROSE -> Triple(pink, violet, orange)
    }
    fun pick(pair: Pair<Accent, Accent>) = if (dark) pair.second else pair.first
    return PaletteColors(pick(primary), pick(secondary), pick(tertiary))
}

/** The three main colours of a palette, for the swatches in Settings. */
fun paletteSwatch(p: ColorPalette, dark: Boolean): List<Color> =
    paletteColors(p, dark).let { listOf(it.primary.main, it.secondary.main, it.tertiary.main) }

private val lightBase = lightColorScheme(
    error = Color(0xFFBA1A1A), onError = Color.White, errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFF8F4), onBackground = Color(0xFF1D1A20), surface = Color(0xFFFFF8F4), onSurface = Color(0xFF1D1A20),
    surfaceVariant = Color(0xFFE8E0EB), onSurfaceVariant = Color(0xFF4A4453), outline = Color(0xFF7B7484), outlineVariant = Color(0xFFCBC3D4),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFBF1F7), surfaceContainer = Color(0xFFF5EBF3),
    surfaceContainerHigh = Color(0xFFEFE5ED), surfaceContainerHighest = Color(0xFFE9DFE8),
)

private val darkBase = darkColorScheme(
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005), errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF151218), onBackground = Color(0xFFE8E0E8), surface = Color(0xFF151218), onSurface = Color(0xFFE8E0E8),
    surfaceVariant = Color(0xFF4A4453), onSurfaceVariant = Color(0xFFCBC3D4), outline = Color(0xFF958E9E), outlineVariant = Color(0xFF4A4453),
    surfaceContainerLowest = Color(0xFF100D13), surfaceContainerLow = Color(0xFF1D1A20), surfaceContainer = Color(0xFF221E25),
    surfaceContainerHigh = Color(0xFF2C2830), surfaceContainerHighest = Color(0xFF37333B),
)

/**
 * Builds light or dark accent colours from any seed colour: the primary keeps the
 * seed's hue, secondary and tertiary are neighbouring hues, all at M3-like tones.
 */
private fun seedColors(seed: Int, dark: Boolean): PaletteColors {
    val hsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.colorToHSL(seed, hsl)
    val hue = hsl[0]
    // Keep greys grey, but give everything else enough colour to look bold.
    val sat = if (hsl[1] < 0.08f) hsl[1] else hsl[1].coerceIn(0.45f, 0.9f)
    fun accent(h: Float, s: Float): Accent {
        val hh = (h % 360f + 360f) % 360f
        fun c(l: Float) = Color.hsl(hh, s, l)
        return if (dark) Accent(c(0.80f), c(0.20f), c(0.30f), c(0.90f))
        else Accent(c(0.40f), Color.White, c(0.90f), c(0.12f))
    }
    return PaletteColors(accent(hue, sat), accent(hue - 35f, sat * 0.85f), accent(hue + 65f, sat * 0.85f))
}

/** Main colour of a seed palette, for swatches. */
fun seedSwatch(seed: Int, dark: Boolean): List<Color> = seedColors(seed, dark).let { listOf(it.primary.main, it.secondary.main, it.tertiary.main) }

private fun colorScheme(p: ColorPalette, dark: Boolean, seed: Int): ColorScheme {
    val c = if (p == ColorPalette.CUSTOM) seedColors(seed, dark) else paletteColors(p, dark)
    return (if (dark) darkBase else lightBase).copy(
        primary = c.primary.main, onPrimary = c.primary.on,
        primaryContainer = c.primary.container, onPrimaryContainer = c.primary.onContainer,
        inversePrimary = c.primary.container,
        secondary = c.secondary.main, onSecondary = c.secondary.on,
        secondaryContainer = c.secondary.container, onSecondaryContainer = c.secondary.onContainer,
        tertiary = c.tertiary.main, onTertiary = c.tertiary.on,
        tertiaryContainer = c.tertiary.container, onTertiaryContainer = c.tertiary.onContainer,
        surfaceTint = c.primary.main,
    )
}

/* ---------- type ---------- */

private fun buildTypography(appearance: Appearance, family: FontFamily): Typography {
    val base = Typography()
    val scale = appearance.textSize.scale
    val bold = appearance.boldText
    fun TextStyle.adjust(boldWeight: FontWeight?, spacing: Float? = null) = copy(
        fontSize = fontSize * scale,
        lineHeight = lineHeight * scale,
        fontFamily = family,
        fontWeight = if (bold && boldWeight != null) boldWeight else fontWeight,
        letterSpacing = if (bold && spacing != null) spacing.sp else letterSpacing,
    )
    return Typography(
        displayLarge = base.displayLarge.adjust(FontWeight.Black, -1f),
        displayMedium = base.displayMedium.adjust(FontWeight.Black, -0.5f),
        displaySmall = base.displaySmall.adjust(FontWeight.ExtraBold),
        headlineLarge = base.headlineLarge.adjust(FontWeight.ExtraBold),
        headlineMedium = base.headlineMedium.adjust(FontWeight.ExtraBold),
        headlineSmall = base.headlineSmall.adjust(FontWeight.Bold),
        titleLarge = base.titleLarge.adjust(FontWeight.Bold),
        titleMedium = base.titleMedium.adjust(FontWeight.Bold),
        titleSmall = base.titleSmall.adjust(FontWeight.Bold),
        bodyLarge = base.bodyLarge.adjust(FontWeight.Medium),
        bodyMedium = base.bodyMedium.adjust(FontWeight.Medium),
        bodySmall = base.bodySmall.adjust(null),
        labelLarge = base.labelLarge.copy(fontSize = 16.sp).adjust(FontWeight.Bold),
        labelMedium = base.labelMedium.adjust(FontWeight.Bold),
        labelSmall = base.labelSmall.adjust(FontWeight.Bold),
    )
}

/** Big number style used for hero stats. */
val heroNumber: TextStyle
    @Composable @ReadOnlyComposable get() {
        val appearance = LocalAppearance.current
        return MaterialTheme.typography.displayLarge.copy(
            fontWeight = appearance.heavyWeight,
            fontSize = 56.sp * appearance.textSize.scale,
            lineHeight = 60.sp * appearance.textSize.scale,
            letterSpacing = (-2).sp,
        )
    }

/* ---------- shapes ---------- */

private fun buildShapes(style: CornerStyle): Shapes = when (style) {
    CornerStyle.EXTRA_ROUND -> Shapes(
        extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(36.dp),
    )
    CornerStyle.ROUNDED -> Shapes(
        extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp), extraLarge = RoundedCornerShape(24.dp),
    )
    CornerStyle.SQUARE -> Shapes(
        extraSmall = RoundedCornerShape(2.dp), small = RoundedCornerShape(4.dp), medium = RoundedCornerShape(6.dp),
        large = RoundedCornerShape(8.dp), extraLarge = RoundedCornerShape(10.dp),
    )
}

/** Resolves the theme mode to light or dark. */
@Composable
fun Appearance.isDark(): Boolean = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BookTrackerTheme(
    appearance: Appearance = Appearance(),
    content: @Composable () -> Unit,
) {
    val dark = appearance.isDark()
    val context = LocalContext.current
    val colors = if (appearance.palette == ColorPalette.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        colorScheme(appearance.palette, dark, appearance.customColor)
    }
    val family = remember(appearance.font) {
        appearance.builtInFont?.family
            ?: appearance.customFontName?.let { com.booktracker.app.settings.CustomFonts.family(context, it) }
            ?: FontFamily.SansSerif
    }
    val typography = remember(appearance.textSize, family, appearance.boldText) { buildTypography(appearance, family) }
    val shapes = remember(appearance.corners) { buildShapes(appearance.corners) }

    CompositionLocalProvider(LocalAppearance provides appearance) {
        MaterialExpressiveTheme(
            colorScheme = colors,
            motionScheme = MotionScheme.expressive(),
            shapes = shapes,
            typography = typography,
            content = content,
        )
    }
}
