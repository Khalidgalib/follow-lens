package app.followlens.ui

import io.github.vinceglb.filekit.core.PlatformDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class DirectoryLister {
    actual suspend fun listCandidateFiles(directory: PlatformDirectory): List<Pair<String, String>> =
        withContext(Dispatchers.IO) {
            val root = directory.file
            root.walkTopDown()
                .filter { it.isFile && it.extension.lowercase() in CANDIDATE_FILE_EXTENSIONS }
                .map { it.name to it.readText() }
                .toList()
        }
}
