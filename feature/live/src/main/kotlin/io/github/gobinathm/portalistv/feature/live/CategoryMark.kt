package io.github.gobinathm.portalistv.feature.live

import androidx.annotation.DrawableRes
import io.github.gobinathm.portalistv.core.model.ALL_CATEGORIES
import io.github.gobinathm.portalistv.core.model.LiveCategory

/** How a category is drawn in the rail: a genre icon when its name says what it is, else its initials. */
internal sealed interface CategoryMark {
    data class Icon(@DrawableRes val res: Int) : CategoryMark

    data class Initials(val text: String) : CategoryMark
}

/**
 * Portals send only a category's name, so the icon is guessed from words in it. Language and
 * other categories get initials rather than a guessed flag, which would often be wrong.
 */
internal fun LiveCategory.mark(): CategoryMark {
    val words = title.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
    val icon = when {
        id == ALL_CATEGORIES -> R.drawable.ic_category_all
        adult -> R.drawable.ic_category_adult
        else -> keywordIcon(words)
    }
    return icon?.let { CategoryMark.Icon(it) } ?: CategoryMark.Initials(initials(words))
}

private val keywordIcons: List<Pair<List<String>, Int>> = listOf(
    listOf("adult", "xxx") to R.drawable.ic_category_adult,
    listOf("kid", "cartoon", "child", "junior", "anime", "family") to R.drawable.ic_category_kids,
    listOf("sport", "football", "soccer", "cricket", "nba", "nfl", "nhl", "mlb", "ufc", "wwe", "f1", "racing", "ppv") to
        R.drawable.ic_category_sports,
    listOf("news") to R.drawable.ic_category_news,
    listOf("movie", "cinema", "film", "vod") to R.drawable.ic_category_movies,
    listOf("music", "radio", "song") to R.drawable.ic_category_music,
    listOf("documentar", "education", "learning", "science", "nature", "history", "discovery") to
        R.drawable.ic_category_learning,
)

private fun keywordIcon(words: List<String>): Int? =
    keywordIcons.firstOrNull { (keywords, _) -> words.any { word -> keywords.any(word::startsWith) } }?.second

private fun initials(words: List<String>): String = when {
    words.isEmpty() -> "#"
    words.size == 1 -> words[0].take(2)
    else -> "${words[0].first()}${words[1].first()}"
}.uppercase()
