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

// Flat, smooth palette — no gradients anywhere.
object Palette {
    // Dark
    val DarkBg = Color(0xFF0E1013)
    val DarkSurface = Color(0xFF15181D)
    val DarkSurfaceLow = Color(0xFF111419)
    val DarkSurfaceHigh = Color(0xFF1B1F26)
    val DarkSurfaceHighest = Color(0xFF242932)
    val DarkOutline = Color(0xFF272C35)
    val DarkOnSurface = Color(0xFFE9ECF1)
    val DarkSecondary = Color(0xFF9AA3AF)
    val DarkTertiary = Color(0xFF6E7681)

    // Light
    val LightBg = Color(0xFFF6F7F9)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceLow = Color(0xFFFAFAFB)
    val LightSurfaceHigh = Color(0xFFF0F1F4)
    val LightSurfaceHighest = Color(0xFFE7E9EE)
    val LightOutline = Color(0xFFE2E4EA)
    val LightOnSurface = Color(0xFF14161A)
    val LightSecondary = Color(0xFF5C6470)
    val LightTertiary = Color(0xFF8A919C)

    // Shared accents
    val PrimaryDark = Color(0xFF8B95F5)
    val PrimaryLight = Color(0xFF4F5BD5)
    val UserBubbleDark = Color(0xFF232A44)
    val UserBubbleLight = Color(0xFFE7EAFB)
    val Success = Color(0xFF34D399)
    val Warning = Color(0xFFFBBF24)
    val Error = Color(0xFFF87171)
    val ThinkingDark = Color(0xFF13161B)
    val ThinkingLight = Color(0xFFF4F5F8)
    val CodeDark = Color(0xFF101318)
    val CodeLight = Color(0xFFEFF1F5)
}

private val DarkScheme = darkColorScheme(
    primary = Palette.PrimaryDark,
    onPrimary = Color(0xFF0E1013),
    primaryContainer = Palette.UserBubbleDark,
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Palette.DarkSecondary,
    onSecondary = Color(0xFF15181D),
    background = Palette.DarkBg,
    onBackground = Palette.DarkOnSurface,
    surface = Palette.DarkSurface,
    onSurface = Palette.DarkOnSurface,
    surfaceVariant = Palette.DarkSurfaceHigh,
    onSurfaceVariant = Palette.DarkSecondary,
    surfaceContainerLowest = Palette.DarkSurfaceLow,
    surfaceContainerLow = Palette.DarkSurface,
    surfaceContainer = Color(0xFF191D24),
    surfaceContainerHigh = Palette.DarkSurfaceHigh,
    surfaceContainerHighest = Palette.DarkSurfaceHighest,
    outline = Palette.DarkOutline,
    outlineVariant = Color(0xFF1F242B),
    error = Palette.Error,
    onError = Color(0xFF1A0A0A),
    scrim = Color(0xFF000000),
)

private val LightScheme = lightColorScheme(
    primary = Palette.PrimaryLight,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Palette.UserBubbleLight,
    onPrimaryContainer = Color(0xFF202A80),
    secondary = Palette.LightSecondary,
    onSecondary = Color(0xFFFFFFFF),
    background = Palette.LightBg,
    onBackground = Palette.LightOnSurface,
    surface = Palette.LightSurface,
    onSurface = Palette.LightOnSurface,
    surfaceVariant = Palette.LightSurfaceHigh,
    onSurfaceVariant = Palette.LightSecondary,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Palette.LightSurfaceLow,
    surfaceContainer = Palette.LightSurface,
    surfaceContainerHigh = Palette.LightSurfaceHigh,
    surfaceContainerHighest = Palette.LightSurfaceHighest,
    outline = Palette.LightOutline,
    outlineVariant = Palette.LightOutline,
    error = Color(0xFFD93025),
    onError = Color(0xFFFFFFFF),
    scrim = Color(0xFF000000),
)

private val ArenaTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 30.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontSize = 22.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontSize = 19.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
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
