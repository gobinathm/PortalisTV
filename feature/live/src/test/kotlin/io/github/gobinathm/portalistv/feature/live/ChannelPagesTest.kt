package io.github.gobinathm.portalistv.feature.live

import com.google.common.truth.Truth.assertThat
import io.github.gobinathm.portalistv.core.model.LiveChannel
import org.junit.Test

class ChannelPagesTest {
    private fun channel(id: Int) = LiveChannel("$id", "Channel $id", id, null, null, adult = false, playbackRef = "$id")

    private val first = ChannelPages("*").append(1, (1..3).map(::channel), pageCount = 3, totalItems = 8)

    @Test
    fun `appends pages in order and records each channel's page`() {
        val two = first.append(2, (4..6).map(::channel), pageCount = 3, totalItems = 8)
        assertThat(two.channels.map { it.id }).containsExactly("1", "2", "3", "4", "5", "6").inOrder()
        assertThat(two.pageOf("5")).isEqualTo(2)
        assertThat(two.pageOf("1")).isEqualTo(1)
        assertThat(two.nextPage).isEqualTo(3)
        assertThat(two.complete).isFalse()
    }

    @Test
    fun `ignores a page that arrives out of order`() {
        assertThat(first.append(3, listOf(channel(9)), 3, 8)).isEqualTo(first)
    }

    @Test
    fun `drops channels repeated across a page boundary`() {
        val two = first.append(2, listOf(channel(3), channel(4)), 3, 8)
        assertThat(two.channels.map { it.id }).containsExactly("1", "2", "3", "4").inOrder()
    }

    @Test
    fun `asks for more near the end until complete`() {
        assertThat(first.needsMore(lastVisibleIndex = 0)).isTrue()
        val done = first.append(2, emptyList(), 2, 3)
        assertThat(done.complete).isTrue()
        assertThat(done.needsMore(lastVisibleIndex = 2)).isFalse()
    }
}
