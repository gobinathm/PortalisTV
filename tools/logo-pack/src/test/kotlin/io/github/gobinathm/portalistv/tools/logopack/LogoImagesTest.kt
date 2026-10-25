package io.github.gobinathm.portalistv.tools.logopack

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class LogoImagesTest {
    /** A red block in the middle of a transparent 1000 × 600 picture. */
    private fun logo(): ByteArray {
        val image = BufferedImage(1000, 600, BufferedImage.TYPE_INT_ARGB)
        image.createGraphics().apply {
            color = Color.RED
            fillRect(250, 200, 500, 200)
        }.dispose()
        return ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
    }

    @Test
    fun `trims margins and fits the logo box`() {
        val packed = requireNotNull(LogoImages.pack(logo()))
        val result = ImageIO.read(ByteArrayInputStream(packed.png))
        assertThat(packed.light).isFalse()
        // 500 × 200 after trimming, scaled to the box's width.
        assertThat(result.width).isEqualTo(LogoImages.MAX_WIDTH)
        assertThat(result.height).isEqualTo(96)
        assertThat(Color(result.getRGB(result.width / 2, result.height / 2), true)).isEqualTo(Color.RED)
    }

    @Test
    fun `flat logos get an exact palette`() {
        val image = BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB).apply { setRGB(1, 1, Color.BLUE.rgb) }
        val indexed = requireNotNull(LogoImages.paletted(image))
        assertThat(indexed.type).isEqualTo(BufferedImage.TYPE_BYTE_INDEXED)
        assertThat(indexed.getRGB(1, 1)).isEqualTo(Color.BLUE.rgb)
        assertThat(indexed.getRGB(0, 0) ushr 24).isEqualTo(0)
    }

    @Test
    fun `small logos are not enlarged and unreadable data is skipped`() {
        val small = BufferedImage(50, 20, BufferedImage.TYPE_INT_ARGB).apply { setRGB(0, 0, Color.WHITE.rgb) }
        assertThat(LogoImages.fit(small).width).isEqualTo(50)
        assertThat(LogoImages.pack(byteArrayOf(1, 2, 3))).isNull()
    }

    @Test
    fun `white lettering on transparency is a light logo`() {
        val white = BufferedImage(100, 50, BufferedImage.TYPE_INT_ARGB).apply {
            createGraphics().apply {
                color = Color.WHITE
                fillRect(10, 10, 30, 30)
            }.dispose()
        }
        assertThat(LogoImages.isLight(white)).isTrue()
        val onWhite = BufferedImage(100, 50, BufferedImage.TYPE_INT_ARGB).apply {
            createGraphics().apply {
                color = Color.WHITE
                fillRect(0, 0, 100, 50)
            }.dispose()
        }
        assertThat(LogoImages.isLight(onWhite)).isFalse()
    }
}
