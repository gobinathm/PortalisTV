package io.github.gobinathm.portalistv.core.model

import java.util.Locale

/**
 * Channel logos packed into an app build by tools/logo-pack, for channels the portal sends no logo
 * for. They are made on the computer that builds the app and never committed. The index maps a
 * channel name's [key] to a file under [DIR] in the app's assets.
 */
object LogoPack {
    const val DIR = "logos"
    const val INDEX = "$DIR/index.properties"

    /** Ends the file name of a logo made for dark backgrounds, e.g. white lettering. */
    const val LIGHT_SUFFIX = ".light.png"

    private val whitespace = Regex("\\s+")

    /** How a channel's name is looked up: case and spacing don't matter. */
    fun key(channelName: String): String = channelName.trim().lowercase(Locale.ROOT).replace(whitespace, " ")
}
