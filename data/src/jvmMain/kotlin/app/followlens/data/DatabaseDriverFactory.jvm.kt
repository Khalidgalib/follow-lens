package app.followlens.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.followlens.data.db.FollowLensDb

/**
 * JVM driver. Used by unit tests today (in-memory) and, later, by the Spring Boot backend
 * (which will pass a file-backed JDBC URL).
 */
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { FollowLensDb.Schema.create(it) }
}
