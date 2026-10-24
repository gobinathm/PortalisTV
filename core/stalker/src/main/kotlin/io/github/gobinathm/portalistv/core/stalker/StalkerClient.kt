package io.github.gobinathm.portalistv.core.stalker

import io.github.gobinathm.portalistv.core.common.log.Log
import io.github.gobinathm.portalistv.core.common.log.Redactor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.io.IOException

/**
 * A session with one Stalker/Ministra portal.
 *
 * [connect] finds the API endpoint, performs the handshake and sends the device profile. Later
 * calls reuse the token; when the portal stops accepting it, the client reconnects once and
 * retries. The client is safe to share between coroutines.
 */
@Suppress("TooManyFunctions") // One method per portal call, plus the session steps.
class StalkerClient(
    httpClient: OkHttpClient,
    private val config: StalkerConfig,
    private val clock: () -> Long = System::currentTimeMillis,
    recorder: ExchangeRecorder = ExchangeRecorder.NONE,
) {
    private val portalUrl: HttpUrl = PortalEndpoints.parse(config.portalUrl, config.allowInsecureHttp)
    private val transport = StalkerTransport(httpClient, config, portalUrl, recorder = recorder)
    private val sessionLock = Mutex()

    private var endpoint: HttpUrl? = null

    /** The API endpoint in use, for resolving relative image paths; null before connecting. */
    internal val apiEndpoint: HttpUrl? get() = endpoint
    private var token: String? = null

    init {
        // Values with no recognizable shape are masked wherever they appear in logs.
        listOfNotNull(
            config.portalUrl,
            portalUrl.host,
            config.serial,
            config.deviceIds?.deviceId,
            config.deviceIds?.deviceId2,
            config.deviceIds?.signature,
            config.login,
            config.password,
        ).forEach(Redactor::register)
    }

    /** Connects (or reconnects) and returns the portal's view of this device. */
    suspend fun connect(): PortalAccount = sessionLock.withLock { openSession() }

    suspend fun genres(): List<Genre> = call("itv", "get_genres").objects().mapNotNull { genre ->
        val id = genre.string("id") ?: return@mapNotNull null
        Genre(id = id, title = genre.string("title") ?: id, censored = genre.flag("censored"))
    }

    /** One page of a genre's channels; [genreId] `"*"` lists all. Pages start at 1. */
    suspend fun channels(genreId: String, page: Int): Page<Channel> {
        val js = call("itv", "get_ordered_list", "genre" to genreId, "fav" to "0", "p" to page.toString())
        val data = js as? JsonObject ?: throw ProtocolException("Unexpected channel page")
        val items = dropSharedLogos(data["data"].objects().mapNotNull { parseChannel(it, endpoint) })
        return Page(
            items = items,
            totalItems = data.int("total_items") ?: items.size,
            pageSize = data.int("max_page_items") ?: 0,
        )
    }

    /** Every channel in one response. Large on some portals; prefer [channels] for browsing. */
    suspend fun allChannels(): List<Channel> {
        val js = call("itv", "get_all_channels")
        val data = (js as? JsonObject)?.get("data") ?: js
        return data.objects().mapNotNull { parseChannel(it, endpoint) }
    }

    /**
     * The programme on now and the next ones on a channel, up to [size], oldest first. Entries
     * without start and stop times are dropped. Portals without a guide return an empty list.
     */
    suspend fun shortEpg(channelId: String, size: Int = 2): List<Programme> {
        val js = call("itv", "get_short_epg", "ch_id" to channelId, "size" to size.toString())
        val list = (js as? JsonObject)?.get("data") ?: js
        return list.objects().mapNotNull(::parseProgramme).sortedBy { it.startSeconds }
    }

    /**
     * Resolves a channel's [cmd] into a URL the player can open.
     *
     * @throws ProtocolException if the portal returns no URL or one with a scheme outside the
     * allow-list (no `file:` or `content:` URLs from a portal, ever).
     */
    suspend fun createLink(cmd: String): String {
        val js = call("itv", "create_link", "cmd" to cmd, "series" to "", "forced_storage" to "0", "disable_ad" to "0")
        val link = (js as? JsonObject)?.string("cmd") ?: throw ProtocolException("No stream link")
        return StreamUrls.clean(link)
    }

    /**
     * One API call with the current token; reconnects once if the portal no longer accepts it.
     * Internal so each content area (e.g. on-demand, in Vod.kt) can keep its calls in its own file.
     */
    internal suspend fun call(type: String, action: String, vararg params: Pair<String, String>): JsonElement {
        val query = listOf("type" to type, "action" to action) + params
        val (currentEndpoint, currentToken) = sessionLock.withLock {
            if (endpoint == null) openSession()
            checkNotNull(endpoint) to token
        }
        when (val reply = transport.get(currentEndpoint, query, currentToken)) {
            is Reply.Ok -> return reply.js
            Reply.AuthFailed -> Log.i(TAG) { "Session expired during $type/$action; reconnecting" }
        }
        val (renewedEndpoint, renewedToken) = sessionLock.withLock {
            // Another caller may have reconnected while this one waited.
            if (token == currentToken) openSession()
            checkNotNull(endpoint) to token
        }
        return when (val retry = transport.get(renewedEndpoint, query, renewedToken)) {
            is Reply.Ok -> retry.js
            Reply.AuthFailed -> throw AuthorizationException()
        }
    }

    /** Must hold [sessionLock]. */
    private suspend fun openSession(): PortalAccount {
        val (found, handshake) = endpoint?.let { it to handshake(it) } ?: discover()
        endpoint = found
        token = handshake.token
        val account = getProfile(found, handshake)
        if (account.status != 0) throw AccountBlockedException(account.status, account.message)
        return account
    }

    private suspend fun discover(): Pair<HttpUrl, Handshake> {
        var lastError: Throwable? = null
        for (candidate in PortalEndpoints.candidates(portalUrl)) {
            try {
                return candidate to handshake(candidate)
            } catch (e: IOException) {
                lastError = e
            }
        }
        throw PortalNotFoundException(lastError)
    }

    internal data class Handshake(val token: String, val random: String?)

    private suspend fun handshake(endpoint: HttpUrl): Handshake {
        val reply = transport.get(endpoint, listOf("type" to "stb", "action" to "handshake", "token" to ""), null)
        val js = (reply as? Reply.Ok)?.js as? JsonObject ?: throw ProtocolException("Handshake rejected")
        val newToken = js.string("token") ?: throw ProtocolException("Handshake without token")
        Redactor.register(newToken)
        return Handshake(newToken, js.string("random"))
    }

    private suspend fun getProfile(endpoint: HttpUrl, handshake: Handshake): PortalAccount {
        val params = profileParams(config, handshake.random, clock())
        val reply = transport.get(endpoint, params, handshake.token)
        val js = (reply as? Reply.Ok)?.js as? JsonObject ?: throw AuthorizationException("Profile rejected")
        return PortalAccount(
            id = js.string("id"),
            name = js.string("name") ?: js.string("fname"),
            status = js.int("status") ?: 0,
            message = js.string("msg"),
        )
    }

    private companion object {
        const val TAG = "Stalker"
    }
}
