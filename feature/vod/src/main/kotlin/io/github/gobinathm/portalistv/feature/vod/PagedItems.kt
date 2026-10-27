package io.github.gobinathm.portalistv.feature.vod

/** The items of one category loaded so far, in portal order; pages are appended as the grid scrolls. */
data class PagedItems<T>(
    val categoryId: String,
    val items: List<T> = emptyList(),
    val loadedPages: Int = 0,
    val pageCount: Int = 1,
    val totalItems: Int = 0,
) {
    val complete: Boolean get() = loadedPages >= pageCount

    val nextPage: Int get() = loadedPages + 1

    /** Adds [page] if it is the next one, dropping items already listed (keys must stay unique). */
    fun append(page: Int, newItems: List<T>, pageCount: Int, totalItems: Int, id: (T) -> String): PagedItems<T> {
        if (page != nextPage) return this
        val known = items.mapTo(HashSet(), id)
        return copy(
            items = items + newItems.filter { known.add(id(it)) },
            loadedPages = page,
            pageCount = pageCount.coerceAtLeast(1),
            totalItems = totalItems,
        )
    }

    /** Whether the next page should load, given the last grid position on screen. */
    fun needsMore(lastVisibleIndex: Int): Boolean = !complete && lastVisibleIndex >= items.size - PREFETCH_DISTANCE

    companion object {
        /**
         * About five rows of posters ahead: loads fetch a few pages at a time, so they start early
         * enough that scrolling rarely reaches the end of what's loaded.
         */
        const val PREFETCH_DISTANCE = 42
    }
}
