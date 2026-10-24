package io.github.gobinathm.portalistv.feature.live

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gobinathm.portalistv.core.common.log.Log
import io.github.gobinathm.portalistv.core.data.ProfileRepository
import io.github.gobinathm.portalistv.core.data.source.ContentSources
import io.github.gobinathm.portalistv.core.datastore.SettingsRepository
import io.github.gobinathm.portalistv.core.model.ALL_CATEGORIES
import io.github.gobinathm.portalistv.core.model.ContentSource
import io.github.gobinathm.portalistv.core.model.LiveCategory
import io.github.gobinathm.portalistv.core.model.LiveChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class LiveUiState(
    val profileName: String = "",
    /** Starts with the all-channels entry (id [ALL_CATEGORIES]). */
    val categories: List<LiveCategory> = emptyList(),
    val selectedCategoryId: String = ALL_CATEGORIES,
    val pages: ChannelPages = ChannelPages(ALL_CATEGORIES),
    val loadingCategories: Boolean = true,
    val loadingChannels: Boolean = false,
    /** The last portal request failed; [LiveViewModel.retry] repeats it. */
    val failed: Boolean = false,
) {
    val channels: List<LiveChannel> get() = pages.channels
    val totalChannels: Int get() = pages.totalItems
}

/**
 * Live TV: the profile's categories and the selected category's channels, loaded page by page as
 * the list scrolls. Adult categories and channels are left out unless the settings allow them.
 */
@HiltViewModel
class LiveViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProfileRepository,
    private val sources: ContentSources,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val profileId: Long = savedStateHandle[ARG_PROFILE_ID] ?: 0L
    private val initialCategoryId: String = savedStateHandle[ARG_CATEGORY_ID] ?: ALL_CATEGORIES

    private val _state = MutableStateFlow(LiveUiState(selectedCategoryId = initialCategoryId))
    val state: StateFlow<LiveUiState> = _state.asStateFlow()

    private var source: ContentSource? = null
    private var showAdult = false
    private var channelsJob: Job? = null

    init {
        loadCategories()
    }

    fun retry() {
        if (_state.value.categories.isEmpty()) loadCategories() else loadMore()
    }

    fun selectCategory(categoryId: String) {
        if (categoryId == _state.value.selectedCategoryId && channelsJob != null) return
        channelsJob?.cancel()
        channelsJob = null
        _state.update {
            it.copy(selectedCategoryId = categoryId, pages = ChannelPages(categoryId), loadingChannels = false)
        }
        loadMore()
    }

    /** Called with the last channel index on screen; loads the next page when it gets close. */
    fun onScrolledTo(lastVisibleIndex: Int) {
        if (_state.value.pages.needsMore(lastVisibleIndex)) loadMore()
    }

    /** The portal page [channel] came from, so the player zaps on from there. */
    fun pageOf(channel: LiveChannel): Int = _state.value.pages.pageOf(channel.id)

    private fun loadCategories() {
        _state.update { it.copy(loadingCategories = true, failed = false) }
        viewModelScope.launch {
            try {
                val profile = repository.get(profileId) ?: return@launch
                showAdult = settings.settings.first().showAdultContent
                val contentSource = sources.forProfile(profile).also { source = it }
                val categories = contentSource.liveCategories().filter { showAdult || !it.adult }
                val all = categories.firstOrNull { it.id == ALL_CATEGORIES } ?: LiveCategory(ALL_CATEGORIES, "", false)
                val list = listOf(all) + categories.filter { it.id != ALL_CATEGORIES }
                val selected = initialCategoryId.takeIf { id -> list.any { it.id == id } } ?: ALL_CATEGORIES
                _state.update {
                    it.copy(profileName = profile.name, categories = list, loadingCategories = false)
                }
                selectCategory(selected)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.w(TAG, e) { "Categories could not be loaded" }
                _state.update { it.copy(loadingCategories = false, failed = true) }
            }
        }
    }

    private fun loadMore() {
        val contentSource = source ?: return
        if (_state.value.loadingChannels || _state.value.pages.complete) return
        _state.update { it.copy(loadingChannels = true, failed = false) }
        channelsJob = viewModelScope.launch {
            try {
                // A page can be all adult channels; keep going until something new shows or the list ends.
                do {
                    val pages = _state.value.pages
                    val result = contentSource.liveChannels(pages.categoryId, pages.nextPage)
                    val items = result.items.filter { showAdult || !it.adult }
                    val next = pages.append(pages.nextPage, items, result.pageCount, result.totalItems)
                    _state.update { it.copy(pages = next) }
                } while (next.channels.size == pages.channels.size && !next.complete)
                _state.update { it.copy(loadingChannels = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.w(TAG, e) { "Channels could not be loaded" }
                _state.update { it.copy(loadingChannels = false, failed = true) }
            }
        }
    }

    companion object {
        const val ARG_PROFILE_ID = "profileId"
        const val ARG_CATEGORY_ID = "categoryId"
        private const val TAG = "Live"
    }
}
