package io.github.gobinathm.portalistv.feature.vod

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gobinathm.portalistv.core.common.log.Log
import io.github.gobinathm.portalistv.core.data.ProfileRepository
import io.github.gobinathm.portalistv.core.data.image.PortalImages
import io.github.gobinathm.portalistv.core.data.source.ContentSources
import io.github.gobinathm.portalistv.core.datastore.SettingsRepository
import io.github.gobinathm.portalistv.core.model.ALL_CATEGORIES
import io.github.gobinathm.portalistv.core.model.ContentSource
import io.github.gobinathm.portalistv.core.model.VodCategory
import io.github.gobinathm.portalistv.core.model.VodItem
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

data class VodBrowseUiState(
    /** Starts with everything (id [ALL_CATEGORIES]). */
    val categories: List<VodCategory> = emptyList(),
    val selectedCategoryId: String = ALL_CATEGORIES,
    val pages: PagedItems<VodItem> = PagedItems(ALL_CATEGORIES),
    val loadingCategories: Boolean = true,
    val loadingItems: Boolean = false,
    /** The last portal request failed; [VodBrowseViewModel.retry] repeats it. */
    val failed: Boolean = false,
) {
    val items: List<VodItem> get() = pages.items
}

/**
 * On demand: the portal's movie and series categories and a poster grid of the selected one,
 * loaded page by page as it scrolls. Adult content is left out unless the settings allow it.
 */
@HiltViewModel
class VodBrowseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProfileRepository,
    private val sources: ContentSources,
    private val settings: SettingsRepository,
    private val images: PortalImages,
    private val seen: VodItems,
) : ViewModel() {
    private val profileId: Long = savedStateHandle[ARG_PROFILE_ID] ?: 0L
    private val initialCategoryId: String = savedStateHandle[ARG_CATEGORY_ID] ?: ALL_CATEGORIES

    private val _state = MutableStateFlow(VodBrowseUiState(selectedCategoryId = initialCategoryId))
    val state: StateFlow<VodBrowseUiState> = _state.asStateFlow()

    private val _imageLoader = MutableStateFlow<ImageLoader?>(null)

    /** Loads posters through the profile's connection settings; null until the profile is read. */
    val imageLoader: StateFlow<ImageLoader?> = _imageLoader.asStateFlow()

    private var source: ContentSource? = null
    private var showAdult = false
    private var itemsJob: Job? = null

    init {
        loadCategories()
    }

    fun retry() {
        if (_state.value.categories.isEmpty()) loadCategories() else loadMore()
    }

    fun selectCategory(categoryId: String) {
        if (categoryId == _state.value.selectedCategoryId && itemsJob != null) return
        itemsJob?.cancel()
        itemsJob = null
        _state.update { it.copy(selectedCategoryId = categoryId, pages = PagedItems(categoryId), loadingItems = false) }
        loadMore()
    }

    /** Called with the last grid index on screen; loads the next page when it gets close. */
    fun onScrolledTo(lastVisibleIndex: Int) {
        if (_state.value.pages.needsMore(lastVisibleIndex)) loadMore()
    }

    private fun loadCategories() {
        _state.update { it.copy(loadingCategories = true, failed = false) }
        viewModelScope.launch {
            try {
                val profile = repository.get(profileId) ?: return@launch
                _imageLoader.value = images.loader(profile)
                showAdult = settings.settings.first().showAdultContent
                val contentSource = sources.forProfile(profile).also { source = it }
                val categories = contentSource.vodCategories().filter { showAdult || !it.adult }
                val all = categories.firstOrNull { it.id == ALL_CATEGORIES } ?: VodCategory(ALL_CATEGORIES, "", false)
                val list = listOf(all) + categories.filter { it.id != ALL_CATEGORIES }
                val selected = initialCategoryId.takeIf { id -> list.any { it.id == id } } ?: ALL_CATEGORIES
                _state.update { it.copy(categories = list, loadingCategories = false) }
                selectCategory(selected)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.w(TAG, e) { "On-demand categories could not be loaded" }
                _state.update { it.copy(loadingCategories = false, failed = true) }
            }
        }
    }

    private fun loadMore() {
        val contentSource = source ?: return
        if (_state.value.loadingItems || _state.value.pages.complete) return
        _state.update { it.copy(loadingItems = true, failed = false) }
        itemsJob = viewModelScope.launch {
            try {
                // A page can be all adult items; keep going until something new shows or the list ends.
                do {
                    val pages = _state.value.pages
                    val result = contentSource.vodItems(pages.categoryId, pages.nextPage)
                    val items = result.items.filter { showAdult || !it.adult }
                    seen.remember(items)
                    val next = pages.append(pages.nextPage, items, result.pageCount, result.totalItems) { it.id }
                    _state.update { it.copy(pages = next) }
                } while (next.items.size == pages.items.size && !next.complete)
                _state.update { it.copy(loadingItems = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.w(TAG, e) { "On-demand items could not be loaded" }
                _state.update { it.copy(loadingItems = false, failed = true) }
            }
        }
    }

    companion object {
        const val ARG_PROFILE_ID = "profileId"
        const val ARG_CATEGORY_ID = "categoryId"
        private const val TAG = "Vod"
    }
}
