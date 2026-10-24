package io.github.gobinathm.portalistv.core.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing
import io.github.gobinathm.portalistv.core.model.CategoryNode
import kotlinx.coroutines.delay

/** Pause on a category before it's selected, so moving down the rail doesn't load every one. */
private const val SELECT_ON_FOCUS_DELAY_MS = 400L

/** One entry of a [CategoryRail]. */
data class RailEntry(
    val id: String,
    val label: String,
    val mark: CategoryMark,
    /** A group header: OK opens or closes it, and resting on it selects nothing. */
    val isGroup: Boolean = false,
    val expanded: Boolean = false,
    /** A category inside an open group, drawn indented. */
    val nested: Boolean = false,
)

private const val GROUP_ID_PREFIX = "group:"

/**
 * Rail entries for [nodes]: categories on their own, and groups with the children of the open
 * one ([expandedKey]) listed under it.
 */
fun <T> railEntries(
    nodes: List<CategoryNode<T>>,
    expandedKey: String?,
    id: (T) -> String,
    label: (T) -> String,
    mark: (T) -> CategoryMark,
): List<RailEntry> = nodes.flatMap { node ->
    when (node) {
        is CategoryNode.Single -> listOf(RailEntry(id(node.item), label(node.item), mark(node.item)))
        is CategoryNode.Group -> {
            val open = node.key == expandedKey
            val header = RailEntry(GROUP_ID_PREFIX + node.key, node.title, markForName(node.title), true, open)
            val children = if (open) {
                node.children.map { RailEntry(id(it.item), it.label, mark(it.item), nested = true) }
            } else {
                emptyList()
            }
            listOf(header) + children
        }
    }
}

/** The group [selectedId] belongs to, so the rail can open on it. */
fun <T> groupOf(nodes: List<CategoryNode<T>>, selectedId: String, id: (T) -> String): String? =
    nodes.filterIsInstance<CategoryNode.Group<T>>()
        .firstOrNull { group -> group.children.any { id(it.item) == selectedId } }
        ?.key

/** The group key behind a group header's rail id, or null for a category's id. */
fun groupKeyOfEntry(entryId: String): String? =
    entryId.removePrefix(GROUP_ID_PREFIX).takeIf { entryId.startsWith(GROUP_ID_PREFIX) }

/**
 * A vertical list of categories at the left of a browse screen. Resting on one selects it; the
 * selected one takes focus when the rail appears, even if it's far down the list.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryRail(
    entries: List<RailEntry>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    onToggleGroup: (groupKey: String) -> Unit = {},
) {
    val selectedFocus = remember { FocusRequester() }
    var focused by remember { mutableStateOf<String?>(null) }
    val currentSelection by rememberUpdatedState(selectedId)
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = modifier.focusRestorer(selectedFocus),
        contentPadding = PaddingValues(start = Spacing.screenHorizontal, end = Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        items(entries, key = { it.id }) { entry ->
            val selected = entry.id == selectedId
            ListItem(
                selected = selected,
                onClick = { groupKeyOfEntry(entry.id)?.let(onToggleGroup) ?: onSelect(entry.id) },
                modifier = Modifier
                    .then(if (entry.nested) Modifier.padding(start = Spacing.l) else Modifier)
                    .then(if (selected) Modifier.focusRequester(selectedFocus) else Modifier)
                    .onFocusChanged { if (it.isFocused) focused = entry.id },
                headlineContent = { Text(entry.label, maxLines = 1) },
                leadingContent = if (entry.nested) null else ({ CategoryMarkView(entry.mark) }),
                trailingContent = if (entry.isGroup) ({ Text(if (entry.expanded) "▾" else "▸") }) else null,
            )
        }
    }
    LaunchedEffect(focused) {
        // Group headers only open and close; resting on one selects nothing.
        val id = focused?.takeIf { groupKeyOfEntry(it) == null } ?: return@LaunchedEffect
        delay(SELECT_ON_FOCUS_DELAY_MS)
        if (id != currentSelection) onSelect(id)
    }
    LaunchedEffect(entries.isNotEmpty()) {
        val index = entries.indexOfFirst { it.id == currentSelection }
        if (index < 0) return@LaunchedEffect
        // A category opened from Home may be far down the rail: bring it on screen, let it be laid
        // out, then focus it. Focusing an item that isn't composed fails, and the first one would
        // take focus and select itself.
        listState.scrollToItem(index)
        withFrameNanos { }
        runCatching { selectedFocus.requestFocus() }
    }
}
