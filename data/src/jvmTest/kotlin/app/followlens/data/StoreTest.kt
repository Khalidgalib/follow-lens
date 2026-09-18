package app.followlens.data

import app.followlens.diff.Account
import app.followlens.diff.ExportSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Exercises the repository layer against a real (in-memory) SQLite database via the JVM driver.
 * The same code paths run on Android/iOS at runtime.
 */
class StoreTest {

    private fun freshDb() = openDatabase(DatabaseDriverFactory())

    private fun snapshot(followers: List<String>, following: List<String>) = ExportSnapshot(
        followers = followers.mapTo(LinkedHashSet()) { Account(it, 1_700_000_000) },
        following = following.mapTo(LinkedHashSet()) { Account(it, 1_700_000_001) },
        exportedAtSeconds = 1_699_999_999,
    )

    @Test
    fun snapshot_roundTrips() {
        val db = freshDb()
        val store = SnapshotStore(db)
        assertNull(store.latest())

        val id = store.save(snapshot(followers = listOf("alice", "dave"), following = listOf("alice", "bob")), takenAtSeconds = 42)

        val loaded = store.latest()!!
        assertEquals(id, loaded.id)
        assertEquals(42, loaded.takenAtSeconds)
        assertEquals(1_699_999_999, loaded.snapshot.exportedAtSeconds)
        assertEquals(setOf("alice", "dave"), loaded.snapshot.followers.map { it.username }.toSet())
        assertEquals(setOf("alice", "bob"), loaded.snapshot.following.map { it.username }.toSet())
    }

    @Test
    fun latest_returnsMostRecentByTakenAt() {
        val s = SnapshotStore(freshDb())
        s.save(snapshot(listOf("a"), listOf("a")), takenAtSeconds = 100)
        s.save(snapshot(listOf("b"), listOf("b")), takenAtSeconds = 200)
        assertEquals(setOf("b"), s.latest()!!.snapshot.followers.map { it.username }.toSet())
    }

    @Test
    fun history_returnsAllSnapshotsOldestFirst() {
        val db = freshDb()
        val s = SnapshotStore(db)
        s.save(snapshot(listOf("a"), listOf("a")), takenAtSeconds = 200)
        s.save(snapshot(listOf("b"), listOf("b")), takenAtSeconds = 100)

        val history = s.history()
        assertEquals(listOf(100L, 200L), history.map { it.takenAtSeconds })

        resetAllData(db)
        assertEquals(emptyList(), s.history())
    }

    @Test
    fun whitelist_addAndRemove() {
        val db = freshDb()
        val wl = WhitelistStore(db)
        assertEquals(emptySet(), wl.all())

        wl.add("Bob", addedAtSeconds = 1)
        wl.add("carol", addedAtSeconds = 2)
        assertEquals(setOf("bob", "carol"), wl.all())

        wl.remove("bob")
        assertEquals(setOf("carol"), wl.all())
    }

    @Test
    fun dismissed_addAndRemove() {
        val db = freshDb()
        val dismissed = DismissedStore(db)
        assertEquals(emptySet(), dismissed.all())

        dismissed.add("Bob", dismissedAtSeconds = 1)
        dismissed.add("carol", dismissedAtSeconds = 2)
        assertEquals(setOf("bob", "carol"), dismissed.all())

        dismissed.remove("bob")
        assertEquals(setOf("carol"), dismissed.all())
    }

    @Test
    fun dismissed_isIndependentFromWhitelist() {
        val db = freshDb()
        val wl = WhitelistStore(db)
        val dismissed = DismissedStore(db)

        wl.add("alice", addedAtSeconds = 1)
        dismissed.add("alice", dismissedAtSeconds = 1)
        wl.remove("alice")

        assertEquals(emptySet(), wl.all())
        assertEquals(setOf("alice"), dismissed.all())
    }
}
