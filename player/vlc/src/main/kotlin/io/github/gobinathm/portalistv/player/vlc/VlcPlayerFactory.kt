package io.github.gobinathm.portalistv.player.vlc

import android.content.Context
import io.github.gobinathm.portalistv.core.player.Player
import io.github.gobinathm.portalistv.core.player.PlayerFactory
import io.github.gobinathm.portalistv.core.player.PlayerSettings

/** Creates libVLC players. Must be called on the main thread. */
class VlcPlayerFactory(private val context: Context, private val verboseLogging: Boolean) : PlayerFactory {
    override fun create(settings: PlayerSettings): Player = VlcPlayer(context, settings, verboseLogging)
}
