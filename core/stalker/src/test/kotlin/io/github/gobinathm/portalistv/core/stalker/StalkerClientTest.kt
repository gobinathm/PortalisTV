package io.github.gobinathm.portalistv.core.stalker

import com.google.common.truth.Truth.assertThat
import io.github.gobinathm.portalistv.core.network.HttpClients
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl
import org.junit.After
import org.junit.Test

class StalkerClientTest {
    private val portal = FakePortal().start()

    @After
    fun tearDown() = portal.stop()

    private fun config(url: HttpUrl = portal.urlOf("/c/"), deviceIds: DeviceIds? = null) = StalkerConfig(
        portalUrl = url.toString(),
        allowInsecureHttp = true,
        mac = "00:1A:79:00:00:01",
        serial = "062014N012345",
        language = "en",
        timezone = "Europe/Berlin",
        identity = DeviceIdentity(
            stbModel = "MAG250",
            userAgent = "Mozilla/5.0 (QtEmbedded; U; Linux; C) MAG200 stbapp ver: 2 rev: 250 Safari/533.3",
            imageVersion = "218",
            imageDate = "Mon Jun 12 11:04:49 EEST 2017",
            imageDescription = "0.2.18-r19-pub-250",
            jsApiVersion = "343",
            stbApiVersion = "146",
            playerEngineVersion = "0x588",
            hardwareVersion = "1.7-BD-00",
        ),
        deviceIds = deviceIds,
    )

    private fun client(config: StalkerConfig = config()) =
        StalkerClient(HttpClients.defaultClient(), config, clock = { 1_700_000_000_000 })

    @Test
    fun `connects through the first endpoint that completes a handshake`() = runTest {
        val account = client().connect()

        assertThat(account.status).isEqualTo(0)
        assertThat(account.name).isEqualTo("Living room")
        // "/c/" has no "stalker_portal" folder, so portal.php at the root is tried first.
        assertThat(portal.requests.first().url.encodedPath).isEqualTo("/portal.php")
    }

    @Test
    fun `falls back to later candidates when the first is missing`() = runTest {
        val loadPhp = FakePortal(apiPath = "/stalker_portal/server/load.php").start()
        try {
            client(config(loadPhp.urlOf("/c/"))).connect()
            val paths = loadPhp.requests.map { it.url.encodedPath }
            assertThat(paths).containsAtLeast("/portal.php", "/stalker_portal/server/load.php").inOrder()
            assertThat(loadPhp.requestsFor("get_profile").single().url.encodedPath)
                .isEqualTo("/stalker_portal/server/load.php")
        } finally {
            loadPhp.stop()
        }
    }

    @Test
    fun `sends the set-top box headers and cookies`() = runTest {
        client().connect()

        val profile = portal.requestsFor("get_profile").single()
        assertThat(profile.headers["Authorization"]).isEqualTo("Bearer 9F2C1D0E7A6B5C4D3E2F1A0B9C8D7E6F")
        assertThat(profile.headers["User-Agent"]).contains("MAG200 stbapp")
        assertThat(profile.headers["X-User-Agent"]).isEqualTo("Model: MAG250; Link: Ethernet")
        assertThat(profile.headers["Cookie"])
            .isEqualTo("mac=00%3A1A%3A79%3A00%3A00%3A01; stb_lang=en; timezone=Europe%2FBerlin")
        assertThat(profile.headers["Referer"]).isEqualTo(portal.urlOf("/c/").toString())
        assertThat(portal.requestsFor("handshake").single().headers["Authorization"]).isNull()
    }

    @Test
    fun `sends the device profile`() = runTest {
        client(config(deviceIds = DeviceIds("ID1", "ID2", "SIG"))).connect()

        val url = portal.requestsFor("get_profile").single().url
        assertThat(url.queryParameter("sn")).isEqualTo("062014N012345")
        assertThat(url.queryParameter("stb_type")).isEqualTo("MAG250")
        assertThat(url.queryParameter("image_version")).isEqualTo("218")
        assertThat(url.queryParameter("hw_version")).isEqualTo("1.7-BD-00")
        assertThat(url.queryParameter("ver")).isEqualTo(
            "ImageDescription: 0.2.18-r19-pub-250; ImageDate: Mon Jun 12 11:04:49 EEST 2017; " +
                "PORTAL version: 5.6.1; API Version: JS API version: 343; STB API version: 146; " +
                "Player Engine version: 0x588",
        )
        assertThat(url.queryParameter("device_id")).isEqualTo("ID1")
        assertThat(url.queryParameter("device_id2")).isEqualTo("ID2")
        assertThat(url.queryParameter("signature")).isEqualTo("SIG")
        assertThat(url.queryParameter("timestamp")).isEqualTo("1700000000")
        assertThat(url.queryParameter("metrics")).contains("\"random\":\"0123456789abcdef")
        assertThat(url.queryParameter("JsHttpRequest")).isEqualTo("1-xml")
    }

    @Test
    fun `omits device ids unless the profile sends them`() = runTest {
        client().connect()
        val url = portal.requestsFor("get_profile").single().url
        assertThat(url.queryParameterNames).containsNoneOf("device_id", "device_id2", "signature")
    }

