package io.github.gobinathm.portalistv.core.stalker

/** One request to the portal and its reply, as seen on the wire. */
class Exchange(
    val label: String,
    val query: List<Pair<String, String>>,
    val httpCode: Int,
    val body: String,
)

/**
 * Receives every portal exchange. Used only by debug builds to capture replies for building test
 * fixtures; implementations must redact before anything is stored.
 */
fun interface ExchangeRecorder {
    fun record(exchange: Exchange)

    companion object {
        val NONE = ExchangeRecorder { }
    }
}
