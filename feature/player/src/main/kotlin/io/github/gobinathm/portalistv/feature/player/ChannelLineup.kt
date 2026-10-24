package io.github.gobinathm.portalistv.feature.player

import io.github.gobinathm.portalistv.core.model.ContentSource
import io.github.gobinathm.portalistv.core.model.LiveChannel
import io.github.gobinathm.portalistv.core.model.PagedResult

/**
 * The channels of one category, in order, loaded a page at a time as the user zaps. Loaded pages
 * stay contiguous; zapping past the last channel wraps to the first. Adult channels are skipped
 * unless [showAdult].
 */
internal class ChannelLineup(
    private val fetch: suspend (page: Int) -> PagedResult<LiveChannel>,
    private val showAdult: Boolean = true,
) {
    constructor(source: ContentSource, categoryId: String, showAdult: Boolean = true) :
        this({ page -> source.liveChannels(categoryId, page) }, showAdult)

    private val pages = sortedMapOf<Int, List<LiveChannel>>()

    /** Channel numbers each loaded page spans, adult channels included, for [findNumber]. */
    private val numberRanges = mutableMapOf<Int, IntRange>()
    private var pageCount = 1

    /** Loads [page] (and remembers the page count) unless it is already loaded. */
    suspend fun load(page: Int): List<LiveChannel> {
        pages[page]?.let { return it }
        val result = fetch(page)
        pageCount = result.pageCount.coerceAtLeast(1)
        val numbers = result.items.mapNotNull { it.number }
        if (numbers.isNotEmpty()) numberRanges[page] = numbers.min()..numbers.max()
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

    /**
     * The channel numbered [number], or null. Portals list channels in number order, so the page
     * is estimated from the numbers on a loaded page and refined from each page fetched; a few
     * requests find a channel even among thousands.
     */
    suspend fun findNumber(number: Int): LiveChannel? {
        val tried = mutableSetOf<Int>()
        while (true) {
            pages.values.firstNotNullOfOrNull { items -> items.firstOrNull { it.number == number } }?.let { return it }
            val page = estimatePage(number)?.takeIf { tried.size < MAX_NUMBER_PROBES && tried.add(it) } ?: return null
            load(page)
        }
    }

    private fun estimatePage(number: Int): Int? {
        // The closest loaded page tells the most about numbering around the target.
        val (page, range) = numberRanges.minByOrNull { (_, range) -> distance(number, range) }
            // Within a loaded page, yet not on it: no such channel.
            ?.takeUnless { (_, range) -> number in range }
            ?: return null
        val span = (range.last - range.first + 1).coerceAtLeast(1)
        val offset = when {
            number > range.last -> (number - range.last - 1) / span + 1
            else -> -((range.first - number - 1) / span + 1)
        }
        return (page + offset).coerceIn(1, pageCount)
    }

    private fun distance(number: Int, range: IntRange) = when {
        number < range.first -> range.first - number
        number > range.last -> number - range.last
        else -> 0
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

    private companion object {
        const val MAX_NUMBER_PROBES = 8
    }
}