    @Test
    fun `reports a device the portal refuses`() = runTest {
        portal.fixtures["get_profile"] = "get_profile_blocked"
        val error = runCatching { client().connect() }.exceptionOrNull()
        assertThat(error).isInstanceOf(AccountBlockedException::class.java)
        assertThat((error as AccountBlockedException).portalMessage).contains("not registered")
    }

    @Test
    fun `reports when no candidate is a portal`() = runTest {
        val nothing = FakePortal(apiPath = "/nowhere").start()
        try {
            val error = runCatching { client(config(nothing.urlOf("/c/"))).connect() }.exceptionOrNull()
            assertThat(error).isInstanceOf(PortalNotFoundException::class.java)
        } finally {
            nothing.stop()
        }
    }

    @Test
    fun `refuses plain http without the opt-in`() {
        val error = runCatching { client(config().copy(allowInsecureHttp = false)) }.exceptionOrNull()
        assertThat(error).isInstanceOf(InvalidPortalUrlException::class.java)
    }

    @Test
    fun `reconnects once when the token expires`() = runTest {
        val client = client()
        client.connect()
        // The portal rejects the next call once, as if the token expired; the new handshake fixes it.
        portal.queued += FakePortal.ok("Authorization failed.")

        val genres = client.genres()

        assertThat(genres).isNotEmpty()
        assertThat(portal.requestsFor("handshake")).hasSize(2)
        assertThat(portal.requestsFor("get_genres")).hasSize(2)
    }

    @Test
    fun `gives up when the portal keeps rejecting the session`() = runTest {
        val client = client()
        client.connect()
        portal.validToken = "never"
        val error = runCatching { client.genres() }.exceptionOrNull()
        // The reconnect itself fails at get_profile, which the portal also rejects.
        assertThat(error).isInstanceOf(AuthorizationException::class.java)
    }

    @Test
    fun `parses loosely typed genres`() = runTest {
        val genres = client().genres()
        assertThat(genres.map { it.id }).containsExactly("*", "3", "7").inOrder()
        assertThat(genres.single { it.id == "7" }.censored).isTrue()
        assertThat(genres.single { it.id == "3" }.censored).isFalse()
    }

    @Test
    fun `parses a channel page`() = runTest {
        val page = client().channels(genreId = "3", page = 1)

        assertThat(page.totalItems).isEqualTo(23)
        assertThat(page.pageSize).isEqualTo(14)
        assertThat(page.pageCount).isEqualTo(2)
        assertThat(page.items.map { it.name }).containsExactly("Test Pattern", "Big Buck Bunny").inOrder()
        val first = page.items.first()
        assertThat(first.number).isEqualTo(1)
        assertThat(first.logoUrl).isEqualTo(portal.urlOf("/stalker_portal/misc/logos/320/101.png").toString())
        assertThat(page.items[1].logoUrl).isEqualTo("https://logos.invalid/102.png")
        val request = portal.requestsFor("get_ordered_list").single().url
        assertThat(request.queryParameter("genre")).isEqualTo("3")
        assertThat(request.queryParameter("p")).isEqualTo("1")
    }

    @Test
    fun `parses all channels sent as a keyed object`() = runTest {
        assertThat(client().allChannels().map { it.id }).containsExactly("101", "102")
    }

    @Test
    fun `resolves a stream link without the player prefix`() = runTest {
        val link = client().createLink("ffrt http://localhost/ch/101_")
        assertThat(link).isEqualTo("http://streams.invalid/live/101.ts?play_token=AbCdEf123")
        assertThat(portal.requestsFor("create_link").single().url.queryParameter("cmd"))
            .isEqualTo("ffrt http://localhost/ch/101_")
    }

    @Test
    fun `reads now and next in time order, skipping entries without times`() = runTest {
        val programmes = client().shortEpg("101")

        assertThat(programmes.map { it.title }).containsExactly("Morning Show", "Later Show").inOrder()
        assertThat(programmes.first().startSeconds).isEqualTo(1_767_261_600)
        assertThat(programmes.first().description).isEqualTo("A test programme.")
        assertThat(programmes.last().description).isNull()
        val request = portal.requestsFor("get_short_epg").single().url
        assertThat(request.queryParameter("ch_id")).isEqualTo("101")
        assertThat(request.queryParameter("size")).isEqualTo("2")
    }

    @Test
    fun `reads an empty guide`() = runTest {
        portal.fixtures["get_short_epg"] = "empty_list"
        assertThat(client().shortEpg("101")).isEmpty()
    }

    @Test
    fun `rejects oversized replies`() = runTest {
        val client = StalkerClient(HttpClients.defaultClient(), config())
        client.connect()
        portal.queued += FakePortal.ok("{\"js\":\"" + "x".repeat(1024) + "\"}")
        val small = StalkerTransport(HttpClients.defaultClient(), config(), portal.urlOf("/c/"), maxBodyBytes = 100)
        val error = runCatching { small.get(portal.urlOf("/portal.php"), emptyList(), null) }.exceptionOrNull()
        assertThat(error).isInstanceOf(ProtocolException::class.java)
    }
}
