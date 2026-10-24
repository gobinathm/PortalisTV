package io.github.gobinathm.portalistv.feature.live

import io.github.gobinathm.portalistv.core.model.Programme

/**
 * Now/next entries per channel id, and when each should be fetched again: once what's on has
 * ended, or a while after the portal said it has no guide for the channel.
 */
internal class GuideCache {
    private class Entry(val programmes: List<Programme>, val fetchedAtMillis: Long)

    private val entries = mutableMapOf<String, Entry>()
    private val inFlight = mutableSetOf<String>()

    /** Marks [channelId] as being fetched if it needs it. @return whether the caller should fetch it. */
    fun claim(channelId: String, nowMillis: Long): Boolean {
        if (channelId in inFlight) return false
        val entry = entries[channelId]
        val stale = when {
            entry == null -> true
            entry.programmes.isEmpty() -> nowMillis - entry.fetchedAtMillis >= EMPTY_RETRY_MS
            // Now/next needs two programmes that haven't ended; refetch once fewer are left.
            else ->
                entry.programmes.count { it.endMillis > nowMillis } < 2 &&
                    nowMillis - entry.fetchedAtMillis >= MIN_REFETCH_MS
        }
        if (stale) inFlight += channelId
        return stale
    }

    fun store(channelId: String, programmes: List<Programme>, nowMillis: Long) {
        inFlight -= channelId
        entries[channelId] = Entry(programmes, nowMillis)
    }

    /** A fetch that was cancelled, so the channel can be claimed again. */
    fun release(channelId: String) {
        inFlight -= channelId
    }

    /** A failed fetch is retried like an empty guide, so a broken portal isn't asked on every scroll. */
    fun failed(channelId: String, nowMillis: Long) = store(channelId, emptyList(), nowMillis)

    fun snapshot(): Map<String, List<Programme>> = entries.mapValues { it.value.programmes }

    companion object {
        const val EMPTY_RETRY_MS = 15 * 60 * 1000L
        const val MIN_REFETCH_MS = 60 * 1000L
    }
}
