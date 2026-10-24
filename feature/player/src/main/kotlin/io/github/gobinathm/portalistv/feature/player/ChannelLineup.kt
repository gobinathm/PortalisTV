package io.github.gobinathm.portalistv.feature.player

import io.github.gobinathm.portalistv.core.model.ContentSource
import io.github.gobinathm.portalistv.core.model.LiveChannel

/**
 * The channels of one category, in order, loaded a page at a time as the user zaps. Loaded pages
 * stay contiguous; zapping past the last channel wraps to the first. Adult channels are skipped
 * unless [showAdult].
 */
internal class ChannelLineup(
    private val source: ContentSource,
    private val categoryId: String,
    private val showAdult: Boolean = true,
) {
    private val pages = sortedMapOf<Int, List<LiveChannel>>()
    private var pageCount = 1

    /** Loads [page] (and remembers the page count) unless it is already loaded. */
    suspend fun load(page: Int): List<LiveChannel> {
        pages[page]?.let { return it }
        val result = source.liveChannels(categoryId, page)
        pageCount = result.pageCount.coerceAtLeast(1)
        return result.items.filter { showAdult || !it.adult }.also { pages[page] = it }
    }

    /** The channel [step] places away from [channel] (+1 next, -1 previous), or null if none. */
    suspend fun neighbour(channel: LiveChannel, step: Int): LiveChannel? {
        val (page, index) = find(channel) ?: return null
        val items = pages.getValue(page)
        val target = index + step
        return when {
            target in items.indices -> items[target]
            step > 0 -> firstFrom(page, forward = true)
            else -> firstFrom(page, forward = false)
        }
    }

    private fun find(channel: LiveChannel): Pair<Int, Int>? = pages.entries.firstNotNullOfOrNull { (page, items) ->
        items.indexOfFirst { it.id == channel.id }.takeIf { it >= 0 }?.let { page to it }
    }

    /** The nearest channel on the pages after (or before) [page], wrapping; filtered pages can be empty. */
    private suspend fun firstFrom(page: Int, forward: Boolean): LiveChannel? {
        var current = page
        repeat(pageCount) {
            current = when {
                forward -> if (current >= pageCount) 1 else current + 1
                else -> if (current <= 1) pageCount else current - 1
            }
            val items = load(current)
            (if (forward) items.firstOrNull() else items.lastOrNull())?.let { return it }
        }
        return null
    }
}
