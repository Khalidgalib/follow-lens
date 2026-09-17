package app.followlens.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.followlens.diff.SnapshotDelta

/** One row on the History tab: an import's headline counts plus its change since the previous one. */
data class HistoryEntry(
    val takenAtSeconds: Long,
    val followingCount: Int,
    val followersCount: Int,
    val notFollowingBackCount: Int,
    val delta: SnapshotDelta?,
)

@Composable
fun HistoryScreen(entries: List<HistoryEntry>) {
    val listState = rememberLazyListState()
    Row(Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) {
            item { ScreenHeader("History") }

            if (entries.isEmpty()) {
                item {
                    Text(
                        "Import your export to start tracking history.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FollowLensColors.textTertiary,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }

            items(entries.reversed(), key = { it.takenAtSeconds }) { entry ->
                HistoryCard(entry)
                Spacer(Modifier.height(10.dp))
            }
        }
        ScrollPositionIndicator(listState, Modifier.fillMaxHeight().padding(vertical = 8.dp, horizontal = 4.dp))
    }
}

@Composable
private fun HistoryCard(entry: HistoryEntry) {
    Surface(color = FollowLensColors.surfaceRaised, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(formatDate(entry.takenAtSeconds), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                CountLabel("Following", entry.followingCount)
                CountLabel("Followers", entry.followersCount)
                CountLabel("Not back", entry.notFollowingBackCount)
            }
            entry.delta?.let { delta ->
                val changes = buildList {
                    if (delta.newFollowers.isNotEmpty()) add("+${delta.newFollowers.size} followers")
                    if (delta.lostFollowers.isNotEmpty()) add("-${delta.lostFollowers.size} unfollowed you")
                    if (delta.newlyFollowed.isNotEmpty()) add("+${delta.newlyFollowed.size} you followed")
                    if (delta.youUnfollowed.isNotEmpty()) add("-${delta.youUnfollowed.size} you unfollowed")
                }
                if (changes.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        changes.joinToString("  ·  "),
                        style = MaterialTheme.typography.labelSmall,
                        color = FollowLensColors.textTertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CountLabel(label: String, value: Int) {
    Column {
        Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = FollowLensColors.textTertiary)
    }
}
