package io.github.gobinathm.portalistv.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.ImageLoader
import io.github.gobinathm.portalistv.core.designsystem.component.PosterImageCard
import io.github.gobinathm.portalistv.core.designsystem.component.mark
import io.github.gobinathm.portalistv.core.designsystem.component.markForName
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing
import io.github.gobinathm.portalistv.core.model.ALL_CATEGORIES
import io.github.gobinathm.portalistv.core.model.CategoryNode
import io.github.gobinathm.portalistv.core.model.LiveCategory
import io.github.gobinathm.portalistv.core.model.LiveChannel
import io.github.gobinathm.portalistv.core.model.VodCategory
import io.github.gobinathm.portalistv.core.model.WatchProgress
import io.github.gobinathm.portalistv.core.model.groupCategories
import kotlinx.coroutines.delay

/** A short pause on a category before its channels load, so moving along the row doesn't load every one. */
private const val PREVIEW_DELAY_MS = 300L
private val ContinuePoster = DpSize(132.dp, 198.dp)

@Composable
internal fun FavoritesRow(
    favorites: List<LiveChannel>,
    imageLoader: ImageLoader?,
    actions: HomeActions,
    focus: FocusRequester?,
) {
    CardRow(stringResource(R.string.home_row_favorites), favorites, key = { it.id }) { index, channel ->
        ChannelCard(
            channel,
            imageLoader,
            onClick = { actions.onPlayFavorite(channel) },
            modifier = if (index == 0 && focus != null) Modifier.focusRequester(focus) else Modifier,
        )
    }
}

/** Live TV categories; the one focused fills the row below with its channels. */
@Composable
internal fun CategoriesRow(
    categories: List<LiveCategory>,
    actions: HomeActions,
    focus: FocusRequester?,
    onFocused: (LiveCategory) -> Unit,
) {
    var focused by remember { mutableStateOf<LiveCategory?>(null) }
    LaunchedEffect(focused) {
        val category = focused ?: return@LaunchedEffect
        delay(PREVIEW_DELAY_MS)
        onFocused(category)
    }
    // One card per group (e.g. all the Tamil categories), so the row stays short.
    val nodes = remember(categories) { groupedWithAllFirst(categories) { it.title } }
    val title = stringResource(R.string.home_row_live)
    CardRow(title, nodes, key = { it.key { category -> category.id } }) { index, node ->
        val first = node.first()
        val modifier = Modifier
            .onFocusChanged { if (it.isFocused) focused = first }
            .then(if (index == 0 && focus != null) Modifier.focusRequester(focus) else Modifier)
        when (node) {
            is CategoryNode.Single -> CategoryCard(node.item, onClick = { actions.onOpenCategory(first) }, modifier)
            is CategoryNode.Group ->
                IconCard(node.title, markForName(node.title), onClick = { actions.onOpenCategory(first) }, modifier)
        }
    }
}

@Composable
internal fun ContinueWatchingRow(
    items: List<WatchProgress>,
    imageLoader: ImageLoader?,
    actions: HomeActions,
    focus: FocusRequester?,
) {
    CardRow(stringResource(R.string.home_row_continue), items, key = { it.playback.itemId }) { index, progress ->
        PosterImageCard(
            title = progress.title,
            imageUrl = progress.posterUrl,
            imageLoader = imageLoader,
            onClick = { actions.onResume(progress) },
            modifier = if (index == 0 && focus != null) Modifier.focusRequester(focus) else Modifier,
            size = ContinuePoster,
            subtitle = progress.subtitle,
            progress = progress.fraction,
        )
    }
}

@Composable
internal fun VodCategoriesRow(categories: List<VodCategory>, actions: HomeActions, focus: FocusRequester?) {
    val nodes = remember(categories) { groupedWithAllFirst(categories) { it.title } }
    val title = stringResource(R.string.home_row_vod)
    CardRow(title, nodes, key = { it.key { category -> category.id } }) { index, node ->
        val first = node.first()
        val label = when {
            node is CategoryNode.Group -> node.title
            first.id == ALL_CATEGORIES -> stringResource(R.string.home_vod_all)
            else -> first.title
        }
        IconCard(
            label = label,
            mark = if (node is CategoryNode.Group) markForName(node.title) else first.mark(),
            onClick = { actions.onOpenVodCategory(first) },
            modifier = if (index == 0 && focus != null) Modifier.focusRequester(focus) else Modifier,
        )
    }
}

/** The all-items entry first, then the portal's categories grouped by their first word. */
private fun <T> groupedWithAllFirst(categories: List<T>, title: (T) -> String): List<CategoryNode<T>> =
    categories.take(1).map { CategoryNode.Single(it) } + groupCategories(categories.drop(1), title)

/** The category a card opens: the item itself, or a group's first category. */
private fun <T> CategoryNode<T>.first(): T = when (this) {
    is CategoryNode.Single -> item
    is CategoryNode.Group -> children.first().item
}

private fun <T> CategoryNode<T>.key(id: (T) -> String): String = when (this) {
    is CategoryNode.Single -> id(item)
    is CategoryNode.Group -> "group:$key"
}

@Composable
internal fun PreviewRow(preview: CategoryPreview, imageLoader: ImageLoader?, actions: HomeActions) {
    CardRow(categoryLabel(preview.category), preview.channels, key = { it.id }) { _, channel ->
        ChannelCard(
            channel,
            imageLoader,
            onClick = { actions.onPlayChannel(channel, preview.category.id) },
            modifier = Modifier,
        )
    }
}

@Composable
private fun <T> CardRow(
    title: String,
    items: List<T>,
    key: (T) -> Any,
    card: @Composable (index: Int, item: T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.s),
            horizontalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            itemsIndexed(items, key = { _, item -> key(item) }) { index, item -> card(index, item) }
        }
    }
}

@Composable
internal fun categoryLabel(category: LiveCategory): String =
    if (category.id == ALL_CATEGORIES) stringResource(R.string.home_all_channels) else category.title
