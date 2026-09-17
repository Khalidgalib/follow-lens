package app.followlens.diff

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * following.html's real shape: username, blank line, the `_u/`-prefixed profile URL, then a date
 * — wrapped here in an arbitrary container to prove the parser doesn't depend on div class names.
 */
private fun followingHtmlEntry(username: String, date: String): String = """
    <div class="_a6-p"><div>
        <div><div><a href="https://www.instagram.com/_u/$username" target="_blank">$username</a></div></div>
        <div class="_a6-p"><div>$date</div></div>
    </div></div>
""".trimIndent()

/**
 * followers_N.html's real shape: username then date, with NO visible URL line — the anchor's href
 * still points at the profile, it's just never shown as text.
 */
private fun followersHtmlEntry(username: String, date: String): String = """
    <div class="_a6-p"><div>
        <a href="https://www.instagram.com/$username">$username</a>
        <div class="_a6-p"><div>$date</div></div>
    </div></div>
""".trimIndent()

private fun htmlDoc(vararg entries: String): String =
    "<html><body>${entries.joinToString("")}</body></html>"

class HtmlExportParserTest {

    private val parser = ExportParser()

    @Test
    fun parseFollowing_extractsUsernames_fromRealFollowingShape() {
        val html = htmlDoc(
            followingHtmlEntry("abo_ni_", "Sep 17, 2026 11:21 am"),
            followingHtmlEntry("_samihaaa._", "Sep 14, 2026 9:44 am"),
        )
        val following = parser.parseFollowing(html)
        assertEquals(setOf("abo_ni_", "_samihaaa._"), following.map { it.username }.toSet())
    }

    @Test
    fun parseFollowers_extractsUsernames_fromRealFollowersShape_noUrlLine() {
        val html = htmlDoc(
            followersHtmlEntry("_samihaaa._", "Sep 14, 2026 10:00 am"),
            followersHtmlEntry("ahmed_redwan172", "Sep 12, 2026 11:56 pm"),
        )
        val followers = parser.parseFollowers(html)
        assertEquals(setOf("_samihaaa._", "ahmed_redwan172"), followers.map { it.username }.toSet())
    }

    @Test
    fun parseFollowing_parsesRealDateFormat_toCorrectEpochSeconds() {
        val html = htmlDoc(followingHtmlEntry("abo_ni_", "Sep 17, 2026 11:21 am"))
        val account = parser.parseFollowing(html).first()
        val decoded = Instant.fromEpochSeconds(account.timestampSeconds!!).toLocalDateTime(TimeZone.UTC)
        assertEquals(2026, decoded.year)
        assertEquals(9, decoded.monthNumber)
        assertEquals(17, decoded.dayOfMonth)
        assertEquals(11, decoded.hour)
        assertEquals(21, decoded.minute)
    }

    @Test
    fun parseFollowing_parsesPmCorrectly() {
        val html = htmlDoc(followingHtmlEntry("ahmed_redwan172", "Sep 12, 2026 11:55 pm"))
        val account = parser.parseFollowing(html).first()
        val decoded = Instant.fromEpochSeconds(account.timestampSeconds!!).toLocalDateTime(TimeZone.UTC)
        assertEquals(23, decoded.hour)
        assertEquals(55, decoded.minute)
    }

    @Test
    fun parseFollowing_missingDate_stillParsesUsername_withNullTimestamp() {
        val html = htmlDoc("""<a href="https://www.instagram.com/_u/nodatehere">nodatehere</a>""")
        val account = parser.parseFollowing(html).first()
        assertEquals("nodatehere", account.username)
        assertNull(account.timestampSeconds)
    }

    @Test
    fun mixedFormats_htmlFollowing_jsonFollowers_diffStillWorks() {
        val followingHtml = htmlDoc(
            followingHtmlEntry("alice", "Sep 17, 2026 11:21 am"),
            followingHtmlEntry("bob", "Sep 14, 2026 9:44 am"),
        )
        val followersJson = """[{"string_list_data":[{"href":"https://instagram.com/alice","value":"alice","timestamp":1}]}]"""
        val snap = parser.parseSnapshot(followingHtml, followersJson)
        val result = FollowDiff.compare(snap)
        assertEquals(listOf("bob"), result.notFollowingBack.map { it.username })
        assertEquals(listOf("alice"), result.mutuals.map { it.username })
    }

    @Test
    fun htmlWithNoInstagramAnchors_throwsParseException() {
        val html = "<html><body><p>no links here</p></body></html>"
        assertFailsWith<ExportParser.ParseException> { parser.parseFollowing(html) }
    }
}
