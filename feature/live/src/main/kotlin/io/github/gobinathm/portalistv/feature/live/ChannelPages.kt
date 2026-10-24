package io.github.gobinathm.portalistv.feature.live

import io.github.gobinathm.portalistv.core.model.LiveChannel

/**
 * The channels of one category loaded so far, in portal order. Pages are appended as the list is
 * scrolled; [pageOf] tells the player which page a channel came from so zapping continues there.
 */
data class ChannelPages(
    val categoryId: String,
    val channels: List<LiveChannel> = emptyList(),
    /** Page of each entry in [channels]. */
    private val pages: List<Int> = emptyList(),
    val loadedPages: Int = 0,
    val pageCount: Int = 1,
    val totalItems: Int = 0,
) {
    val complete: Boolean get() = loadedPages >= pageCount

    val nextPage: Int get() = loadedPages + 1

    fun append(page: Int, items: List<LiveChannel>, pageCount: Int, totalItems: Int): ChannelPages {
        if (page != nextPage) return this
        // Portals sometimes repeat a channel across a page boundary; keys must stay unique.
        val known = channels.mapTo(HashSet()) { it.id }
        val fresh = items.filter { known.add(it.id) }
        return copy(
            channels = channels + fresh,
            pages = pages + List(fresh.size) { page },
            loadedPages = page,
            pageCount = pageCount.coerceAtLeast(1),
            totalItems = totalItems,
        )
    }

    fun pageOf(channelId: String): Int {
        val index = channels.indexOfFirst { it.id == channelId }
        return if (index < 0) 1 else pages[index]
    }

    /** Whether the next page should load, given the last list position on screen. */
    fun needsMore(lastVisibleIndex: Int): Boolean = !complete && lastVisibleIndex >= channels.size - PREFETCH_DISTANCE

    companion object {
        /** About two portal pages ahead, so scrolling rarely reaches the end of what's loaded. */
        const val PREFETCH_DISTANCE = 30
    }
}
