package io.github.gobinathm.portalistv.tools.portalcheck

import io.github.gobinathm.portalistv.core.catalog.DeviceCatalog
import io.github.gobinathm.portalistv.core.catalog.resolve
import io.github.gobinathm.portalistv.core.common.log.Redactor
import io.github.gobinathm.portalistv.core.model.DeviceIdentity
import io.github.gobinathm.portalistv.core.model.Portal
import io.github.gobinathm.portalistv.core.model.Profile
import io.github.gobinathm.portalistv.core.model.Secret
import io.github.gobinathm.portalistv.core.network.HttpClients
import io.github.gobinathm.portalistv.core.stalker.CaptureRedaction
import io.github.gobinathm.portalistv.core.stalker.DeviceIds
import io.github.gobinathm.portalistv.core.stalker.ExchangeRecorder
import io.github.gobinathm.portalistv.core.stalker.StalkerClient
import io.github.gobinathm.portalistv.core.stalker.StalkerConfig
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
    val steps: List<Pair<String, suspend () -> String>> = listOf(
        "connect" to {
            val account = client.connect()
            "status ${account.status}${account.message?.let { ", portal message present" }.orEmpty()}"
        },
        "genres" to { "${client.genres().size} categories" },
        "channels" to {
            val page = client.channels(genreId = "*", page = 1)
            channelCmd = page.items.firstOrNull()?.cmd
            "${page.items.size} on page 1, ${page.totalItems} in total, ${page.pageCount} pages"
        },
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
            e.cause?.let { cause ->
                println("      caused by ${cause.javaClass.simpleName}: ${Redactor.redact(cause.message.orEmpty())}")
            }
            return false
        }
    }
    return true
}

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

private const val DEFAULT_SERIAL = "062014N012345"
