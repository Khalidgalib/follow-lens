package app.followlens.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** The Dashboard tab: headline counts, an at-a-glance trend, and the last-import shortcut. */
@Composable
fun DashboardScreen(
    followingCount: Int,
    followersCount: Int,
    notFollowingBackCount: Int,
    trend: List<TrendPoint>,
    trendRangeLabel: String?,
    lastImportAtSeconds: Long?,
    onReImport: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        ScreenHeader("Dashboard")
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("FOLLOWING", followingCount, Modifier.weight(1f))
            StatTile("FOLLOWERS", followersCount, Modifier.weight(1f))
            StatTile("NOT BACK", notFollowingBackCount, Modifier.weight(1f), highlight = true)
        }

        Spacer(Modifier.height(14.dp))

        Surface(color = FollowLensColors.surfaceRaised, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "${trend.size} import${if (trend.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (trendRangeLabel != null) {
                        Text(trendRangeLabel, style = MaterialTheme.typography.labelSmall, color = FollowLensColors.textTertiary)
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (trend.size < 2) {
                    Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Import again later to see your trend.",
                            style = MaterialTheme.typography.bodySmall,
                            color = FollowLensColors.textTertiary,
                        )
                    }
                } else {
                    TrendChart(trend)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendDot(FollowLensColors.accent, "Following")
                    LegendDot(FollowLensColors.seriesFollowers, "Followers")
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Surface(color = FollowLensColors.surfaceRaised, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Last import", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        lastImportAtSeconds?.let { formatDate(it) } ?: "Never",
                        style = MaterialTheme.typography.labelSmall,
                        color = FollowLensColors.textTertiary,
                    )
                }
                Button(onClick = onReImport) { Text("Import new export") }
            }
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = FollowLensColors.textSecondary)
    }
}
