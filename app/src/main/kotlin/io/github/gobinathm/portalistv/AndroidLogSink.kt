package io.github.gobinathm.portalistv

import io.github.gobinathm.portalistv.core.common.log.LogLevel
import io.github.gobinathm.portalistv.core.common.log.LogSink
import android.util.Log as PlatformLog

/** Forwards already-redacted log lines to logcat. */
internal object AndroidLogSink : LogSink {
    private const val TAG_PREFIX = "Portalis/"

    override fun write(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val priority = when (level) {
            LogLevel.VERBOSE -> PlatformLog.VERBOSE
            LogLevel.DEBUG -> PlatformLog.DEBUG
            LogLevel.INFO -> PlatformLog.INFO
            LogLevel.WARN -> PlatformLog.WARN
            LogLevel.ERROR -> PlatformLog.ERROR
        }
        val text = if (throwable == null) message else "$message\n${PlatformLog.getStackTraceString(throwable)}"
        PlatformLog.println(priority, TAG_PREFIX + tag, text)
    }
}
