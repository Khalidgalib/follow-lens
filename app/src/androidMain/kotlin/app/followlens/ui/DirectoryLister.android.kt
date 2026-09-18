package app.followlens.ui

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import io.github.vinceglb.filekit.core.PlatformDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class DirectoryLister(private val context: Context) {
    actual suspend fun listCandidateFiles(directory: PlatformDirectory): List<Pair<String, String>> =
        withContext(Dispatchers.IO) {
            val root = DocumentFile.fromTreeUri(context, directory.uri) ?: return@withContext emptyList()
            val out = mutableListOf<Pair<String, String>>()
            fun walk(doc: DocumentFile) {
                if (doc.isDirectory) {
                    doc.listFiles().forEach(::walk)
                    return
                }
                val name = doc.name ?: return
                if (name.substringAfterLast('.', "").lowercase() !in CANDIDATE_FILE_EXTENSIONS) return
                val bytes = context.contentResolver.openInputStream(doc.uri)?.use { it.readBytes() } ?: return
                out += name to bytes.decodeToString()
            }
            walk(root)
            out
        }
}
