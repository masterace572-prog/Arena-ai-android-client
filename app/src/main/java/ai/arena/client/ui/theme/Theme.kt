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
 * Premium dark-first system: charcoal / dark graphite backgrounds,
 * soft coral accent, subtle borders, flat fills only — no gradients.
 * A warm light theme is derived from the same accent for day mode.
 */
object Palette {
    // ---- Dark graphite (primary) ----
    val BgDark = Color(0xFF191817)          // main background
    val Bg2Dark = Color(0xFF242220)         // secondary background
    val SurfaceDark = Color(0xFF2D2B28)     // surface / cards
    val SurfaceHighDark = Color(0xFF35322E) // raised (stop button, code header)
    val InkDark = Color(0xFFF4F1ED)         // primary text
    val InkSoftDark = Color(0xFFA7A29C)     // secondary text
    val InkFaintDark = Color(0xFF7D7A72)    // faint text
    val LineDark = Color(0xFF3B3834)        // borders

    // ---- Warm light (day mode) ----
    val Paper = Color(0xFFFAF9F6)
    val CardLight = Color(0xFFFFFFFF)
    val CardHighLight = Color(0xFFF2F0EA)
    val CardHighestLight = Color(0xFFE9E6DE)
    val InkLight = Color(0xFF1F1E1C)
    val InkSoftLight = Color(0xFF75736C)
    val InkFaintLight = Color(0xFF9C998F)
    val LineLight = Color(0xFFE6E3DB)

    // ---- Soft coral accent ----
    val Coral = Color(0xFFE17B5D)           // accent on dark
    val CoralDeep = Color(0xFFC96442)       // accent on light (contrast)
    val CoralSoft = Color(0xFFF4E4DC)       // tinted container (light)

    // ---- Status ----
    val Green = Color(0xFF4ADE80)
    val Success = Green
    val Warning = Color(0xFFFBBF24)
    val ErrorLight = Color(0xFFD93025)
    val ErrorDark = Color(0xFFF87171)

    // ---- Message surfaces ----
    val UserBubbleLight = Color(0xFFEFEEE8)
    val UserBubbleDark = Color(0xFF2D2B28)
    val ThinkingLight = Color(0xFFF6F4EE)
    val ThinkingDark = Color(0xFF242220)
    val CodeLight = Color(0xFFF4F2EC)
    val CodeDark = Color(0xFF242220)

    // ---- Aliases kept for existing call sites ----
    val Terracotta: Color get() = CoralDeep
    val TerracottaDark: Color get() = Coral
    val TerracottaSoft: Color get() = CoralSoft
    val Charcoal: Color get() = BgDark
    val CardDark: Color get() = SurfaceDark
    val CardHighDark: Color get() = Bg2Dark
    val CardHighestDark: Color get() = SurfaceHighDark
    val Error: Color get() = ErrorDark
}

private val DarkScheme = darkColorScheme(
    primary = Palette.Coral,
    onPrimary = Palette.BgDark,
    primaryContainer = Palette.UserBubbleDark,
    onPrimaryContainer = Palette.InkDark,
    secondary = Palette.InkSoftDark,
    onSecondary = Palette.BgDark,
    background = Palette.BgDark,
    onBackground = Palette.InkDark,
    surface = Palette.SurfaceDark,
    onSurface = Palette.InkDark,
    surfaceVariant = Palette.Bg2Dark,
    onSurfaceVariant = Palette.InkSoftDark,
    surfaceContainerLowest = Color(0xFF141312),
    surfaceContainerLow = Palette.BgDark,
    surfaceContainer = Palette.Bg2Dark,
    surfaceContainerHigh = Palette.Bg2Dark,
    surfaceContainerHighest = Palette.SurfaceHighDark,
    outline = Palette.LineDark,
    outlineVariant = Palette.LineDark,
    error = Palette.ErrorDark,
    onError = Color(0xFF1A0A0A),
    scrim = Color(0xFF000000),
)

private val LightScheme = lightColorScheme(
    primary = Palette.CoralDeep,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Palette.CoralSoft,
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
    error = Palette.ErrorLight,
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
