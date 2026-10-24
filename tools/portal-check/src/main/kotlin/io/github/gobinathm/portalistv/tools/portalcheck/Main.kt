package io.github.gobinathm.portalistv.tools.portalcheck

import io.github.gobinathm.portalistv.core.catalog.DeviceCatalog
import io.github.gobinathm.portalistv.core.catalog.resolve
import io.github.gobinathm.portalistv.core.common.log.Redactor
import io.github.gobinathm.portalistv.core.model.DeviceIdentity
import io.github.gobinathm.portalistv.core.model.Portal
import io.github.gobinathm.portalistv.core.model.Profile
import io.github.gobinathm.portalistv.core.model.Secret
import io.github.gobinathm.portalistv.core.network.HttpClients
import io.github.gobinathm.portalistv.core.stalker.AccountBlockedException
import io.github.gobinathm.portalistv.core.stalker.CaptureRedaction
import io.github.gobinathm.portalistv.core.stalker.DeviceIds
import io.github.gobinathm.portalistv.core.stalker.ExchangeRecorder
import io.github.gobinathm.portalistv.core.stalker.StalkerClient
import io.github.gobinathm.portalistv.core.stalker.StalkerConfig
import io.github.gobinathm.portalistv.core.stalker.VodSection
import io.github.gobinathm.portalistv.core.stalker.seriesContents
import io.github.gobinathm.portalistv.core.stalker.vodCategories
import io.github.gobinathm.portalistv.core.stalker.vodLink
import io.github.gobinathm.portalistv.core.stalker.vodPage
import kotlinx.coroutines.runBlocking
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import java.util.TimeZone
import io.github.gobinathm.portalistv.core.stalker.DeviceIdentity as ClientIdentity

/**
 * Connects to a real portal with the app's own client and records the exchange, redacted, so
 * the protocol can be checked without a TV or emulator.
 *
 * Usage: `./gradlew :tools:portal-check:run --args="portal-check.properties"`. The settings file
 * holds the portal URL and MAC and must never be committed (it is gitignored). Console output
 * shows only counts and outcomes; the full redacted exchange goes to `build/portal-check/`.
 */
fun main(args: Array<String>) {
    val settingsFile = File(args.firstOrNull() ?: "portal-check.properties")
    require(settingsFile.isFile) { "Settings file not found. Copy portal-check.properties.example and fill it in." }
    val settings = Properties().apply { settingsFile.inputStream().use(::load) }
    val config = configFrom(settings)

    val capture = File("build/portal-check").apply { mkdirs() }
        .resolve("capture-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}.txt")
    val recorder = ExchangeRecorder { exchange ->
        val query = exchange.query.joinToString("&") { (name, value) -> CaptureRedaction.redact("$name=$value") }
        capture.appendText("### ${exchange.label} HTTP ${exchange.httpCode}\n$query\n")
        capture.appendText("${CaptureRedaction.redact(exchange.body)}\n\n")
    }

    val client = StalkerClient(HttpClients.defaultClient(), config, recorder = recorder)
    val ok = runBlocking { runSteps(client) }
    println("Redacted capture: ${capture.path}")
    if (!ok) System.exit(1)
}

private suspend fun runSteps(client: StalkerClient): Boolean {
    var channelCmd: String? = null
    var channelIds: List<String> = emptyList()
    val steps: List<Pair<String, suspend () -> String>> = listOf(
        "connect" to {
            val account = client.connect()
            "status ${account.status}${account.message?.let { ", portal message present" }.orEmpty()}"
        },
        "genres" to { "${client.genres().size} categories" },
        "channels" to {
            val page = client.channels(genreId = "*", page = 1)
            channelCmd = page.items.firstOrNull()?.cmd
            channelIds = page.items.map { it.id }
            "${page.items.size} on page 1, ${page.totalItems} in total, ${page.pageCount} pages"
        },
        "short_epg" to {
            // Some channels have no guide; report how many of the first page do.
            val guides = channelIds.take(EPG_SAMPLE).map { client.shortEpg(it) }
            "${guides.count { it.isNotEmpty() }} of ${guides.size} channels have now/next"
        },
        "vod" to { vodSummary(client, VodSection.MOVIES) },
        "series" to { vodSummary(client, VodSection.SERIES) },
        "create_link" to {
            val cmd = channelCmd ?: error("no channel to resolve")
            "stream scheme ${client.createLink(cmd).substringBefore(':')}"
        },
    )
    for ((name, step) in steps) {
        val result = runCatching { step() }
        result.onSuccess { println("OK    $name: $it") }
        result.onFailure { e ->
            println("FAIL  $name: ${e.javaClass.simpleName}: ${Redactor.redact(e.message.orEmpty())}")
            (e as? AccountBlockedException)?.portalMessage?.let { println("      portal says: ${Redactor.redact(it)}") }
            e.cause?.let { cause ->
                println("      caused by ${cause.javaClass.simpleName}: ${Redactor.redact(cause.message.orEmpty())}")
            }
            return false
        }
    }
    return true
}

