package io.github.gobinathm.portalistv.core.stalker

import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import java.util.concurrent.CopyOnWriteArrayList

/** A Stalker portal on MockWebServer that answers from the fixture files. */
class FakePortal(
    /** Path of the API endpoint, e.g. "/portal.php". Other paths answer 404. */
    private val apiPath: String = "/portal.php",
) : Dispatcher() {
    val server = MockWebServer().apply { dispatcher = this@FakePortal }
    val requests = CopyOnWriteArrayList<RecordedRequest>()

    /** Fixture name per action, overridable per test. */
    val fixtures = mutableMapOf(
        "handshake" to "handshake",
        "get_profile" to "get_profile",
        "get_genres" to "get_genres",
        "get_ordered_list" to "get_ordered_list",
        "get_all_channels" to "get_all_channels",
        "create_link" to "create_link",
        "get_short_epg" to "get_short_epg",
        "vod/get_categories" to "vod_categories",
        "vod/get_years" to "vod_years",
        "vod/get_ordered_list" to "vod_list",
        "vod/create_link" to "vod_create_link",
    )

    /** Tokens the portal accepts; the handshake fixture's token is valid until a test revokes it. */
    var validToken = "9F2C1D0E7A6B5C4D3E2F1A0B9C8D7E6F"

    /** Overrides the next replies, oldest first, regardless of the action. */
    val queued = ArrayDeque<MockResponse>()

    fun start(): FakePortal = apply { server.start() }

    fun stop() = server.close()

    fun urlOf(path: String) = server.url(path)

    fun requestsFor(action: String) = requests.filter { it.url.queryParameter("action") == action }

    override fun dispatch(request: RecordedRequest): MockResponse {
        requests += request
        val action = request.url.queryParameter("action")
        val type = request.url.queryParameter("type")
        val authorized = action == "handshake" || request.headers["Authorization"] == "Bearer $validToken"
        // "type/action" entries win, for actions shared by several areas (e.g. get_ordered_list).
        val fixture = fixtures["$type/$action"] ?: fixtures[action]
        return when {
            queued.isNotEmpty() -> queued.removeFirst()
            request.url.encodedPath != apiPath || fixture == null -> MockResponse.Builder().code(404).build()
            !authorized -> ok("Authorization failed.")
            else -> ok(fixture(fixture))
        }
    }

    companion object {
        fun fixture(name: String): String =
            checkNotNull(FakePortal::class.java.getResource("/fixtures/$name.json")) { "fixture $name" }.readText()

        fun ok(body: String): MockResponse = MockResponse.Builder().code(200).body(body).build()
    }
}
