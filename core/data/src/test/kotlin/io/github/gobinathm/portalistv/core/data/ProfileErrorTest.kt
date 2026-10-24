package io.github.gobinathm.portalistv.core.data

import com.google.common.truth.Truth.assertThat
import io.github.gobinathm.portalistv.core.model.Playback
import io.github.gobinathm.portalistv.core.model.Profile
import io.github.gobinathm.portalistv.core.model.Proxy
import io.github.gobinathm.portalistv.core.model.Secret
import io.github.gobinathm.portalistv.core.model.TestProfiles
import org.junit.Test

class ProfileErrorTest {
    private val valid = TestProfiles.minimal()

    @Test
    fun `detects plain HTTP portals`() {
        assertThat(valid.withUrl("http://portal.example/c/", allowHttp = true).usesPlainHttp).isTrue()
        assertThat(valid.withUrl(" HTTP://portal.example/c/", allowHttp = true).usesPlainHttp).isTrue()
        assertThat(valid.withUrl("https://portal.example/c/").usesPlainHttp).isFalse()
        assertThat(valid.withUrl("not a url").usesPlainHttp).isFalse()
    }

    private fun Profile.withUrl(url: String, allowHttp: Boolean = false) =
        copy(portal = portal.copy(url = Secret(url), allowInsecureHttp = allowHttp))

    @Test
    fun `a complete profile is valid`() {
        assertThat(valid.validate()).isEmpty()
        assertThat(TestProfiles.full().validate()).isEmpty()
    }

    @Test
    fun `requires a name and a portal url`() {
        assertThat(valid.copy(name = " ").withUrl("").validate())
            .containsExactly(ProfileError.NAME_REQUIRED, ProfileError.URL_REQUIRED)
    }

    @Test
    fun `rejects urls that are not http or https`() {
        assertThat(valid.withUrl("portal.example/c/").validate()).containsExactly(ProfileError.URL_INVALID)
        assertThat(valid.withUrl("ftp://portal.example/").validate()).containsExactly(ProfileError.URL_INVALID)
        assertThat(valid.withUrl("http://").validate()).containsExactly(ProfileError.URL_INVALID)
    }

    @Test
    fun `plain http needs the opt-in`() {
        assertThat(valid.withUrl("http://portal.example/c/").validate()).containsExactly(ProfileError.URL_INSECURE)
        assertThat(valid.withUrl("http://portal.example/c/", allowHttp = true).validate()).isEmpty()
    }

    @Test
    fun `normalizes the mac address before checking it`() {
        val lower = valid.copy(device = valid.device.copy(mac = Secret(" 00:1a:79:ab:cd:ef ")))
        assertThat(lower.validate()).containsExactly(ProfileError.MAC_INVALID)
        assertThat(lower.normalized().device.mac.value).isEqualTo("00:1A:79:AB:CD:EF")
        assertThat(lower.normalized().validate()).isEmpty()
    }

    @Test
    fun `checks the proxy only when enabled`() {
        assertThat(valid.copy(proxy = Proxy(enabled = false, port = 0)).validate()).isEmpty()
        assertThat(valid.copy(proxy = Proxy(enabled = true, host = "", port = 70_000)).validate())
            .containsExactly(ProfileError.PROXY_HOST_REQUIRED, ProfileError.PROXY_PORT_INVALID)
    }

    @Test
    fun `checks the udpxy url only when enabled`() {
        val enabled = valid.copy(playback = Playback(udpxyEnabled = true, udpxyUrl = "nope"))
        assertThat(enabled.validate()).containsExactly(ProfileError.UDPXY_URL_INVALID)
        assertThat(enabled.copy(playback = Playback(udpxyEnabled = false, udpxyUrl = "nope")).validate()).isEmpty()
    }
}
