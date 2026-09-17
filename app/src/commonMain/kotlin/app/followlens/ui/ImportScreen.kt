package app.followlens.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
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
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch

private val importableExtensions = listOf("json", "html", "htm")

@Composable
fun ImportScreen(
    error: String?,
    canCancel: Boolean,
    onCancel: () -> Unit,
    onSubmit: (following: String, followers: String) -> Unit,
) {
    var followingContent by remember { mutableStateOf<String?>(null) }
    var followingFileName by remember { mutableStateOf<String?>(null) }
    var followersContent by remember { mutableStateOf<String?>(null) }
    var followersFileName by remember { mutableStateOf<String?>(null) }
    var readError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    val followingLauncher = rememberFilePickerLauncher(
        type = PickerType.File(extensions = importableExtensions),
        onResult = { file ->
            if (file != null) {
                scope.launch {
                    try {
                        followingContent = file.readBytes().decodeToString()
                        followingFileName = file.name
                        readError = null
                    } catch (e: Exception) {
                        readError = "Couldn't read ${file.name}: ${e.message}"
                    }
                }
            }
        },
    )
    val followersLauncher = rememberFilePickerLauncher(
        type = PickerType.File(extensions = importableExtensions),
        onResult = { file ->
            if (file != null) {
                scope.launch {
                    try {
                        followersContent = file.readBytes().decodeToString()
                        followersFileName = file.name
                        readError = null
                    } catch (e: Exception) {
                        readError = "Couldn't read ${file.name}: ${e.message}"
                    }
                }
            }
        },
    )

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Import your Instagram export",
            style = MaterialTheme.typography.headlineSmall,
            color = FollowLensColors.accentStrong,
        )
        Text(
            "Instagram → Settings → Accounts Center → Your information and permissions → " +
                "Download your information → Followers and following → JSON or HTML. Upload the " +
                "two files below.",
            style = MaterialTheme.typography.bodySmall,
            color = FollowLensColors.textSecondary,
        )

        UploadCard(
            label = "Following",
            fileName = followingFileName,
            onPick = { followingLauncher.launch() },
        )
        UploadCard(
            label = "Followers",
            fileName = followersFileName,
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
private fun UploadCard(label: String, fileName: String?, onPick: () -> Unit) {
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
        }
    }
}
