package io.github.gobinathm.portalistv.tools.mockportal

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** A movie or series served by the mock portal's on-demand area. */
data class MockVod(
    val id: Int,
    val name: String,
    val categoryId: String,
    val streamUrl: String,
    /** Episodes per season; empty for a movie. */
    val seasons: List<Int> = emptyList(),
) {
    val isSeries: Boolean get() = seasons.isNotEmpty()
}

/**
 * The on-demand calls, shaped like a current Ministra portal: lists are drilled into with
 * `movie_id`, `season_id` and `episode_id`, and movies and episodes play through their file
 * (`/media/file_<id>.mpg`).
 */
internal class MockVodApi(private val catalog: Catalog) {
    fun categories() = buildJsonArray {
        add(category("*", "All"))
        catalog.vodCategories.forEach { (id, title) -> add(category(id, title)) }
    }

    fun orderedList(query: Map<String, String>): JsonObject {
        val movieId = query["movie_id"]?.toIntOrNull()
        val page = query["p"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val entries: List<JsonObject> = if (movieId == null) {
            val category = query["category"] ?: "*"
            catalog.vod.filter { category == "*" || it.categoryId == category }.reversed().map(::itemJson)
        } else {
            contents(movieId, query["season_id"]?.toIntOrNull() ?: 0, query["episode_id"]?.toIntOrNull() ?: 0)
        }
        return buildJsonObject {
            put("total_items", entries.size.toString())
            put("max_page_items", MockPortal.PAGE_SIZE)
            put("cur_page", 0)
            val pageEntries = entries.drop((page - 1) * MockPortal.PAGE_SIZE).take(MockPortal.PAGE_SIZE)
            put("data", buildJsonArray { pageEntries.forEach(::add) })
        }
    }

    /** `/media/file_<id>.mpg` for a file; anything else plays nothing, as on real portals. */
    fun createLink(query: Map<String, String>): JsonElement {
        val fileId = query["cmd"]?.removePrefix("/media/file_")?.removeSuffix(".mpg")?.toIntOrNull()
        val item = fileId?.let { catalog.vod.firstOrNull { vod -> vod.id == it / FILE_ID_BASE } }
        return buildJsonObject {
            put("id", item?.id ?: 0)
            put("cmd", item?.streamUrl ?: "")
            put("error", if (item == null) "nothing_to_play" else "")
        }
    }

    /** Seasons of a series, a season's episodes, or the single file of a movie or episode. */
    private fun contents(movieId: Int, seasonId: Int, episodeId: Int): List<JsonObject> {
        val item = catalog.vod.firstOrNull { it.id == movieId } ?: return emptyList()
        return when {
            !item.isSeries || episodeId != 0 -> listOf(fileJson(item, episodeId))
            seasonId == 0 -> item.seasons.indices.reversed().map { index ->
                buildJsonObject {
                    put("id", (movieId * SEASON_ID_BASE + index + 1).toString())
                    put("video_id", movieId.toString())
                    put("season_number", (index + 1).toString())
                    put("name", "Season ${index + 1}. ${item.seasons[index]} episodes available")
                    put("is_season", true)
                }
            }
            else -> {
                val season = seasonId % SEASON_ID_BASE
                // Newest first, as real portals list them.
                (item.seasons.getOrElse(season - 1) { 0 } downTo 1).map { number ->
                    buildJsonObject {
                        put("id", (seasonId * SEASON_ID_BASE + number).toString())
                        put("season_id", seasonId.toString())
                        put("series_number", number.toString())
                        put("series_name", "Part $number")
                        put("name", "Episode $number. Part $number")
                        put("is_episode", true)
                    }
                }
            }
        }
    }

    private fun fileJson(item: MockVod, episodeId: Int) = buildJsonObject {
        // The file id encodes the item, so create_link can find the stream again.
        put("id", (item.id * FILE_ID_BASE + episodeId % FILE_ID_BASE).toString())
        put("video_id", item.id.toString())
        put("name", "English / Test quality")
        put("is_file", true)
        put("cmd", "http://localhost/not-playable-directly")
    }

    private fun itemJson(item: MockVod) = buildJsonObject {
        put("id", item.id.toString())
        put("name", item.name)
        put("description", "A freely licensed test ${if (item.isSeries) "series" else "movie"}.")
        put("screenshot_uri", "")
        put("year", "2008")
        put("rating_imdb", 0)
        put("genres_str", "Test")
        put("time", if (item.isSeries) "0" else "10")
        put("cmd", "/media/${item.id}.mpg")
        put("is_series", if (item.isSeries) "1" else "0")
        put("censored", "0")
        put("category_id", item.categoryId)
    }

    private fun category(id: String, title: String) = buildJsonObject {
        put("id", id)
        put("title", title)
        put("alias", title.lowercase())
        put("censored", 0)
    }

    private companion object {
        const val SEASON_ID_BASE = 100
        const val FILE_ID_BASE = 100_000
    }
}
