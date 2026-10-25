package io.github.gobinathm.portalistv.feature.live

import com.google.common.truth.Truth.assertThat
import io.github.gobinathm.portalistv.core.model.Programme
import org.junit.Test

class GuideLayoutTest {
    private val minute = 60_000L
    private fun show(start: Long, end: Long) = Programme("Show $start", null, start * minute, end * minute)

    @Test
    fun `clips programmes to the range and fills gaps`() {
        val blocks = guideBlocks(listOf(show(90, 150), show(0, 60)), fromMillis = 30 * minute, toMillis = 180 * minute)
        assertThat(blocks.map { it.startMillis / minute to it.endMillis / minute })
            .containsExactly(30L to 60L, 60L to 90L, 90L to 150L, 150L to 180L).inOrder()
        assertThat(blocks.map { it.programme != null }).containsExactly(true, false, true, false).inOrder()
    }

    @Test
    fun `a channel without a guide is empty half hours`() {
        assertThat(guideBlocks(emptyList(), 10 * minute, 70 * minute)).containsExactly(
            GuideBlock(null, 10 * minute, 30 * minute),
            GuideBlock(null, 30 * minute, 60 * minute),
            GuideBlock(null, 60 * minute, 70 * minute),
        ).inOrder()
    }

    @Test
    fun `overlapping programmes don't overlap on screen`() {
        val blocks = guideBlocks(listOf(show(0, 60), show(30, 90)), 0, 120 * minute)
        assertThat(blocks.map { it.startMillis / minute to it.endMillis / minute })
            .containsExactly(0L to 60L, 60L to 90L, 90L to 120L).inOrder()
    }

    @Test
    fun `moves the window only when the focused programme starts off screen`() {
        val start = 600 * minute
        val running = GuideBlock(show(570, 660), 570 * minute, 660 * minute)
        assertThat(windowFor(running, start, earliest = start)).isEqualTo(start)
        val later = GuideBlock(null, 760 * minute, 800 * minute)
        assertThat(windowFor(later, start, earliest = start)).isEqualTo(750 * minute)
        val earlier = GuideBlock(null, 500 * minute, 520 * minute)
        assertThat(windowFor(earlier, 700 * minute, earliest = start)).isEqualTo(start)
    }

    @Test
    fun `moves back to a programme that shows only a few minutes at the left edge`() {
        val start = 600 * minute
        val endingSoon = GuideBlock(show(570, 605), 570 * minute, 605 * minute)
        assertThat(windowFor(endingSoon, start, earliest = 540 * minute)).isEqualTo(570 * minute)
    }

    @Test
    fun `rounds down to the half hour`() {
        assertThat(halfHourBefore(95 * minute)).isEqualTo(90 * minute)
    }
}
