package io.github.gobinathm.portalistv.tools.logopack

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.image.IndexColorModel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** A logo ready for the app; [light] ones are made for dark backgrounds (e.g. white lettering). */
internal class PackedLogo(val png: ByteArray, val light: Boolean)

/** Turns downloaded logos into small PNGs for the app. */
internal object LogoImages {
    /** The logo box on a 1080p TV (120 × 64 dp at 2×); larger logos are scaled down to fit. */
    const val MAX_WIDTH = 240
    const val MAX_HEIGHT = 128

    /** Bigger downloads are skipped; logos are far smaller. */
    const val MAX_SOURCE_PIXELS = 4096 * 4096

    private const val ALPHA_SHIFT = 24
    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8
    private const val BYTE = 0xFF
    private const val VISIBLE_ALPHA = 8
    private const val PALETTE_SIZE = 256
    private const val LUMA_RED = 0.2126
    private const val LUMA_GREEN = 0.7152
    private const val LUMA_BLUE = 0.0722
    private const val LIGHT_LUMINANCE = 200
    private const val MOSTLY = 0.6

    /** A PNG of [source] fitting the logo box, or null if it isn't an image Java can read. */
    fun pack(source: ByteArray): PackedLogo? {
        val image = runCatching { ImageIO.read(ByteArrayInputStream(source)) }.getOrNull()
            ?.takeIf { it.width.toLong() * it.height <= MAX_SOURCE_PIXELS }
            ?: return null
        val fitted = fit(trim(toArgb(image)))
        // Exact palette when the logo has few colours; otherwise reduced to 256, which keeps the pack small.
        val png = encode(paletted(fitted) ?: requireNotNull(paletted(Quantizer.quantize(fitted))))
        return PackedLogo(png, isLight(fitted))
    }

    /**
     * Whether most of what is drawn is light and the rest of the picture is see-through: lettering
     * meant for a dark background, which would vanish on the light plate behind other logos.
     */
    internal fun isLight(image: BufferedImage): Boolean {
        var visible = 0
        var light = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val pixel = image.getRGB(x, y)
                if (pixel ushr ALPHA_SHIFT < VISIBLE_ALPHA) continue
                visible++
                val luminance = LUMA_RED * (pixel shr RED_SHIFT and BYTE) +
                    LUMA_GREEN * (pixel shr GREEN_SHIFT and BYTE) + LUMA_BLUE * (pixel and BYTE)
                if (luminance >= LIGHT_LUMINANCE) light++
            }
        }
        val seeThrough = visible < image.width * image.height * MOSTLY
        return seeThrough && visible > 0 && light >= visible * MOSTLY
    }

    private fun toArgb(image: BufferedImage): BufferedImage {
        if (image.type == BufferedImage.TYPE_INT_ARGB) return image
        return BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB).also {
            it.createGraphics().apply { drawImage(image, 0, 0, null) }.dispose()
        }
    }

    /** Cuts transparent margins, so the logo itself fills the box. */
    internal fun trim(image: BufferedImage): BufferedImage {
        var left = image.width
        var top = image.height
        var right = -1
        var bottom = -1
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if (image.getRGB(x, y) ushr ALPHA_SHIFT >= VISIBLE_ALPHA) {
                    left = min(left, x)
                    top = min(top, y)
                    right = max(right, x)
                    bottom = max(bottom, y)
                }
            }
        }
        if (right < 0) return image
        return image.getSubimage(left, top, right - left + 1, bottom - top + 1)
    }

    /** Scales down to fit the logo box, halving first so detail survives large reductions. */
    internal fun fit(image: BufferedImage): BufferedImage {
        val scale = min(1.0, min(MAX_WIDTH.toDouble() / image.width, MAX_HEIGHT.toDouble() / image.height))
        val width = max(1, (image.width * scale).roundToInt())
        val height = max(1, (image.height * scale).roundToInt())
        var current = image
        while (current.width / 2 >= width && current.height / 2 >= height) {
            current = resize(current, current.width / 2, current.height / 2)
        }
        return if (current.width == width && current.height == height) current else resize(current, width, height)
    }

    private fun resize(image: BufferedImage, width: Int, height: Int): BufferedImage =
        BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also { target ->
            target.createGraphics().apply {
                setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                drawImage(image, 0, 0, width, height, null)
            }.dispose()
        }

    /** The same image with an exact palette, if it has few enough colours: much smaller, nothing lost. */
    internal fun paletted(image: BufferedImage): BufferedImage? {
        val pixels = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
        val colours = LinkedHashMap<Int, Int>()
        for (pixel in pixels) {
            colours.getOrPut(pixel) { colours.size }
            if (colours.size > PALETTE_SIZE) return null
        }
        val palette = colours.keys.toIntArray()
        val model = IndexColorModel(
            Byte.SIZE_BITS,
            palette.size,
            ByteArray(palette.size) { (palette[it] shr RED_SHIFT and BYTE).toByte() },
            ByteArray(palette.size) { (palette[it] shr GREEN_SHIFT and BYTE).toByte() },
            ByteArray(palette.size) { (palette[it] and BYTE).toByte() },
            ByteArray(palette.size) { (palette[it] ushr ALPHA_SHIFT).toByte() },
        )
        val indexed = BufferedImage(image.width, image.height, BufferedImage.TYPE_BYTE_INDEXED, model)
        val raster = indexed.raster
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                raster.setSample(x, y, 0, colours.getValue(pixels[y * image.width + x]))
            }
        }
        return indexed
    }

    private fun encode(image: BufferedImage): ByteArray {
        val writer = ImageIO.getImageWritersByFormatName("png").next()
        val out = ByteArrayOutputStream()
        ImageIO.createImageOutputStream(out).use { stream ->
            writer.output = stream
            val param = writer.defaultWriteParam.apply {
                if (canWriteCompressed()) {
                    compressionMode = ImageWriteParam.MODE_EXPLICIT
                    compressionQuality = 0f // Smallest file; PNG is lossless either way.
                }
            }
            writer.write(null, IIOImage(image, null, null), param)
        }
        writer.dispose()
        return out.toByteArray()
    }
}
