package app.followlens.ui

import io.github.vinceglb.filekit.core.PlatformDirectory
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSDataReadingUncached
import platform.Foundation.NSDirectoryEnumerationSkipsHiddenFiles
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.lastPathComponent
import platform.Foundation.pathExtension
import platform.posix.memcpy

actual class DirectoryLister {
    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun listCandidateFiles(directory: PlatformDirectory): List<Pair<String, String>> =
        withContext(Dispatchers.IO) {
            val root = directory.nsUrl
            val accessing = root.startAccessingSecurityScopedResource()
            try {
                val enumerator = NSFileManager.defaultManager.enumeratorAtURL(
                    url = root,
                    includingPropertiesForKeys = null,
                    options = NSDirectoryEnumerationSkipsHiddenFiles,
                    errorHandler = null,
                )
                val out = mutableListOf<Pair<String, String>>()
                while (true) {
                    val url = enumerator?.nextObject() as? NSURL ?: break
                    val ext = url.pathExtension?.lowercase() ?: continue
                    if (ext !in CANDIDATE_FILE_EXTENSIONS) continue
                    val data = NSData.dataWithContentsOfURL(url, NSDataReadingUncached, null) ?: continue
                    out += (url.lastPathComponent ?: "unknown") to data.toByteArray().decodeToString()
                }
                out
            } finally {
                if (accessing) root.stopAccessingSecurityScopedResource()
            }
        }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val result = ByteArray(length.toInt())
    if (result.isNotEmpty()) {
        memcpy(result.refTo(0), bytes, length)
    }
    return result
}
