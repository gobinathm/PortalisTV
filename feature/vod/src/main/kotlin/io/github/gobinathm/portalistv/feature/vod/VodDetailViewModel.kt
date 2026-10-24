package io.github.gobinathm.portalistv.feature.vod

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gobinathm.portalistv.core.common.log.Log
import io.github.gobinathm.portalistv.core.data.ProfileRepository
import io.github.gobinathm.portalistv.core.data.WatchProgressRepository
import io.github.gobinathm.portalistv.core.data.image.PortalImages
import io.github.gobinathm.portalistv.core.data.source.ContentSources
import io.github.gobinathm.portalistv.core.model.ContentSource
import io.github.gobinathm.portalistv.core.model.Episode
import io.github.gobinathm.portalistv.core.model.Season
import io.github.gobinathm.portalistv.core.model.VodItem
import io.github.gobinathm.portalistv.core.model.WatchProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class VodDetailUiState(
    /** Null when the item is no longer known (e.g. after the app was restarted). */
    val item: VodItem? = null,
    /** Progress of the movie, or of each episode watched, most recent first. */
    val progress: List<WatchProgress> = emptyList(),
    val seasons: List<Season> = emptyList(),
    val selectedSeasonId: String? = null,
    val episodes: List<Episode> = emptyList(),
    val loadingSeasons: Boolean = false,
    val loadingEpisodes: Boolean = false,
    val failed: Boolean = false,
) {
    val selectedSeason: Season? get() = seasons.firstOrNull { it.id == selectedSeasonId }

    /** Where a movie stopped, if it was started and not finished. */
    val movieProgress: WatchProgress? get() = progress.firstOrNull { it.playback.episodeId == null }

    /** The episode watched last, if any. */
    val lastEpisode: WatchProgress? get() = progress.firstOrNull { it.playback.episodeId != null }

    fun progressOf(episode: Episode): WatchProgress? = progress.firstOrNull { it.playback.episodeId == episode.id }
}

/** A movie's or series' details; for a series also its seasons and the selected season's episodes. */
@HiltViewModel
class VodDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProfileRepository,
    private val sources: ContentSources,
    private val images: PortalImages,
    seen: VodItems,
    watchProgress: WatchProgressRepository,
) : ViewModel() {
    private val profileId: Long = savedStateHandle[ARG_PROFILE_ID] ?: 0L
    private val itemId: String = savedStateHandle[ARG_ITEM_ID] ?: ""

    private val _state = MutableStateFlow(VodDetailUiState(item = seen[itemId]))
    val state: StateFlow<VodDetailUiState> = _state.asStateFlow()

    private val _imageLoader = MutableStateFlow<ImageLoader?>(null)
    val imageLoader: StateFlow<ImageLoader?> = _imageLoader.asStateFlow()

    private var source: ContentSource? = null
    private var episodesJob: Job? = null

    init {
        viewModelScope.launch {
            val profile = repository.get(profileId) ?: return@launch
            _imageLoader.value = images.loader(profile)
            source = sources.forProfile(profile)
            if (_state.value.item?.isSeries == true) loadSeasons()
        }
        viewModelScope.launch {
            watchProgress.forItem(profileId, itemId).collect { list -> _state.update { it.copy(progress = list) } }
        }
    }

    fun selectSeason(season: Season) {
        if (season.id == _state.value.selectedSeasonId) return
        _state.update { it.copy(selectedSeasonId = season.id, episodes = emptyList()) }
        loadEpisodes(season)
    }

    fun retry() {
        val season = _state.value.selectedSeason
        if (season == null) loadSeasons() else loadEpisodes(season)
    }

    private fun loadSeasons() {
        val item = _state.value.item ?: return
        val contentSource = source ?: return
        _state.update { it.copy(loadingSeasons = true, failed = false) }
        viewModelScope.launch {
            try {
                val seasons = contentSource.seasons(item)
                // Open on the season of the episode watched last, else the first.
                val lastSeason = _state.value.lastEpisode?.playback?.seasonId
                val selected = seasons.firstOrNull { it.id == lastSeason } ?: seasons.firstOrNull()
                _state.update { it.copy(seasons = seasons, loadingSeasons = false, selectedSeasonId = selected?.id) }
                selected?.let(::loadEpisodes)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.w(TAG, e) { "Seasons of ${item.id} could not be loaded" }
                _state.update { it.copy(loadingSeasons = false, failed = true) }
            }
        }
    }

    private fun loadEpisodes(season: Season) {
        val item = _state.value.item ?: return
        val contentSource = source ?: return
        episodesJob?.cancel()
        _state.update { it.copy(loadingEpisodes = true, failed = false) }
        episodesJob = viewModelScope.launch {
            try {
                val episodes = contentSource.episodes(item, season)
                _state.update { it.copy(episodes = episodes, loadingEpisodes = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.w(TAG, e) { "Episodes of ${item.id} could not be loaded" }
                _state.update { it.copy(loadingEpisodes = false, failed = true) }
            }
        }
    }

    companion object {
        const val ARG_PROFILE_ID = "profileId"
        const val ARG_ITEM_ID = "itemId"
        private const val TAG = "VodDetail"
    }
}
