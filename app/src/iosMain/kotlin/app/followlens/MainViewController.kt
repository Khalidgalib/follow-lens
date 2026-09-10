package app.followlens

import androidx.compose.ui.window.ComposeUIViewController
import app.followlens.data.DatabaseDriverFactory
import app.followlens.ui.App

/** Entry point called from the iOS Swift app (`ContentView` / `UIViewControllerRepresentable`). */
@Suppress("unused", "FunctionName")
fun MainViewController() = ComposeUIViewController {
    App(DatabaseDriverFactory())
}
