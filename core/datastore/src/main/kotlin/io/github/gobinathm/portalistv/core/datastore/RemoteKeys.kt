package io.github.gobinathm.portalistv.core.datastore

import android.view.KeyEvent

/** What a remote button can do during playback. */
enum class PlayerAction {
    NEXT_CHANNEL,
    PREVIOUS_CHANNEL,
    LAST_CHANNEL,
    SHOW_CONTROLS,
    PLAY_PAUSE,
    AUDIO_SUBTITLES,
    PICTURE_SIZE,
    TOGGLE_FAVORITE,
}

object RemoteKeys {
    /** What set-top-box remotes' extra buttons usually do. Remotes without them lose nothing. */
    val DEFAULT: Map<Int, PlayerAction> = mapOf(
        KeyEvent.KEYCODE_CHANNEL_UP to PlayerAction.NEXT_CHANNEL,
        KeyEvent.KEYCODE_CHANNEL_DOWN to PlayerAction.PREVIOUS_CHANNEL,
        KeyEvent.KEYCODE_LAST_CHANNEL to PlayerAction.LAST_CHANNEL,
        KeyEvent.KEYCODE_INFO to PlayerAction.SHOW_CONTROLS,
        KeyEvent.KEYCODE_PROG_RED to PlayerAction.TOGGLE_FAVORITE,
        KeyEvent.KEYCODE_CAPTIONS to PlayerAction.AUDIO_SUBTITLES,
    )

    /** Buttons with a fixed meaning everywhere in the app; they can't be given another. */
    val RESERVED: Set<Int> = setOf(
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_BACK,
        KeyEvent.KEYCODE_HOME,
        KeyEvent.KEYCODE_ESCAPE,
    ) + (KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9) + (KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9)

    fun isMappable(keyCode: Int): Boolean = keyCode != KeyEvent.KEYCODE_UNKNOWN && keyCode !in RESERVED
}
