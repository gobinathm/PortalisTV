package io.github.gobinathm.portalistv.core.common.log

import java.util.concurrent.CopyOnWriteArraySet

/**
 * Masks secrets in text before it reaches a log sink.
 *
 * Pattern rules catch the common shapes (MAC addresses, bearer tokens, credential query
 * parameters, URL user-info). Values that have no recognizable shape, such as a portal URL,
 * are registered with [register] when they are loaded and masked wherever they appear.
 */
object Redactor {
    const val MASK = "<redacted>"

    private const val MIN_REGISTERED_LENGTH = 4

    private val sensitiveParams = listOf(
        "token", "access_token", "password", "passwd", "pass", "login", "mac", "sn", "serial",
        "device_id", "device_id2", "signature", "hw_version_2", "random",
    ).joinToString("|")

    private val rules: List<Pair<Regex, String>> = listOf(
        // MAC addresses: 00:1A:79:12:34:56 or 00-1A-79-12-34-56
        Regex("""\b[0-9A-Fa-f]{2}(?:[:-][0-9A-Fa-f]{2}){5}\b""") to MASK,
        // URL-encoded MAC addresses in cookies or query strings: 00%3A1A%3A...
        Regex("""\b[0-9A-Fa-f]{2}(?:%3[Aa][0-9A-Fa-f]{2}){5}\b""") to MASK,
        // Authorization headers
        Regex("""(?i)\b(Bearer|Basic)\s+[A-Za-z0-9._~+/=-]+""") to "$1 $MASK",
        // key=value in query strings, form bodies and cookies
        Regex("""(?i)\b($sensitiveParams)=[^&;\s"]*""") to "$1=$MASK",
        // "key":"value" in JSON
        Regex("""(?i)"($sensitiveParams)"\s*:\s*"[^"]*"""") to "\"$1\":\"$MASK\"",
        // user:password@ in URLs
        Regex("""(?i)\b([a-z][a-z0-9+.-]*://)[^/\s@]+@""") to "$1$MASK@",
    )

    private val registered = CopyOnWriteArraySet<String>()

    /** Masks [secret] in all later log output. Very short values are ignored to keep logs readable. */
    fun register(secret: String) {
        if (secret.length >= MIN_REGISTERED_LENGTH) registered.add(secret)
    }

    fun unregister(secret: String) {
        registered.remove(secret)
    }

    fun redact(text: String): String {
        // Longest first, so a secret that contains another one is masked whole.
        var result = registered.sortedByDescending { it.length }
            .fold(text) { acc, secret -> acc.replace(secret, MASK) }
        for ((regex, replacement) in rules) {
            result = regex.replace(result, replacement)
        }
        return result
    }
}
