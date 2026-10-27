package io.github.gobinathm.portalistv.feature.player

import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_0
import android.view.KeyEvent.KEYCODE_9
import android.view.KeyEvent.KEYCODE_CHANNEL_DOWN
import android.view.KeyEvent.KEYCODE_CHANNEL_UP
import android.view.KeyEvent.KEYCODE_DPAD_CENTER
import android.view.KeyEvent.KEYCODE_DPAD_DOWN
import android.view.KeyEvent.KEYCODE_DPAD_LEFT
import android.view.KeyEvent.KEYCODE_DPAD_RIGHT
import android.view.KeyEvent.KEYCODE_DPAD_UP
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
import android.view.KeyEvent.KEYCODE_NUMPAD_0
import android.view.KeyEvent.KEYCODE_NUMPAD_9
import io.github.gobinathm.portalistv.core.datastore.PlayerAction
import io.github.gobinathm.portalistv.core.player.AspectMode
import io.github.gobinathm.portalistv.core.player.PlaybackState
import io.github.gobinathm.portalistv.core.player.Player

/** The digit a number key stands for, from the remote's or a keyboard's number row or keypad. */
private fun digitOf(code: Int): Int? = when (code) {
    in KEYCODE_0..KEYCODE_9 -> code - KEYCODE_0
    in KEYCODE_NUMPAD_0..KEYCODE_NUMPAD_9 -> code - KEYCODE_NUMPAD_0
    else -> null
}

/** Up/Down and the remote's channel buttons, with the step each one zaps. */
private fun zapStepOf(code: Int): Int? = when (code) {
    KEYCODE_DPAD_UP, KEYCODE_CHANNEL_UP -> +1
    KEYCODE_DPAD_DOWN, KEYCODE_CHANNEL_DOWN -> -1
    else -> null
}

/**
 * Keys on the playback screen. Number keys always pick a channel, and on live TV Up/Down zap
 * unless a side panel is open; the rest act only while the controls are hidden. Returns true
 * when the key was used.
 */
@Suppress("LongParameterList") // What the screen shows decides what a key does.
internal fun PlayerViewModel.handleKey(
    event: KeyEvent,
    overlayVisible: Boolean,
    panel: PlayerPanel?,
    playback: PlaybackState,
    player: Player,
    ui: PlayerUi,
): Boolean {
    val digit = digitOf(event.keyCode)
    val seek = event.keyCode == KEYCODE_DPAD_LEFT || event.keyCode == KEYCODE_DPAD_RIGHT
    val zapStep = zapStepOf(event.keyCode)
    val bound = keyBindings[event.keyCode]
    return when {
        digit != null -> {
            typeDigit(digit)
            true
        }
        // Mapped buttons are never navigation keys, so they work whether or not the controls show.
        bound != null -> {
            perform(bound, playback, player, ui)
            true
        }
        // The control bar is a single row, so Up/Down are free to zap while it shows, as they
        // must: a new channel shows its title and the controls for a few seconds.
        zapStep != null && panel == null && state.value.channel != null -> {
            zap(zapStep)
            true
        }
        overlayVisible -> false
        seek && playback.seekable -> {
            scrub(forward = event.keyCode == KEYCODE_DPAD_RIGHT, repeatCount = event.repeatCount)
            true
        }
        else -> handleControlKey(event.keyCode, playback, player, ui.showOverlay)
    }
}

private fun AspectMode.next(): AspectMode = AspectMode.entries[(ordinal + 1) % AspectMode.entries.size]

/** What the screen itself does for a remote button: show the controls or one of their panels. */
internal class PlayerUi(val showOverlay: () -> Unit, val openPanel: (PlayerPanel) -> Unit)

private fun PlayerViewModel.perform(action: PlayerAction, playback: PlaybackState, player: Player, ui: PlayerUi) {
    when (action) {
        PlayerAction.NEXT_CHANNEL -> zap(+1)
        PlayerAction.PREVIOUS_CHANNEL -> zap(-1)
        PlayerAction.LAST_CHANNEL -> lastChannel()
        PlayerAction.SHOW_CONTROLS -> ui.showOverlay()
        PlayerAction.PLAY_PAUSE ->
            if (playback.status == PlaybackState.Status.PAUSED) player.resume() else player.pause()
        PlayerAction.AUDIO_SUBTITLES -> ui.openPanel(PlayerPanel.TRACKS)
        PlayerAction.PICTURE_SIZE -> player.setAspect(playback.aspect.next())
        PlayerAction.TOGGLE_FAVORITE -> toggleFavorite()
    }
}

/** Keys while the controls are hidden. Returns true when the key was used. */
private fun handleControlKey(
    code: Int,
    playback: PlaybackState,
    player: Player,
    showOverlay: () -> Unit,
): Boolean {
    when (code) {
        KEYCODE_DPAD_CENTER, KEYCODE_ENTER -> showOverlay()
        // Seekable streams scrub instead (see handleKey); live ones show the controls.
        KEYCODE_DPAD_LEFT, KEYCODE_DPAD_RIGHT -> showOverlay()
        KEYCODE_MEDIA_PLAY_PAUSE ->
            if (playback.status == PlaybackState.Status.PAUSED) player.resume() else player.pause()
        else -> return false
    }
    return true
}
