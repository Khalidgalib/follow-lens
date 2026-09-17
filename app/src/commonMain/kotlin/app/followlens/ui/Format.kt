package app.followlens.ui

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val months = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

/** e.g. "Sep 14, 2026". */
fun formatDate(epochSeconds: Long): String {
    val date = Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "${months[date.monthNumber - 1]} ${date.dayOfMonth}, ${date.year}"
}

/** e.g. "MAR–SEP 2026" — used as the trend chart's date-range subtitle. */
fun formatMonthRange(fromEpochSeconds: Long, toEpochSeconds: Long): String {
    val from = Instant.fromEpochSeconds(fromEpochSeconds).toLocalDateTime(TimeZone.currentSystemDefault()).date
    val to = Instant.fromEpochSeconds(toEpochSeconds).toLocalDateTime(TimeZone.currentSystemDefault()).date
    val fromMonth = months[from.monthNumber - 1].uppercase()
    val toMonth = months[to.monthNumber - 1].uppercase()
    return if (from.year == to.year) "$fromMonth–$toMonth ${to.year}" else "$fromMonth ${from.year}–$toMonth ${to.year}"
}

fun sinceLabel(timestampSeconds: Long?): String? =
    timestampSeconds?.let { "First seen ${formatDate(it)}" }
