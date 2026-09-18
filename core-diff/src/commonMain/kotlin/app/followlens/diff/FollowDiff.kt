package app.followlens.diff

import kotlinx.datetime.Clock

/**
 * Pure set math over a parsed [ExportSnapshot]. No I/O, no platform APIs — this is the code that
 * runs on-device in v1 and, unchanged, server-side when the online backend is added.
 */
object FollowDiff {

    /** Below this many days old, an export's oldest timestamp looks suspiciously recent. */
    private const val SUSPICIOUS_RANGE_DAYS = 370
    private const val SECONDS_PER_DAY = 86_400L

    /** Need timestamps on at least this fraction of accounts before trusting the signal. */
    private const val MIN_TIMESTAMP_COVERAGE = 0.5

    /**
     * @param whitelist usernames the user never wants flagged (stored lower-cased). Removed from
     *   [FollowDiffResult.notFollowingBack] and [FollowDiffResult.fans].
     * @param dismissed usernames the user already marked "handled" on a previous
     *   [FollowDiffResult.notFollowingBack] (stored lower-cased) — hidden from that list only,
     *   until they're un-dismissed or the whole list is cleared.
     * @param nowSeconds injectable for tests; real callers use the default (actual clock).
     */
    fun compare(
        snapshot: ExportSnapshot,
        whitelist: Set<String> = emptySet(),
        dismissed: Set<String> = emptySet(),
        nowSeconds: Long = Clock.System.now().epochSeconds,
    ): FollowDiffResult {
        val followerNames = snapshot.followers.mapTo(HashSet()) { it.username }
        val followingNames = snapshot.following.mapTo(HashSet()) { it.username }

        fun notWhitelisted(a: Account) = a.username !in whitelist

        val notFollowingBack = snapshot.following
            .filter { it.username !in followerNames && notWhitelisted(it) && it.username !in dismissed }
            .sortedBy { it.username }

        val fans = snapshot.followers
            .filter { it.username !in followingNames && notWhitelisted(it) }
            .sortedBy { it.username }

        val mutuals = snapshot.following
            .filter { it.username in followerNames }
            .sortedBy { it.username }

        val possiblyLimitedRange = looksRangeLimited(snapshot.following, nowSeconds) ||
            looksRangeLimited(snapshot.followers, nowSeconds)

        return FollowDiffResult(notFollowingBack, fans, mutuals, possiblyLimitedRange)
    }

    /**
     * True when [accounts] has enough timestamp coverage to trust, and even the oldest of those
     * timestamps is more recent than [SUSPICIOUS_RANGE_DAYS] days ago — i.e. it looks like the
     * export was requested with a date range shorter than "All time" rather than the account
     * genuinely being brand new.
     */
    private fun looksRangeLimited(accounts: Set<Account>, nowSeconds: Long): Boolean {
        if (accounts.isEmpty()) return false
        val timestamps = accounts.mapNotNull { it.timestampSeconds }
        if (timestamps.size < accounts.size * MIN_TIMESTAMP_COVERAGE) return false
        val oldestAgeDays = (nowSeconds - timestamps.min()) / SECONDS_PER_DAY
        return oldestAgeDays < SUSPICIOUS_RANGE_DAYS
    }

    /** Compare two snapshots taken at different times. [previous] is the older one. */
    fun delta(previous: ExportSnapshot, current: ExportSnapshot): SnapshotDelta {
        val prevFollowers = previous.followers.mapTo(HashSet()) { it.username }
        val curFollowers = current.followers.mapTo(HashSet()) { it.username }
        val prevFollowing = previous.following.mapTo(HashSet()) { it.username }
        val curFollowing = current.following.mapTo(HashSet()) { it.username }

        return SnapshotDelta(
            newFollowers = current.followers.filter { it.username !in prevFollowers }.sortedBy { it.username },
            lostFollowers = previous.followers.filter { it.username !in curFollowers }.sortedBy { it.username },
            newlyFollowed = current.following.filter { it.username !in prevFollowing }.sortedBy { it.username },
            youUnfollowed = previous.following.filter { it.username !in curFollowing }.sortedBy { it.username },
        )
    }
}
