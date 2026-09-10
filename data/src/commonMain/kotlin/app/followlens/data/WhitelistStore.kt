package app.followlens.data

import app.followlens.data.db.FollowLensDb
import app.followlens.diff.Account

/**
 * The "never flag this account" list. Usernames are stored lower-cased so lookups match
 * [Account.username].
 */
class WhitelistStore(private val db: FollowLensDb) {

    private val q get() = db.followLensQueries

    fun all(): Set<String> = q.allWhitelistUsernames().executeAsList().toSet()

    fun add(username: String, addedAtSeconds: Long) = db.transaction {
        val key = username.trim().lowercase()
        q.upsertAccount(key)
        val accountId = q.accountIdByUsername(key).executeAsOne()
        q.addToWhitelist(accountId, addedAtSeconds)
    }

    fun remove(username: String) {
        val accountId = q.accountIdByUsername(username.trim().lowercase())
            .executeAsOneOrNull() ?: return
        q.removeFromWhitelist(accountId)
    }
}
