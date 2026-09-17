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
import androidx.compose.foundation.lazy.LazyListScope
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

@Composable
fun Centered(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}
