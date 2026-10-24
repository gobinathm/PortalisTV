package io.github.gobinathm.portalistv.core.stalker

import io.github.gobinathm.portalistv.core.common.log.Redactor

/** Redaction for captures: the logger's rules plus hosts, personal account fields and URL tokens. */
object CaptureRedaction {
    private const val MASK = Redactor.MASK

    private val personalKeys = listOf(
        "fname", "phone", "email", "ip", "account", "account_balance", "account_info", "comment",
        "contract", "address", "login", "password", "parent_password", "settings_password",
    ).joinToString("|")

    private val rules: List<Pair<Regex, String>> = listOf(
        // Hosts in URLs, also JSON-escaped ("http:\/\/host"). Keeps scheme and path shape.
        Regex("""(?i)\b(https?|rtsp|rtp|udp):((?:\\?/){2})(?:[^/\\\s"'@]*@)?[^/\\\s"':?]+""") to
            "$1:$2host.invalid",
        // IP addresses anywhere, e.g. a provider's storage servers listed in get_profile.
        Regex("""\b(?:\d{1,3}\.){3}\d{1,3}\b""") to "192.0.2.1",
        // Personal fields in JSON.
        Regex("""(?i)"($personalKeys)"\s*:\s*"[^"]*"""") to "\"$1\":\"$MASK\"",
        Regex("""(?i)"($personalKeys)"\s*:\s*(-?\d+(?:\.\d+)?)""") to "\"$1\":0",
        // Token-like parameters inside URLs, e.g. play_token=..., stream_token=...
        Regex("""(?i)\b([a-z_]*(?:token|key|sig|auth)[a-z_]*=)[^&"\s\\]+""") to "$1$MASK",
    )

    fun redact(text: String): String = rules.fold(Redactor.redact(text)) { acc, (regex, replacement) ->
        regex.replace(acc, replacement)
    }
}
