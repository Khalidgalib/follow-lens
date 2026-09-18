package app.followlens.ui

import io.github.vinceglb.filekit.core.PlatformDirectory

/**
 * Recursively finds every candidate export file (.json/.html/.htm) inside a folder the user
 * picked with FileKit's directory picker. `actual` implementations live in androidMain / iosMain
 * / desktopMain because FileKit's [PlatformDirectory] itself has no listing API — Android needs a
 * `Context` + `DocumentFile` to walk a SAF tree URI, iOS needs `NSFileManager` + security-scoped
 * resource access, desktop can just walk a real [java.io.File].
 */
expect class DirectoryLister {
    suspend fun listCandidateFiles(directory: PlatformDirectory): List<Pair<String, String>>
}

internal val CANDIDATE_FILE_EXTENSIONS = setOf("json", "html", "htm")
