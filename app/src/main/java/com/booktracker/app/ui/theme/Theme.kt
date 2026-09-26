package com.booktracker.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// A bold, high-contrast violet / pink / amber palette.
private val LightColors = lightColorScheme(
    primary = Color(0xFF6B2BD9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE9DDFF),
    onPrimaryContainer = Color(0xFF22005D),
    secondary = Color(0xFFB0235F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9E2),
    onSecondaryContainer = Color(0xFF3E001D),
    tertiary = Color(0xFF8A5100),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCBE),
    onTertiaryContainer = Color(0xFF2C1600),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFF8F4),
    onBackground = Color(0xFF1D1A20),
    surface = Color(0xFFFFF8F4),
    onSurface = Color(0xFF1D1A20),
    surfaceVariant = Color(0xFFE8E0EB),
    onSurfaceVariant = Color(0xFF4A4453),
    outline = Color(0xFF7B7484),
    outlineVariant = Color(0xFFCBC3D4),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBF1F7),
    surfaceContainer = Color(0xFFF5EBF3),
    surfaceContainerHigh = Color(0xFFEFE5ED),
    surfaceContainerHighest = Color(0xFFE9DFE8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF3B0092),
    primaryContainer = Color(0xFF5516BE),
    onPrimaryContainer = Color(0xFFE9DDFF),
    secondary = Color(0xFFFFB1C8),
    onSecondary = Color(0xFF650033),
    secondaryContainer = Color(0xFF8E0048),
    onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = Color(0xFFFFB870),
    onTertiary = Color(0xFF4A2800),
    tertiaryContainer = Color(0xFF693C00),
    onTertiaryContainer = Color(0xFFFFDCBE),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF151218),
    onBackground = Color(0xFFE8E0E8),
    surface = Color(0xFF151218),
    onSurface = Color(0xFFE8E0E8),
    surfaceVariant = Color(0xFF4A4453),
    onSurfaceVariant = Color(0xFFCBC3D4),
    outline = Color(0xFF958E9E),
    outlineVariant = Color(0xFF4A4453),
    surfaceContainerLowest = Color(0xFF100D13),
    surfaceContainerLow = Color(0xFF1D1A20),
    surfaceContainer = Color(0xFF221E25),
    surfaceContainerHigh = Color(0xFF2C2830),
    surfaceContainerHighest = Color(0xFF37333B),
)

private val base = Typography()

// Heavier weights and tighter tracking everywhere for a bold, clear look.
private val BoldTypography = Typography(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Bold),
    bodyLarge = base.bodyLarge.copy(fontWeight = FontWeight.Medium),
    bodyMedium = base.bodyMedium.copy(fontWeight = FontWeight.Medium),
    bodySmall = base.bodySmall,
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
    labelMedium = base.labelMedium.copy(fontWeight = FontWeight.Bold),
    labelSmall = base.labelSmall.copy(fontWeight = FontWeight.Bold),
)

// Extra-round corners, a hallmark of M3 Expressive.
private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/** Big number style used for hero stats. */
val HeroNumber = TextStyle(fontWeight = FontWeight.Black, fontSize = 56.sp, lineHeight = 60.sp, letterSpacing = (-2).sp)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BookTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialExpressiveTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        motionScheme = MotionScheme.expressive(),
        shapes = ExpressiveShapes,
        typography = BoldTypography,
        content = content,
    )
}
