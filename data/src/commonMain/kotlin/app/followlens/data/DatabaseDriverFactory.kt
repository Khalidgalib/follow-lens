package app.followlens.data

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform-specific creation of the SQLite driver. `actual` implementations live in
 * androidMain / iosMain / jvmMain.
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
