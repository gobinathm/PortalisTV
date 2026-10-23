package io.github.gobinathm.portalistv.core.stalker

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CaptureRedactionTest {
    private fun redact(text: String) = CaptureRedaction.redact(text)

    @Test
    fun `masks hosts, also in json-escaped urls`() {
        assertThat(redact("""{"cmd":"ffmpeg http:\/\/streams.example.com:8080\/live\/1.ts"}"""))
            .isEqualTo("""{"cmd":"ffmpeg http:\/\/host.invalid:8080\/live\/1.ts"}""")
        assertThat(redact("rtsp://user:pw@cam.example.com/x")).isEqualTo("rtsp://host.invalid/x")
    }

    @Test
    fun `masks personal account fields`() {
        val text = """{"fname":"Jane Doe","phone":"+49 151 0000","ip":"203.0.113.7",""" +
            """"account_balance":12.5,"name":"News"}"""
        val redacted = redact(text)
        assertThat(redacted).doesNotContain("Jane")
        assertThat(redacted).doesNotContain("203.0.113.7")
        assertThat(redacted).doesNotContain("12.5")
        // Channel and genre names are what fixtures need.
        assertThat(redacted).contains("\"name\":\"News\"")
    }

    @Test
    fun `masks tokens in stream urls and credential parameters`() {
        val redacted = redact("http://s.example/1.ts?play_token=AbC123&stream=1 token=xyz mac=00:1A:79:12:34:56")
        assertThat(redacted).doesNotContain("AbC123")
        assertThat(redacted).doesNotContain("xyz")
        assertThat(redacted).doesNotContain("12:34:56")
        assertThat(redacted).contains("stream=1")
    }
}
