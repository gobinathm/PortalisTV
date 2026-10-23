package io.github.gobinathm.portalistv.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/** The base surfaces for each theme. Text and accent colors are shared. */
enum class ThemeVariant(
    internal val background: Color,
    internal val surface: Color,
    internal val surfaceVariant: Color,
) {
    DARK(background = Color(0xFF0E1014), surface = Color(0xFF171A20), surfaceVariant = Color(0xFF222630)),
    OLED_BLACK(background = Color(0xFF000000), surface = Color(0xFF0C0D10), surfaceVariant = Color(0xFF181A1F)),
    DIM(background = Color(0xFF1C1F26), surface = Color(0xFF262A33), surfaceVariant = Color(0xFF323744)),
}

/** User-selectable accent colors. The accent is the only saturated color in the UI. */
enum class Accent(internal val color: Color, internal val onColor: Color) {
    AZURE(Color(0xFF4C9BFF), Color(0xFF00213F)),
    CORAL(Color(0xFFFF7A66), Color(0xFF3B0A02)),
    MINT(Color(0xFF4FD8A8), Color(0xFF00382A)),
    AMBER(Color(0xFFFFC14D), Color(0xFF3A2600)),
    ORCHID(Color(0xFFC792FF), Color(0xFF2C0A4F)),
}

internal object TextColors {
    val primary = Color(0xFFF2F3F5)
    val secondary = Color(0xFFB4B8C2)
    val error = Color(0xFFFF6B6B)
    val onError = Color(0xFF3B0000)
}
