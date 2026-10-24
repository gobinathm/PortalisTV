package io.github.gobinathm.portalistv.core.stalker

import com.google.common.truth.Truth.assertThat
import io.github.gobinathm.portalistv.core.network.HttpClients
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

/** The client against replies shaped exactly like a real portal's (see fixtures/recorded). */
class RecordedShapesTest {
    private val portal = FakePortal().apply {
        validToken = "0123456789ABCDEF0123456789ABCDEF"
        listOf("handshake", "get_profile", "get_genres", "get_ordered_list", "create_link").forEach {
            fixtures[it] = "recorded/$it"
        }
    }.start()

    @After
    fun tearDown() = portal.stop()

    private val client = StalkerClient(
        HttpClients.defaultClient(),
        StalkerConfig(
            portalUrl = portal.urlOf("/c/").toString(),
            allowInsecureHttp = true,
            mac = "00:1A:79:00:00:01",
            serial = "062014N012345",
            language = "en",
            timezone = "UTC",
            identity = DeviceIdentity("MAG250", "UA", "218", "d", "desc", "343", "146", "0x588", null),
            deviceIds = null,
        ),
    )

    @Test
    fun `connects with a full real profile reply`() = runTest {
        val account = client.connect()
        assertThat(account.status).isEqualTo(0)
        assertThat(account.name).isEqualTo("Test account")
    }

    @Test
    fun `reads genres with numeric flags and extra fields`() = runTest {
        val genres = client.genres()
        assertThat(genres.map { it.id }).containsExactly("*", "2", "3", "4").inOrder()
        assertThat(genres.last().censored).isTrue()
    }

    @Test
    fun `reads a channel page with the real field set`() = runTest {
        val page = client.channels("*", 1)
        assertThat(page.totalItems).isEqualTo(30)
        assertThat(page.pageSize).isEqualTo(14)
        assertThat(page.items.map { it.name }).containsExactly("Channel 1", "Channel 2").inOrder()
        assertThat(page.items.first().cmd).isEqualTo("ffrt http://localhost/ch/101")
        assertThat(page.items.first().number).isEqualTo(1)
    }

    @Test
    fun `resolves a link sent without a player prefix`() = runTest {
        assertThat(client.createLink("ffrt http://localhost/ch/101"))
            .isEqualTo("http://streams.invalid/live/101.ts?play_token=x")
    }
}
