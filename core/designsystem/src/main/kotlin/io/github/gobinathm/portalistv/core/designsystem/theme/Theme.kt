package io.github.gobinathm.portalistv.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@Composable
fun PortalisTheme(
    variant: ThemeVariant = ThemeVariant.DARK,
    accent: Accent = Accent.AZURE,
    content: @Composable () -> Unit,
) {
    val colors = darkColorScheme(
        primary = accent.color,
        onPrimary = accent.onColor,
        secondary = accent.color,
        onSecondary = accent.onColor,
        background = variant.background,
        onBackground = TextColors.primary,
        surface = variant.surface,
        onSurface = TextColors.primary,
        surfaceVariant = variant.surfaceVariant,
        onSurfaceVariant = TextColors.secondary,
        border = accent.color,
        error = TextColors.error,
        onError = TextColors.onError,
    )
    MaterialTheme(colorScheme = colors, typography = PortalisTypography, content = content)
}
