package io.github.gobinathm.portalistv.core.model

import java.io.IOException

/**
 * Where a profile's content comes from. Screens use only this interface, so another kind of
 * source (M3U, Xtream) can be added without touching them. Every call may throw
 * `java.io.IOException` for network and portal errors.
 */
@Suppress("TooManyFunctions") // One call per kind of content; sources implement what they have.
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

    /** The programmes on [channel] from now on, as far ahead as the source tells (often most of a day). */
    suspend fun guide(channel: LiveChannel): List<Programme> = nowNext(channel)

    /** A URL the player can open. Resolve it right before playback; links often expire. */
    suspend fun streamUrl(channel: LiveChannel): String

    /** On-demand categories; empty when the source has no on-demand content. */
    suspend fun vodCategories(): List<VodCategory> = emptyList()

    /** One page of an on-demand category, newest first, narrowed by [filter]. Pages start at 1. */
    suspend fun vodItems(categoryId: String, page: Int, filter: VodFilter = VodFilter()): PagedResult<VodItem> =
        PagedResult(emptyList(), 0, 1)

    /** The years an on-demand category can be filtered by; empty when the source can't filter by year. */
    suspend fun vodYears(categoryId: String): List<VodYear> = emptyList()

    /** A series' seasons, in order. */
    suspend fun seasons(series: VodItem): List<Season> = emptyList()

    /** A season's episodes, in order. */
    suspend fun episodes(series: VodItem, season: Season): List<Episode> = emptyList()

    /** A URL for a movie, or for [episode] of a series. Resolve it right before playback. */
    suspend fun vodStreamUrl(item: VodPlayback): String = throw IOException("No on-demand content")
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

data class VodCategory(val id: String, val title: String, val adult: Boolean)

/** A movie or a series in an on-demand list. */
data class VodItem(
    val id: String,
    val title: String,
    val description: String?,
    val posterUrl: String?,
    val year: String?,
    val rating: String?,
    val genres: String?,
    val director: String?,
    val actors: String?,
    val durationMinutes: Int?,
    val isSeries: Boolean,
    val adult: Boolean,
    /** Opaque to the app; only the source that produced the item understands it. */
    val playbackRef: String?,
)

/**
 * Narrows an on-demand list: [search] matches titles (and, on most portals, actors); [yearId] is
 * a [VodYear.id]. Both empty lists everything.
 */
data class VodFilter(val search: String = "", val yearId: String? = null) {
    val isEmpty: Boolean get() = search.isBlank() && yearId == null
}

/** A year (or range of years) an on-demand list can be filtered by. */
data class VodYear(val id: String, val title: String)

data class Season(val id: String, val number: Int?, val title: String)

data class Episode(val id: String, val number: Int?, val title: String)

/**
 * What to play from on-demand: a movie (no season or episode), or an episode of a series. Small
 * and serializable, so it can travel in navigation arguments and be stored for resuming.
 */
data class VodPlayback(
    val itemId: String,
    val seasonId: String? = null,
    val episodeId: String? = null,
    val playbackRef: String? = null,
)

/** How far a movie or episode has been watched, for resuming and the Continue Watching row. */
data class WatchProgress(
    val playback: VodPlayback,
    /** The movie or series title. */
    val title: String,
    /** For an episode, e.g. "S1 E3 · Pilot". */
    val subtitle: String?,
    val posterUrl: String?,
    val isSeries: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtMillis: Long,
) {
    /** Near enough to the end that the credits are probably all that's left. */
    val finished: Boolean get() = durationMs > 0 && positionMs >= durationMs * FINISHED_FRACTION

    val fraction: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    /** Where to start playing: the saved position, or the start once finished. */
    val resumeMs: Long get() = if (finished) 0 else positionMs

    private companion object {
        const val FINISHED_FRACTION = 0.95
    }
}

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
