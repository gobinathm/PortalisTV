package io.github.gobinathm.portalistv.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ColorScheme

private const val TOP_GLOW_ALPHA = 0.22f
private const val BOTTOM_GLOW_ALPHA = 0.08f

/**
 * The backdrop behind every screen except the player: the theme background with a soft glow of
 * the accent from the top-left corner and a fainter one from the bottom-right. Drawn, not an
 * image, so it costs nothing to load and follows the accent.
 */
fun Modifier.backdrop(colors: ColorScheme): Modifier = drawBehind {
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
