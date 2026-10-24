package io.github.gobinathm.portalistv.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing
import io.github.gobinathm.portalistv.core.player.PlaybackState
import kotlin.math.abs

/** The seek bar while left/right choose a position: where playback is, and where it will go. */
@Composable
internal fun ScrubBar(targetMs: Long, playback: PlaybackState, modifier: Modifier = Modifier) {
    val delta = targetMs - playback.positionMs
    Column(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(ScrimColors))
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(top = Spacing.xl * 2, bottom = Spacing.screenVertical),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Text(
            formatTime(targetMs) + "   (" + (if (delta >= 0) "+" else "-") + formatTime(abs(delta)) + ")",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
        )
        Progress(playback.copy(positionMs = targetMs))
    }
}

/** Confirms a remote button changed the channel's favorite status. */
@Composable
internal fun FavoriteNotice(ui: PlayerUiState, modifier: Modifier = Modifier) {
    val added = ui.favoriteChanged ?: return
    Text(
        stringResource(if (added) R.string.player_favorite_added else R.string.player_favorite_removed),
        style = MaterialTheme.typography.titleLarge,
        color = Color.White,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(Spacing.s))
            .padding(horizontal = Spacing.l, vertical = Spacing.s),
    )
}

/** The channel number being typed, or that the typed one doesn't exist. */
@Composable
internal fun NumberEntry(ui: PlayerUiState, modifier: Modifier = Modifier) {
    val text = when {
        ui.typedNumber.isNotEmpty() -> ui.typedNumber
        ui.numberNotFound != null -> stringResource(R.string.player_number_not_found, ui.numberNotFound)
        else -> return
    }
    Text(
        text,
        style = MaterialTheme.typography.displayMedium,
        color = Color.White,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(Spacing.s))
            .padding(horizontal = Spacing.l, vertical = Spacing.s),
    )
}
