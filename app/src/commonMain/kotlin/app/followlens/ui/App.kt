package app.followlens.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.followlens.data.DatabaseDriverFactory
import app.followlens.data.SnapshotStore
import app.followlens.data.StoredSnapshot
import app.followlens.data.WhitelistStore
import app.followlens.data.openDatabase
import app.followlens.data.resetAllData
import app.followlens.diff.ExportParser
import app.followlens.diff.ExportSnapshot
import app.followlens.diff.FollowDiff
import app.followlens.diff.FollowDiffResult
import kotlinx.datetime.Clock

/**
 * FollowLens v1 UI. Everything is offline: the user pastes the two JSON files from their
 * Instagram export, we run [FollowDiff] and show who doesn't follow them back. Each import is
 * saved as a snapshot so the Dashboard trend and History tab work across imports.
 */
@Composable
fun App(driverFactory: DatabaseDriverFactory) {
    val repo = remember { FollowLensRepo(driverFactory) }

    var loaded by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<FollowDiffResult?>(null) }
    var showUploadPanel by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var screen by remember { mutableStateOf(Screen.DASHBOARD) }
    var version by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        repo.loadLatest()
        result = repo.diff()
        loaded = true
    }

    val history = remember(version) { repo.history() }
    val whitelist = remember(version) { repo.whitelistUsernames() }

    fun refresh() {
        result = repo.diff()
        version++
    }

    fun submitImport(following: String, followers: String) {
        try {
            repo.import(following, followers)
            showUploadPanel = false
            error = null
            refresh()
        } catch (e: ExportParser.ParseException) {
            error = e.message ?: "That doesn't look like a valid Instagram export."
        }
    }

    MaterialTheme(colorScheme = FollowLensDarkColorScheme, shapes = FollowLensShapes) {
        CompositionLocalProvider(LocalIndication provides ripple(color = FollowLensColors.accentStrong)) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                    if (!loaded) {
                        Centered { CircularProgressIndicator() }
                    } else {
                        // Dashboard is always the home screen — with no data yet it just shows
                        // zeros plus the upload panel instead of gating the whole app behind it.
                        Column(Modifier.fillMaxSize()) {
                            Column(Modifier.weight(1f)) {
                                when (screen) {
                                    Screen.DASHBOARD -> DashboardScreen(
                                        hasData = result != null,
                                        followingCount = (result?.mutuals?.size ?: 0) + (result?.notFollowingBack?.size ?: 0),
                                        followersCount = (result?.mutuals?.size ?: 0) + (result?.fans?.size ?: 0),
                                        notFollowingBackCount = result?.notFollowingBack?.size ?: 0,
                                        trend = history.map { TrendPoint(it.followingCount, it.followersCount) },
                                        trendRangeLabel = history.takeIf { it.size >= 2 }
                                            ?.let { formatMonthRange(it.first().takenAtSeconds, it.last().takenAtSeconds) },
                                        lastImportAtSeconds = history.lastOrNull()?.takenAtSeconds,
                                        showUploadPanel = showUploadPanel,
                                        onShowUploadPanel = { showUploadPanel = true },
                                        onHideUploadPanel = { showUploadPanel = false; error = null },
                                        importError = error,
                                        onSubmitImport = ::submitImport,
                                    )

                                    Screen.NOT_FOLLOWING_BACK -> AccountListScreen(
                                        title = "Not following back",
                                        accounts = result?.notFollowingBack ?: emptyList(),
                                        onWhitelist = { username -> repo.addToWhitelist(username); refresh() },
                                    )

                                    Screen.FANS -> AccountListScreen(
                                        title = "Fans",
                                        accounts = result?.fans ?: emptyList(),
                                        onWhitelist = { username -> repo.addToWhitelist(username); refresh() },
                                    )

                                    Screen.HISTORY -> HistoryScreen(entries = history)

                                    Screen.SETTINGS -> SettingsScreen(
                                        whitelist = whitelist,
                                        onRemoveFromWhitelist = { username -> repo.removeFromWhitelist(username); refresh() },
                                        onClearAllData = {
                                            repo.clearAllData()
                                            result = null
                                            showUploadPanel = false
                                            screen = Screen.DASHBOARD
                                            version++
                                        },
                                    )
                                }
                            }
                            BottomNavBar(current = screen, onSelect = { screen = it })
                        }
                    }
                }
            }
        }
    }
}

// --- glue: repository over the :data + :core-diff modules --------------------------------------

private class FollowLensRepo(driverFactory: DatabaseDriverFactory) {
    private val db = openDatabase(driverFactory)
    private val snapshots = SnapshotStore(db)
    private val whitelist = WhitelistStore(db)
    private val parser = ExportParser()

    private var current: ExportSnapshot? = null

    fun loadLatest() {
        current = snapshots.latest()?.snapshot
    }

    /** @throws ExportParser.ParseException on bad input. */
    fun import(followingJson: String, followersJson: String) {
        val snapshot = parser.parseSnapshot(followingJson, followersJson)
        snapshots.save(snapshot, Clock.System.now().epochSeconds)
        current = snapshot
    }

    fun addToWhitelist(username: String) {
        whitelist.add(username, Clock.System.now().epochSeconds)
    }

    fun removeFromWhitelist(username: String) {
        whitelist.remove(username)
    }

    fun whitelistUsernames(): List<String> = whitelist.all().sorted()

    fun clearAllData() {
        resetAllData(db)
        current = null
    }

    fun diff(): FollowDiffResult? = current?.let { FollowDiff.compare(it, whitelist.all()) }

    /** Oldest-first, one entry per import, each carrying its change since the previous one. */
    fun history(): List<HistoryEntry> {
        val stored: List<StoredSnapshot> = snapshots.history()
        val wl = whitelist.all()
        return stored.mapIndexed { i, s ->
            HistoryEntry(
                takenAtSeconds = s.takenAtSeconds,
                followingCount = s.snapshot.following.size,
                followersCount = s.snapshot.followers.size,
                notFollowingBackCount = FollowDiff.compare(s.snapshot, wl).notFollowingBack.size,
                delta = if (i == 0) null else FollowDiff.delta(stored[i - 1].snapshot, s.snapshot),
            )
        }
    }
}
