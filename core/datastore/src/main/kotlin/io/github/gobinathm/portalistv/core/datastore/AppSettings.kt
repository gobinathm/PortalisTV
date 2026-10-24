package io.github.gobinathm.portalistv.core.datastore

import io.github.gobinathm.portalistv.core.player.PlayerSettings

/** Settings that apply to every profile. */
data class AppSettings(
    val player: PlayerSettings = PlayerSettings(),
    /** Keep playing in a small window when the user presses Home during playback. */
    val pictureInPicture: Boolean = true,
)
