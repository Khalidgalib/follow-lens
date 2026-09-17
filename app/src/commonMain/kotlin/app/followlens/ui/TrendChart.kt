package app.followlens.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/** One point on the dashboard trend line: an import's following/followers counts. */
data class TrendPoint(val following: Int, val followers: Int)

/**
 * A minimal two-series sparkline (following vs. followers across imports). Deliberately not a
 * charting library dependency — this is the whole feature, drawn once on a [Canvas].
 */
@Composable
fun TrendChart(points: List<TrendPoint>, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(96.dp)) {
        if (points.size < 2) return@Canvas

        val maxValue = points.flatMap { listOf(it.following, it.followers) }.max().coerceAtLeast(1)
        val minValue = points.flatMap { listOf(it.following, it.followers) }.min()
        val range = (maxValue - minValue).coerceAtLeast(1)

        val stepX = size.width / (points.size - 1)
        fun yOf(value: Int): Float {
            val fraction = (value - minValue).toFloat() / range
            return size.height - fraction * size.height
        }

        fun path(selector: (TrendPoint) -> Int): List<Offset> =
            points.mapIndexed { i, p -> Offset(i * stepX, yOf(selector(p))) }

        fun drawSeries(offsets: List<Offset>, color: Color) {
            for (i in 0 until offsets.size - 1) {
                drawLine(color, offsets[i], offsets[i + 1], strokeWidth = 3f, cap = StrokeCap.Round)
            }
            offsets.lastOrNull()?.let { drawCircle(color, radius = 4f, center = it) }
        }

        drawSeries(path { it.following }, FollowLensColors.accent)
        drawSeries(path { it.followers }, FollowLensColors.seriesFollowers)
    }
}
