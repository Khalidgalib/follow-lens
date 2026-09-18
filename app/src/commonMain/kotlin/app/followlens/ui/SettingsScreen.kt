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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    whitelist: List<String>,
    onRemoveFromWhitelist: (String) -> Unit,
    handled: List<String>,
    onUnmarkHandled: (String) -> Unit,
    onClearAllData: () -> Unit,
) {
    var confirmingReset by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    Row(Modifier.fillMaxSize()) {
    LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) {
        item { ScreenHeader("Settings") }

        sectionHeader("WHITELIST · ${whitelist.size}")
        if (whitelist.isEmpty()) {
            item {
                Text(
                    "Accounts you whitelist stop being flagged as non-followers or fans.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FollowLensColors.textTertiary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
        items(whitelist, key = { it }) { username ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("@$username", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { onRemoveFromWhitelist(username) }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Remove from whitelist", tint = FollowLensColors.textSecondary)
                }
            }
        }

        sectionHeader("HANDLED · ${handled.size}")
        if (handled.isEmpty()) {
            item {
                Text(
                    "Accounts you mark \"handled\" on the Not Back list stay hidden until you undo it here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FollowLensColors.textTertiary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
        items(handled, key = { it }) { username ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("@$username", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { onUnmarkHandled(username) }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Unmark handled", tint = FollowLensColors.textSecondary)
                }
            }
        }

        sectionHeader("DATA")
        item {
            Surface(color = FollowLensColors.surfaceRaised, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "FollowLens is fully offline — nothing you import ever leaves this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FollowLensColors.textSecondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { confirmingReset = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FollowLensColors.critical),
                    ) { Text("Clear all data") }
                }
            }
        }

        sectionHeader("ABOUT")
        item {
            Text(
                "FollowLens v0.1.0",
                style = MaterialTheme.typography.bodySmall,
                color = FollowLensColors.textTertiary,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
    ScrollPositionIndicator(listState, Modifier.fillMaxHeight().padding(vertical = 8.dp, horizontal = 4.dp))
    }

    if (confirmingReset) {
        AlertDialog(
            onDismissRequest = { confirmingReset = false },
            title = { Text("Clear all data?") },
            text = { Text("This deletes every imported snapshot, your whitelist, and your handled list. It can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmingReset = false; onClearAllData() }) {
                    Text("Clear", color = FollowLensColors.critical, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingReset = false }) { Text("Cancel") }
            },
        )
    }
}
