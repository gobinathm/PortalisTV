package io.github.gobinathm.portalistv.core.stalker

import com.google.common.truth.Truth.assertThat
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertThrows
import org.junit.Test

class PortalEndpointsTest {
    private fun candidates(url: String) = PortalEndpoints.candidates(url.toHttpUrl()).map { it.toString() }

    @Test
    fun `a root c folder tries portal php first`() {
        assertThat(candidates("http://portal.invalid:8080/c/")).containsExactly(
            "http://portal.invalid:8080/portal.php",
            "http://portal.invalid:8080/server/load.php",
            "http://portal.invalid:8080/stalker_portal/server/load.php",
            "http://portal.invalid:8080/stalker_portal/portal.php",
        ).inOrder()
    }

    @Test
    fun `a stalker_portal folder tries load php first`() {
        assertThat(candidates("https://portal.invalid/stalker_portal/c/index.html").take(2)).containsExactly(
            "https://portal.invalid/stalker_portal/server/load.php",
            "https://portal.invalid/stalker_portal/portal.php",
        ).inOrder()
    }

    @Test
    fun `a direct api address is used as is`() {
        assertThat(candidates("https://portal.invalid/stalker_portal/server/load.php?x=1"))
            .containsExactly("https://portal.invalid/stalker_portal/server/load.php")
    }

    @Test
    fun `query and fragment are dropped`() {
        assertThat(candidates("https://portal.invalid/c/?a=b#top")).contains("https://portal.invalid/portal.php")
    }

    @Test
    fun `plain http needs the opt-in`() {
        assertThrows(InvalidPortalUrlException::class.java) { PortalEndpoints.parse("http://p.invalid/c/", false) }
        assertThat(PortalEndpoints.parse("http://p.invalid/c/", true).host).isEqualTo("p.invalid")
        assertThrows(InvalidPortalUrlException::class.java) { PortalEndpoints.parse("ftp://p.invalid/", true) }
    }
}
