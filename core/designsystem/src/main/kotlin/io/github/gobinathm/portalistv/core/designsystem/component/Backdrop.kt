package io.github.gobinathm.portalistv.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.tv.material3.ColorScheme
import kotlin.math.ceil

private const val TOP_GLOW_ALPHA = 0.22f
private const val BOTTOM_GLOW_ALPHA = 0.08f

/** The backdrop is painted this many times smaller than the screen; the glows are soft enough not to show it. */
private const val DOWNSCALE = 4

/**
 * The backdrop behind every screen except the player: the theme background with a soft glow of
 * the accent from the top-left corner and a fainter one from the bottom-right. Drawn, not an
 * image, so it costs nothing to load and follows the accent.
 *
 * It is painted once into a small bitmap and stretched on every frame: one texture pass instead
 * of three full-screen passes (two of them gradients), which slow scrolling down on TV GPUs.
 */
fun Modifier.backdrop(colors: ColorScheme): Modifier = drawWithCache {
    val small = IntSize(
        ceil(size.width / DOWNSCALE).toInt().coerceAtLeast(1),
        ceil(size.height / DOWNSCALE).toInt().coerceAtLeast(1),
    )
    val image = ImageBitmap(small.width, small.height)
    CanvasDrawScope().draw(this, layoutDirection, Canvas(image), Size(small.width.toFloat(), small.height.toFloat())) {
        paintBackdrop(colors)
    }
    val target = IntSize(size.width.toInt(), size.height.toInt())
    onDrawBehind { drawImage(image, dstOffset = IntOffset.Zero, dstSize = target, filterQuality = FilterQuality.Low) }
}

private fun DrawScope.paintBackdrop(colors: ColorScheme) {
    drawRect(colors.background)
    drawRect(
        Brush.radialGradient(
            listOf(colors.primary.copy(alpha = TOP_GLOW_ALPHA), Color.Transparent),
            center = Offset(size.width * 0.1f, 0f),
            radius = size.maxDimension * 0.75f,
        ),
    )
    drawRect(
        Brush.radialGradient(
            listOf(colors.primary.copy(alpha = BOTTOM_GLOW_ALPHA), Color.Transparent),
            center = Offset(size.width, size.height),
            radius = size.maxDimension * 0.6f,
        ),
    )
}
