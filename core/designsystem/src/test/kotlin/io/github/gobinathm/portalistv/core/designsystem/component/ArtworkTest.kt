package io.github.gobinathm.portalistv.core.designsystem.component

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ArtworkTest {
    @Test
    fun `initials skip quality words, numbers and punctuation`() {
        assertThat(initialsOf("DISNEY XD HD USA")).isEqualTo("DX")
        assertThat(initialsOf("|IN| SUN TV HD")).isEqualTo("IS")
        assertThat(initialsOf("4K 24 Movies")).isEqualTo("M")
        assertThat(initialsOf("star vijay")).isEqualTo("SV")
    }

    @Test
    fun `a name without letters has no initials`() {
        assertThat(initialsOf("123 --")).isEmpty()
    }

    @Test
    fun `the same name always gets the same colour`() {
        assertThat(artworkBrush("Sun TV")).isEqualTo(artworkBrush(" sun tv "))
    }
}
