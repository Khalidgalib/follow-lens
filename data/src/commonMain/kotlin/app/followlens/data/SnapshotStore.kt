package app.followlens.data

import app.followlens.data.db.FollowLensDb
import app.followlens.diff.Account
import app.followlens.diff.ExportSnapshot

/** One stored import, plus when it was taken. */
data class StoredSnapshot(
    val id: Long,
    val takenAtSeconds: Long,
    val snapshot: ExportSnapshot,
)

/**
 * Persists parsed [ExportSnapshot]s (the snapshot-history feature) and reads them back.
 * Pure Kotlin on top of the generated SQLDelight queries — reusable by the future backend.
 */
class SnapshotStore(private val db: FollowLensDb) {

    private val q get() = db.followLensQueries

    /** Store one import. Returns the new snapshot id. */
    fun save(snapshot: ExportSnapshot, takenAtSeconds: Long): Long = db.transactionWithResult {
        q.insertSnapshot(takenAtSeconds, snapshot.exportedAtSeconds)
        val snapshotId = q.lastInsertedId().executeAsOne()
        snapshot.followers.forEach { link(snapshotId, it, ROLE_FOLLOWER) }
        snapshot.following.forEach { link(snapshotId, it, ROLE_FOLLOWING) }
        snapshotId
    }

    /** The most recent import, or null if the user hasn't imported anything yet. */
    fun latest(): StoredSnapshot? {
        val row = q.latestSnapshot().executeAsOneOrNull() ?: return null
        return storedSnapshot(row.id, row.taken_at_seconds, row.exported_at_seconds)
    }

    /** Every import, oldest first — powers the dashboard trend and the history screen. */
    fun history(): List<StoredSnapshot> = q.allSnapshots().executeAsList().map { row ->
        storedSnapshot(row.id, row.taken_at_seconds, row.exported_at_seconds)
    }

    private fun storedSnapshot(id: Long, takenAtSeconds: Long, exportedAtSeconds: Long?) = StoredSnapshot(
        id = id,
        takenAtSeconds = takenAtSeconds,
        snapshot = ExportSnapshot(
            followers = accounts(id, ROLE_FOLLOWER),
            following = accounts(id, ROLE_FOLLOWING),
            exportedAtSeconds = exportedAtSeconds,
        ),
    )

    private fun accounts(snapshotId: Long, role: String): Set<Account> =
        q.accountsForSnapshot(snapshotId, role).executeAsList()
            .mapTo(LinkedHashSet()) { Account(it.username, it.since_seconds) }

    private fun link(snapshotId: Long, account: Account, role: String) {
        q.upsertAccount(account.username)
        val accountId = q.accountIdByUsername(account.username).executeAsOne()
        q.linkAccountToSnapshot(snapshotId, accountId, role, account.timestampSeconds)
    }
}
