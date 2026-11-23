package ai.arena.client.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Mirrors platform.arena.ai: warm-neutral charcoal, hairline borders,
 * serif display headings, white primary buttons, green status accent.
 * Flat fills only — no gradients.
 */
object Palette {
    // Dark (neutral, matching the Arena dashboard)
    val DarkBg = Color(0xFF161616)
    val DarkSurface = Color(0xFF1D1D1D)
    val DarkSurfaceLow = Color(0xFF191919)
    val DarkSurfaceHigh = Color(0xFF242424)
    val DarkSurfaceHighest = Color(0xFF2C2C2C)
    val DarkOutline = Color(0xFF2E2E2E)
    val DarkOutlineVariant = Color(0xFF262626)
    val DarkOnSurface = Color(0xFFEDEDED)
    val DarkSecondary = Color(0xFF9B9B9B)
    val DarkTertiary = Color(0xFF737373)

    // Light
    val LightBg = Color(0xFFFAFAFA)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceLow = Color(0xFFFFFFFF)
    val LightSurfaceHigh = Color(0xFFF2F2F2)
    val LightSurfaceHighest = Color(0xFFE9E9E9)
    val LightOutline = Color(0xFFE4E4E4)
    val LightOutlineVariant = Color(0xFFECECEC)
    val LightOnSurface = Color(0xFF161616)
    val LightSecondary = Color(0xFF5C5C5C)
    val LightTertiary = Color(0xFF8A8A8A)

    // Shared accents
    val Green = Color(0xFF4ADE80)
    val Warning = Color(0xFFFBBF24)
    val Error = Color(0xFFF87171)
    val CodeDark = Color(0xFF1B1B1B)
    val CodeLight = Color(0xFFF4F4F4)
}

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF), // white buttons, like "Add funds"
    onPrimary = Color(0xFF111111),
    primaryContainer = Color(0xFF2A2A2A),
    onPrimaryContainer = Color(0xFFEDEDED),
    secondary = Palette.DarkSecondary,
    onSecondary = Color(0xFFFFFFFF),
    background = Palette.DarkBg,
    onBackground = Palette.DarkOnSurface,
    surface = Palette.DarkSurface,
    onSurface = Palette.DarkOnSurface,
    surfaceVariant = Palette.DarkSurfaceHigh,
    onSurfaceVariant = Palette.DarkSecondary,
    surfaceContainerLowest = Palette.DarkSurfaceLow,
    surfaceContainerLow = Palette.DarkSurface,
    surfaceContainer = Color(0xFF1F1F1F),
    surfaceContainerHigh = Palette.DarkSurfaceHigh,
    surfaceContainerHighest = Palette.DarkSurfaceHighest,
    outline = Palette.DarkOutline,
    outlineVariant = Palette.DarkOutlineVariant,
    error = Palette.Error,
    onError = Color(0xFF1A0A0A),
    scrim = Color(0xFF000000),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF161616), // black buttons on light, same as dashboard
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDEDED),
    onPrimaryContainer = Color(0xFF161616),
    secondary = Palette.LightSecondary,
    onSecondary = Color(0xFFFFFFFF),
    background = Palette.LightBg,
    onBackground = Palette.LightOnSurface,
    surface = Palette.LightSurface,
    onSurface = Palette.LightOnSurface,
    surfaceVariant = Palette.LightSurfaceHigh,
    onSurfaceVariant = Palette.LightSecondary,
    surfaceContainerLowest = Palette.LightSurfaceLow,
    surfaceContainerLow = Palette.LightSurface,
    surfaceContainer = Palette.LightSurface,
    surfaceContainerHigh = Palette.LightSurfaceHigh,
    surfaceContainerHighest = Palette.LightSurfaceHighest,
    outline = Palette.LightOutline,
    outlineVariant = Palette.LightOutlineVariant,
    error = Color(0xFFD93025),
    onError = Color(0xFFFFFFFF),
    scrim = Color(0xFF000000),
)

private val ArenaTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 30.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.Serif,
        letterSpacing = (-0.3).sp,
    ),
    headlineSmall = TextStyle(
        fontSize = 22.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.Serif,
    ),
    titleLarge = TextStyle(
        fontSize = 19.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.Serif,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodySmall = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    ),
    labelMedium = TextStyle(
        fontSize = 12.5.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.3.sp,
    ),
)

private val ArenaShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
)

@Composable
fun ArenaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = ArenaTypography,
        shapes = ArenaShapes,
        content = content,
    )
}
