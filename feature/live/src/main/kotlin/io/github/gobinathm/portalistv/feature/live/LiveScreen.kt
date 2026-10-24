package io.github.gobinathm.portalistv.feature.live

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.ImageLoader
import io.github.gobinathm.portalistv.core.designsystem.component.CategoryMarkView
import io.github.gobinathm.portalistv.core.designsystem.component.backdrop
import io.github.gobinathm.portalistv.core.designsystem.component.mark
import io.github.gobinathm.portalistv.core.designsystem.theme.PortalisTheme
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing
import io.github.gobinathm.portalistv.core.model.ALL_CATEGORIES
import io.github.gobinathm.portalistv.core.model.FAVORITES_CATEGORY
import io.github.gobinathm.portalistv.core.model.LiveCategory
import io.github.gobinathm.portalistv.core.model.LiveChannel
import io.github.gobinathm.portalistv.core.model.NowNext
import io.github.gobinathm.portalistv.core.model.Programme
import io.github.gobinathm.portalistv.core.model.nowNext
import kotlinx.coroutines.delay
import android.text.format.DateFormat as AndroidDateFormat

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
    val imageLoader by viewModel.imageLoader.collectAsStateWithLifecycle()
    LiveContent(
        state = state,
        imageLoader = imageLoader,
        actions = LiveActions(
            onSelectCategory = viewModel::selectCategory,
            onVisible = viewModel::onVisible,
            onRetry = viewModel::retry,
            onPlay = { channel -> onPlay(channel, state.selectedCategoryId, viewModel.pageOf(channel)) },
            onToggleFavorite = viewModel::toggleFavorite,
        ),
        modifier = modifier,
    )
}

private class LiveActions(
    val onSelectCategory: (String) -> Unit,
    val onVisible: (first: Int, last: Int) -> Unit,
    val onRetry: () -> Unit,
    val onPlay: (LiveChannel) -> Unit,
    val onToggleFavorite: (LiveChannel) -> Unit,
)

@Composable
private fun LiveContent(
    state: LiveUiState,
    imageLoader: ImageLoader?,
    actions: LiveActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .backdrop(MaterialTheme.colorScheme)
            .padding(vertical = Spacing.screenVertical),
    ) {
        CategoryRail(state, actions.onSelectCategory, Modifier.width(320.dp).fillMaxHeight())
        ChannelPane(state, imageLoader, actions, Modifier.weight(1f).fillMaxHeight())
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryRail(state: LiveUiState, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val selectedFocus = remember { FocusRequester() }
    var focused by remember { mutableStateOf<String?>(null) }
    val selectedId by rememberUpdatedState(state.selectedCategoryId)
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
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
        val index = state.categories.indexOfFirst { it.id == selectedId }
        if (index < 0) return@LaunchedEffect
        // A category opened from Home may be far down the rail: bring it on screen, let it be laid
        // out, then focus it. Focusing an item that isn't composed fails, and the first one would
        // take focus and select itself.
        listState.scrollToItem(index)
        withFrameNanos { }
        runCatching { selectedFocus.requestFocus() }
    }
}

@Composable
private fun ChannelPane(
    state: LiveUiState,
    imageLoader: ImageLoader?,
    actions: LiveActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(end = Spacing.screenHorizontal), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        PaneHeader(state)
        val favorites = state.selectedCategoryId == FAVORITES_CATEGORY
        val message = when {
            state.failed -> R.string.live_failed
            state.loadingCategories || (state.loadingChannels && state.channels.isEmpty()) -> R.string.live_loading
            state.channels.isEmpty() && state.pages.complete ->
                if (favorites) R.string.live_no_favorites else R.string.live_empty
            else -> null
        }
        if (message != null && state.channels.isEmpty()) {
            Column(Modifier.padding(horizontal = Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                Text(stringResource(message), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.failed) Button(onClick = actions.onRetry) { Text(stringResource(R.string.live_retry)) }
            }
        } else {
            ChannelList(state, imageLoader, actions)
        }
    }
}

@Composable
private fun PaneHeader(state: LiveUiState) {
    val category = state.categories.firstOrNull { it.id == state.selectedCategoryId }
    Column(Modifier.padding(horizontal = Spacing.m)) {
        Text(category?.label().orEmpty(), style = MaterialTheme.typography.headlineMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            if (state.totalChannels > 0) {
                Text(
                    pluralStringResource(R.plurals.live_channel_count, state.totalChannels, state.totalChannels) +
                        "  ·  ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FavoriteIcon()
            Text(
                stringResource(R.string.live_favorite_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChannelList(state: LiveUiState, imageLoader: ImageLoader?, actions: LiveActions) {
    val context = LocalContext.current
    // Follows the TV's 12/24-hour setting.
    val timeFormat = remember(context) { AndroidDateFormat.getTimeFormat(context) }
    // A new list per category, so each starts at the top.
    val listState = remember(state.selectedCategoryId) { LazyListState() }
    LazyColumn(
        state = listState,
        modifier = Modifier.focusGroup().focusRestorer(),
        contentPadding = PaddingValues(horizontal = Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        items(state.channels, key = { it.id }) { channel ->
            ChannelRow(
                channel = channel,
                nowNext = state.guide[channel.id]?.nowNext(state.nowMillis) ?: NowNext(null, null),
                nowMillis = state.nowMillis,
                favorite = channel.id in state.favoriteIds,
                imageLoader = imageLoader,
                timeFormat = timeFormat,
                onClick = { actions.onPlay(channel) },
                onLongClick = { actions.onToggleFavorite(channel) },
            )
        }
        if (state.failed) {
            item(key = "retry") {
                Button(onClick = actions.onRetry) { Text(stringResource(R.string.live_retry)) }
            }
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow {
            val items = listState.layoutInfo.visibleItemsInfo
            (items.firstOrNull()?.index ?: 0) to (items.lastOrNull()?.index ?: 0)
        }.collect { (first, last) -> actions.onVisible(first, last) }
    }
}

@Composable
private fun LiveCategory.label(): String = when (id) {
    ALL_CATEGORIES -> stringResource(R.string.live_all_channels)
    FAVORITES_CATEGORY -> stringResource(R.string.live_favorites)
    else -> title
}

@Preview(device = "id:tv_1080p")
@Composable
private fun LivePreview() {
    val channels = (1..8).map { LiveChannel("$it", "Channel $it", it, null, "1", adult = false, playbackRef = "") }
    val now = 1_800_000L
    PortalisTheme {
        LiveContent(
            state = LiveUiState(
                profileName = "Living room",
                categories = listOf(
                    LiveCategory(ALL_CATEGORIES, "", adult = false),
                    LiveCategory(FAVORITES_CATEGORY, "", adult = false),
                    LiveCategory("1", "UK | News", adult = false),
                    LiveCategory("2", "Tamil", adult = false),
                ),
                selectedCategoryId = "1",
                pages = ChannelPages("1").append(1, channels, pageCount = 1, totalItems = channels.size),
                loadingCategories = false,
                favoriteIds = setOf("2"),
                guide = mapOf(
                    "1" to listOf(
                        Programme("Morning news", null, 0, 3_600_000),
                        Programme("Weather", null, 3_600_000, 5_400_000),
                    ),
                ),
                nowMillis = now,
            ),
            imageLoader = null,
            actions = LiveActions({}, { _, _ -> }, {}, {}, {}),
        )
    }
}
