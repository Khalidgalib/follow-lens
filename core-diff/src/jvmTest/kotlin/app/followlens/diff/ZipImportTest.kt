package app.followlens.diff

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * JVM-only (not commonTest) because building a real test .zip is easiest with
 * java.util.zip.ZipOutputStream — [ZipImport] itself is pure commonMain and runs identically on
 * every target, this just needs a JVM-only way to construct fixture data.
 */
class ZipImportTest {

    private fun buildZip(vararg entries: Pair<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            for ((name, content) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.encodeToByteArray())
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    @Test
    fun extractsJsonAndHtmlEntries_andSkipsOthers() {
        val zip = buildZip(
            "connections/followers_and_following/following.json" to """{"relationships_following":[]}""",
            "connections/followers_and_following/followers_1.json" to "[]",
            "media/profile_photos/photo.jpg" to "not real image bytes, doesn't matter for this test",
            "connections/followers_and_following/readme.txt" to "ignored, not a candidate extension",
        )

        val extracted = ZipImport.extractCandidateFiles(zip)

        assertEquals(setOf("following.json", "followers_1.json"), extracted.map { it.first }.toSet())
        assertEquals(
            """{"relationships_following":[]}""",
            extracted.first { it.first == "following.json" }.second,
        )
    }

    @Test
    fun extractsNestedEntries_caseInsensitiveExtension() {
        val zip = buildZip("a/b/c/FOLLOWERS_1.HTML" to "<html>hi</html>")
        val extracted = ZipImport.extractCandidateFiles(zip)
        assertEquals(listOf("FOLLOWERS_1.HTML" to "<html>hi</html>"), extracted)
    }

    @Test
    fun emptyZip_returnsNoCandidates() {
        assertEquals(emptyList(), ZipImport.extractCandidateFiles(buildZip()))
    }
}
