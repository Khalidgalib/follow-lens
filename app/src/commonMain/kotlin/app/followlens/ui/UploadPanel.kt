package app.followlens.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.followlens.diff.ExportParser
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.launch

private val importableExtensions = listOf("json", "html", "htm")

private data class PickedFile(val name: String, val content: String, val kind: ExportParser.ExportKind)

/**
 * The "pick your export files" + Analyze UI, embedded in the Dashboard tab (see
 * [DashboardScreen]) rather than a standalone screen — it's shown by default when there's no
 * imported data yet, and on demand afterwards for re-importing.
 *
 * Files are picked all at once (one multi-select instead of two separate single-file pickers) and
 * classified by content via [ExportParser.detectKind], so the user doesn't have to remember which
 * button corresponds to which file — and a followers export split across multiple parts
 * (`followers_1.json`, `followers_2.json`, ...) all just get picked together and merged.
 */
@Composable
fun UploadPanel(
    error: String?,
    canCancel: Boolean,
    onCancel: () -> Unit,
    onSubmit: (following: String, followers: List<String>) -> Unit,
) {
    var pickedFiles by remember { mutableStateOf<List<PickedFile>>(emptyList()) }
    var readError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val parser = remember { ExportParser() }

    fun setKind(name: String, kind: ExportParser.ExportKind) {
        pickedFiles = pickedFiles.map { if (it.name == name) it.copy(kind = kind) else it }
    }

    fun handlePicked(files: List<PlatformFile>) {
        scope.launch {
            try {
                pickedFiles = files.map { file ->
                    val content = file.readBytes().decodeToString()
                    PickedFile(file.name, content, parser.detectKind(content))
                }
                readError = null
            } catch (e: Exception) {
                readError = "Couldn't read one of the files: ${e.message}"
            }
        }
    }

    val launcher = rememberFilePickerLauncher(
        type = PickerType.File(extensions = importableExtensions),
        mode = PickerMode.Multiple(),
        onResult = { files -> if (files != null) handlePicked(files) },
    )

    val followingFile = pickedFiles.firstOrNull { it.kind == ExportParser.ExportKind.FOLLOWING }
    val followersFiles = pickedFiles.filter { it.kind == ExportParser.ExportKind.FOLLOWERS }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ImportInstructions()

        if (pickedFiles.isEmpty()) {
            Button(onClick = { launcher.launch() }) { Text("Pick your export files") }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                pickedFiles.forEach { file ->
                    PickedFileRow(file = file, onSetKind = { kind -> setKind(file.name, kind) })
                }
            }
            OutlinedButton(onClick = { launcher.launch() }) { Text("Change files") }
        }

        val shownError = error ?: readError
        if (shownError != null) {
            Text(shownError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        } else if (pickedFiles.isNotEmpty() && (followingFile == null || followersFiles.isEmpty())) {
            Text(
                "Pick at least one Following file and one Followers file (use the chips above to correct a file we guessed wrong).",
                color = FollowLensColors.accent,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onSubmit(followingFile!!.content, followersFiles.map { it.content }) },
                enabled = followingFile != null && followersFiles.isNotEmpty(),
            ) { Text("Analyze") }
            if (canCancel) OutlinedButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}

@Composable
private fun PickedFileRow(file: PickedFile, onSetKind: (ExportParser.ExportKind) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = FollowLensColors.surfaceRaised,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.AutoMirrored.Outlined.InsertDriveFile, contentDescription = null, tint = FollowLensColors.textSecondary, modifier = Modifier.size(18.dp))
                Text(file.name, style = MaterialTheme.typography.bodyMedium, color = FollowLensColors.textPrimary, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = file.kind == ExportParser.ExportKind.FOLLOWING,
                    onClick = { onSetKind(ExportParser.ExportKind.FOLLOWING) },
                    label = { Text("Following") },
                )
                FilterChip(
                    selected = file.kind == ExportParser.ExportKind.FOLLOWERS,
                    onClick = { onSetKind(ExportParser.ExportKind.FOLLOWERS) },
                    label = { Text("Followers") },
                )
            }
            if (file.kind == ExportParser.ExportKind.UNKNOWN) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = FollowLensColors.accent, modifier = Modifier.size(16.dp))
                    Text(
                        "Couldn't tell which this is — pick one above.",
                        style = MaterialTheme.typography.labelSmall,
                        color = FollowLensColors.accent,
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportInstructions() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "1. Open Instagram → your profile → ☰ Menu.\n" +
                "2. Accounts Center → Your information and permissions.\n" +
                "3. Download your information → Download or transfer information.\n" +
                "4. Choose your Instagram account. Leave all information types selected — " +
                "FollowLens finds the files it needs regardless of what else is in the export, " +
                "and keeping everything means a future FollowLens feature can reuse this same " +
                "export without asking you to download again.\n" +
                "5. Format: JSON (or HTML — both work). Date range: All time.\n" +
                "6. Submit the request. After a few minutes, go back to this same Download your " +
                "information section in Instagram and download it directly from there — no need " +
                "to wait for the email, though Instagram sends one too if you'd rather use that.",
            style = MaterialTheme.typography.bodySmall,
            color = FollowLensColors.textSecondary,
        )
        Surface(color = FollowLensColors.surfaceRaised, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = FollowLensColors.accent, modifier = Modifier.size(18.dp))
                Text(
                    "Date range: All time. Instagram defaults to Last Year — anything shorter " +
                        "silently drops older followers/following from the file, which makes " +
                        "people who do follow you back wrongly show up as \"Not following back.\"",
                    style = MaterialTheme.typography.labelSmall,
                    color = FollowLensColors.accent,
                )
            }
        }
    }
}
