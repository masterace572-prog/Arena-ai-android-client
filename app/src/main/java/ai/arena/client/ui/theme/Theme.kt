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
 * Claude-inspired: warm paper light / warm charcoal dark, terracotta accent,
 * serif display type, flat fills only — no gradients.
 */
object Palette {
    // Warm light (paper)
    val Paper = Color(0xFFFAF9F6)
    val CardLight = Color(0xFFFFFFFF)
    val CardHighLight = Color(0xFFF2F0EA)
    val CardHighestLight = Color(0xFFE9E6DE)
    val InkLight = Color(0xFF1F1E1C)
    val InkSoftLight = Color(0xFF75736C)
    val InkFaintLight = Color(0xFF9C998F)
    val LineLight = Color(0xFFE6E3DB)

    // Warm charcoal (dark)
    val Charcoal = Color(0xFF1C1B19)
    val CardDark = Color(0xFF262523)
    val CardHighDark = Color(0xFF32302D)
    val CardHighestDark = Color(0xFF3B3936)
    val InkDark = Color(0xFFF2F0EA)
    val InkSoftDark = Color(0xFFA5A199)
    val InkFaintDark = Color(0xFF7D7A72)
    val LineDark = Color(0xFF3B3936)

    // Terracotta accent (Claude-like)
    val Terracotta = Color(0xFFC96442)
    val TerracottaDark = Color(0xFFD97757)
    val TerracottaSoft = Color(0xFFF4E4DC)

    // Status
    val Green = Color(0xFF4ADE80)
    val Success = Green
    val Warning = Color(0xFFFBBF24)
    val Error = Color(0xFFF87171)

    // Message surfaces
    val UserBubbleLight = Color(0xFFEFEEE8)
    val UserBubbleDark = Color(0xFF32302D)
    val ThinkingLight = Color(0xFFF6F4EE)
    val ThinkingDark = Color(0xFF242321)
    val CodeLight = Color(0xFFF4F2EC)
    val CodeDark = Color(0xFF242321)
}

private val DarkScheme = darkColorScheme(
    primary = Palette.TerracottaDark,
    onPrimary = Color(0xFF1C1B19),
    primaryContainer = Palette.UserBubbleDark,
    onPrimaryContainer = Palette.InkDark,
    secondary = Palette.InkSoftDark,
    onSecondary = Color(0xFF1C1B19),
    background = Palette.Charcoal,
    onBackground = Palette.InkDark,
    surface = Palette.CardDark,
    onSurface = Palette.InkDark,
    surfaceVariant = Palette.CardHighDark,
    onSurfaceVariant = Palette.InkSoftDark,
    surfaceContainerLowest = Color(0xFF181715),
    surfaceContainerLow = Palette.Charcoal,
    surfaceContainer = Palette.CardDark,
    surfaceContainerHigh = Palette.CardHighDark,
    surfaceContainerHighest = Palette.CardHighestDark,
    outline = Palette.LineDark,
    outlineVariant = Palette.LineDark,
    error = Palette.Error,
    onError = Color(0xFF1A0A0A),
    scrim = Color(0xFF000000),
)

private val LightScheme = lightColorScheme(
    primary = Palette.Terracotta,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Palette.TerracottaSoft,
    onPrimaryContainer = Color(0xFF6B2F1B),
    secondary = Palette.InkSoftLight,
    onSecondary = Color(0xFFFFFFFF),
    background = Palette.Paper,
    onBackground = Palette.InkLight,
    surface = Palette.CardLight,
    onSurface = Palette.InkLight,
    surfaceVariant = Palette.CardHighLight,
    onSurfaceVariant = Palette.InkSoftLight,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Palette.Paper,
    surfaceContainer = Palette.CardLight,
    surfaceContainerHigh = Palette.CardHighLight,
    surfaceContainerHighest = Palette.CardHighestLight,
    outline = Palette.LineLight,
    outlineVariant = Palette.LineLight,
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
        lineHeight = 25.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontSize = 15.sp,
        lineHeight = 23.sp,
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
