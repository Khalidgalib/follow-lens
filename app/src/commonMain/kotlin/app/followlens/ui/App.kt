package app.followlens.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.followlens.data.DatabaseDriverFactory
import app.followlens.data.SnapshotStore
import app.followlens.data.WhitelistStore
import app.followlens.data.openDatabase
import app.followlens.diff.Account
import app.followlens.diff.ExportParser
import app.followlens.diff.ExportSnapshot
import app.followlens.diff.FollowDiff
import app.followlens.diff.FollowDiffResult
import kotlinx.datetime.Clock

/**
 * FollowLens v1 UI. Everything is offline: the user pastes the two JSON files from their
 * Instagram export, we run [FollowDiff] and show who doesn't follow them back. The import is
 * saved as a snapshot so history works across imports. (Native `.zip`/file picker is a follow-up.)
 */
@Composable
fun App(driverFactory: DatabaseDriverFactory) {
    val repo = remember { FollowLensRepo(driverFactory) }

    var loaded by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<FollowDiffResult?>(null) }
    var showImport by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        repo.loadLatest()
        result = repo.diff()
        loaded = true
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when {
                !loaded -> Centered { CircularProgressIndicator() }

                result == null || showImport -> ImportScreen(
                    error = error,
                    canCancel = result != null,
                    onCancel = { showImport = false; error = null },
                    onSubmit = { following, followers ->
                        try {
                            repo.import(following, followers)
                            result = repo.diff()
                            showImport = false
                            error = null
                        } catch (e: ExportParser.ParseException) {
                            error = e.message ?: "That doesn't look like a valid Instagram export."
                        }
                    },
                )

                else -> ResultsScreen(
                    result = result!!,
                    onReImport = { showImport = true },
                    onWhitelist = { username ->
                        repo.addToWhitelist(username)
                        result = repo.diff()
                    },
                )
            }
        }
    }
}

// --- screens -------------------------------------------------------------------------------

@Composable
private fun ImportScreen(
    error: String?,
    canCancel: Boolean,
    onCancel: () -> Unit,
    onSubmit: (following: String, followers: String) -> Unit,
) {
    var following by remember { mutableStateOf("") }
    var followers by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Import your Instagram export", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Instagram → Settings → Accounts Center → Your information and permissions → " +
                "Download your information → Followers and following → JSON. Paste the two files below.",
            style = MaterialTheme.typography.bodySmall,
        )

        OutlinedTextField(
            value = following,
            onValueChange = { following = it },
            label = { Text("following.json") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 8,
        )
        OutlinedTextField(
            value = followers,
            onValueChange = { followers = it },
            label = { Text("followers_1.json") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 8,
        )

        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onSubmit(following, followers) },
                enabled = following.isNotBlank() && followers.isNotBlank(),
            ) { Text("Analyze") }
            if (canCancel) OutlinedButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}

@Composable
private fun ResultsScreen(
    result: FollowDiffResult,
    onReImport: () -> Unit,
    onWhitelist: (username: String) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Column(Modifier.padding(vertical = 16.dp)) {
                Text("FollowLens", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${result.notFollowingBack.size} don't follow you back  •  " +
                        "${result.fans.size} fans  •  ${result.mutuals.size} mutuals",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onReImport) { Text("Import a new export") }
            }
        }

        sectionHeader("Not following you back (${result.notFollowingBack.size})")
        items(result.notFollowingBack, key = { "nfb-${it.username}" }) { account ->
            AccountRow(account) {
                TextButton(onClick = { onWhitelist(account.username) }) { Text("Whitelist") }
            }
        }

        sectionHeader("Fans — they follow you, you don't follow back (${result.fans.size})")
        items(result.fans, key = { "fan-${it.username}" }) { account ->
            AccountRow(account)
        }
    }
}

// --- small pieces -------------------------------------------------------------------------

@Composable
private fun AccountRow(account: Account, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("@${account.username}", style = MaterialTheme.typography.bodyLarge)
        trailing()
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.sectionHeader(text: String) = item {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

// --- glue: repository over the :data stores ---------------------------------------------------

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

    fun diff(): FollowDiffResult? = current?.let { FollowDiff.compare(it, whitelist.all()) }
}
