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

/** Copy — two overlapping rounded sheets with a clear offset. */
val CopyGlyph: ImageVector by lazy {
    ImageVector.Builder(
        name = "CopyGlyph",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // Back sheet (peeks top-right)
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 3f)
            lineTo(18f, 3f)
            quadTo(20f, 3f, 20f, 5f)
            lineTo(20f, 13f)
            quadTo(20f, 15f, 18f, 15f)
            lineTo(12f, 15f)
            quadTo(10f, 15f, 10f, 13f)
            lineTo(10f, 5f)
            quadTo(10f, 3f, 12f, 3f)
            close()
        }
        // Front sheet
        path(fill = SolidColor(Color.Black)) {
            moveTo(6f, 8.5f)
            lineTo(13f, 8.5f)
            quadTo(15f, 8.5f, 15f, 10.5f)
            lineTo(15f, 19f)
            quadTo(15f, 21f, 13f, 21f)
            lineTo(6f, 21f)
            quadTo(4f, 21f, 4f, 19f)
            lineTo(4f, 10.5f)
            quadTo(4f, 8.5f, 6f, 8.5f)
            close()
        }
    }.build()
}

/** Attach — a document with a folded corner (file attachment). */
val AttachGlyph: ImageVector by lazy {
    ImageVector.Builder(
        name = "AttachGlyph",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(6f, 3f)
            quadTo(4.9f, 3f, 4.9f, 4.1f)
            lineTo(4.9f, 19.9f)
            quadTo(4.9f, 21f, 6f, 21f)
            lineTo(18f, 21f)
            quadTo(19.1f, 21f, 19.1f, 19.9f)
            lineTo(19.1f, 8.6f)
            quadTo(19.1f, 8.1f, 18.8f, 7.7f)
            lineTo(14.4f, 3.5f)
            quadTo(14f, 3.1f, 13.5f, 3.1f)
            lineTo(6f, 3f)
            close()
            moveTo(13.6f, 5.2f)
            lineTo(16.9f, 8.4f)
            lineTo(14.4f, 8.4f)
            quadTo(13.9f, 8.4f, 13.9f, 7.9f)
            lineTo(13.9f, 5.2f)
            close()
        }
    }.build()
}
