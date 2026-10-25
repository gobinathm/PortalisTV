package io.github.gobinathm.portalistv.tools.logopack

import java.awt.image.BufferedImage

/**
 * Reduces an image to at most [MAX_COLOURS] colours by median cut, as pngquant does, so it can be
 * stored with a palette: a fraction of the size, and hard to tell apart at logo size. Fully
 * transparent pixels stay fully transparent, so edges don't pick up a halo.
 */
internal object Quantizer {
    const val MAX_COLOURS = 256

    private val channelShifts = intArrayOf(24, 16, 8, 0)
    private const val BYTE = 0xFF
    private const val INVISIBLE = 0
    private const val VISIBLE_ALPHA = 8

    /**
     * The palette is chosen from colours rounded to 5 bits per colour channel (to the middle of each
     * step), which keeps the search fast; transparency isn't rounded, so opaque stays opaque. Pixels
     * are then matched against the palette at full precision.
     */
    private const val ROUND_MASK = 0xFFF8F8F8.toInt()
    private const val ROUND_MIDDLE = 0x00040404

    fun quantize(image: BufferedImage): BufferedImage {
        val pixels = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
        for (i in pixels.indices) {
            if (pixels[i] ushr channelShifts[0] < VISIBLE_ALPHA) pixels[i] = INVISIBLE
        }
        val counts = HashMap<Int, Int>()
        for (pixel in pixels) {
            if (pixel != INVISIBLE) counts.merge(pixel and ROUND_MASK or ROUND_MIDDLE, 1, Int::plus)
        }
        val palette = medianCut(counts, MAX_COLOURS - 1)
        val nearest = HashMap<Int, Int>()
        for (i in pixels.indices) {
            if (pixels[i] != INVISIBLE) pixels[i] = nearest.getOrPut(pixels[i]) { closest(palette, pixels[i]) }
        }
        return BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB).apply {
            setRGB(0, 0, width, height, pixels, 0, width)
        }
    }

    private fun channel(colour: Int, index: Int) = colour ushr channelShifts[index] and BYTE

    /** Colours to be split, with the channel they spread most along and by how much. */
    private class Box(val colours: List<Int>) {
        val widest: Pair<Int, Int> = widestChannel(colours)
    }

    /** Splits the colours into at most [size] boxes, always the one spanning the widest range, at its median. */
    private fun medianCut(counts: Map<Int, Int>, size: Int): IntArray {
        if (counts.isEmpty()) return IntArray(0)
        val boxes = mutableListOf(Box(counts.keys.toList()))
        while (boxes.size < size) {
            val widest = boxes.filter { it.colours.size > 1 }.maxByOrNull { it.widest.second } ?: break
            boxes.remove(widest)
            val sorted = widest.colours.sortedBy { channel(it, widest.widest.first) }
            val half = sorted.sumOf { counts.getValue(it) } / 2
            var seen = 0
            val median = sorted.indexOfFirst {
                seen += counts.getValue(it)
                seen >= half
            }
            val split = median.coerceIn(0, sorted.size - 2)
            boxes += Box(sorted.subList(0, split + 1))
            boxes += Box(sorted.subList(split + 1, sorted.size))
        }
        return IntArray(boxes.size) { average(boxes[it].colours, counts) }
    }

    /** The channel with the largest spread in [box], and that spread. */
    private fun widestChannel(box: List<Int>): Pair<Int, Int> = channelShifts.indices.map { index ->
        index to box.maxOf { channel(it, index) } - box.minOf { channel(it, index) }
    }.maxBy { it.second }

    private fun average(box: List<Int>, counts: Map<Int, Int>): Int {
        val total = box.sumOf { counts.getValue(it).toLong() }
        return channelShifts.indices.fold(0) { colour, index ->
            val mean = box.sumOf { channel(it, index).toLong() * counts.getValue(it) } / total
            colour or (mean.toInt() shl channelShifts[index])
        }
    }

    private fun closest(palette: IntArray, colour: Int): Int = palette.minBy { candidate ->
        channelShifts.indices.sumOf { index ->
            val difference = channel(candidate, index) - channel(colour, index)
            difference * difference
        }
    }
}
