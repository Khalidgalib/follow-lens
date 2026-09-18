package app.followlens.diff

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import okio.openZip

/**
 * Pulls candidate export files straight out of the raw `.zip` Instagram emails, so the caller
 * doesn't have to unzip it themselves first. Pure byte-array in, text out — no real filesystem
 * path needed (the zip's bytes are staged in an in-memory [FakeFileSystem] before
 * [okio.openZip]), so this runs identically on-device and, later, server-side, same as the rest
 * of `core-diff`.
 */
object ZipImport {

    private val CANDIDATE_EXTENSIONS = setOf("json", "html", "htm")

    /** One entry per file inside the zip that looks like an export file: its name and text. */
    fun extractCandidateFiles(zipBytes: ByteArray): List<Pair<String, String>> {
        val stagingFs = FakeFileSystem()
        val zipPath = "/import.zip".toPath()
        stagingFs.write(zipPath) { write(zipBytes) }

        val zipFs: FileSystem = stagingFs.openZip(zipPath)
        return zipFs.listRecursively("/".toPath())
            .filter { path ->
                zipFs.metadataOrNull(path)?.isRegularFile == true &&
                    path.name.substringAfterLast('.', "").lowercase() in CANDIDATE_EXTENSIONS
            }
            .map { path -> path.name to zipFs.read(path) { readUtf8() } }
            .toList()
    }
}
