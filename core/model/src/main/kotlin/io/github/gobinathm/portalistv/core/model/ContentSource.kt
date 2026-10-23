package io.github.gobinathm.portalistv.core.model

/**
 * Where a profile's content comes from. Screens use only this interface, so another kind of
 * source (M3U, Xtream) can be added without touching them. Every call may throw
 * `java.io.IOException` for network and portal errors.
 */
interface ContentSource {
    /** Opens or renews the session. */
    suspend fun connect(): SourceAccount

    suspend fun liveCategories(): List<LiveCategory>

    /** One page of a category's channels. Pages start at 1. */
    suspend fun liveChannels(categoryId: String, page: Int): PagedResult<LiveChannel>

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

data class PagedResult<T>(
    val items: List<T>,
    val totalItems: Int,
    val pageCount: Int,
)

/** The category id that lists every live channel. */
const val ALL_CATEGORIES = "*"
