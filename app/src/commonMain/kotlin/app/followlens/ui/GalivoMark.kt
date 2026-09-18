package app.followlens.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Also used by [ScreenHeader] for the glow painted behind the mark. */
val GalivoMarkGold = Color(0xFFFFCB57)

private var _galivoMark: ImageVector? = null

/**
 * The Galivo mark as themed for FollowLens: a thin ring with an inward crossbar, reading as an
 * abstract "G", in a vivid gold brighter than the app's own accent (see [ScreenHeader], which pairs
 * this with a soft glow behind it). Built as vector path data — the same geometry as the canonical
 * Galivo logo (concept 4 in the logo-exploration artifact, which uses a white glow-circle badge
 * instead; that's the actual brand asset, this is an app-specific derivative) — rather than an
 * image asset, so it renders crisply at any size or density with no pixelation, ever.
 */
val GalivoMark: ImageVector
    get() {
        _galivoMark?.let { return it }
        val built = ImageVector.Builder(
            name = "GalivoMark",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 100f,
            viewportHeight = 100f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(GalivoMarkGold),
                strokeLineWidth = 14f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(74.5f, 70.6f)
                arcTo(
                    horizontalEllipseRadius = 32f,
                    verticalEllipseRadius = 32f,
                    theta = 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    x1 = 80.1f,
                    y1 = 39.1f,
                )
            }
            path(
                fill = null,
                stroke = SolidColor(GalivoMarkGold),
                strokeLineWidth = 14f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(81f, 55f)
                lineTo(63f, 55f)
                lineTo(63f, 67f)
            }
        }.build()
        _galivoMark = built
        return built
    }
