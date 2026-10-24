package io.github.gobinathm.portalistv.tools.mockportal

import com.google.common.truth.Truth.assertThat
import io.github.gobinathm.portalistv.core.network.HttpClients
import io.github.gobinathm.portalistv.core.stalker.AccountBlockedException
import io.github.gobinathm.portalistv.core.stalker.DeviceIdentity
import io.github.gobinathm.portalistv.core.stalker.StalkerClient
import io.github.gobinathm.portalistv.core.stalker.StalkerConfig
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

/** The app's real portal client against the mock portal over real HTTP. */
class MockPortalEndToEndTest {
    private var now = 0L
    private val catalog = Catalog()
    private val server = serve(
        MockPortal(catalog, allowedMacs = setOf(MAC), tokenLifetimeMillis = 1_000, clock = { now }),
        port = 0,
        host = "127.0.0.1",
    )

    @After
    fun tearDown() = server.stop(0)

    private fun client(mac: String = MAC, path: String = "/c/") = StalkerClient(
        HttpClients.defaultClient(),
        StalkerConfig(
            portalUrl = "http://127.0.0.1:${server.address.port}$path",
            allowInsecureHttp = true,
            mac = mac,
            serial = "062014N012345",
            language = "en",
            timezone = "UTC",
            identity = DeviceIdentity(
                stbModel = "MAG250",
                userAgent = "Mozilla/5.0 (QtEmbedded; U; Linux; C) MAG200 stbapp ver: 2 rev: 250 Safari/533.3",
                imageVersion = "218",
                imageDate = "Mon Jun 12 11:04:49 EEST 2017",
                imageDescription = "0.2.18-r19-pub-250",
                jsApiVersion = "343",
                stbApiVersion = "146",
                playerEngineVersion = "0x588",
                hardwareVersion = null,
            ),
            deviceIds = null,
        ),
    )

    @Test
    fun `connects and browses live tv`() = runTest {
        val client = client()

        assertThat(client.connect().name).isEqualTo("Mock portal")
        assertThat(client.genres().map { it.title }).contains("Test streams")

        val first = client.channels(genreId = "*", page = 1)
        assertThat(first.items).hasSize(MockPortal.PAGE_SIZE)
        assertThat(first.totalItems).isEqualTo(catalog.channels.size)
        val last = client.channels(genreId = "*", page = first.pageCount)
        assertThat(last.items.last().name).isEqualTo(catalog.channels.last().name)

        assertThat(client.channels(genreId = "2", page = 1).items.map { it.name }).containsExactly("Big Buck Bunny")
        assertThat(client.allChannels()).hasSize(catalog.channels.size)

        val bunny = client.channels(genreId = "2", page = 1).items.single()
        assertThat(client.createLink(bunny.cmd)).isEqualTo("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")

        val guide = client.shortEpg(bunny.id)
        assertThat(guide).hasSize(2)
        assertThat(guide[0].stopSeconds).isEqualTo(guide[1].startSeconds)
        assertThat(client.shortEpg(client.channels(genreId = "3", page = 1).items.first().id)).isEmpty()
    }

    @Test
    fun `works through the stalker_portal path too`() = runTest {
        assertThat(client(path = "/stalker_portal/c/").connect().status).isEqualTo(0)
    }

    @Test
    fun `refuses an unregistered mac address`() = runTest {
        val error = runCatching { client(mac = "00:1A:79:FF:FF:FF").connect() }.exceptionOrNull()
        assertThat(error).isInstanceOf(AccountBlockedException::class.java)
        assertThat((error as AccountBlockedException).portalMessage).contains("not registered")
    }

    @Test
    fun `reconnects after the token expires`() = runTest {
        val client = client()
        client.connect()
        now += 5_000
        assertThat(client.genres()).isNotEmpty()
    }

    private companion object {
        const val MAC = "00:1A:79:00:00:01"
    }
}
