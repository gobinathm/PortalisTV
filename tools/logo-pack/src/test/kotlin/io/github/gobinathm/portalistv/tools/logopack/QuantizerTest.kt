package io.github.gobinathm.portalistv.tools.logopack

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.awt.image.BufferedImage

class QuantizerTest {
    /** A 64 × 64 gradient: 4,096 colours, with a transparent row. */
    private fun gradient() = BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB).apply {
        for (y in 1 until 64) for (x in 0 until 64) setRGB(x, y, (0xFF shl 24) or (x * 4 shl 16) or (y * 4 shl 8))
    }

    @Test
    fun `reduces to a palette and keeps transparency`() {
        val result = Quantizer.quantize(gradient())
        val colours = (0 until 64).flatMap { y -> (0 until 64).map { x -> result.getRGB(x, y) } }.toSet()
        assertThat(colours.size).isAtMost(Quantizer.MAX_COLOURS)
        assertThat(result.getRGB(10, 0)).isEqualTo(0)
        assertThat(result.getRGB(10, 10) ushr 24).isEqualTo(0xFF)
    }

    @Test
    fun `colours stay close to the original`() {
        val source = gradient()
        val result = Quantizer.quantize(source)
        val red = { c: Int -> c shr 16 and 0xFF }
        assertThat(kotlin.math.abs(red(result.getRGB(40, 40)) - red(source.getRGB(40, 40)))).isAtMost(12)
    }
}
