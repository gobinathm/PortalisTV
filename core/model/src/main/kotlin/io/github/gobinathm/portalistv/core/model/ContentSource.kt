package io.github.gobinathm.portalistv.core.model

/**
 * Where a profile's content comes from. Screens use only this interface, so another kind of
 * source (M3U, Xtream) can be added without touching them. Every call may throw
 * `java.io.IOException` for network and portal errors.
 */
interface ContentSource {
    /** User agent the player should send with stream requests; some providers check it. */
    val playbackUserAgent: String?

    /** Opens or renews the session. */
    suspend fun connect(): SourceAccount

    suspend fun liveCategories(): List<LiveCategory>

    /** One page of a category's channels. Pages start at 1. */
    suspend fun liveChannels(categoryId: String, page: Int): PagedResult<LiveChannel>

    /** What's on [channel] now and next, oldest first. Empty when the source has no guide for it. */
    suspend fun nowNext(channel: LiveChannel): List<Programme> = emptyList()

    /** A URL the player can open. Resolve it right before playback; links often expire. */
    suspend fun streamUrl(channel: LiveChannel): String
}

data class SourceAccount(val displayName: String?, val message: String?)

data class LiveCategory(
    val id: String,
    val title: String,
    /** Adult content, hidden behind the parental lock by default. */
    val adult: Boolean,
)

data class LiveChannel(
    val id: String,
    val name: String,
    val number: Int?,
    val logoUrl: String?,
    val categoryId: String?,
    val adult: Boolean,
    /** Opaque to the app; only the source that produced the channel understands it. */
    val playbackRef: String,
)

/** A guide entry. */
data class Programme(
    val title: String,
    val description: String?,
    val startMillis: Long,
    val endMillis: Long,
) {
    /** How far through the programme [nowMillis] is, from 0 to 1. */
    fun progress(nowMillis: Long): Float =
        ((nowMillis - startMillis).toFloat() / (endMillis - startMillis)).coerceIn(0f, 1f)
}

data class NowNext(val now: Programme?, val next: Programme?)

/** The programme on at [nowMillis] and the one after it, from a channel's guide entries. */
fun List<Programme>.nowNext(nowMillis: Long): NowNext {
    val upcoming = filter { it.endMillis > nowMillis }.sortedBy { it.startMillis }
    val now = upcoming.firstOrNull()?.takeIf { it.startMillis <= nowMillis }
    val next = upcoming.firstOrNull { it !== now }
    return NowNext(now, next)
}

data class PagedResult<T>(
    val items: List<T>,
    val totalItems: Int,
    val pageCount: Int,
)

/** The category id that lists every live channel. */
const val ALL_CATEGORIES = "*"

/** The category id of the profile's favorite channels, kept on the device rather than the portal. */
const val FAVORITES_CATEGORY = "portalis:favorites"
