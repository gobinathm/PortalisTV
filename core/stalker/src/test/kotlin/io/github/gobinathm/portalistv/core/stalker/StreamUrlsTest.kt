package io.github.gobinathm.portalistv.core.stalker

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class StreamUrlsTest {
    @Test
    fun `strips player prefixes`() {
        assertThat(StreamUrls.clean("ffmpeg http://h.invalid/1.ts")).isEqualTo("http://h.invalid/1.ts")
        assertThat(StreamUrls.clean("ffrt2 rtsp://h.invalid/1")).isEqualTo("rtsp://h.invalid/1")
        assertThat(StreamUrls.clean("  auto udp://@239.0.0.1:1234 ")).isEqualTo("udp://@239.0.0.1:1234")
        assertThat(StreamUrls.clean("https://h.invalid/1.m3u8")).isEqualTo("https://h.invalid/1.m3u8")
    }

    @Test
    fun `rejects schemes outside the allow-list`() {
        for (bad in listOf("ffmpeg file:///data/data/x", "content://x/y", "javascript:alert(1)", "/relative", "")) {
            assertThrows(bad, ProtocolException::class.java) { StreamUrls.clean(bad) }
        }
    }
}
