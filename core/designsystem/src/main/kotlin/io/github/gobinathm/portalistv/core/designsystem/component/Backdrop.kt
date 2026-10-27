package io.github.gobinathm.portalistv.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
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
 * of three full-screen passes (two of them gradients), which slow scrolling down on TV GPUs. The
 * bitmap is kept until the size or the colours change, not repainted when the screen recomposes.
 */
fun Modifier.backdrop(colors: ColorScheme): Modifier = this then BackdropElement(colors.background, colors.primary)

private data class BackdropElement(val background: Color, val accent: Color) : ModifierNodeElement<BackdropNode>() {
    override fun create() = BackdropNode(background, accent)

    override fun update(node: BackdropNode) = node.setColors(background, accent)
}

private class BackdropNode(private var background: Color, private var accent: Color) :
    Modifier.Node(),
    DrawModifierNode {
    private var image: ImageBitmap? = null
    private var imageFor = Size.Unspecified

    fun setColors(background: Color, accent: Color) {
        if (background == this.background && accent == this.accent) return
        this.background = background
        this.accent = accent
        image = null
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val current = image?.takeIf { imageFor == size } ?: paint().also {
            image = it
            imageFor = size
        }
        drawImage(
            current,
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            filterQuality = FilterQuality.Low,
        )
        drawContent()
    }

    private fun ContentDrawScope.paint(): ImageBitmap {
        val small = IntSize(
            ceil(size.width / DOWNSCALE).toInt().coerceAtLeast(1),
            ceil(size.height / DOWNSCALE).toInt().coerceAtLeast(1),
        )
        val bitmap = ImageBitmap(small.width, small.height)
        val smallSize = Size(small.width.toFloat(), small.height.toFloat())
        CanvasDrawScope().draw(this, layoutDirection, Canvas(bitmap), smallSize) { paintBackdrop(background, accent) }
        return bitmap
    }
}

private fun DrawScope.paintBackdrop(background: Color, accent: Color) {
    drawRect(background)
    drawRect(
        Brush.radialGradient(
            listOf(accent.copy(alpha = TOP_GLOW_ALPHA), Color.Transparent),
            center = Offset(size.width * 0.1f, 0f),
            radius = size.maxDimension * 0.75f,
        ),
    )
    drawRect(
        Brush.radialGradient(
            listOf(accent.copy(alpha = BOTTOM_GLOW_ALPHA), Color.Transparent),
            center = Offset(size.width, size.height),
            radius = size.maxDimension * 0.6f,
        ),
    )
}
