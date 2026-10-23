package io.github.gobinathm.portalistv.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.gobinathm.portalistv.core.designsystem.theme.Focus
import io.github.gobinathm.portalistv.core.designsystem.theme.Radius
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing

/** A focusable card that scales and glows in the accent color when focused. */
@Composable
fun PosterCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: DpSize = PosterSize.Landscape,
) {
    Card(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CardDefaults.shape(RoundedCornerShape(Radius.card)),
        scale = CardDefaults.scale(focusedScale = Focus.CARD_SCALE),
        border = CardDefaults.border(focusedBorder = Border.None),
        glow = CardDefaults.glow(
            focusedGlow = Glow(elevationColor = MaterialTheme.colorScheme.primary, elevation = 16.dp),
        ),
    ) {
        Box(Modifier.fillMaxSize().padding(Spacing.m), contentAlignment = Alignment.BottomStart) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
        }
    }
}
