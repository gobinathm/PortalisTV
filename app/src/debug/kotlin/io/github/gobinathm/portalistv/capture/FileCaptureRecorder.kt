package io.github.gobinathm.portalistv.capture

import io.github.gobinathm.portalistv.core.stalker.CaptureRedaction
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
