package io.github.gobinathm.portalistv.tools.logopack

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

@Serializable
private data class ChannelEntry(
    val id: String,
    val name: String,
    @SerialName("alt_names") val altNames: List<String> = emptyList(),
    val country: String = "",
    @SerialName("is_nsfw") val nsfw: Boolean = false,
)

@Serializable
private data class LogoEntry(
    val channel: String,
    val feed: String? = null,
    @SerialName("in_use") val inUse: Boolean = true,
    val width: Int = 0,
    val format: String? = null,
    val url: String,
)

/** The channels of the iptv-org database (public domain) that have a logo Java can read, and those logos. */
internal class LogoDatabase(val channels: List<KnownChannel>, val logoUrls: Map<String, String>)

/** Loads the iptv-org database, keeping a copy in [cacheDir] for a week. */
internal class IptvOrg(private val cacheDir: File, private val http: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): LogoDatabase {
        val channels = json.decodeFromString<List<ChannelEntry>>(fetch("channels.json"))
        val logos = json.decodeFromString<List<LogoEntry>>(fetch("logos.json"))
        val logoUrls = logos.filter { it.inUse && it.format in READABLE_FORMATS }
            .groupBy { it.channel }
            .mapValues { (_, entries) -> best(entries).url }
        val known = channels.filter { !it.nsfw && it.id in logoUrls }
            .map { KnownChannel(it.id, it.name, it.altNames, it.country) }
        return LogoDatabase(known, logoUrls)
    }

    /** The channel's own logo over a feed's, and the smallest that still fills the logo box. */
    private fun best(entries: List<LogoEntry>): LogoEntry {
        val own = entries.filter { it.feed == null }.ifEmpty { entries }
        val bigEnough = own.filter { it.width >= LogoImages.MAX_WIDTH }
        return bigEnough.minByOrNull { it.width } ?: own.maxBy { it.width }
    }

    private fun fetch(name: String): String {
        val file = cacheDir.resolve(name)
        val fresh = file.isFile && System.currentTimeMillis() - file.lastModified() < TimeUnit.DAYS.toMillis(CACHE_DAYS)
        if (!fresh) {
            http.newCall(Request.Builder().url("$API$name").build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("iptv-org $name: HTTP ${response.code}")
                cacheDir.mkdirs()
                file.writeText(response.body.string())
            }
        }
        return file.readText()
    }

    private companion object {
        const val API = "https://iptv-org.github.io/api/"
        const val CACHE_DAYS = 7L
        val READABLE_FORMATS = setOf("PNG", "JPEG", "GIF")
    }
}
