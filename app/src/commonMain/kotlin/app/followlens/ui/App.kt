package app.followlens.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * Placeholder root composable. Real screens (onboarding, import, dashboard, non-followers, fans,
 * history, settings) come in build-order step 4 — see docs/PROJECT_PLAN.md.
 */
@Composable
fun App() {
    MaterialTheme {
        Surface {
            Text("FollowLens — scaffold. See docs/PROJECT_PLAN.md for the build order.")
        }
    }
}
