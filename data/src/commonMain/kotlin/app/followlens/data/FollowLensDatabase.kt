package app.followlens.data

import app.followlens.data.db.FollowLensDb

/**
 * Opens the on-device SQLite database. Each platform supplies its own [DatabaseDriverFactory]
 * (Android needs a `Context`, iOS/JVM don't); everything above this line is shared.
 */
fun openDatabase(driverFactory: DatabaseDriverFactory): FollowLensDb =
    FollowLensDb(driverFactory.createDriver())

/** Wipes every snapshot, account, and whitelist entry. Used by the Settings "reset" action. */
fun resetAllData(db: FollowLensDb) = db.followLensQueries.clearAllData()

/** Role tags stored in `snapshot_account.role`. */
internal const val ROLE_FOLLOWER = "FOLLOWER"
internal const val ROLE_FOLLOWING = "FOLLOWING"
