package io.github.gobinathm.portalistv.tools.mockportal

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLDecoder
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

/** One HTTP request as the portal sees it. */
data class PortalRequest(val path: String, val query: Map<String, String>, val headers: Map<String, String>)

data class PortalResponse(val code: Int, val contentType: String, val body: String)

/**
 * A small Stalker-compatible portal for development, tests and screenshots. It implements the
 * calls the app uses, with the same envelope (`{"js": ...}`) and loose typing real portals use,
 * and answers "Authorization failed." for missing or expired tokens.
 */
@Suppress("TooManyFunctions") // One handler per portal call.
class MockPortal(
    private val catalog: Catalog = Catalog(),
    /** MAC addresses allowed to use the portal; empty allows every device. */
    private val allowedMacs: Set<String> = emptySet(),
    /** How long a token stays valid; set it low to exercise re-authentication. */
    private val tokenLifetimeMillis: Long = DEFAULT_TOKEN_LIFETIME_MS,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val tokens = ConcurrentHashMap<String, Long>()
    private val vod = MockVodApi(catalog)
    private val random = SecureRandom()

    fun handle(request: PortalRequest): PortalResponse {
        val path = request.path
        val action = request.query["action"]
        return when {
            path.trimEnd('/') in WEB_PATHS -> html(WEB_PAGE)
            path !in API_PATHS -> notFound("Not found")
            action == "handshake" -> json(handshake())
            !authorized(request) -> PortalResponse(HTTP_OK, "text/plain", "Authorization failed.")
            else -> api(request.query["type"], action, request)?.let(::json) ?: notFound("Unknown action")
        }
    }

    private fun api(type: String?, action: String?, request: PortalRequest): JsonElement? = when (type to action) {
        "stb" to "get_profile" -> profile(request)
        "itv" to "get_genres" -> genres()
        "itv" to "get_ordered_list" -> orderedList(request)
        "itv" to "get_all_channels" -> allChannels()
        "itv" to "create_link" -> createLink(request)
        "itv" to "get_short_epg" -> shortEpg(request)
        "vod" to "get_categories" -> vod.categories()
        "vod" to "get_years" -> vod.years(request.query)
        "vod" to "get_ordered_list" -> vod.orderedList(request.query)
        "vod" to "create_link" -> vod.createLink(request.query)
        else -> null
    }

    private fun authorized(request: PortalRequest): Boolean {
        val token = request.headers["authorization"]?.removePrefix("Bearer ")?.trim()
        val issued = token?.let(tokens::get)
        return issued != null && clock() - issued < tokenLifetimeMillis
    }

    private fun handshake(): JsonObject {
        val token = random.hex(TOKEN_BYTES)
        tokens[token] = clock()
        return buildJsonObject {
            put("token", token)
            put("random", random.hex(RANDOM_BYTES))
        }
    }

    private fun profile(request: PortalRequest): JsonObject {
        val mac = cookie(request, "mac")?.let { URLDecoder.decode(it, Charsets.UTF_8) }?.uppercase()
        val allowed = allowedMacs.isEmpty() || mac in allowedMacs
        return buildJsonObject {
            put("id", if (allowed) "1" else null)
            put("fname", if (allowed) "Mock portal" else "")
            put("status", if (allowed) 0 else STATUS_NOT_REGISTERED)
            put("msg", if (allowed) "" else "Your STB is not registered on the mock portal.")
            put("stb_type", request.query["stb_type"])
            put("watchdog_timeout", WATCHDOG_SECONDS)
        }
    }

    private fun genres() = buildJsonArray {
        add(
            buildJsonObject {
                put("id", "*")
                put("title", "All")
                put("alias", "*")
                put("censored", 0)
            },
        )
        catalog.genres.forEach { genre ->
            add(
                buildJsonObject {
                    put("id", genre.id)
                    put("title", genre.title)
                    put("alias", genre.title.lowercase())
                    // Real portals mix strings and numbers for flags.
                    put("censored", if (genre.censored) "1" else "0")
                },
            )
        }
    }

    private fun orderedList(request: PortalRequest): JsonObject {
        val genre = request.query["genre"] ?: "*"
        val page = request.query["p"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val matching = catalog.channels.filter { genre == "*" || it.genreId == genre }
        val items = matching.drop((page - 1) * PAGE_SIZE).take(PAGE_SIZE)
        return buildJsonObject {
            put("total_items", matching.size.toString())
            put("max_page_items", PAGE_SIZE)
            put("cur_page", page)
            put("data", buildJsonArray { items.forEach { add(channelJson(it)) } })
        }
    }

    /** Sent as an object keyed by index, as some portals do. */
    private fun allChannels() = buildJsonObject {
        put("total_items", catalog.channels.size)
        put(
            "data",
            buildJsonObject {
                catalog.channels.forEachIndexed { i, channel -> put(i.toString(), channelJson(channel)) }
            },
        )
    }

    private fun createLink(request: PortalRequest): JsonElement {
        val id = request.query["cmd"]?.substringAfterLast("/ch/")?.trimEnd('_')?.toIntOrNull()
        val channel = catalog.channels.firstOrNull { it.id == id }
        return buildJsonObject {
            put("id", channel?.id?.toString() ?: "")
            put("cmd", channel?.let { "ffmpeg ${it.streamUrl}" } ?: "")
            put("error", if (channel == null) "nothing_to_play" else "")
        }
    }

    /** Half-hour programmes around now; filler channels have no guide, as many real channels don't. */
    private fun shortEpg(request: PortalRequest) = buildJsonArray {
        val channel = catalog.channels.firstOrNull { it.id.toString() == request.query["ch_id"] }
        if (channel == null || channel.genreId == FILLER_GENRE) return@buildJsonArray
        val size = request.query["size"]?.toIntOrNull()?.coerceIn(1, MAX_EPG_SIZE) ?: 2
        val slot = clock() / MILLIS / SLOT_SECONDS
        repeat(size) { i ->
            val start = (slot + i) * SLOT_SECONDS
            add(
                buildJsonObject {
                    put("id", "${channel.id}${slot + i}")
                    put("ch_id", channel.id.toString())
                    put("name", "${channel.name}: part ${(slot + i) % PARTS + 1}")
                    put("descr", "A test programme on ${channel.name}.")
                    // Real portals send the times as strings.
                    put("start_timestamp", start.toString())
                    put("stop_timestamp", (start + SLOT_SECONDS).toString())
                },
            )
        }
    }

    private fun channelJson(channel: MockChannel) = buildJsonObject {
        put("id", channel.id.toString())
        put("name", channel.name)
        put("number", channel.id.toString())
        // Like real portals: an opaque command that only create_link turns into a stream URL.
        put("cmd", "ffrt http://localhost/ch/${channel.id}_")
        put("logo", "")
        put("tv_genre_id", channel.genreId)
        put("censored", 0)
    }

    companion object {
        const val PAGE_SIZE = 14
        private const val FILLER_GENRE = "3"
        private const val SLOT_SECONDS = 30L * 60
        private const val MILLIS = 1000L
        private const val PARTS = 12
        private const val MAX_EPG_SIZE = 48
        const val DEFAULT_TOKEN_LIFETIME_MS = 60L * 60 * 1000
        private const val STATUS_NOT_REGISTERED = 2
        private const val WATCHDOG_SECONDS = 120
        private const val TOKEN_BYTES = 16
        private const val RANDOM_BYTES = 20
        private val API_PATHS = setOf("/portal.php", "/stalker_portal/server/load.php")
        private val WEB_PATHS = setOf("/c", "/stalker_portal/c")
        private const val WEB_PAGE =
            "<html><body><h1>Portalis TV mock portal</h1><p>Use this address in a profile.</p></body></html>"
    }
}

private fun cookie(request: PortalRequest, name: String): String? =
    request.headers["cookie"]?.split(';')?.map { it.trim() }
        ?.firstOrNull { it.startsWith("$name=") }?.substringAfter('=')

private fun json(js: JsonElement): PortalResponse {
    val body = buildJsonObject {
        put("js", js)
        put("text", "mock portal")
    }
    return PortalResponse(HTTP_OK, "application/json", body.toString())
}

private fun html(body: String) = PortalResponse(HTTP_OK, "text/html", body)

private fun notFound(message: String) = PortalResponse(HTTP_NOT_FOUND, "text/plain", message)

private const val HTTP_OK = 200
private const val HTTP_NOT_FOUND = 404

private fun SecureRandom.hex(bytes: Int) = ByteArray(bytes).also(::nextBytes).joinToString("") { "%02X".format(it) }
