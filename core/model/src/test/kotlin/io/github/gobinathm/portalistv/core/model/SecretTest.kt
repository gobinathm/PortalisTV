package io.github.gobinathm.portalistv.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SecretTest {
    @Test
    fun `toString hides the value`() {
        assertThat(Secret("00:1A:79:12:34:56").toString()).doesNotContain("1A")
    }

    @Test
    fun `profile toString does not leak secrets`() {
        val profile = Profile(
            uuid = "u",
            name = "Home",
            createdAtMillis = 0,
            portal = Portal(url = Secret("https://portal.example/c/"), password = Secret("hunter2")),
            device = DeviceIdentity(stbModel = "MAG250", mac = Secret("00:1A:79:12:34:56")),
        )
        val text = profile.toString()
        assertThat(text).contains("Home")
        assertThat(text).doesNotContain("portal.example")
        assertThat(text).doesNotContain("hunter2")
        assertThat(text).doesNotContain("00:1A:79")
    }
}
