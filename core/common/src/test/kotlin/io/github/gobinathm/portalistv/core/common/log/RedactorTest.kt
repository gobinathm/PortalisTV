package io.github.gobinathm.portalistv.core.common.log

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test

class RedactorTest {
    @After
    fun tearDown() {
        Redactor.unregister("https://portal.example.test/c/")
        Redactor.unregister("https://portal.example.test")
    }

    @Test
    fun `masks MAC addresses in any separator style`() {
        assertThat(Redactor.redact("mac 00:1A:79:12:34:56 and 00-1a-79-ab-cd-ef"))
            .isEqualTo("mac <redacted> and <redacted>")
        assertThat(Redactor.redact("cookie mac=00%3A1A%3A79%3A12%3A34%3A56"))
            .isEqualTo("cookie mac=<redacted>")
    }

    @Test
    fun `masks bearer tokens`() {
        assertThat(Redactor.redact("Authorization: Bearer 9F1C2E7A.b-c_d"))
            .isEqualTo("Authorization: Bearer <redacted>")
    }

    @Test
    fun `masks credential query parameters but keeps the rest`() {
        assertThat(Redactor.redact("load.php?type=stb&action=handshake&token=abc123&sn=XYZ&JsHttpRequest=1-xml"))
            .isEqualTo("load.php?type=stb&action=handshake&token=<redacted>&sn=<redacted>&JsHttpRequest=1-xml")
    }

    @Test
    fun `masks credential fields in JSON`() {
        assertThat(Redactor.redact("""{"js":{"token":"abc","random":"r1","name":"ok"}}"""))
            .isEqualTo("""{"js":{"token":"<redacted>","random":"<redacted>","name":"ok"}}""")
    }

    @Test
    fun `masks user info in URLs`() {
        assertThat(Redactor.redact("http://user:pa55@proxy.example.test:8080/"))
            .isEqualTo("http://<redacted>@proxy.example.test:8080/")
    }

    @Test
    fun `masks registered secrets, longest first`() {
        Redactor.register("https://portal.example.test")
        Redactor.register("https://portal.example.test/c/")
        assertThat(Redactor.redact("GET https://portal.example.test/c/index.html"))
            .isEqualTo("GET <redacted>index.html")
    }

    @Test
    fun `ignores very short registered values`() {
        Redactor.register("abc")
        assertThat(Redactor.redact("abc")).isEqualTo("abc")
    }

    @Test
    fun `leaves ordinary text alone`() {
        val text = "Loaded 42 channels in 120 ms (genre=7)"
        assertThat(Redactor.redact(text)).isEqualTo(text)
    }
}
