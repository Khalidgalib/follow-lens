package app.followlens.diff

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Parses the JSON files from Instagram's "Download your information" export.
 *
 * Layout inside the ZIP: `connections/followers_and_following/`
 *   - `followers_1.json` (and `_2`, ...) — a TOP-LEVEL JSON ARRAY of entries
 *   - `following.json` — an OBJECT with key `relationships_following` -> array of entries
 *
 * Each entry, in `followers_1.json`:
 * ```
 * { "title": "", "string_list_data": [
 *     { "href": "https://www.instagram.com/USERNAME", "value": "USERNAME", "timestamp": 1700000000 }
 * ]}
 * ```
 * `following.json` entries carry the username differently — `string_list_data[0]` has no `value`,
 * `href` is shaped `.../_u/USERNAME`, and the username is instead the entry's own `title`:
 * ```
 * { "title": "USERNAME", "string_list_data": [
 *     { "href": "https://www.instagram.com/_u/USERNAME", "timestamp": 1700000000 }
 * ]}
 * ```
 * [collectEntries] tries `value`, then `title`, then parses `href` as a last resort, so both
 * shapes (and any file that mixes them) work.
 *
 * The caller is responsible for unzipping and locating the files (platform-specific). This class
 * only turns raw JSON text into [Account] sets.
 */
class ExportParser {

    // Kept private so core-diff's choice of JSON library stays out of its public API.
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true }

    class ParseException(message: String) : Exception(message)

    /**
     * Parse one or more `followers_N.json` files. Pass every part; they are merged.
     * Accepts either a bare array (the normal shape) or an object wrapping
     * `relationships_followers` (seen in some exports).
     */
    fun parseFollowers(vararg followerFileContents: String): Set<Account> {
        if (followerFileContents.isEmpty()) throw ParseException("no followers file provided")
        val out = LinkedHashMap<String, Account>()
        followerFileContents.forEachIndexed { i, content ->
            val root = parseRoot(content, "followers file #${i + 1}")
            val entries = when (root) {
                is JsonArray -> root
                is JsonObject -> root["relationships_followers"]?.jsonArray
                    ?: throw ParseException("followers file #${i + 1}: object without 'relationships_followers'")
                else -> throw ParseException("followers file #${i + 1}: unexpected top-level JSON")
            }
            collectEntries(entries, out)
        }
        return out.values.toSet()
    }

    /** Parse `following.json`. Accepts the standard `relationships_following` wrapper or a bare array. */
    fun parseFollowing(followingFileContent: String): Set<Account> {
        val root = parseRoot(followingFileContent, "following file")
        val entries = when (root) {
            is JsonArray -> root
            is JsonObject -> root["relationships_following"]?.jsonArray
                ?: throw ParseException("following file: object without 'relationships_following'")
            else -> throw ParseException("following file: unexpected top-level JSON")
        }
        val out = LinkedHashMap<String, Account>()
        collectEntries(entries, out)
        return out.values.toSet()
    }

    fun parseSnapshot(followingFileContent: String, vararg followerFileContents: String): ExportSnapshot =
        ExportSnapshot(
            followers = parseFollowers(*followerFileContents),
            following = parseFollowing(followingFileContent),
        )

    private fun parseRoot(content: String, label: String): JsonElement {
        if (content.isBlank()) throw ParseException("$label is empty")
        return try {
            json.parseToJsonElement(content)
        } catch (e: Exception) {
            throw ParseException("$label is not valid JSON: ${e.message}")
        }
    }

    private fun collectEntries(entries: JsonArray, out: MutableMap<String, Account>) {
        for (entry in entries) {
            val entryObject = entry.jsonObject
            val sld = entryObject["string_list_data"]?.jsonArray ?: continue
            val first = sld.firstOrNull()?.jsonObject ?: continue

            // followers_1.json puts the username in string_list_data[0].value. following.json
            // (as of the 2026 export format) omits "value" entirely and puts it in the entry's
            // own "title" instead, with href shaped ".../_u/USERNAME" rather than ".../USERNAME".
            // Deactivated/deleted accounts have none of these — skip them.
            val rawValue = first["value"]?.jsonPrimitive?.contentOrNull()?.takeIf { it.isNotBlank() }
                ?: entryObject["title"]?.jsonPrimitive?.contentOrNull()?.takeIf { it.isNotBlank() }
                ?: usernameFromHref(first["href"]?.jsonPrimitive?.contentOrNull())
                ?: continue

            val ts = first["timestamp"]?.jsonPrimitive?.longOrNull
            val account = Account.of(rawValue, ts?.takeIf { it > 0 })
            // de-dupe, keep first occurrence (putIfAbsent is JVM-only, so do it by hand)
            if (account.username !in out) out[account.username] = account
        }
    }

    private fun usernameFromHref(href: String?): String? {
        if (href.isNullOrBlank()) return null
        return href.substringAfter("instagram.com/", "")
            .trim('/')
            .removePrefix("_u/")
            .trim('/')
            .ifBlank { null }
    }

    private fun kotlinx.serialization.json.JsonPrimitive.contentOrNull(): String? =
        if (isString) content else content.ifBlank { null }
}
