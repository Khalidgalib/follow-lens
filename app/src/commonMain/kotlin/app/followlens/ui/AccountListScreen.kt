package app.followlens.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.followlens.diff.Account
import kotlinx.coroutines.launch

private enum class SortMode(val label: String) {
    RECENT("RECENT ↓"),
    ALPHABETICAL("A–Z"),
}

/**
 * Search + sortable list, shared by the "Not following back" and "Fans" tabs — the whitelist
 * filters both [app.followlens.diff.FollowDiffResult] lists, so both get the same actions.
 */
@Composable
fun AccountListScreen(
    title: String,
    accounts: List<Account>,
    onWhitelist: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    onMarkHandled: ((String) -> Unit)? = null,
) {
    var query by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(SortMode.RECENT) }
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val visible = accounts
        .filter { query.isBlank() || it.username.contains(query.trim(), ignoreCase = true) }
        .let { list ->
            when (sortMode) {
                SortMode.ALPHABETICAL -> list.sortedBy { it.username }
                SortMode.RECENT -> list.sortedWith(compareByDescending { it.timestampSeconds ?: Long.MIN_VALUE })
            }
        }

    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) {
                item {
                    ScreenHeader("$title · ${accounts.size}", onBack = onBack)
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Search the list") },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                            singleLine = true,
                        )
                        TextButton(onClick = { sortMode = if (sortMode == SortMode.RECENT) SortMode.ALPHABETICAL else SortMode.RECENT }) {
                            Text(sortMode.label, color = FollowLensColors.accentStrong)
                        }
                    }
                }

                if (visible.isEmpty()) {
                    item {
                        Text(
                            if (accounts.isEmpty()) "Nothing here." else "No matches for \"$query\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FollowLensColors.textTertiary,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    }
                }

                items(visible, key = { it.username }) { account ->
                    AccountRow(account) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(onClick = {
                                val opened = runCatching {
                                    uriHandler.openUri("https://www.instagram.com/${account.username}/")
                                }.isSuccess
                                if (!opened) {
                                    scope.launch { snackbarHostState.showSnackbar("Couldn't open @${account.username}") }
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = "Open profile", tint = FollowLensColors.textSecondary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onWhitelist(account.username) }) {
                                Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Whitelist", tint = FollowLensColors.textSecondary, modifier = Modifier.size(18.dp))
                            }
                            if (onMarkHandled != null) {
                                IconButton(onClick = { onMarkHandled(account.username) }) {
                                    Icon(Icons.Outlined.CheckCircleOutline, contentDescription = "Mark handled", tint = FollowLensColors.textSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                            IconButton(onClick = { clipboard.setText(AnnotatedString("@${account.username}")) }) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy username", tint = FollowLensColors.textSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            ScrollPositionIndicator(listState, Modifier.fillMaxHeight().padding(vertical = 8.dp, horizontal = 4.dp))
        }
        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
