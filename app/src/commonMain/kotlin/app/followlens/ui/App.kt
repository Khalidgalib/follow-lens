package app.followlens.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
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
import app.followlens.data.DismissedStore
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(driverFactory: DatabaseDriverFactory) {
    val repo = remember { FollowLensRepo(driverFactory) }

    var loaded by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<FollowDiffResult?>(null) }
    var showUploadPanel by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var screen by remember { mutableStateOf(Screen.DASHBOARD) }
    var detailScreen by remember { mutableStateOf<DetailScreen?>(null) }
    var version by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        repo.loadLatest()
        result = repo.diff()
        loaded = true
    }

    val history = remember(version) { repo.history() }
    val whitelist = remember(version) { repo.whitelistUsernames() }
    val handled = remember(version) { repo.dismissedUsernames() }

    fun refresh() {
        result = repo.diff()
        version++
    }

    fun submitImport(following: String, followers: List<String>) {
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
        CompositionLocalProvider(
            LocalIndication provides ripple(color = FollowLensColors.accentStrong),
            LocalRippleConfiguration provides RippleConfiguration(color = FollowLensColors.accentStrong),
        ) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                    if (!loaded) {
                        Centered { CircularProgressIndicator() }
                    } else {
                        // Dashboard is always the home screen — with no data yet it just shows
                        // zeros plus the upload panel instead of gating the whole app behind it.
                        Column(Modifier.fillMaxSize()) {
                            Column(Modifier.weight(1f)) {
                                when (val detail = detailScreen) {
                                    DetailScreen.FOLLOWING -> AccountListScreen(
                                        title = "Following",
                                        accounts = (result?.mutuals ?: emptyList()) + (result?.notFollowingBack ?: emptyList()),
                                        onWhitelist = { username -> repo.addToWhitelist(username); refresh() },
                                        onBack = { detailScreen = null },
                                    )

                                    DetailScreen.FOLLOWERS -> AccountListScreen(
                                        title = "Followers",
                                        accounts = (result?.mutuals ?: emptyList()) + (result?.fans ?: emptyList()),
                                        onWhitelist = { username -> repo.addToWhitelist(username); refresh() },
                                        onBack = { detailScreen = null },
                                    )

                                    null -> when (screen) {
                                        Screen.DASHBOARD -> DashboardScreen(
                                            hasData = result != null,
                                            followingCount = (result?.mutuals?.size ?: 0) + (result?.notFollowingBack?.size ?: 0),
                                            followersCount = (result?.mutuals?.size ?: 0) + (result?.fans?.size ?: 0),
                                            notFollowingBackCount = result?.notFollowingBack?.size ?: 0,
                                            fansCount = result?.fans?.size ?: 0,
                                            possiblyLimitedRange = result?.possiblyLimitedRange ?: false,
                                            trend = history.map { TrendPoint(it.followingCount, it.followersCount) },
                                            trendRangeLabel = history.takeIf { it.size >= 2 }
                                                ?.let { formatMonthRange(it.first().takenAtSeconds, it.last().takenAtSeconds) },
                                            lastImportAtSeconds = history.lastOrNull()?.takenAtSeconds,
                                            showUploadPanel = showUploadPanel,
                                            onShowUploadPanel = { showUploadPanel = true },
                                            onHideUploadPanel = { showUploadPanel = false; error = null },
                                            importError = error,
                                            onSubmitImport = ::submitImport,
                                            onOpenFollowing = { detailScreen = DetailScreen.FOLLOWING },
                                            onOpenFollowers = { detailScreen = DetailScreen.FOLLOWERS },
                                            onOpenNotFollowingBack = { screen = Screen.NOT_FOLLOWING_BACK },
                                            onOpenFans = { screen = Screen.FANS },
                                        )

                                        Screen.NOT_FOLLOWING_BACK -> AccountListScreen(
                                            title = "Not following back",
                                            accounts = result?.notFollowingBack ?: emptyList(),
                                            onWhitelist = { username -> repo.addToWhitelist(username); refresh() },
                                            onMarkHandled = { username -> repo.markHandled(username); refresh() },
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
                                            handled = handled,
                                            onUnmarkHandled = { username -> repo.unmarkHandled(username); refresh() },
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
                            }
                            BottomNavBar(current = screen, onSelect = { screen = it; detailScreen = null })
                        }
                    }
                }
            }
        }
    }
}

private enum class DetailScreen { FOLLOWING, FOLLOWERS }

// --- glue: repository over the :data + :core-diff modules --------------------------------------

private class FollowLensRepo(driverFactory: DatabaseDriverFactory) {
    private val db = openDatabase(driverFactory)
    private val snapshots = SnapshotStore(db)
    private val whitelist = WhitelistStore(db)
    private val dismissed = DismissedStore(db)
    private val parser = ExportParser()

    private var current: ExportSnapshot? = null

    fun loadLatest() {
        current = snapshots.latest()?.snapshot
    }

    /** @throws ExportParser.ParseException on bad input. */
    fun import(followingJson: String, followersJson: List<String>) {
        val snapshot = parser.parseSnapshot(followingJson, *followersJson.toTypedArray())
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

    fun markHandled(username: String) {
        dismissed.add(username, Clock.System.now().epochSeconds)
    }

    fun unmarkHandled(username: String) {
        dismissed.remove(username)
    }

    fun dismissedUsernames(): List<String> = dismissed.all().sorted()

    fun clearAllData() {
        resetAllData(db)
        current = null
    }

    fun diff(): FollowDiffResult? = current?.let { FollowDiff.compare(it, whitelist.all(), dismissed.all()) }

    /** Oldest-first, one entry per import, each carrying its change since the previous one. */
    fun history(): List<HistoryEntry> {
        val stored: List<StoredSnapshot> = snapshots.history()
        val wl = whitelist.all()
        val dismissedNames = dismissed.all()
        return stored.mapIndexed { i, s ->
            HistoryEntry(
                takenAtSeconds = s.takenAtSeconds,
                followingCount = s.snapshot.following.size,
                followersCount = s.snapshot.followers.size,
                notFollowingBackCount = FollowDiff.compare(s.snapshot, wl, dismissedNames).notFollowingBack.size,
                delta = if (i == 0) null else FollowDiff.delta(stored[i - 1].snapshot, s.snapshot),
            )
        }
    }
}
