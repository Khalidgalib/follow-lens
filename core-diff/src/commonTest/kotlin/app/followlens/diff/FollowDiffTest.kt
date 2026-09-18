package app.followlens.diff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun followersJson(vararg usernames: String): String =
    usernames.joinToString(prefix = "[", postfix = "]") { u ->
        """{"string_list_data":[{"href":"https://instagram.com/$u","value":"$u","timestamp":1700000000}]}"""
    }

private fun followingJson(vararg usernames: String): String =
    usernames.joinToString(prefix = """{"relationships_following":[""", postfix = "]}") { u ->
        """{"string_list_data":[{"href":"https://instagram.com/$u","value":"$u","timestamp":1700000001}]}"""
    }

class FollowDiffTest {

    private val parser = ExportParser()

    @Test
    fun notFollowingBack_isFollowingMinusFollowers() {
        val snap = parser.parseSnapshot(
            followingFileContent = followingJson("alice", "bob", "carol"),
            followersJson("alice", "dave"),
        )
        val result = FollowDiff.compare(snap)
        assertEquals(listOf("bob", "carol"), result.notFollowingBack.map { it.username })
        assertEquals(listOf("dave"), result.fans.map { it.username })
        assertEquals(listOf("alice"), result.mutuals.map { it.username })
    }

    @Test
    fun compare_isCaseInsensitive() {
        val snap = parser.parseSnapshot(
            followingFileContent = followingJson("Alice", "BOB"),
            followersJson("alice"),
        )
        val result = FollowDiff.compare(snap)
        assertEquals(listOf("bob"), result.notFollowingBack.map { it.username })
    }

    @Test
    fun whitelist_removesFromNotFollowingBack() {
        val snap = parser.parseSnapshot(
            followingFileContent = followingJson("bob", "carol"),
            followersJson(),
        )
        val result = FollowDiff.compare(snap, whitelist = setOf("bob"))
        assertEquals(listOf("carol"), result.notFollowingBack.map { it.username })
    }

    @Test
    fun dismissed_removesFromNotFollowingBackOnly_notFans() {
        val snap = parser.parseSnapshot(
            followingFileContent = followingJson("bob", "carol"),
            followersJson("dave"),
        )
        val result = FollowDiff.compare(snap, dismissed = setOf("bob"))
        assertEquals(listOf("carol"), result.notFollowingBack.map { it.username })
        assertEquals(listOf("dave"), result.fans.map { it.username })
    }

    @Test
    fun possiblyLimitedRange_falseWhenTimestampsSpanOverAYear() {
        // followingJson/followersJson use fixed 2023-era timestamps, decades before "now".
        val snap = parser.parseSnapshot(
            followingFileContent = followingJson("alice"),
            followersJson("alice"),
        )
        val result = FollowDiff.compare(snap)
        assertFalse(result.possiblyLimitedRange)
    }

    @Test
    fun possiblyLimitedRange_trueWhenAllTimestampsAreRecent() {
        val now = 1_800_000_000L
        val recent = now - 30 * 86_400L // 30 days ago
        val json = """{"relationships_following":[
            {"title":"alice","string_list_data":[{"href":"https://instagram.com/_u/alice","timestamp":$recent}]}
        ]}"""
        val snap = parser.parseSnapshot(followingFileContent = json, followersJson("alice"))
        val result = FollowDiff.compare(snap, nowSeconds = now)
        assertTrue(result.possiblyLimitedRange)
    }

    @Test
    fun possiblyLimitedRange_falseWhenTimestampsAreMostlyMissing() {
        val now = 1_800_000_000L
        val recent = now - 30 * 86_400L
        // Only 1 of 3 following-entries has a timestamp — not enough coverage to trust the signal.
        val json = """{"relationships_following":[
            {"title":"a","string_list_data":[{"href":"https://instagram.com/_u/a","timestamp":$recent}]},
            {"title":"b","string_list_data":[{"href":"https://instagram.com/_u/b"}]},
            {"title":"c","string_list_data":[{"href":"https://instagram.com/_u/c"}]}
        ]}"""
        val snap = parser.parseSnapshot(followingFileContent = json, followersJson("a", "b", "c"))
        val result = FollowDiff.compare(snap, nowSeconds = now)
        assertFalse(result.possiblyLimitedRange)
    }

    @Test
    fun parseFollowers_mergesMultipleParts_andDeDupes() {
        val followers = parser.parseFollowers(
            followersJson("a", "b"),
            followersJson("b", "c"),
        )
        assertEquals(setOf("a", "b", "c"), followers.map { it.username }.toSet())
    }

    @Test
    fun parser_skipsDeactivatedAccountsWithEmptyValue() {
        val json = """[
            {"string_list_data":[{"href":"","value":"","timestamp":0}]},
            {"string_list_data":[{"href":"https://instagram.com/real","value":"real","timestamp":1}]}
        ]"""
        val followers = parser.parseFollowers(json)
        assertEquals(setOf("real"), followers.map { it.username }.toSet())
    }

    @Test
    fun parseFollowing_readsRealExportShape_titleInsteadOfValue() {
        // following.json (2026 export format) omits string_list_data[0].value entirely and puts
        // the username in the entry's own "title" instead, with an "_u/" segment in the href.
        val json = """{"relationships_following":[
            {"title":"abo_ni_","string_list_data":[{"href":"https://www.instagram.com/_u/abo_ni_","timestamp":1789572079}]},
            {"title":"_samihaaa._","string_list_data":[{"href":"https://www.instagram.com/_u/_samihaaa._","timestamp":1789407849}]}
        ]}"""
        val following = parser.parseFollowing(json)
        assertEquals(setOf("abo_ni_", "_samihaaa._"), following.map { it.username }.toSet())
    }

    @Test
    fun parseFollowing_fallsBackToHref_whenNeitherValueNorTitlePresent() {
        val json = """{"relationships_following":[
            {"string_list_data":[{"href":"https://www.instagram.com/_u/onlyhref","timestamp":1700000000}]}
        ]}"""
        val following = parser.parseFollowing(json)
        assertEquals(setOf("onlyhref"), following.map { it.username }.toSet())
    }

    @Test
    fun detectKind_identifiesJsonFollowingByKey() {
        assertEquals(ExportParser.ExportKind.FOLLOWING, parser.detectKind(followingJson("alice")))
    }

    @Test
    fun detectKind_identifiesJsonFollowersByBareArray() {
        assertEquals(ExportParser.ExportKind.FOLLOWERS, parser.detectKind(followersJson("alice")))
    }

    @Test
    fun parser_rejectsEmptyAndCorruptInput() {
        assertFailsWith<ExportParser.ParseException> { parser.parseFollowing("") }
        assertFailsWith<ExportParser.ParseException> { parser.parseFollowing("{ not json") }
    }

    @Test
    fun delta_detectsGainedAndLost() {
        val prev = parser.parseSnapshot(followingJson("x"), followersJson("a", "b"))
        val cur = parser.parseSnapshot(followingJson("x", "y"), followersJson("b", "c"))
        val d = FollowDiff.delta(prev, cur)
        assertEquals(listOf("c"), d.newFollowers.map { it.username })
        assertEquals(listOf("a"), d.lostFollowers.map { it.username })
        assertEquals(listOf("y"), d.newlyFollowed.map { it.username })
        assertTrue(d.youUnfollowed.isEmpty())
    }
}
