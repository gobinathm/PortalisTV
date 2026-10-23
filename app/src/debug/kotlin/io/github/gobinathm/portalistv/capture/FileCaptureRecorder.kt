package io.github.gobinathm.portalistv.capture

import io.github.gobinathm.portalistv.core.common.log.Redactor
import io.github.gobinathm.portalistv.core.stalker.Exchange
import io.github.gobinathm.portalistv.core.stalker.ExchangeRecorder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Appends each exchange to one file per app run, after [CaptureRedaction]. Debug builds only;
 * the files stay on the device until pulled with adb, and are reviewed before any of their
 * content is committed as a fixture.
 */
internal class FileCaptureRecorder(private val dir: File) : ExchangeRecorder {
    private val file by lazy {
        dir.mkdirs()
        File(dir, "capture-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}.txt")
    }

    @Synchronized
    override fun record(exchange: Exchange) {
        val query = exchange.query.joinToString("&") { (name, value) -> CaptureRedaction.redact("$name=$value") }
        val body = CaptureRedaction.redact(exchange.body)
        file.appendText("### ${exchange.label} HTTP ${exchange.httpCode}\n$query\n$body\n\n")
    }
}

/** Redaction for captures: the logger's rules plus hosts, personal account fields and URL tokens. */
internal object CaptureRedaction {
    private const val MASK = Redactor.MASK

    private val personalKeys = listOf(
        "fname", "phone", "email", "ip", "account", "account_balance", "account_info", "comment",
        "contract", "address", "login", "password", "parent_password", "settings_password",
    ).joinToString("|")

    private val rules: List<Pair<Regex, String>> = listOf(
        // Hosts in URLs, also JSON-escaped ("http:\/\/host"). Keeps scheme and path shape.
        Regex("""(?i)\b(https?|rtsp|rtp|udp):((?:\\?/){2})(?:[^/\\\s"'@]*@)?[^/\\\s"':?]+""") to
            "$1:$2host.invalid",
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
