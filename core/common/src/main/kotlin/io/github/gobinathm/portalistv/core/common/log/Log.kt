package io.github.gobinathm.portalistv.core.common.log

enum class LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR }

/** Where log lines end up. The app installs a platform sink; tests can install a recording one. */
fun interface LogSink {
    fun write(level: LogLevel, tag: String, message: String, throwable: Throwable?)
}

/**
 * The only logging entry point for app code. Every message and throwable is passed through
 * [Redactor] before it reaches the sink. Release builds strip [v] and [d] calls with R8.
 */
object Log {
    @Volatile
    private var sink: LogSink? = null

    @Volatile
    var minLevel: LogLevel = LogLevel.INFO

    fun install(sink: LogSink, minLevel: LogLevel) {
        this.sink = sink
        this.minLevel = minLevel
    }

    fun v(tag: String, message: () -> String) = emit(LogLevel.VERBOSE, tag, null, message)

    fun d(tag: String, message: () -> String) = emit(LogLevel.DEBUG, tag, null, message)

    fun i(tag: String, message: () -> String) = emit(LogLevel.INFO, tag, null, message)

    fun w(tag: String, throwable: Throwable? = null, message: () -> String) =
        emit(LogLevel.WARN, tag, throwable, message)

    fun e(tag: String, throwable: Throwable? = null, message: () -> String) =
        emit(LogLevel.ERROR, tag, throwable, message)

    private fun emit(level: LogLevel, tag: String, throwable: Throwable?, message: () -> String) {
        val target = sink ?: return
        if (level < minLevel) return
        target.write(level, tag, Redactor.redact(message()), throwable?.let(::RedactedThrowable))
    }
}

/**
 * Wraps a throwable so its message and causes are redacted while the stack trace is kept.
 * Exception messages often contain URLs with credentials.
 */
class RedactedThrowable(original: Throwable) : Throwable(
    original.message?.let { "${original.javaClass.name}: ${Redactor.redact(it)}" } ?: original.javaClass.name,
    original.cause?.let(::RedactedThrowable),
) {
    init {
        stackTrace = original.stackTrace
    }

    override fun toString(): String = message.orEmpty()
}
