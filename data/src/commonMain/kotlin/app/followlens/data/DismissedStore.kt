package app.followlens.data

import app.followlens.data.db.FollowLensDb

/**
 * Accounts the user marked "handled" on the not-following-back list — distinct from
 * [WhitelistStore]: this says "I already acted on this specific flag", not "never flag this
 * account". Usernames are stored lower-cased to match [app.followlens.diff.Account.username].
 */
class DismissedStore(private val db: FollowLensDb) {

    private val q get() = db.followLensQueries

    fun all(): Set<String> = q.allDismissedUsernames().executeAsList().toSet()

    fun add(username: String, dismissedAtSeconds: Long) = db.transaction {
        val key = username.trim().lowercase()
        q.upsertAccount(key)
        val accountId = q.accountIdByUsername(key).executeAsOne()
        q.addDismissed(accountId, dismissedAtSeconds)
    }

    fun remove(username: String) {
        val accountId = q.accountIdByUsername(username.trim().lowercase())
            .executeAsOneOrNull() ?: return
        q.removeDismissed(accountId)
    }
}
