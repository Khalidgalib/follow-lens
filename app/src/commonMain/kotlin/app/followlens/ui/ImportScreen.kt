package app.followlens.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ImportScreen(
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
        Text(
            "Import your Instagram export",
            style = MaterialTheme.typography.headlineSmall,
            color = FollowLensColors.accentStrong,
        )
        Text(
            "Instagram → Settings → Accounts Center → Your information and permissions → " +
                "Download your information → Followers and following → JSON. Paste the two files below.",
            style = MaterialTheme.typography.bodySmall,
            color = FollowLensColors.textSecondary,
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
