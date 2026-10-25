package io.github.gobinathm.portalistv.tools.logopack

import io.github.gobinathm.portalistv.core.model.LogoPack
import io.github.gobinathm.portalistv.core.network.HttpClients
import io.github.gobinathm.portalistv.core.stalker.Channel
import io.github.gobinathm.portalistv.core.stalker.StalkerClient
import io.github.gobinathm.portalistv.tools.portalcheck.loadPortalSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.Properties
import java.util.concurrent.TimeUnit

/**
 * Makes channel logos for your own build of the app. It reads your portal's channels (with the
 * settings of tools/portal-check), finds each one without a logo in the iptv-org database, and
 * writes small PNGs and an index to `local-logos/`, which the next app build packs in. Nothing is
 * committed: the folder is git-ignored, and the logos are their owners' trademarks.
 *
 * Usage: `./gradlew :tools:logo-pack:run --args="[portal-check.properties] [--only=HINDI,TAMIL]"`.
 * `--only` keeps channels whose category name contains one of the words. Leave the logos out of a
 * build with `-Pportalis.logos=false`, or delete `local-logos/`.
 */
fun main(args: Array<String>) {
    val settingsFile = File(args.firstOrNull { !it.startsWith("--") } ?: "portal-check.properties")
    require(settingsFile.isFile) { "Settings file not found. Copy portal-check.properties.example and fill it in." }
    val only = args.firstOrNull { it.startsWith(ONLY) }?.removePrefix(ONLY)
        ?.split(',')?.map { it.trim().lowercase() }?.filter { it.isNotEmpty() }.orEmpty()

    val work = File("build/logo-pack").apply { mkdirs() }
    val output = File("local-logos/${LogoPack.DIR}")
    val http = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_S, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_S, TimeUnit.SECONDS)
        .addInterceptor { it.proceed(it.request().newBuilder().header("User-Agent", USER_AGENT).build()) }
        .build()

    runBlocking {
        val portal = StalkerClient(HttpClients.defaultClient(), loadPortalSettings(settingsFile))
        portal.connect()
        val categories = portal.genres().associate { it.id to it.title }
        val channels = withoutLogos(portal.allChannels()).filter { channel ->
            val category = categories[channel.genreId].orEmpty().lowercase()
            !channel.censored && (only.isEmpty() || only.any { it in category })
        }
        println("${channels.size} channels without a logo from the portal")

        val database = withContext(Dispatchers.IO) { IptvOrg(work, http).load() }
        val matcher = LogoMatcher(database.channels)
        // Channels whose name or category names a country show which countries this portal carries;
        // those break ties for the rest.
        val preferred = channels.filter { matcher.hasCountryHint(it.name, categories[it.genreId]) }
            .mapNotNull { matcher.match(it.name, categories[it.genreId])?.country }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key }
        val matches = channels.associateWith { matcher.match(it.name, categories[it.genreId], preferred) }
        val ids = matches.values.filterNotNull().map { it.id }.distinct()
        println("${matches.values.count { it != null }} matched to ${ids.size} logos; downloading")

        output.deleteRecursively()
        output.mkdirs()
        val files = download(ids, database, work.resolve("originals"), output, http)
        writeIndex(output, matches, files)
        writeReport(work.resolve("report.tsv"), matches, categories, files)
        val kilobytes = output.listFiles().orEmpty().sumOf { it.length() } / BYTES_PER_KB
        println("${files.size} logos, $kilobytes KB in ${output.parentFile.path}/")
        println("What each channel matched: ${work.path}/report.tsv")
    }
}

/** Channels the portal sends no logo for, counting a logo most channels share as none. */
private fun withoutLogos(channels: List<Channel>): List<Channel> {
    val shared = channels.mapNotNull { it.logoUrl }.groupingBy { it }.eachCount().filterValues { it >= SHARED }.keys
    return channels.filter { it.logoUrl == null || it.logoUrl in shared }
}

/** Downloads and converts each logo; @return the file written per iptv-org channel id. */
private suspend fun download(
    ids: List<String>,
    database: LogoDatabase,
    originals: File,
    output: File,
    http: OkHttpClient,
): Map<String, String> = withContext(Dispatchers.IO) {
    originals.mkdirs()
    val permits = Semaphore(PARALLEL_DOWNLOADS)
    ids.map { id ->
        async {
            permits.withPermit {
                val source = runCatching { original(database.logoUrls.getValue(id), originals, id, http) }.getOrNull()
                val logo = source?.let(LogoImages::pack) ?: return@withPermit null
                // The app shows light logos without the light plate it puts behind others.
                val name = id.replace(UNSAFE, "_") + if (logo.light) LogoPack.LIGHT_SUFFIX else ".png"
                output.resolve(name).writeBytes(logo.png)
                id to name
            }
        }
    }.awaitAll().filterNotNull().toMap()
}

/** The downloaded logo, kept in [originals] so later runs don't fetch it again. */
private fun original(url: String, originals: File, id: String, http: OkHttpClient): ByteArray? {
    val cached = originals.resolve(id.replace(UNSAFE, "_"))
    if (cached.isFile) return cached.readBytes()
    val bytes = http.newCall(Request.Builder().url(url).build()).execute().use { response ->
        // Reads at most one byte past the limit, so an oversized download is never held whole.
        val source = response.body.source().takeIf { response.isSuccessful }
        source?.request(MAX_DOWNLOAD_BYTES + 1)
        source?.buffer?.takeIf { it.size <= MAX_DOWNLOAD_BYTES }?.readByteArray()
    }
    return bytes?.also(cached::writeBytes)
}

private fun writeIndex(output: File, matches: Map<Channel, KnownChannel?>, files: Map<String, String>) {
    val index = Properties()
    matches.forEach { (channel, known) ->
        files[known?.id]?.let { index[LogoPack.key(channel.name)] = it }
    }
    output.resolve(LogoPack.INDEX.substringAfter('/')).outputStream().use {
        index.store(it, "Made by tools/logo-pack from the iptv-org database. Logos are their owners' trademarks.")
    }
}

/** What each channel matched, for checking the result; it names your portal's channels, so it stays in build/. */
private fun writeReport(
    file: File,
    matches: Map<Channel, KnownChannel?>,
    categories: Map<String, String>,
    files: Map<String, String>,
) {
    file.bufferedWriter().use { out ->
        out.write("channel\tcategory\tlogo\n")
        matches.entries.sortedBy { it.key.name }.forEach { (channel, known) ->
            val result = when {
                known == null -> "-"
                known.id !in files -> "${known.id} (download failed)"
                else -> known.id
            }
            out.write("${channel.name}\t${categories[channel.genreId].orEmpty()}\t$result\n")
        }
    }
}

private const val ONLY = "--only="
private const val USER_AGENT = "PortalisTV-logo-pack"
private const val TIMEOUT_S = 20L
private const val SHARED = 3
private const val PARALLEL_DOWNLOADS = 8
private const val MAX_DOWNLOAD_BYTES = 5L * 1024 * 1024
private const val BYTES_PER_KB = 1024
private val UNSAFE = Regex("[^A-Za-z0-9._-]")