/**
 * Checks on demand the way the app uses it: categories, a page, a movie played through its file,
 * and a series drilled down to an episode's file.
 */
private suspend fun vodSummary(client: StalkerClient, section: VodSection): String {
    val categories = client.vodCategories(section).filter { it.id != "*" }
    if (categories.isEmpty()) return "no categories"
    // The first categories may hold only series (or only movies); look a little further.
    val pages = categories.take(SEARCH_CATEGORIES).map { client.vodPage(section, it.id, page = 1) }
    val movie = pages.firstNotNullOfOrNull { page -> page.items.firstOrNull { !it.isSeries } }
    val series = pages.firstNotNullOfOrNull { page -> page.items.firstOrNull { it.isSeries } }
    val parts = mutableListOf("${categories.size} categories", "${pages.first().totalItems} items in the first")
    movie?.let { parts += "movie ${outcome { fileLink(client, section, it.id) }}" }
    series?.let { parts += seriesSummary(client, section, it.id) }
    return parts.joinToString(", ")
}

private suspend fun seriesSummary(client: StalkerClient, section: VodSection, seriesId: String): String {
    val season = client.seriesContents(section, seriesId).items.firstOrNull { it.isSeason }
    val episodes = season?.let { client.seriesContents(section, seriesId, it.id).items.filter { e -> e.isEpisode } }
    val episode = episodes?.firstOrNull()
    if (season == null || episode == null) return "series without seasons or episodes"
    val link = outcome { fileLink(client, section, seriesId, season.id, episode.id) }
    return "series: ${episodes.size} episodes in a season, episode $link"
}

/** The scheme of the stream link for an item's first file, as the app resolves it. */
private suspend fun fileLink(
    client: StalkerClient,
    section: VodSection,
    itemId: String,
    seasonId: String = "0",
    episodeId: String = "0",
): String {
    val file = client.seriesContents(section, itemId, seasonId, episodeId).items.first { it.isFile }
    return "link ${client.vodLink("/media/file_${file.id}.mpg").substringBefore(':')}"
}

private suspend fun outcome(block: suspend () -> String): String =
    runCatching { block() }.getOrElse { "failed (${it.javaClass.simpleName})" }

private const val SEARCH_CATEGORIES = 6

private fun configFrom(settings: Properties): StalkerConfig {
    fun value(key: String) = settings.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }
    val url = requireNotNull(value("url")) { "url is required" }
    val mac = requireNotNull(value("mac")) { "mac is required" }.uppercase()
    val catalog = DeviceCatalog.load()
    val model = value("model") ?: catalog.defaultModel
    val profile = Profile(
        uuid = "portal-check",
        name = "portal-check",
        createdAtMillis = 0,
        portal = Portal(url = Secret(url)),
        device = DeviceIdentity(stbModel = model, mac = Secret(mac)),
    ).let { it.copy(firmware = it.firmware.copy(presetId = catalog.model(model)?.defaultFirmwareId)) }
    val reported = catalog.resolve(profile)
    val ids = value("deviceId")?.let { DeviceIds(it, value("deviceId2").orEmpty(), value("signature").orEmpty()) }
    return StalkerConfig(
        portalUrl = url,
        allowInsecureHttp = value("allowInsecureHttp").toBoolean(),
        mac = mac,
        serial = value("serial") ?: DEFAULT_SERIAL,
        language = value("language") ?: "en",
        timezone = value("timezone") ?: TimeZone.getDefault().id,
        identity = ClientIdentity(
            stbModel = reported.stbModel,
            userAgent = reported.userAgent,
            imageVersion = reported.imageVersion,
            imageDate = reported.imageDate,
            imageDescription = reported.imageDescription,
            jsApiVersion = reported.jsApiVersion,
            stbApiVersion = reported.stbApiVersion,
            playerEngineVersion = reported.playerEngineVersion,
            hardwareVersion = null,
        ),
        deviceIds = ids,
    )
}

private const val EPG_SAMPLE = 5
private const val DEFAULT_SERIAL = "062014N012345"
