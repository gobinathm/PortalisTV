package io.github.gobinathm.portalistv.tools.portalcheck

import io.github.gobinathm.portalistv.core.catalog.resolve
import io.github.gobinathm.portalistv.core.common.log.Redactor
import io.github.gobinathm.portalistv.core.network.HttpClients
import io.github.gobinathm.portalistv.core.stalker.AccountBlockedException
import io.github.gobinathm.portalistv.core.stalker.CaptureRedaction
import io.github.gobinathm.portalistv.core.stalker.ExchangeRecorder
import io.github.gobinathm.portalistv.core.stalker.StalkerClient
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
    val config = loadPortalSettings(settingsFile)

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
    var logoUrls: List<Pair<String, String?>> = emptyList()
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
            logoUrls = page.items.map { it.id to it.logoUrl }
            "${page.items.size} on page 1, ${page.totalItems} in total, ${page.pageCount} pages"
        },
        "short_epg" to {
            // Some channels have no guide; report how many of the first page do.
            val guides = channelIds.take(EPG_SAMPLE).map { client.shortEpg(it) }
            "${guides.count { it.isNotEmpty() }} of ${guides.size} channels have now/next"
        },
        "logos" to {
            // Some portals send the same logo for every channel; compare with a file named after the id.
            val distinct = logoUrls.mapNotNull { it.second }.distinct()
            val (id, url) = logoUrls.drop(1).firstOrNull { it.second != null } ?: ("" to null)
            val byId = url?.substringBeforeLast('/')?.let { "$it/$id.png" }
            "${distinct.size} distinct logo URLs for ${logoUrls.size} channels; " +
                "as sent ${url?.let(::httpStatus)}, named after the id ${byId?.let(::httpStatus)}"
        },
        "guide" to {
            // The guide needs hours ahead, not just now/next; see how far one request reaches.
            val guides = channelIds.take(EPG_SAMPLE).map { client.shortEpg(it, size = LONG_EPG_SIZE) }
            val longest = guides.maxByOrNull { it.size }.orEmpty()
            val seconds = if (longest.size < 2) 0 else longest.last().stopSeconds - longest.first().startSeconds
            val hours = seconds / SECONDS_PER_HOUR
            "up to ${longest.size} programmes ($hours h) per channel when asking for $LONG_EPG_SIZE"
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

/** HTTP status and size of an image, without printing its address. */
private fun httpStatus(url: String): String = runCatching {
    HttpClients.defaultClient().newCall(okhttp3.Request.Builder().url(url).head().build()).execute().use {
        "${it.code} (${it.header("Content-Type")}, ${it.header("Content-Length")} bytes)"
    }
}.getOrElse { "failed (${it.javaClass.simpleName})" }

private suspend fun outcome(block: suspend () -> String): String =
    runCatching { block() }.getOrElse { "failed (${it.javaClass.simpleName})" }

private const val SEARCH_CATEGORIES = 6

private const val EPG_SAMPLE = 5
private const val LONG_EPG_SIZE = 30
private const val SECONDS_PER_HOUR = 3600
