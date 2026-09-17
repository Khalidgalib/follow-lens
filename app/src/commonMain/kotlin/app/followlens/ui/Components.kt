package app.followlens.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.followlens.diff.Account

/** Small brand mark + screen title, echoed at the top of every tab (matches the app icon). */
@Composable
fun ScreenHeader(title: String, modifier: Modifier = Modifier) {
    Row(modifier.padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = FollowLensColors.accentStrong, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = FollowLensColors.accentStrong, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatTile(label: String, value: Int, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Surface(
        modifier = modifier,
        color = FollowLensColors.surfaceRaised,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 11.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = FollowLensColors.textTertiary,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                value.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (highlight) FollowLensColors.accentStrong else FollowLensColors.textPrimary,
            )
        }
    }
}

@Composable
fun AccountAvatar(username: String, modifier: Modifier = Modifier) {
    Box(
        modifier.size(36.dp).clip(CircleShape).background(FollowLensColors.surfaceSunken),
        contentAlignment = Alignment.Center,
    ) {
        Text(username.take(1).uppercase(), style = MaterialTheme.typography.titleSmall, color = FollowLensColors.textSecondary)
    }
}

@Composable
fun AccountRow(account: Account, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        AccountAvatar(account.username)
        Column(Modifier.weight(1f)) {
            Text("@${account.username}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            sinceLabel(account.timestampSeconds)?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = FollowLensColors.textTertiary)
            }
        }
        trailing()
    }
}

fun LazyListScope.sectionHeader(text: String) = item {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.8.sp),
        color = FollowLensColors.textTertiary,
        modifier = Modifier.padding(top = 22.dp, bottom = 6.dp),
    )
}

/** Thin trailing-edge thumb showing where a [LazyColumn][androidx.compose.foundation.lazy.LazyColumn] is scrolled to. Hidden when the whole list already fits. */
@Composable
fun ScrollPositionIndicator(listState: LazyListState, modifier: Modifier = Modifier) {
    val info = listState.layoutInfo
    val totalItems = info.totalItemsCount
    val visibleItems = info.visibleItemsInfo.size
    if (totalItems == 0 || visibleItems >= totalItems) return

    val thumbFraction = (visibleItems.toFloat() / totalItems).coerceIn(0.08f, 1f)
    val scrollableItems = (totalItems - visibleItems).coerceAtLeast(1)
    val firstIndex = info.visibleItemsInfo.firstOrNull()?.index ?: 0
    val progress = (firstIndex.toFloat() / scrollableItems).coerceIn(0f, 1f)
    ScrollTrack(progress = progress, thumbFraction = thumbFraction, modifier = modifier)
}

/** Same idea as the [LazyListState] overload, for a plain [ScrollState]-based scrolling [Column]. */
@Composable
fun ScrollPositionIndicator(scrollState: ScrollState, modifier: Modifier = Modifier) {
    val max = scrollState.maxValue
    if (max <= 0) return

    val viewport = scrollState.viewportSize
    val contentSize = viewport + max
    if (contentSize <= 0) return

    val thumbFraction = (viewport.toFloat() / contentSize).coerceIn(0.08f, 1f)
    if (thumbFraction >= 1f) return
    val progress = (scrollState.value.toFloat() / max).coerceIn(0f, 1f)
    ScrollTrack(progress = progress, thumbFraction = thumbFraction, modifier = modifier)
}

@Composable
private fun ScrollTrack(progress: Float, thumbFraction: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.width(4.dp).fillMaxHeight()) {
        val corner = CornerRadius(size.width / 2f)
        drawRoundRect(color = FollowLensColors.outline, cornerRadius = corner)
        val thumbHeight = size.height * thumbFraction
        val thumbTop = (size.height - thumbHeight) * progress
        drawRoundRect(
            color = FollowLensColors.accent,
            topLeft = Offset(0f, thumbTop),
            size = Size(size.width, thumbHeight),
            cornerRadius = corner,
        )
    }
}

@Composable
fun Centered(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}
