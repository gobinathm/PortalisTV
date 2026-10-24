package io.github.gobinathm.portalistv.core.stalker

import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** A channel entry, or null when it lacks the id or command needed to play it. */
internal fun parseChannel(channel: JsonObject, endpoint: HttpUrl?): Channel? {
    val id = channel.string("id")
    val cmd = channel.string("cmd")
    if (id == null || cmd == null) return null
    return Channel(
        id = id,
        name = channel.string("name") ?: id,
        number = channel.int("number"),
        cmd = cmd,
        logoUrl = channel.string("logo")?.let { resolveLogo(it, endpoint) },
        genreId = channel.string("tv_genre_id"),
        censored = channel.flag("censored"),
    )
}

/** Logos are absolute URLs or file names under the portal's logo folder. */
private fun resolveLogo(logo: String, endpoint: HttpUrl?): String? =
    logo.toHttpUrlOrNull()?.toString() ?: endpoint?.resolve("/stalker_portal/misc/logos/320/$logo")?.toString()

/** A guide entry, or null without a title or valid times. */
internal fun parseProgramme(programme: JsonObject): Programme? {
    val title = programme.string("name")
    val start = programme.long("start_timestamp") ?: MISSING
    val stop = programme.long("stop_timestamp") ?: MISSING
    // A missing time fails the check too.
    if (title == null || start == MISSING || stop <= start) return null
    return Programme(title, programme.string("descr"), start, stop)
}

private const val MISSING = -1L
