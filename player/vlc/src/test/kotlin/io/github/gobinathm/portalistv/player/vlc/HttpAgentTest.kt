package io.github.gobinathm.portalistv.player.vlc

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HttpAgentTest {
    @Test
    fun `replaces colons outside comments`() {
        val agent = "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 (KHTML, like Gecko) " +
            "MAG200 stbapp ver: 2 rev: 250 Safari/533.3"
        assertThat(httpSafeAgent(agent)).isEqualTo(
            "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 (KHTML, like Gecko) " +
                "MAG200 stbapp ver_ 2 rev_ 250 Safari/533.3",
        )
    }

    @Test
    fun `keeps colons inside comments`() {
        assertThat(httpSafeAgent("Box/1.0 (build: 7)")).isEqualTo("Box/1.0 (build: 7)")
    }
}
