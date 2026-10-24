package io.github.gobinathm.portalistv.feature.player

import com.google.common.truth.Truth.assertThat
import io.github.gobinathm.portalistv.core.model.ContentSource
import io.github.gobinathm.portalistv.core.model.LiveCategory
import io.github.gobinathm.portalistv.core.model.LiveChannel
import io.github.gobinathm.portalistv.core.model.PagedResult
import io.github.gobinathm.portalistv.core.model.SourceAccount
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ChannelLineupTest {
    /** 7 channels, 3 per page: pages [1,2,3] [4,5,6] [7]. */
    private val source = PagedSource(total = 7, pageSize = 3)
    private val lineup = ChannelLineup(source, "*")

    private suspend fun ChannelLineup.next(id: Int, step: Int = 1) =
        neighbour(channel(id), step)?.id?.toInt()

    @Test
    fun `moves within a page without loading`() = runTest {
        lineup.load(1)
        assertThat(lineup.next(1)).isEqualTo(2)
        assertThat(source.loads).containsExactly(1)
    }

    @Test
    fun `loads the next page at the end of a page`() = runTest {
        lineup.load(1)
        assertThat(lineup.next(3)).isEqualTo(4)
        assertThat(source.loads).containsExactly(1, 2).inOrder()
    }

    @Test
    fun `loads the previous page at the start of a page`() = runTest {
        lineup.load(2)
        assertThat(lineup.next(4, step = -1)).isEqualTo(3)
    }

    @Test
    fun `wraps from the last channel to the first and back`() = runTest {
        lineup.load(3)
        assertThat(lineup.next(7)).isEqualTo(1)
        lineup.load(1)
        assertThat(lineup.next(1, step = -1)).isEqualTo(7)
    }

    @Test
    fun `does not reload pages`() = runTest {
        lineup.load(1)
        lineup.next(3)
        lineup.next(4, step = -1)
        assertThat(source.loads).containsExactly(1, 2).inOrder()
    }

    @Test
    fun `formats playback time`() {
        assertThat(formatTime(65_000)).isEqualTo("1:05")
        assertThat(formatTime(3_725_000)).isEqualTo("1:02:05")
    }
}

private fun channel(id: Int) =
    LiveChannel(id.toString(), "Channel $id", id, null, null, adult = false, playbackRef = "$id")

private class PagedSource(private val total: Int, private val pageSize: Int) : ContentSource {
    val loads = mutableListOf<Int>()
    override val playbackUserAgent: String? = null

    override suspend fun connect() = SourceAccount(null, null)

    override suspend fun liveCategories() = emptyList<LiveCategory>()

    override suspend fun liveChannels(categoryId: String, page: Int): PagedResult<LiveChannel> {
        loads += page
        val ids = (1..total).drop((page - 1) * pageSize).take(pageSize)
        return PagedResult(ids.map(::channel), total, (total + pageSize - 1) / pageSize)
    }

    override suspend fun streamUrl(channel: LiveChannel) = "http://streams.invalid/${channel.id}"
}
