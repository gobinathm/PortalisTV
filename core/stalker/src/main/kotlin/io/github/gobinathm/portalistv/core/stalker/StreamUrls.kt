package io.github.gobinathm.portalistv.core.stalker

import java.net.URI
import java.net.URISyntaxException

/** Turns a portal's stream command into a URL the player may open. */
object StreamUrls {
    /** Schemes a portal may hand to the player. Never `file:`, `content:` or `javascript:`. */
    val allowedSchemes = setOf("http", "https", "rtsp", "rtp", "udp")

    // Commands carry a player prefix from the set-top-box world, e.g. "ffmpeg http://host/stream".
    private val playerPrefix = Regex("^(ffmpeg|ffrt\\d*|auto)\\s+", RegexOption.IGNORE_CASE)

    /** @throws ProtocolException if no allowed URL remains. */
    fun clean(command: String): String {
        val url = command.trim().replace(playerPrefix, "").trim()
        val scheme = try {
            URI(url).scheme?.lowercase()
        } catch (_: URISyntaxException) {
            null
        }
        if (scheme !in allowedSchemes) throw ProtocolException("Stream URL scheme not allowed: $scheme")
        return url
    }
}
