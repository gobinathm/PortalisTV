package io.github.gobinathm.portalistv.core.model

/**
 * A credential or device identifier. [toString] never reveals the value, so a profile that ends
 * up in a log line, crash report or debugger summary does not leak it.
 */
@JvmInline
value class Secret(val value: String) {
    override fun toString(): String = "Secret(****)"
}
