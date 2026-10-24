package io.github.gobinathm.portalistv.player.vlc

/**
 * libVLC's HTTP client drops a user agent that isn't valid HTTP, and set-top-box agents such as
 * "... stbapp ver: 2 rev: 250 ..." have colons outside a comment. Its fallback client sends them
 * with "_" instead, so this does the same up front for every request. Comments, in parentheses,
 * may contain colons and are kept as they are.
 */
internal fun httpSafeAgent(agent: String): String {
    var depth = 0
    return buildString {
        for (c in agent) {
            when (c) {
                '(' -> depth++
                ')' -> depth = (depth - 1).coerceAtLeast(0)
            }
            append(if (c == ':' && depth == 0) '_' else c)
        }
    }
}
