package io.github.gobinathm.portalistv.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.gobinathm.portalistv.core.designsystem.component.tapToClick
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing
import io.github.gobinathm.portalistv.core.player.AspectMode
import io.github.gobinathm.portalistv.core.player.PlaybackState
import io.github.gobinathm.portalistv.core.player.Player
import io.github.gobinathm.portalistv.core.player.labelRes

private const val PANEL_ALPHA = 0.88f
private val ColumnWidth = 320.dp
private val CheckSize = 24.dp

private data class PanelOption(val label: String, val selected: Boolean, val onSelect: () -> Unit)

/**
 * A panel over the right side of the picture, as streaming apps use for language and picture
 * choices. The selection applies at once; Back closes it.
 */
@Composable
internal fun SidePanel(panel: PlayerPanel, playback: PlaybackState, player: Player, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
        Row(
            Modifier
                .fillMaxHeight()
                .background(Color.Black.copy(alpha = PANEL_ALPHA))
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.screenVertical),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            when (panel) {
                PlayerPanel.TRACKS -> {
                    OptionColumn(
                        stringResource(R.string.player_audio),
                        audioOptions(playback, player),
                        takeFocus = true,
                    )
                    OptionColumn(stringResource(R.string.player_subtitles), subtitleOptions(playback, player))
                }
                PlayerPanel.PICTURE -> OptionColumn(
                    stringResource(R.string.player_aspect_title),
                    AspectMode.entries.map { mode ->
                        PanelOption(stringResource(mode.labelRes()), mode == playback.aspect) { player.setAspect(mode) }
                    },
                    takeFocus = true,
                )
            }
        }
    }
}

@Composable
private fun audioOptions(playback: PlaybackState, player: Player): List<PanelOption> =
    playback.audioTracks.map { track ->
        PanelOption(track.name, track.id == playback.selectedAudioTrack) { player.selectAudioTrack(track.id) }
    }.ifEmpty {
        // A single unnamed track: nothing to choose, but say what is playing.
        listOf(PanelOption(stringResource(R.string.player_audio_default), selected = true) {})
    }

@Composable
private fun subtitleOptions(playback: PlaybackState, player: Player): List<PanelOption> =
    listOf(
        PanelOption(stringResource(R.string.player_subtitles_off), playback.selectedSubtitleTrack == null) {
            player.selectSubtitleTrack(null)
        },
    ) + playback.subtitleTracks.map { track ->
        PanelOption(track.name, track.id == playback.selectedSubtitleTrack) { player.selectSubtitleTrack(track.id) }
    }

@Composable
private fun OptionColumn(title: String, options: List<PanelOption>, takeFocus: Boolean = false) {
    val selectedIndex = options.indexOfFirst { it.selected }.coerceAtLeast(0)
    val focus = remember { FocusRequester() }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    Column(Modifier.width(ColumnWidth), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        LazyColumn(state = listState) {
            itemsIndexed(options) { index, option ->
                ListItem(
                    selected = false,
                    onClick = option.onSelect,
                    headlineContent = { Text(option.label, maxLines = 1) },
                    leadingContent = {
                        if (option.selected) {
                            Icon(painterResource(R.drawable.ic_player_check), contentDescription = null)
                        } else {
                            Spacer(Modifier.size(CheckSize))
                        }
                    },
                    modifier = (if (index == selectedIndex) Modifier.focusRequester(focus) else Modifier)
                        .tapToClick(option.onSelect),
                )
            }
        }
    }
    if (takeFocus) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}
