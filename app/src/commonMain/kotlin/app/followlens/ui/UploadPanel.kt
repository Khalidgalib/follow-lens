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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
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
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.launch

private val importableExtensions = listOf("json", "html", "htm")

private enum class ImportSlot(val label: String) {
    FOLLOWING("Following"),
    FOLLOWERS("Followers"),
}

/**
 * The "Upload Following" / "Upload Followers" + Analyze UI, embedded in the Dashboard tab (see
 * [DashboardScreen]) rather than a standalone screen — it's shown by default when there's no
 * imported data yet, and on demand afterwards for re-importing.
 */
@Composable
fun UploadPanel(
    error: String?,
    canCancel: Boolean,
    onCancel: () -> Unit,
    onSubmit: (following: String, followers: String) -> Unit,
) {
    var followingContent by remember { mutableStateOf<String?>(null) }
    var followingFileName by remember { mutableStateOf<String?>(null) }
    var followingWarning by remember { mutableStateOf<String?>(null) }
    var followersContent by remember { mutableStateOf<String?>(null) }
    var followersFileName by remember { mutableStateOf<String?>(null) }
    var followersWarning by remember { mutableStateOf<String?>(null) }
    var readError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val parser = remember { ExportParser() }

    fun handlePicked(slot: ImportSlot, file: PlatformFile) {
        scope.launch {
            try {
                val content = file.readBytes().decodeToString()
                val kind = parser.detectKind(content)
                val mismatch = when (slot) {
                    ImportSlot.FOLLOWING -> kind == ExportParser.ExportKind.FOLLOWERS
                    ImportSlot.FOLLOWERS -> kind == ExportParser.ExportKind.FOLLOWING
                }
                if (mismatch) {
                    val detectedLabel = if (kind == ExportParser.ExportKind.FOLLOWING) "Following" else "Followers"
                    readError = "This looks like a $detectedLabel export — upload it in the $detectedLabel slot instead."
                    return@launch
                }

                val warning = if (kind == ExportParser.ExportKind.UNKNOWN) {
                    "Couldn't confirm this is a ${slot.label} export — double check the file."
                } else {
                    null
                }
                when (slot) {
                    ImportSlot.FOLLOWING -> {
                        followingContent = content
                        followingFileName = file.name
                        followingWarning = warning
                    }
                    ImportSlot.FOLLOWERS -> {
                        followersContent = content
                        followersFileName = file.name
                        followersWarning = warning
                    }
                }
                readError = null
            } catch (e: Exception) {
                readError = "Couldn't read ${file.name}: ${e.message}"
            }
        }
    }

    val followingLauncher = rememberFilePickerLauncher(
        type = PickerType.File(extensions = importableExtensions),
        onResult = { file -> if (file != null) handlePicked(ImportSlot.FOLLOWING, file) },
    )
    val followersLauncher = rememberFilePickerLauncher(
        type = PickerType.File(extensions = importableExtensions),
        onResult = { file -> if (file != null) handlePicked(ImportSlot.FOLLOWERS, file) },
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Instagram → Settings → Accounts Center → Your information and permissions → " +
                "Download your information → Followers and following → JSON or HTML, date range " +
                "All time. Upload the two files below once the export email arrives.",
            style = MaterialTheme.typography.bodySmall,
            color = FollowLensColors.textSecondary,
        )

        UploadCard(
            label = "Following",
            fileName = followingFileName,
            warning = followingWarning,
            onPick = { followingLauncher.launch() },
        )
        UploadCard(
            label = "Followers",
            fileName = followersFileName,
            warning = followersWarning,
            onPick = { followersLauncher.launch() },
        )

        val shownError = error ?: readError
        if (shownError != null) {
            Text(shownError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onSubmit(followingContent!!, followersContent!!) },
                enabled = followingContent != null && followersContent != null,
            ) { Text("Analyze") }
            if (canCancel) OutlinedButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}

@Composable
private fun UploadCard(label: String, fileName: String?, warning: String?, onPick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = FollowLensColors.surfaceRaised,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = FollowLensColors.textTertiary)
            Spacer(Modifier.height(8.dp))
            if (fileName == null) {
                Button(onClick = onPick) { Text("Upload $label file") }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = FollowLensColors.good)
                    Text(
                        fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = FollowLensColors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = onPick) { Text("Change") }
                }
            }
            if (warning != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = FollowLensColors.accent, modifier = Modifier.size(16.dp))
                    Text(warning, style = MaterialTheme.typography.labelSmall, color = FollowLensColors.accent)
                }
            }
        }
    }
}
