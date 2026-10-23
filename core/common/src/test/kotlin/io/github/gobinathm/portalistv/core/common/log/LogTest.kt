package io.github.gobinathm.portalistv.core.common.log

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class LogTest {
    private val lines = mutableListOf<Triple<LogLevel, String, Throwable?>>()

    @Before
    fun setUp() {
        lines.clear()
        Log.install({ level, _, message, throwable -> lines += Triple(level, message, throwable) }, LogLevel.INFO)
    }

    @Test
    fun `drops messages below the minimum level without building them`() {
        var built = false
        Log.d("T") {
            built = true
            "debug"
        }
        assertThat(built).isFalse()
        assertThat(lines).isEmpty()
    }

    @Test
    fun `redacts messages and throwables`() {
        val cause = IllegalStateException("token=abc")
        Log.e("T", RuntimeException("failed for 00:1A:79:00:00:01", cause)) { "mac=00:1A:79:00:00:01" }

        val (level, message, throwable) = lines.single()
        assertThat(level).isEqualTo(LogLevel.ERROR)
        assertThat(message).isEqualTo("mac=<redacted>")
        assertThat(throwable!!.message).isEqualTo("java.lang.RuntimeException: failed for <redacted>")
        assertThat(throwable.cause!!.message).isEqualTo("java.lang.IllegalStateException: token=<redacted>")
        assertThat(throwable.stackTrace).isNotEmpty()
    }
}
