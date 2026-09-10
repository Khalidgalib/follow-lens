package app.followlens.diff

/**
 * Pure set math over a parsed [ExportSnapshot]. No I/O, no platform APIs — this is the code that
 * runs on-device in v1 and, unchanged, server-side when the online backend is added.
 */
object FollowDiff {

    /**
     * @param whitelist usernames the user never wants flagged (stored lower-cased). Removed from
     *   [FollowDiffResult.notFollowingBack] and [FollowDiffResult.fans] only.
     */
    fun compare(snapshot: ExportSnapshot, whitelist: Set<String> = emptySet()): FollowDiffResult {
        val followerNames = snapshot.followers.mapTo(HashSet()) { it.username }
        val followingNames = snapshot.following.mapTo(HashSet()) { it.username }

        fun notWhitelisted(a: Account) = a.username !in whitelist

        val notFollowingBack = snapshot.following
            .filter { it.username !in followerNames && notWhitelisted(it) }
            .sortedBy { it.username }

        val fans = snapshot.followers
            .filter { it.username !in followingNames && notWhitelisted(it) }
            .sortedBy { it.username }

        val mutuals = snapshot.following
            .filter { it.username in followerNames }
            .sortedBy { it.username }

        return FollowDiffResult(notFollowingBack, fans, mutuals)
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
