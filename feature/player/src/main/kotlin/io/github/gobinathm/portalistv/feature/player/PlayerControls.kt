package io.github.gobinathm.portalistv.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import io.github.gobinathm.portalistv.core.designsystem.component.ChoiceDialog
import io.github.gobinathm.portalistv.core.designsystem.component.ChoiceOption
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing
import io.github.gobinathm.portalistv.core.player.AspectMode
import io.github.gobinathm.portalistv.core.player.PlaybackState
import io.github.gobinathm.portalistv.core.player.PlaybackState.Status
import io.github.gobinathm.portalistv.core.player.Player
import io.github.gobinathm.portalistv.core.player.labelRes

private const val SUBTITLES_OFF = "off"
private const val MILLIS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60
private val ProgressHeight = 4.dp

private enum class Picker { AUDIO, SUBTITLES, ASPECT }

/** Title, status and the playback buttons over a soft scrim at the bottom of the screen. */
@Composable
internal fun ControlsOverlay(
    ui: PlayerUiState,
    playback: PlaybackState,
    player: Player,
    modifier: Modifier = Modifier,
) {
    var picker by remember { mutableStateOf<Picker?>(null) }
    val firstButton = remember { FocusRequester() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.screenVertical),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Text(
            ui.channel?.let { listOfNotNull(it.number?.toString(), it.name).joinToString("  ") }.orEmpty(),
            style = MaterialTheme.typography.headlineMedium,
        )
        if (playback.seekable && playback.durationMs > 0) Progress(playback)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            val paused = playback.status == Status.PAUSED
            Button(
                onClick = { if (paused) player.resume() else player.pause() },
                modifier = Modifier.focusRequester(firstButton),
            ) { Text(stringResource(if (paused) R.string.player_play else R.string.player_pause)) }
            OutlinedButton(onClick = { picker = Picker.AUDIO }, enabled = playback.audioTracks.size > 1) {
                Text(stringResource(R.string.player_audio))
            }
            OutlinedButton(onClick = { picker = Picker.SUBTITLES }, enabled = playback.subtitleTracks.isNotEmpty()) {
                Text(stringResource(R.string.player_subtitles))
            }
            OutlinedButton(onClick = { picker = Picker.ASPECT }) {
                Text(stringResource(R.string.player_aspect, stringResource(playback.aspect.labelRes())))
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { firstButton.requestFocus() } }
    PickerDialog(picker, playback, player, onDismiss = { picker = null })
}

@Composable
private fun Progress(playback: PlaybackState) {
    val fraction = (playback.positionMs.toFloat() / playback.durationMs).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(ProgressHeight)
                .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(ProgressHeight)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(ProgressHeight)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(ProgressHeight)),
            )
        }
        Text(
            "${formatTime(playback.positionMs)} / ${formatTime(playback.durationMs)}",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun PickerDialog(picker: Picker?, playback: PlaybackState, player: Player, onDismiss: () -> Unit) {
    when (picker) {
        null -> Unit
        Picker.AUDIO -> ChoiceDialog(
            title = stringResource(R.string.player_audio),
            options = playback.audioTracks.map { ChoiceOption(it.id.toString(), it.name) },
            selectedKey = playback.selectedAudioTrack?.toString(),
            onSelect = {
                player.selectAudioTrack(it.toInt())
                onDismiss()
            },
            onDismiss = onDismiss,
        )
        Picker.SUBTITLES -> ChoiceDialog(
            title = stringResource(R.string.player_subtitles),
            options = listOf(ChoiceOption(SUBTITLES_OFF, stringResource(R.string.player_subtitles_off))) +
                playback.subtitleTracks.map { ChoiceOption(it.id.toString(), it.name) },
            selectedKey = playback.selectedSubtitleTrack?.toString() ?: SUBTITLES_OFF,
            onSelect = {
                player.selectSubtitleTrack(it.toIntOrNull())
                onDismiss()
            },
            onDismiss = onDismiss,
        )
        Picker.ASPECT -> ChoiceDialog(
            title = stringResource(R.string.player_aspect_title),
            options = AspectMode.entries.map { ChoiceOption(it.name, stringResource(it.labelRes())) },
            selectedKey = playback.aspect.name,
            onSelect = {
                player.setAspect(AspectMode.valueOf(it))
                onDismiss()
            },
            onDismiss = onDismiss,
        )
    }
}

/** Loading, buffering and error messages in the middle of the screen. */
@Composable
internal fun PlaybackStatus(ui: PlayerUiState, playback: PlaybackState, modifier: Modifier = Modifier) {
    val message = when {
        ui.noExternalPlayer -> stringResource(R.string.player_no_external)
        ui.linkFailed -> stringResource(R.string.player_error_link)
        playback.status == Status.ERROR -> stringResource(R.string.player_error_stream)
        ui.resolving || playback.status == Status.OPENING -> stringResource(R.string.player_loading)
        playback.status == Status.BUFFERING -> stringResource(R.string.player_buffering, playback.bufferingPercent)
        else -> null
    } ?: return
    Text(
        message,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(Spacing.s))
            .padding(Spacing.m),
    )
}

internal fun formatTime(ms: Long): String {
    val totalSeconds = ms / MILLIS_PER_SECOND
    val hours = totalSeconds / (SECONDS_PER_MINUTE * SECONDS_PER_MINUTE)
    val minutes = totalSeconds / SECONDS_PER_MINUTE % SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}
