package io.github.gobinathm.portalistv.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.gobinathm.portalistv.core.designsystem.R
import io.github.gobinathm.portalistv.core.model.ALL_CATEGORIES
import io.github.gobinathm.portalistv.core.model.FAVORITES_CATEGORY
import io.github.gobinathm.portalistv.core.model.LiveCategory
import io.github.gobinathm.portalistv.core.model.VodCategory

/**
 * How a category is drawn: a genre icon when its name says what it is, the language's own script
 * when it names a language, else its initials.
 */
sealed interface CategoryMark {
    data class Icon(@DrawableRes val res: Int) : CategoryMark

    data class Initials(val text: String) : CategoryMark

    data class Script(val language: Language) : CategoryMark
}

/**
 * Portals send only a category's name, so the mark is guessed from words in it. A language wins
 * over a genre: portals group by language, so "HINDI MOVIES" next to "TAMIL MOVIES" is told apart
 * by its language. Languages are written, not flagged; a flag would often be the wrong country.
 */
fun LiveCategory.mark(): CategoryMark {
    val words = title.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
    val icon = when {
        id == ALL_CATEGORIES -> R.drawable.ic_category_all
        id == FAVORITES_CATEGORY -> R.drawable.ic_favorite
        adult -> R.drawable.ic_category_adult
        else -> null
    }
    val language = if (icon == null) languageIn(words) else null
    return when {
        icon != null -> CategoryMark.Icon(icon)
        language != null -> CategoryMark.Script(language)
        else -> keywordIcon(words)?.let { CategoryMark.Icon(it) } ?: CategoryMark.Initials(initials(words))
    }
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

/** The mark for a bare name, such as a group of categories. */
fun markForName(name: String): CategoryMark = LiveCategory("", name, adult = false).mark()

/** On-demand categories get the same icons, from the same words. */
fun VodCategory.mark(): CategoryMark = LiveCategory(id, title, adult).mark()

private fun keywordIcon(words: List<String>): Int? =
    keywordIcons.firstOrNull { (keywords, _) -> words.any { word -> keywords.any(word::startsWith) } }?.second

private fun initials(words: List<String>): String = when {
    words.isEmpty() -> "#"
    words.size == 1 -> words[0].take(2)
    else -> "${words[0].first()}${words[1].first()}"
}.uppercase()

private val LARGE_MARK = 32.dp

/** A category's icon, its language's initial in that script, or its initials, centered in a [size] square. */
@Composable
fun CategoryMarkView(mark: CategoryMark, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        val textStyle = with(MaterialTheme.typography) { if (size > LARGE_MARK) headlineMedium else labelMedium }
        when (mark) {
            is CategoryMark.Icon -> Icon(painterResource(mark.res), contentDescription = null, Modifier.size(size))
            is CategoryMark.Initials -> Text(mark.text, style = textStyle)
            is CategoryMark.Script -> Text(mark.language.short, style = textStyle, maxLines = 1, softWrap = false)
        }
    }
}
