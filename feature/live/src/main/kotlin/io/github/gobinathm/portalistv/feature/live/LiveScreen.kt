package io.github.gobinathm.portalistv.feature.live

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.gobinathm.portalistv.core.designsystem.theme.PortalisTheme
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing
import io.github.gobinathm.portalistv.core.model.ALL_CATEGORIES
import io.github.gobinathm.portalistv.core.model.LiveCategory
import io.github.gobinathm.portalistv.core.model.LiveChannel
import kotlinx.coroutines.delay

/** Pause on a category before its channels load, so moving down the rail doesn't load every one. */
private const val SELECT_ON_FOCUS_DELAY_MS = 400L

/**
 * Live TV: a category rail on the left and the selected category's channels on the right.
 * [onPlay] gets the channel, its category and the portal page it came from.
 */
@Composable
fun LiveScreen(
    onPlay: (channel: LiveChannel, categoryId: String, page: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LiveViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LiveContent(
        state = state,
        onSelectCategory = viewModel::selectCategory,
        onScrolledTo = viewModel::onScrolledTo,
        onRetry = viewModel::retry,
        onPlay = { channel -> onPlay(channel, state.selectedCategoryId, viewModel.pageOf(channel)) },
        modifier = modifier,
    )
}

@Composable
private fun LiveContent(
    state: LiveUiState,
    onSelectCategory: (String) -> Unit,
    onScrolledTo: (Int) -> Unit,
    onRetry: () -> Unit,
    onPlay: (LiveChannel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = Spacing.screenVertical),
    ) {
        CategoryRail(state, onSelectCategory, Modifier.width(300.dp).fillMaxHeight())
        ChannelPane(state, onScrolledTo, onRetry, onPlay, Modifier.weight(1f).fillMaxHeight())
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryRail(state: LiveUiState, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val selectedFocus = remember { FocusRequester() }
    var focused by remember { mutableStateOf<String?>(null) }
    val selectedId by rememberUpdatedState(state.selectedCategoryId)
    LazyColumn(
        modifier = modifier.focusRestorer(selectedFocus),
        contentPadding = PaddingValues(start = Spacing.screenHorizontal, end = Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        items(state.categories, key = { it.id }) { category ->
            val selected = category.id == state.selectedCategoryId
            ListItem(
                selected = selected,
                onClick = { onSelect(category.id) },
                modifier = Modifier
                    .then(if (selected) Modifier.focusRequester(selectedFocus) else Modifier)
                    .onFocusChanged { if (it.isFocused) focused = category.id },
                headlineContent = { Text(category.label(), maxLines = 1) },
                leadingContent = { CategoryMarkView(category.mark()) },
            )
        }
    }
    LaunchedEffect(focused) {
        val id = focused ?: return@LaunchedEffect
        delay(SELECT_ON_FOCUS_DELAY_MS)
        if (id != selectedId) onSelect(id)
    }
    LaunchedEffect(state.categories.isNotEmpty()) {
        if (state.categories.isNotEmpty()) runCatching { selectedFocus.requestFocus() }
    }
}

@Composable
private fun CategoryMarkView(mark: CategoryMark) {
    Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        when (mark) {
            is CategoryMark.Icon -> Icon(painterResource(mark.res), contentDescription = null)
            is CategoryMark.Initials -> Text(mark.text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ChannelPane(
    state: LiveUiState,
    onScrolledTo: (Int) -> Unit,
    onRetry: () -> Unit,
    onPlay: (LiveChannel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(end = Spacing.screenHorizontal), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        val category = state.categories.firstOrNull { it.id == state.selectedCategoryId }
        Column(Modifier.padding(horizontal = Spacing.m)) {
            Text(state.profileName, style = MaterialTheme.typography.labelLarge)
            Text(category?.label().orEmpty(), style = MaterialTheme.typography.headlineMedium)
            if (state.totalChannels > 0) {
                Text(
                    pluralStringResource(R.plurals.live_channel_count, state.totalChannels, state.totalChannels),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val message = when {
            state.failed -> R.string.live_failed
            state.loadingCategories || (state.loadingChannels && state.channels.isEmpty()) -> R.string.live_loading
            state.channels.isEmpty() && state.pages.complete -> R.string.live_empty
            else -> null
        }
        if (message != null && state.channels.isEmpty()) {
            Column(Modifier.padding(horizontal = Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                Text(stringResource(message), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.failed) Button(onClick = onRetry) { Text(stringResource(R.string.live_retry)) }
            }
        } else {
            ChannelList(state, onScrolledTo, onRetry, onPlay)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChannelList(
    state: LiveUiState,
    onScrolledTo: (Int) -> Unit,
    onRetry: () -> Unit,
    onPlay: (LiveChannel) -> Unit,
) {
    // A new list per category, so each starts at the top.
    val listState = remember(state.selectedCategoryId) { LazyListState() }
    LazyColumn(
        state = listState,
        modifier = Modifier.focusGroup().focusRestorer(),
        contentPadding = PaddingValues(horizontal = Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        items(state.channels, key = { it.id }) { channel ->
            ListItem(
                selected = false,
                onClick = { onPlay(channel) },
                headlineContent = { Text(channel.name, maxLines = 1) },
                leadingContent = {
                    Text(
                        channel.number?.toString().orEmpty(),
                        modifier = Modifier.width(56.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
        if (state.failed) {
            item(key = "retry") {
                Button(onClick = onRetry) { Text(stringResource(R.string.live_retry)) }
            }
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect(onScrolledTo)
    }
}

@Composable
private fun LiveCategory.label(): String =
    if (id == ALL_CATEGORIES) stringResource(R.string.live_all_channels) else title

@Preview(device = "id:tv_1080p")
@Composable
private fun LivePreview() {
    val channels = (1..8).map { LiveChannel("$it", "Channel $it", it, null, "1", adult = false, playbackRef = "") }
    PortalisTheme {
        LiveContent(
            state = LiveUiState(
                profileName = "Living room",
                categories = listOf(
                    LiveCategory(ALL_CATEGORIES, "", adult = false),
                    LiveCategory("1", "UK | News", adult = false),
                    LiveCategory("2", "Tamil", adult = false),
                ),
                selectedCategoryId = "1",
                pages = ChannelPages("1").append(1, channels, pageCount = 1, totalItems = channels.size),
                loadingCategories = false,
            ),
            onSelectCategory = {},
            onScrolledTo = {},
            onRetry = {},
            onPlay = {},
        )
    }
}
