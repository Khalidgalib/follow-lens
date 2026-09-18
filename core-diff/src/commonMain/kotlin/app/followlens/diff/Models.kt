package app.followlens.diff

/**
 * One Instagram account as it appears in an export file.
 *
 * [username] is stored lower-cased so set math is case-insensitive (Instagram usernames are
 * case-insensitive). [timestampSeconds] is the epoch-seconds Instagram recorded for the
 * relationship (when available) — used to show "following since" / "follower since".
 */
data class Account(
    val username: String,
    val timestampSeconds: Long? = null,
) {
    init {
        require(username.isNotBlank()) { "username must not be blank" }
    }

    companion object {
        fun of(rawUsername: String, timestampSeconds: Long? = null): Account =
            Account(rawUsername.trim().lowercase(), timestampSeconds)
    }
}

/** The parsed contents of one full export (one point in time). */
data class ExportSnapshot(
    val followers: Set<Account>,
    val following: Set<Account>,
    /** epoch-seconds the export was taken, if derivable from the file; else null. */
    val exportedAtSeconds: Long? = null,
)

/** Result of comparing followers vs following for a single snapshot. */
data class FollowDiffResult(
    /** You follow them, they do NOT follow you back. The headline list. */
    val notFollowingBack: List<Account>,
    /** They follow you, you do NOT follow them back. */
    val fans: List<Account>,
    /** Mutual follows. */
    val mutuals: List<Account>,
    /**
     * Best-effort signal that this export was requested with a date range shorter than "All
     * time" (Instagram defaults to "Last Year"), which silently omits older relationships from
     * the file and can make real followers wrongly appear in [notFollowingBack]. See
     * [FollowDiff.looksRangeLimited].
     */
    val possiblyLimitedRange: Boolean = false,
)

/** Change between a previous snapshot and the current one. */
data class SnapshotDelta(
    val newFollowers: List<Account>,      // started following you
    val lostFollowers: List<Account>,     // unfollowed you
    val newlyFollowed: List<Account>,     // you started following
    val youUnfollowed: List<Account>,     // you unfollowed
)
