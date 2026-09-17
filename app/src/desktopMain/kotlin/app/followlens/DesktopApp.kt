package app.followlens

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import app.followlens.data.DatabaseDriverFactory
import app.followlens.ui.App

/**
 * Desktop entry point — a dev harness so the shared [App] composable can be run on this machine
 * without an emulator. Not a shipping target (v1 is iOS + Android). The JVM SQLite driver is
 * in-memory, so snapshots don't persist between runs here.
 */
fun main() {
    println("[FollowLens] main() start; headless=" + System.getProperty("java.awt.headless"))
    try {
        application {
            Window(
                onCloseRequest = ::exitApplication,
                title = "FollowLens (desktop dev harness)",
                state = WindowState(size = DpSize(420.dp, 860.dp)),
            ) {
                App(DatabaseDriverFactory())
            }
        }
        println("[FollowLens] application{} returned normally")
    } catch (t: Throwable) {
        println("[FollowLens] application{} threw:")
        t.printStackTrace()
        throw t
    }
}
