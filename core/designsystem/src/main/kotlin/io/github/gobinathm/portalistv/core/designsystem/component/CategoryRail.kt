package io.github.gobinathm.portalistv.core.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
import kotlinx.coroutines.delay

/** Pause on a category before it's selected, so moving down the rail doesn't load every one. */
private const val SELECT_ON_FOCUS_DELAY_MS = 400L

/** One entry of a [CategoryRail]. */
data class RailEntry(val id: String, val label: String, val mark: CategoryMark)

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
                onClick = { onSelect(entry.id) },
                modifier = Modifier
                    .then(if (selected) Modifier.focusRequester(selectedFocus) else Modifier)
                    .onFocusChanged { if (it.isFocused) focused = entry.id },
                headlineContent = { Text(entry.label, maxLines = 1) },
                leadingContent = { CategoryMarkView(entry.mark) },
            )
        }
    }
    LaunchedEffect(focused) {
        val id = focused ?: return@LaunchedEffect
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
