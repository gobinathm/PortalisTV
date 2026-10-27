package io.github.gobinathm.portalistv.core.stalker

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** The portal's two on-demand areas. Older portals list series among movies instead. */
enum class VodSection(internal val type: String) { MOVIES("vod"), SERIES("series") }

/** A movie, a series, or one of a series' seasons, as an on-demand list returns it. */
data class VodItem(
    val id: String,
    val name: String,
    val description: String?,
    val posterUrl: String?,
    val year: String?,
    val rating: String?,
    val genres: String?,
    val director: String?,
    val actors: String?,
    /** Running time in minutes, when the portal says. */
    val durationMinutes: Int?,
    /** Passed back to [vodLink]; absent for entries that only lead to seasons or episodes. */
    val cmd: String?,
    val isSeries: Boolean,
    val isSeason: Boolean,
    val isEpisode: Boolean,
    /** Season or episode number, for seasons and episodes. */
    val number: Int?,
    /** An episode's own title, without the "Episode n." the portal puts in [name]. */
    val episodeTitle: String?,
    val censored: Boolean,
    /** A playable file of a movie or episode; played through its id. */
    val isFile: Boolean,
    /** Episode numbers of a series listed among movies (older portals), or of a season. */
    val episodes: List<Int>,
)

suspend fun StalkerClient.vodCategories(section: VodSection): List<Genre> =
    call(section.type, "get_categories").objects().mapNotNull { category ->
        val id = category.string("id") ?: return@mapNotNull null
        Genre(id = id, title = category.string("title") ?: id, censored = category.flag("censored"))
    }

/**
 * One page of a category, newest first. Pages start at 1. [search] matches titles on the portal;
 * [yearId] is an id from [vodYears]. Portals that don't filter ignore them and send the whole category.
 */
@Suppress("SpreadOperator") // At most two optional parameters; the copy is negligible next to the request.
suspend fun StalkerClient.vodPage(
    section: VodSection,
    categoryId: String,
    page: Int,
    search: String? = null,
    yearId: String? = null,
): Page<VodItem> = vodList(
    section.type,
    "category" to categoryId,
    "genre" to "*",
    "sortby" to "added",
    "fav" to "0",
    "p" to page.toString(),
    *listOfNotNull(search?.let { "search" to it }, yearId?.let { "years" to it }).toTypedArray(),
)

/** A year (or range of years) the portal can filter a category by. */
data class VodYear(val id: String, val title: String)

/** The years [vodPage] can filter a category by, as the portal lists them; empty when it doesn't. */
suspend fun StalkerClient.vodYears(section: VodSection, categoryId: String): List<VodYear> =
    call(section.type, "get_years", "category" to categoryId).objects().mapNotNull { year ->
        val id = year.string("id") ?: return@mapNotNull null
        VodYear(id, year.string("title") ?: id)
    }.filter { it.id != "*" }

/**
 * Drills into a series in [section]: its seasons, a season's episodes (with [seasonId]), or an
 * episode's files (with [episodeId]).
 */
suspend fun StalkerClient.seriesContents(
    section: VodSection,
    seriesId: String,
    seasonId: String = "0",
    episodeId: String = "0",
    page: Int = 1,
): Page<VodItem> = vodList(
    section.type,
    "movie_id" to seriesId,
    "season_id" to seasonId,
    "episode_id" to episodeId,
    "p" to page.toString(),
)

/**
 * Resolves an on-demand [cmd] into a URL the player can open; [episode] picks an episode of a
 * series listed among movies.
 */
suspend fun StalkerClient.vodLink(cmd: String, episode: Int? = null): String {
    val js = call(
        "vod",
        "create_link",
        "cmd" to cmd,
        "series" to (episode?.toString() ?: ""),
        "forced_storage" to "0",
        "disable_ad" to "0",
    )
    val link = (js as? JsonObject)?.string("cmd") ?: throw ProtocolException("No stream link")
    return StreamUrls.clean(link)
}

private suspend fun StalkerClient.vodList(type: String, vararg params: Pair<String, String>): Page<VodItem> {
    val data = call(type, "get_ordered_list", *params) as? JsonObject ?: throw ProtocolException("Unexpected list")
    val items = data["data"].objects().mapNotNull { parseVodItem(it, apiEndpoint) }
    return Page(items, totalItems = data.int("total_items") ?: items.size, pageSize = data.int("max_page_items") ?: 0)
}

internal fun parseVodItem(item: JsonObject, endpoint: HttpUrl?): VodItem? {
    val id = item.string("id") ?: return null
    return VodItem(
        id = id,
        name = item.string("name") ?: item.string("o_name") ?: id,
        description = item.string("description"),
        posterUrl = item.string("screenshot_uri")?.let { resolveImage(it, endpoint) },
        year = item.string("year")?.takeUnless { it == "0" || it == "N/A" },
        rating = item.string("rating_imdb")?.takeUnless { it == "0" || it == "N/A" },
        genres = item.string("genres_str"),
        director = item.string("director"),
        actors = item.string("actors"),
        durationMinutes = item.int("time")?.takeIf { it > 0 },
        cmd = item.string("cmd"),
        isSeries = item.flag("is_series"),
        isSeason = item.flag("is_season"),
        isEpisode = item.flag("is_episode"),
        number = item.int("season_number") ?: item.int("series_number"),
        episodeTitle = item.string("series_name"),
        censored = item.flag("censored"),
        isFile = item.flag("is_file"),
        episodes = (item["series"] as? JsonArray).orEmpty()
            .mapNotNull { (it as? JsonPrimitive)?.content?.toIntOrNull() },
    )
}

/** Posters are absolute URLs or paths on the portal's host. */
private fun resolveImage(path: String, endpoint: HttpUrl?): String? =
    path.toHttpUrlOrNull()?.toString() ?: endpoint?.resolve(path)?.toString()
