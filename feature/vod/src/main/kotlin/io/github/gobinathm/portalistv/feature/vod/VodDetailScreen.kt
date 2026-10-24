package io.github.gobinathm.portalistv.feature.vod

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import coil3.ImageLoader
import coil3.compose.AsyncImage
import io.github.gobinathm.portalistv.core.designsystem.component.backdrop
import io.github.gobinathm.portalistv.core.designsystem.theme.Radius
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing
import io.github.gobinathm.portalistv.core.model.Episode
import io.github.gobinathm.portalistv.core.model.Season
import io.github.gobinathm.portalistv.core.model.VodItem

private val PosterWidth = 180.dp
private val PosterHeight = 270.dp
private val EpisodeProgressHeight = 3.dp
private const val DESCRIPTION_LINES = 5
private const val EPISODE_PROGRESS_WIDTH = 0.4f

/** A movie's or series' page. [onPlay] starts a movie or an episode. */
@Composable
fun VodDetailScreen(
    onPlay: (VodPlayRequest) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VodDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val imageLoader by viewModel.imageLoader.collectAsStateWithLifecycle()
    val item = state.item
    Box(modifier.fillMaxSize().backdrop(MaterialTheme.colorScheme)) {
        if (item == null) {
            Text(
                stringResource(R.string.vod_not_found),
                modifier = Modifier.align(Alignment.Center).padding(Spacing.screenHorizontal),
            )
        } else {
            DetailContent(item, state, imageLoader, onPlay, viewModel::selectSeason, viewModel::retry)
        }
    }
}

@Composable
private fun DetailContent(
    item: VodItem,
    state: VodDetailUiState,
    imageLoader: ImageLoader?,
    onPlay: (VodPlayRequest) -> Unit,
    onSelectSeason: (Season) -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.screenVertical),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        item(key = "header") { Header(item, state, imageLoader, onPlay) }
        if (item.isSeries) {
            item(key = "seasons") { SeasonTabs(state, onSelectSeason) }
            val season = state.selectedSeason
            when {
                state.failed -> item(key = "failed") {
                    Button(onClick = onRetry) { Text(stringResource(R.string.vod_retry)) }
                }
                state.loadingSeasons -> item(key = "loading") { Hint(stringResource(R.string.vod_seasons_loading)) }
                state.loadingEpisodes -> item(key = "loading") { Hint(stringResource(R.string.vod_episodes_loading)) }
                season == null || state.episodes.isEmpty() ->
                    item(key = "none") { Hint(stringResource(R.string.vod_no_episodes)) }
                else -> items(state.episodes, key = { it.id }) { episode ->
                    val label = episodeLabel(season, episode)
                    EpisodeRow(episode, state.progressOf(episode)?.fraction) {
                        onPlay(VodPlayRequest.episode(item, season, episode, label))
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    item: VodItem,
    state: VodDetailUiState,
    imageLoader: ImageLoader?,
    onPlay: (VodPlayRequest) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
        Box(
            Modifier
                .size(PosterWidth, PosterHeight)
                .clip(RoundedCornerShape(Radius.card))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (item.posterUrl != null && imageLoader != null) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = null,
                    imageLoader = imageLoader,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(item.title, style = MaterialTheme.typography.displaySmall, maxLines = 2)
            MetaLine(item)
            item.description?.let {
                Text(it, maxLines = DESCRIPTION_LINES, overflow = TextOverflow.Ellipsis)
            }
            item.director?.let { Muted(stringResource(R.string.vod_director, it)) }
            item.actors?.let { Muted(stringResource(R.string.vod_cast, it)) }
            PlayButtons(item, state, onPlay, Modifier.padding(top = Spacing.s))
        }
    }
}

@Composable
private fun MetaLine(item: VodItem) {
    val parts = listOfNotNull(
        item.year,
        item.rating?.let { "★ $it" },
        item.durationMinutes?.let { stringResource(R.string.vod_minutes, it) },
        item.genres,
    )
    if (parts.isNotEmpty()) Muted(parts.joinToString("  ·  "))
}

@Composable
private fun PlayButtons(item: VodItem, state: VodDetailUiState, onPlay: (VodPlayRequest) -> Unit, modifier: Modifier) {
    val focus = remember { FocusRequester() }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        if (item.isSeries) {
            // Series start from an episode; offer to carry on with the last one watched.
            val last = state.lastEpisode
            if (last != null && !last.finished) {
                Button(
                    onClick = { onPlay(VodPlayRequest(item, last.playback, last.subtitle, startOver = false)) },
                    modifier = Modifier.focusRequester(focus),
                ) { Text(stringResource(R.string.vod_continue_episode, last.subtitle.orEmpty())) }
            }
        } else {
            val progress = state.movieProgress?.takeIf { !it.finished && it.positionMs > 0 }
            Button(onClick = { onPlay(VodPlayRequest.movie(item)) }, modifier = Modifier.focusRequester(focus)) {
                Text(
                    progress?.let { stringResource(R.string.vod_resume, formatTime(it.positionMs)) }
                        ?: stringResource(R.string.vod_play),
                )
            }
            if (progress != null) {
                OutlinedButton(onClick = { onPlay(VodPlayRequest.movie(item, startOver = true)) }) {
                    Text(stringResource(R.string.vod_start_over))
                }
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

@Composable
private fun SeasonTabs(state: VodDetailUiState, onSelect: (Season) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        items(state.seasons, key = { it.id }) { season ->
            val label = season.number?.let { "S$it" } ?: season.title
            if (season.id == state.selectedSeasonId) {
                Button(onClick = { onSelect(season) }) { Text(label) }
            } else {
                OutlinedButton(onClick = { onSelect(season) }) { Text(label) }
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: Episode, progress: Float?, onClick: () -> Unit) {
    ListItem(
        selected = false,
        onClick = onClick,
        headlineContent = {
            Text(
                listOfNotNull(episode.number?.let { "E$it" }, episode.title).joinToString("   "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = progress?.let {
            {
                Box(
                    Modifier
                        .fillMaxWidth(EPISODE_PROGRESS_WIDTH)
                        .height(EpisodeProgressHeight)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
                ) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(it).background(MaterialTheme.colorScheme.primary))
                }
            }
        },
    )
}

@Composable
private fun episodeLabel(season: Season, episode: Episode): String {
    val seasonNumber = season.number
    val episodeNumber = episode.number
    return if (seasonNumber != null && episodeNumber != null) {
        stringResource(R.string.vod_episode_label, seasonNumber, episodeNumber, episode.title)
    } else {
        episode.title
    }
}
