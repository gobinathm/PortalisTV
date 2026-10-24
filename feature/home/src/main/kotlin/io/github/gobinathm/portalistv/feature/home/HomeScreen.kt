package io.github.gobinathm.portalistv.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.gobinathm.portalistv.core.designsystem.component.PosterCard
import io.github.gobinathm.portalistv.core.designsystem.theme.PortalisTheme
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing
import io.github.gobinathm.portalistv.core.model.LiveCategory
import io.github.gobinathm.portalistv.core.model.LiveChannel

/** The selected profile's home: connection status and a first look at its live TV. */
@Composable
fun HomeScreen(
    onPlayChannel: (LiveChannel) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(state, onRetry = viewModel::retry, onPlayChannel = onPlayChannel, modifier = modifier)
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onRetry: () -> Unit,
    onPlayChannel: (LiveChannel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstFocus = remember { FocusRequester() }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(vertical = Spacing.screenVertical),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        item(key = "header") { Header(state, onRetry, firstFocus.takeIf { state.error != null }) }
        if (state.categories.isNotEmpty()) {
            item(key = "categories") {
                ContentRow(
                    title = stringResource(R.string.home_row_categories, state.categories.size),
                    items = state.categories,
                    key = { it.id },
                    label = LiveCategory::title,
                    focus = firstFocus.takeIf { state.error == null },
                    onClick = {},
                )
            }
        }
        if (state.channels.isNotEmpty()) {
            item(key = "channels") {
                ContentRow(
                    title = stringResource(R.string.home_row_channels, state.totalChannels),
                    items = state.channels,
                    key = { it.id },
                    label = { channel -> channel.number?.let { "$it  ${channel.name}" } ?: channel.name },
                    focus = null,
                    onClick = onPlayChannel,
                )
            }
        }
    }
    LaunchedEffect(state.error != null, state.categories.isNotEmpty()) {
        runCatching { firstFocus.requestFocus() }
    }
}

@Composable
private fun Header(state: HomeUiState, onRetry: () -> Unit, retryFocus: FocusRequester?) {
    Column(
        Modifier.padding(horizontal = Spacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Text(state.profileName, style = MaterialTheme.typography.displayMedium)
        if (state.notSecure) {
            Text(
                stringResource(R.string.home_not_secure),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        val error = state.error
        if (error != null) {
            Text(stringResource(error.messageRes()), color = MaterialTheme.colorScheme.error)
            state.portalMessage?.let { Text(stringResource(R.string.home_portal_says, it)) }
            Button(
                onClick = onRetry,
                modifier = if (retryFocus != null) Modifier.focusRequester(retryFocus) else Modifier,
            ) { Text(stringResource(R.string.home_retry)) }
        } else {
            Text(
                stringResource(state.step.messageRes()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.accountName?.let { Text(stringResource(R.string.home_account, it)) }
        state.sampleStream?.let { Text(stringResource(R.string.home_stream_ok, it)) }
    }
}

@Composable
private fun <T> ContentRow(
    title: String,
    items: List<T>,
    key: (T) -> Any,
    label: (T) -> String,
    focus: FocusRequester?,
    onClick: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            items(items, key = key) { item ->
                PosterCard(
                    title = label(item),
                    onClick = { onClick(item) },
                    modifier = if (item == items.first() && focus != null) Modifier.focusRequester(focus) else Modifier,
                )
            }
        }
    }
}

private fun ConnectionStep.messageRes() = when (this) {
    ConnectionStep.CONNECTING -> R.string.home_step_connecting
    ConnectionStep.LOADING_CATEGORIES -> R.string.home_step_categories
    ConnectionStep.LOADING_CHANNELS -> R.string.home_step_channels
    ConnectionStep.RESOLVING_STREAM -> R.string.home_step_stream
    ConnectionStep.DONE -> R.string.home_step_done
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
                step = ConnectionStep.DONE,
                categories = listOf(LiveCategory("1", "News", adult = false)),
                channels = listOf(LiveChannel("1", "Test Pattern", 1, null, "1", adult = false, playbackRef = "")),
                totalChannels = 1,
            ),
            onRetry = {},
            onPlayChannel = {},
        )
    }
}
