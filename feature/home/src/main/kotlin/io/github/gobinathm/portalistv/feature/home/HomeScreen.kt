package io.github.gobinathm.portalistv.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import coil3.ImageLoader
import io.github.gobinathm.portalistv.core.datastore.HomeRow
import io.github.gobinathm.portalistv.core.designsystem.component.ActionDialog
import io.github.gobinathm.portalistv.core.designsystem.component.ChoiceOption
import io.github.gobinathm.portalistv.core.designsystem.component.FavoriteAction
import io.github.gobinathm.portalistv.core.designsystem.component.FavoriteMenu
import io.github.gobinathm.portalistv.core.designsystem.component.backdrop
import io.github.gobinathm.portalistv.core.designsystem.component.tapToClick
import io.github.gobinathm.portalistv.core.designsystem.theme.PortalisTheme
import io.github.gobinathm.portalistv.core.designsystem.theme.Radius
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing
import io.github.gobinathm.portalistv.core.model.ALL_CATEGORIES
import io.github.gobinathm.portalistv.core.model.LiveCategory
import io.github.gobinathm.portalistv.core.model.LiveChannel
import io.github.gobinathm.portalistv.core.model.VodCategory
import io.github.gobinathm.portalistv.core.model.WatchProgress

private val StatusDotSize = 10.dp

/** What the home screen can ask the app to do. */
@Suppress("LongParameterList") // One callback per destination.
class HomeActions(
    val onOpenCategory: (LiveCategory) -> Unit,
    val onPlayFavorite: (LiveChannel) -> Unit,
    /** A channel from the row under the categories: play it, zapping within [categoryId]. */
    val onPlayChannel: (channel: LiveChannel, categoryId: String) -> Unit,
    /** A movie or episode from Continue Watching: play it from where it stopped. */
    val onResume: (WatchProgress) -> Unit,
    val onOpenVodCategory: (VodCategory) -> Unit,
    val onSwitchProfile: () -> Unit,
    val onSettings: () -> Unit,
)

/** The selected profile's home: favorite channels, then live TV categories. */
@Composable
fun HomeScreen(actions: HomeActions, modifier: Modifier = Modifier, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val imageLoader by viewModel.imageLoader.collectAsStateWithLifecycle()
    HomeContent(
        state,
        imageLoader,
        actions,
        onRetry = viewModel::retry,
        onCategoryFocused = viewModel::previewCategory,
        onFavoriteAction = viewModel::onFavoriteAction,
        onRemoveProgress = viewModel::removeFromContinueWatching,
        modifier = modifier,
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    imageLoader: ImageLoader?,
    actions: HomeActions,
    onRetry: () -> Unit,
    onCategoryFocused: (LiveCategory) -> Unit,
    onFavoriteAction: (LiveChannel, FavoriteAction) -> Unit,
    onRemoveProgress: (WatchProgress) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstFocus = remember { FocusRequester() }
    // Hold OK on a channel or on Continue Watching opens a small menu for it.
    var channelMenu by remember { mutableStateOf<LiveChannel?>(null) }
    var progressMenu by remember { mutableStateOf<WatchProgress?>(null) }
    // The first focus goes to the first row that is shown and has something in it.
    val firstRow = state.rows.firstOrNull { row -> state.hasContent(row) }.takeIf { state.error == null }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .backdrop(MaterialTheme.colorScheme),
        contentPadding = PaddingValues(vertical = Spacing.screenVertical),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        item(key = "header") { Header(state, actions) }
        if (state.error != null) {
            item(key = "error") { ErrorPanel(state, onRetry, firstFocus) }
        }
        state.rows.filter { state.hasContent(it) }.forEach { row ->
            val focus = firstFocus.takeIf { row == firstRow }
            when (row) {
                HomeRow.CONTINUE_WATCHING -> item(key = "continue") {
                    ContinueWatchingRow(state.continueWatching, imageLoader, actions, focus) { progressMenu = it }
                }
                HomeRow.FAVORITES -> item(key = "favorites") {
                    FavoritesRow(state.homeFavorites, imageLoader, actions, focus) { channelMenu = it }
                }
                HomeRow.LIVE_TV -> {
                    item(key = "categories") {
                        val hasFavorites = state.favorites.isNotEmpty()
                        CategoriesRow(state.categories, hasFavorites, actions, focus, onCategoryFocused)
                    }
                    // The focused category's channels stay right under the categories.
                    state.preview?.takeIf { it.channels.isNotEmpty() }?.let { preview ->
                        item(key = "preview") { PreviewRow(preview, imageLoader, actions) { channelMenu = it } }
                    }
                }
                HomeRow.ON_DEMAND -> item(key = "vod") { VodCategoriesRow(state.vodCategories, actions, focus) }
            }
        }
    }
    LaunchedEffect(state.error != null, firstRow) {
        runCatching { firstFocus.requestFocus() }
    }
    HomeMenus(
        state,
        channelMenu,
        progressMenu,
        onFavoriteAction,
        onRemoveProgress,
        onClose = {
            channelMenu = null
            progressMenu = null
        },
    )
}

