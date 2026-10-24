package io.github.gobinathm.portalistv.feature.player

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ScrubbingTest {
    private val hour = 3_600_000L

    @Test
    fun `steps 10 seconds per press, more while held`() {
        assertThat(scrubTarget(60_000, forward = true, repeatCount = 0, hour)).isEqualTo(70_000)
        assertThat(scrubTarget(60_000, forward = true, repeatCount = 6, hour)).isEqualTo(90_000)
        assertThat(scrubTarget(60_000, forward = false, repeatCount = 20, hour)).isEqualTo(0)
    }

    @Test
    fun `stays within the recording`() {
        assertThat(scrubTarget(5_000, forward = false, repeatCount = 0, hour)).isEqualTo(0)
        assertThat(scrubTarget(hour - 1_000, forward = true, repeatCount = 0, hour)).isEqualTo(hour - 5_000)
    }
}
