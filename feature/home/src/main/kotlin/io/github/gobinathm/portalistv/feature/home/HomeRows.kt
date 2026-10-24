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
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.ImageLoader
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing
import io.github.gobinathm.portalistv.core.model.ALL_CATEGORIES
import io.github.gobinathm.portalistv.core.model.LiveCategory
import io.github.gobinathm.portalistv.core.model.LiveChannel
import kotlinx.coroutines.delay

/** A short pause on a category before its channels load, so moving along the row doesn't load every one. */
private const val PREVIEW_DELAY_MS = 300L

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
    CardRow(stringResource(R.string.home_row_live), categories, key = { it.id }) { index, category ->
        CategoryCard(
            category,
            onClick = { actions.onOpenCategory(category) },
            modifier = Modifier
                .onFocusChanged { if (it.isFocused) focused = category }
                .then(if (index == 0 && focus != null) Modifier.focusRequester(focus) else Modifier),
        )
    }
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
