package io.github.gobinathm.portalistv.feature.player

import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.gobinathm.portalistv.core.designsystem.component.tapToClick
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing
import io.github.gobinathm.portalistv.core.model.nowNext
import io.github.gobinathm.portalistv.core.player.PlaybackState
import io.github.gobinathm.portalistv.core.player.PlaybackState.Status
import io.github.gobinathm.portalistv.core.player.Player
import io.github.gobinathm.portalistv.core.player.labelRes
import java.util.Date

private const val MILLIS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60
private val ProgressHeight = 4.dp
private val ControlSize = 56.dp
private val ControlIconSize = 28.dp

/** Dark enough behind the controls that text burned into the video doesn't compete with them. */
internal val ScrimColors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f), Color.Black.copy(alpha = 0.95f))

/** The side panels the control bar opens. */
internal enum class PlayerPanel { TRACKS, PICTURE }

/**
 * The control bar: title, progress for seekable streams, and a row of icon buttons whose name shows
 * under the focused one. [returnFocusTo] is the panel just closed, so its button gets focus back.
 */
@Composable
internal fun ControlsOverlay(
    ui: PlayerUiState,
    playback: PlaybackState,
    player: Player,
    returnFocusTo: PlayerPanel?,
    onOpenPanel: (PlayerPanel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playFocus = remember { FocusRequester() }
    val tracksFocus = remember { FocusRequester() }
    val pictureFocus = remember { FocusRequester() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(ScrimColors))
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(top = Spacing.xl * 2, bottom = Spacing.screenVertical),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        // Live HLS reports itself seekable within its short window; a progress bar there only confuses.
        val live = ui.channel != null
        TitleRow(ui, live)
        if (!live && playback.seekable && playback.durationMs > 0) Progress(playback)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.l)) {
            val paused = playback.status == Status.PAUSED
            ControlButton(
                icon = if (paused) R.drawable.ic_player_play else R.drawable.ic_player_pause,
                label = stringResource(if (paused) R.string.player_play else R.string.player_pause),
                onClick = { if (paused) player.resume() else player.pause() },
                focusRequester = playFocus,
            )
            ControlButton(
                icon = R.drawable.ic_player_subtitles,
                label = stringResource(R.string.player_audio_subtitles),
                onClick = { onOpenPanel(PlayerPanel.TRACKS) },
                focusRequester = tracksFocus,
            )
            ControlButton(
                icon = R.drawable.ic_player_aspect,
                label = stringResource(R.string.player_aspect, stringResource(playback.aspect.labelRes())),
                onClick = { onOpenPanel(PlayerPanel.PICTURE) },
                focusRequester = pictureFocus,
            )
        }
    }
    LaunchedEffect(Unit) {
        val target = when (returnFocusTo) {
            PlayerPanel.TRACKS -> tracksFocus
            PlayerPanel.PICTURE -> pictureFocus
            null -> playFocus
        }
        runCatching { target.requestFocus() }
    }
}

@Composable
private fun TitleRow(ui: PlayerUiState, live: Boolean) {
    val channel = ui.channel
    if (channel == null) {
        ui.vod?.let { VodTitleRow(it) }
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        if (live) {
            Text(
                stringResource(R.string.player_live),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.error, RoundedCornerShape(Spacing.xs))
                    .padding(horizontal = Spacing.s, vertical = Spacing.xs),
            )
        }
        channel.number?.let {
            Text(
                it.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(channel.name, style = MaterialTheme.typography.headlineMedium, maxLines = 1)
    }
    NowPlaying(ui)
}

@Composable
private fun VodTitleRow(title: VodTitle) {
    Text(title.title, style = MaterialTheme.typography.headlineMedium, maxLines = 1)
    title.subtitle?.let {
        Text(
            it,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** What's on now and next, under the channel name, when the portal has a guide for the channel. */
@Composable
private fun NowPlaying(ui: PlayerUiState) {
    val nowNext = ui.guide.nowNext(System.currentTimeMillis())
    val context = LocalContext.current
    val timeFormat = remember(context) { DateFormat.getTimeFormat(context) }
    val parts = listOfNotNull(
        nowNext.now?.title,
        nowNext.next?.let { stringResource(R.string.player_next, timeFormat.format(Date(it.startMillis)), it.title) },
    )
    if (parts.isNotEmpty()) {
        Text(
            parts.joinToString("   ·   "),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A round icon button; its name appears underneath while it has focus. */
@Composable
private fun ControlButton(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester,
) {
    var focused by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(ControlSize)
                .tapToClick(onClick)
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused },
        ) {
            Icon(painterResource(icon), contentDescription = label, modifier = Modifier.size(ControlIconSize))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            modifier = Modifier.alpha(if (focused) 1f else 0f),
        )
    }
}

@Composable
internal fun Progress(playback: PlaybackState) {
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(playback.positionMs), style = MaterialTheme.typography.bodyMedium)
            Text(
                "-" + formatTime((playback.durationMs - playback.positionMs).coerceAtLeast(0)),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
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
