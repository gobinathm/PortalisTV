package io.github.gobinathm.portalistv.feature.player

import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SwipeTest {
    @Test
    fun `short moves are not swipes`() {
        assertThat(swipeOf(Offset(30f, -40f), minDistance = 64f)).isNull()
    }

    @Test
    fun `the longer axis decides the direction`() {
        assertThat(swipeOf(Offset(20f, -100f), minDistance = 64f)).isEqualTo(Swipe.UP)
        assertThat(swipeOf(Offset(-20f, 100f), minDistance = 64f)).isEqualTo(Swipe.DOWN)
        assertThat(swipeOf(Offset(-120f, 50f), minDistance = 64f)).isEqualTo(Swipe.LEFT)
        assertThat(swipeOf(Offset(120f, -50f), minDistance = 64f)).isEqualTo(Swipe.RIGHT)
    }
}