/** The hold-OK menu open, if any: for a channel, or for an entry of Continue Watching. */
@Composable
@Suppress("LongParameterList") // The menu targets and their callbacks.
private fun HomeMenus(
    state: HomeUiState,
    channelMenu: LiveChannel?,
    progressMenu: WatchProgress?,
    onFavoriteAction: (LiveChannel, FavoriteAction) -> Unit,
    onRemoveProgress: (WatchProgress) -> Unit,
    onClose: () -> Unit,
) {
    channelMenu?.let { channel ->
        FavoriteMenu(
            channel = channel,
            isFavorite = state.favorites.any { it.id == channel.id },
            onHome = state.homeFavorites.any { it.id == channel.id },
            onAction = { action ->
                onFavoriteAction(channel, action)
                onClose()
            },
            onDismiss = onClose,
        )
    }
    progressMenu?.let { progress ->
        ActionDialog(
            title = progress.title,
            actions = listOf(ChoiceOption(REMOVE, stringResource(R.string.home_continue_remove))),
            onSelect = {
                onRemoveProgress(progress)
                onClose()
            },
            onDismiss = onClose,
        )
    }
}

private const val REMOVE = "remove"

private fun HomeUiState.hasContent(row: HomeRow): Boolean = when (row) {
    HomeRow.CONTINUE_WATCHING -> continueWatching.isNotEmpty()
    HomeRow.FAVORITES -> homeFavorites.isNotEmpty()
    HomeRow.LIVE_TV -> categories.isNotEmpty()
    HomeRow.ON_DEMAND -> vodCategories.isNotEmpty()
}

@Composable
private fun Header(state: HomeUiState, actions: HomeActions) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.screenHorizontal),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(state.profileName, style = MaterialTheme.typography.displayMedium, maxLines = 1)
            ConnectionStatus(state)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            OutlinedButton(onClick = actions.onSwitchProfile, modifier = Modifier.tapToClick(actions.onSwitchProfile)) {
                Text(stringResource(R.string.home_switch_profile))
            }
            OutlinedButton(onClick = actions.onSettings, modifier = Modifier.tapToClick(actions.onSettings)) {
                Text(stringResource(R.string.home_settings))
            }
        }
    }
}

/** A small dot and a word: quiet when all is well, so the page stays about the content. */
@Composable
private fun ConnectionStatus(state: HomeUiState) {
    val (color, text) = when {
        state.error != null -> MaterialTheme.colorScheme.error to stringResource(R.string.home_status_offline)
        state.connecting ->
            MaterialTheme.colorScheme.onSurfaceVariant to stringResource(R.string.home_status_connecting)
        else -> MaterialTheme.colorScheme.primary to stringResource(R.string.home_status_connected)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Box(Modifier.size(StatusDotSize).background(color, CircleShape))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.notSecure) {
            Text(
                stringResource(R.string.home_not_secure),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ErrorPanel(state: HomeUiState, onRetry: () -> Unit, retryFocus: FocusRequester) {
    val error = state.error ?: return
    Column(
        Modifier
            .padding(horizontal = Spacing.screenHorizontal)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(Radius.card))
            .padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Text(stringResource(error.messageRes()), style = MaterialTheme.typography.titleMedium)
        state.portalMessage?.let {
            Text(
                stringResource(R.string.home_portal_says, it),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = onRetry, modifier = Modifier.focusRequester(retryFocus).tapToClick(onRetry)) {
            Text(stringResource(R.string.home_retry))
        }
    }
}

private fun ConnectionError.messageRes() = when (this) {
    ConnectionError.INVALID_ADDRESS -> R.string.home_error_address
    ConnectionError.NOT_FOUND -> R.string.home_error_not_found
    ConnectionError.DEVICE_REFUSED -> R.string.home_error_refused
    ConnectionError.AUTHORIZATION -> R.string.home_error_authorization
    ConnectionError.BAD_RESPONSE -> R.string.home_error_bad_response
    ConnectionError.NO_NETWORK -> R.string.home_error_network
    ConnectionError.TIMEOUT -> R.string.home_error_timeout
    ConnectionError.TLS -> R.string.home_error_tls
    ConnectionError.UNKNOWN -> R.string.home_error_unknown
}

@Preview(device = "id:tv_1080p")
@Composable
private fun HomePreview() {
    PortalisTheme {
        HomeContent(
            HomeUiState(
                profileName = "Living room",
                connecting = false,
                categories = listOf(
                    LiveCategory(ALL_CATEGORIES, "", adult = false),
                    LiveCategory("1", "News", adult = false),
                    LiveCategory("2", "Kids", adult = false),
                    LiveCategory("3", "Tamil", adult = false),
                ),
                favorites = listOf(LiveChannel("1", "Test Pattern", 1, null, "1", adult = false, playbackRef = "")),
                homeFavorites = listOf(LiveChannel("1", "Test Pattern", 1, null, "1", adult = false, playbackRef = "")),
            ),
            imageLoader = null,
            actions = HomeActions({}, {}, { _, _ -> }, {}, {}, {}, {}),
            onRetry = {},
            onCategoryFocused = {},
            onFavoriteAction = { _, _ -> },
            onRemoveProgress = {},
        )
    }
}
