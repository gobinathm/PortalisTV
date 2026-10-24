package io.github.gobinathm.portalistv.core.model

/** An entry of a grouped category list: a category on its own, or a group of related ones. */
sealed interface CategoryNode<out T> {
    data class Single<T>(val item: T) : CategoryNode<T>

    /** Categories sharing their first word, e.g. "TAMIL", "TAMIL NEWS" and "TAMIL MOVIES". */
    data class Group<T>(val key: String, val title: String, val children: List<GroupChild<T>>) : CategoryNode<T>
}

/** A category inside a group; [label] is its name without the group's word ("NEWS" for "TAMIL NEWS"). */
data class GroupChild<out T>(val item: T, val label: String)

private val SEPARATORS = Regex("[\\s|:/-]+")
private const val MIN_GROUP_SIZE = 2
private const val MIN_KEY_LENGTH = 2

/**
 * Groups categories by their first word, which portals commonly use for the language or theme
 * ("HINDI NEWS", "HINDI SERIES"; "SPORTS NBA", "SPORTS NFL"). A word shared by fewer than two
 * categories leaves the category on its own. Groups take the place of their first member, so the
 * portal's order is kept.
 */
fun <T> groupCategories(items: List<T>, title: (T) -> String): List<CategoryNode<T>> {
    val keys = items.map { groupKey(title(it)) }
    val sizes = keys.filterNotNull().groupingBy { it }.eachCount()
    val placed = mutableSetOf<String>()
    return items.mapIndexedNotNull { index, item ->
        val key = keys[index]?.takeIf { (sizes[it] ?: 0) >= MIN_GROUP_SIZE }
        when {
            key == null -> CategoryNode.Single(item)
            !placed.add(key) -> null
            else -> {
                val members = items.filterIndexed { i, _ -> keys[i] == key }
                val groupTitle = title(item).trim().split(SEPARATORS).first()
                CategoryNode.Group(key, groupTitle, members.map { GroupChild(it, childLabel(title(it), groupTitle)) })
            }
        }
    }
}

/** The first word, upper-cased, when it can name a group (letters, at least two). */
private fun groupKey(title: String): String? =
    title.trim().split(SEPARATORS).firstOrNull()
        ?.takeIf { word -> word.length >= MIN_KEY_LENGTH && word.all { it.isLetter() } }
        ?.uppercase()

/** The name without the group's word and separators; the group's own name for the bare category. */
private fun childLabel(title: String, groupTitle: String): String =
    title.trim().drop(groupTitle.length).trimStart { it.isWhitespace() || it in "|:/-" }.ifEmpty { groupTitle }
