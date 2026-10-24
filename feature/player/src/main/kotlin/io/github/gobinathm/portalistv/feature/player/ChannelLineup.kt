package io.github.gobinathm.portalistv.feature.player

import io.github.gobinathm.portalistv.core.model.ContentSource
import io.github.gobinathm.portalistv.core.model.LiveChannel

/**
 * The channels of one category, in order, loaded a page at a time as the user zaps. Loaded pages
 * stay contiguous; zapping past the last channel wraps to the first.
 */
internal class ChannelLineup(private val source: ContentSource, private val categoryId: String) {
    private val pages = sortedMapOf<Int, List<LiveChannel>>()
    private var pageCount = 1

    /** Loads [page] (and remembers the page count) unless it is already loaded. */
    suspend fun load(page: Int): List<LiveChannel> {
        pages[page]?.let { return it }
        val result = source.liveChannels(categoryId, page)
        pageCount = result.pageCount.coerceAtLeast(1)
        return result.items.also { pages[page] = it }
    }

    /** The channel [step] places away from [channel] (+1 next, -1 previous), or null if none. */
    suspend fun neighbour(channel: LiveChannel, step: Int): LiveChannel? {
        val (page, index) = find(channel) ?: return null
        val items = pages.getValue(page)
        val target = index + step
        return when {
            target in items.indices -> items[target]
            step > 0 -> firstOf(if (page >= pageCount) 1 else page + 1)
            else -> lastOf(if (page <= 1) pageCount else page - 1)
        }
    }

    private fun find(channel: LiveChannel): Pair<Int, Int>? = pages.entries.firstNotNullOfOrNull { (page, items) ->
        items.indexOfFirst { it.id == channel.id }.takeIf { it >= 0 }?.let { page to it }
    }

    private suspend fun firstOf(page: Int) = load(page).firstOrNull()

    private suspend fun lastOf(page: Int) = load(page).lastOrNull()
}
