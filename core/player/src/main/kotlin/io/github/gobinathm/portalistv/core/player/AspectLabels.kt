package io.github.gobinathm.portalistv.core.player

import androidx.annotation.StringRes

/** The name shown for this picture size in the player and the settings. */
@StringRes
fun AspectMode.labelRes(): Int = when (this) {
    AspectMode.BEST_FIT -> R.string.aspect_best_fit
    AspectMode.FILL -> R.string.aspect_fill
    AspectMode.STRETCH -> R.string.aspect_stretch
    AspectMode.RATIO_16_9 -> R.string.aspect_16_9
    AspectMode.RATIO_4_3 -> R.string.aspect_4_3
    AspectMode.ORIGINAL -> R.string.aspect_original
}
