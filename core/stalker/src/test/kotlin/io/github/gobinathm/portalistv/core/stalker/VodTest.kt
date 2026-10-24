package io.github.gobinathm.portalistv.core.stalker

import com.google.common.truth.Truth.assertThat
import io.github.gobinathm.portalistv.core.network.HttpClients
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

/** On-demand calls against fixtures shaped like a current Ministra portal's replies. */
class VodTest {
    private val portal = FakePortal().start()

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
    fun `reads categories with their adult flag`() = runTest {
        val categories = client.vodCategories(VodSection.MOVIES)
        assertThat(categories.map { it.id }).containsExactly("*", "12", "40").inOrder()
        assertThat(categories.last().censored).isTrue()
    }

    @Test
    fun `reads a page of movies and series, newest first`() = runTest {
        val page = client.vodPage(VodSection.MOVIES, "12", page = 2)

        assertThat(page.totalItems).isEqualTo(30)
        assertThat(page.pageCount).isEqualTo(3)
        val (movie, series) = page.items
        assertThat(movie.name).isEqualTo("Test Movie")
        assertThat(movie.posterUrl).isEqualTo(portal.urlOf("/stalker_portal/screenshots/5/501.jpg").toString())
        assertThat(movie.durationMinutes).isEqualTo(95)
        assertThat(movie.rating).isEqualTo("7.1")
        assertThat(movie.isSeries).isFalse()
        assertThat(series.isSeries).isTrue()
        assertThat(series.posterUrl).isEqualTo("https://images.invalid/502.jpg")
        // Placeholders portals use for "unknown" read as missing.
        assertThat(series.year).isNull()
        assertThat(series.rating).isNull()
        assertThat(series.durationMinutes).isNull()
        val request = portal.requestsFor("get_ordered_list").single().url
        assertThat(request.queryParameter("type")).isEqualTo("vod")
        assertThat(request.queryParameter("category")).isEqualTo("12")
        assertThat(request.queryParameter("p")).isEqualTo("2")
    }

    @Test
    fun `drills into seasons, episodes and files`() = runTest {
        portal.fixtures["vod/get_ordered_list"] = "vod_seasons"
        val seasons = client.seriesContents(VodSection.MOVIES, "502").items
        assertThat(seasons.map { it.number }).containsExactly(2, 1).inOrder()
        assertThat(seasons.all { it.isSeason }).isTrue()

        portal.fixtures["vod/get_ordered_list"] = "vod_episodes"
        val episodes = client.seriesContents(VodSection.MOVIES, "502", seasonId = "601").items
        assertThat(episodes.map { it.number }).containsExactly(3, 1, 2).inOrder()
        assertThat(episodes[1].episodeTitle).isEqualTo("Pilot")

        portal.fixtures["vod/get_ordered_list"] = "vod_files"
        val files = client.seriesContents(VodSection.MOVIES, "502", "601", episodeId = "701").items
        assertThat(files.single().isFile).isTrue()

        val last = portal.requestsFor("get_ordered_list").last().url
        assertThat(last.queryParameter("movie_id")).isEqualTo("502")
        assertThat(last.queryParameter("season_id")).isEqualTo("601")
        assertThat(last.queryParameter("episode_id")).isEqualTo("701")
    }

    @Test
    fun `resolves a file into a stream link`() = runTest {
        assertThat(client.vodLink("/media/file_801.mpg"))
            .isEqualTo("http://streams.invalid/vod/801.mp4?token=AbCdEf123")
        val request = portal.requestsFor("create_link").single().url
        assertThat(request.queryParameter("type")).isEqualTo("vod")
        assertThat(request.queryParameter("cmd")).isEqualTo("/media/file_801.mpg")
    }

    @Test
    fun `reports a link the portal can't give`() = runTest {
        portal.fixtures["vod/create_link"] = "vod_nothing_to_play"
        assertThat(runCatching { client.vodLink("/media/501.mpg") }.exceptionOrNull())
            .isInstanceOf(ProtocolException::class.java)
    }
}
