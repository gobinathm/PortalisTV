package io.github.gobinathm.portalistv.core.designsystem.theme

import androidx.compose.ui.unit.dp

/** Spacing scale. TV layouts keep a 48 dp title-safe inset from the screen edges. */
object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
    val screenHorizontal = 48.dp
    val screenVertical = 27.dp
}

object Radius {
    val card = 12.dp
    val button = 24.dp
}

/** Focused items scale gently instead of getting an outline. */
object Focus {
    const val CARD_SCALE = 1.06f
    const val BUTTON_SCALE = 1.04f
}

/** Durations in milliseconds; motion is quick and spring-based. */
object Motion {
    const val SHORT_MS = 200
    const val MEDIUM_MS = 300
}
