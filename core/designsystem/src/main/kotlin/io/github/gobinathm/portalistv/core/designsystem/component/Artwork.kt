package io.github.gobinathm.portalistv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Deep colour pairs for artwork that portals don't send. Each reads well under white text and next to
 * either accent; the same name always gets the same pair, and neighbours usually differ.
 */
private val Palette = listOf(
    Color(0xFF1E3A5F) to Color(0xFF0E1A2B), // navy
    Color(0xFF3D2C6B) to Color(0xFF17112C), // indigo
    Color(0xFF5C2266) to Color(0xFF220C27), // plum
    Color(0xFF6E2C2C) to Color(0xFF2A1010), // oxblood
    Color(0xFF6B4519) to Color(0xFF29190A), // amber
    Color(0xFF1F5040) to Color(0xFF0C2019), // forest
    Color(0xFF1B4D5A) to Color(0xFF0A1F25), // teal
    Color(0xFF3B4152) to Color(0xFF171A21), // slate
)

/** Words that say nothing about which channel it is, left out of initials. */
private val QualityWords = setOf("HD", "FHD", "UHD", "SD", "4K", "8K", "HEVC", "H265", "H264", "HQ", "LQ")

/** A diagonal gradient picked from [seed], e.g. a channel or film name. */
fun artworkBrush(seed: String): Brush {
    val (light, dark) = Palette[Math.floorMod(seed.trim().lowercase().hashCode(), Palette.size)]
    return Brush.linearGradient(listOf(light, dark), start = Offset.Zero, end = Offset.Infinite)
}

/** Paints the artwork gradient for [seed] behind the content. */
fun Modifier.artwork(seed: String): Modifier = background(artworkBrush(seed))

/**
 * Up to two initials for [name], skipping numbers, punctuation and quality words, so
 * "DISNEY XD HD USA" gives "DX" and "|IN| SUN TV HD" gives "IS".
 */
fun initialsOf(name: String): String {
    val words = name.split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.isNotEmpty() && it.uppercase() !in QualityWords && it.any(Char::isLetter) }
    return words.take(2).joinToString("") { it.take(1) }.uppercase()
}
