package ai.arena.client.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Minimal modern glyphs (Claude-style), hand-drawn as vectors so the APK
 * stays lean — no icon-library dependency.
 */

/** Four-point sparkle — the AI/reasoning mark. */
val Sparkle: ImageVector by lazy {
    ImageVector.Builder(
        name = "Sparkle",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 2f)
            quadTo(13.2f, 10.8f, 22f, 12f)
            quadTo(13.2f, 13.2f, 12f, 22f)
            quadTo(10.8f, 13.2f, 2f, 12f)
            quadTo(10.8f, 10.8f, 12f, 2f)
            close()
        }
    }.build()
}

/** Rounded square — the stop control while streaming. */
val StopSquare: ImageVector by lazy {
    ImageVector.Builder(
        name = "StopSquare",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(8f, 6f)
            lineTo(16f, 6f)
            quadTo(18f, 6f, 18f, 8f)
            lineTo(18f, 16f)
            quadTo(18f, 18f, 16f, 18f)
            lineTo(8f, 18f)
            quadTo(6f, 18f, 6f, 16f)
            lineTo(6f, 8f)
            quadTo(6f, 6f, 8f, 6f)
            close()
        }
    }.build()
}
