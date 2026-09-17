package app.followlens.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The five v1 sections, reachable once there's at least one import to show. */
enum class Screen(val label: String, val icon: ImageVector) {
    DASHBOARD("DASH", Icons.Outlined.RadioButtonUnchecked),
    NOT_FOLLOWING_BACK("LIST", Icons.AutoMirrored.Outlined.List),
    FANS("FANS", Icons.Outlined.FavoriteBorder),
    HISTORY("HIST", Icons.Outlined.History),
    SETTINGS("SET", Icons.Outlined.Settings),
}

@Composable
fun BottomNavBar(current: Screen, onSelect: (Screen) -> Unit) {
    Surface(color = FollowLensColors.surface) {
        Row(Modifier.fillMaxWidth().height(64.dp)) {
            Screen.entries.forEach { screen ->
                val selected = screen == current
                val tint = if (selected) FollowLensColors.accentStrong else FollowLensColors.textTertiary
                Column(
                    Modifier.weight(1f)
                        .selectable(selected = selected, onClick = { onSelect(screen) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(screen.icon, contentDescription = screen.label, tint = tint, modifier = Modifier.padding(bottom = 2.dp))
                    Text(
                        screen.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.6.sp),
                        color = tint,
                    )
                }
            }
        }
    }
}
