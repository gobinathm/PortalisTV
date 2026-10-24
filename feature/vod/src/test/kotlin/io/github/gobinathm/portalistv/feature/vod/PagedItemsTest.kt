package io.github.gobinathm.portalistv.feature.vod

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PagedItemsTest {
    private val first = PagedItems<String>("1").append(1, listOf("a", "b"), pageCount = 2, totalItems = 4) { it }

    @Test
    fun `appends the next page and drops repeats`() {
        val two = first.append(2, listOf("b", "c"), 2, 4) { it }
        assertThat(two.items).containsExactly("a", "b", "c").inOrder()
        assertThat(two.complete).isTrue()
    }

    @Test
    fun `ignores pages out of order`() {
        assertThat(first.append(3, listOf("z"), 3, 5) { it }).isEqualTo(first)
    }

    @Test
    fun `formats resume times`() {
        assertThat(formatTime(65_000)).isEqualTo("1:05")
        assertThat(formatTime(3_725_000)).isEqualTo("1:02:05")
    }
}
