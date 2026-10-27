package io.github.gobinathm.portalistv.feature.vod

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gobinathm.portalistv.core.common.log.Log
import io.github.gobinathm.portalistv.core.data.ParentalLock
import io.github.gobinathm.portalistv.core.data.PinResult
import io.github.gobinathm.portalistv.core.data.ProfileRepository
import io.github.gobinathm.portalistv.core.data.image.PortalImages
import io.github.gobinathm.portalistv.core.data.source.ContentSources
import io.github.gobinathm.portalistv.core.datastore.SettingsRepository
import io.github.gobinathm.portalistv.core.model.ALL_CATEGORIES
import io.github.gobinathm.portalistv.core.model.ContentSource
import io.github.gobinathm.portalistv.core.model.VodCategory
import io.github.gobinathm.portalistv.core.model.VodFilter
import io.github.gobinathm.portalistv.core.model.VodItem
import io.github.gobinathm.portalistv.core.model.VodYear
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
    /** The search and year the grid is narrowed by; the search stays when the category changes, the year doesn't. */
    val filter: VodFilter = VodFilter(),
    /** The years the selected category can be filtered by; empty when the portal lists none. */
    val years: List<VodYear> = emptyList(),
    val loadingCategories: Boolean = true,
    val loadingItems: Boolean = false,
    /** The last portal request failed; [VodBrowseViewModel.retry] repeats it. */
    val failed: Boolean = false,
    /** An adult category waiting for the parental PIN, with the last PIN's outcome. */
    val pinFor: String? = null,
    val pinResult: PinResult? = null,
) {
    val items: List<VodItem> get() = pages.items
}

/**
 * On demand: the portal's movie and series categories and a poster grid of the selected one,
 * loaded page by page as it scrolls. Adult content is left out unless the settings allow it.
 */
@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions") // Injected collaborators; one entry point per user action.
class VodBrowseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProfileRepository,
    private val sources: ContentSources,
    private val settings: SettingsRepository,
    private val images: PortalImages,
    private val seen: VodItems,
    private val parentalLock: ParentalLock,
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
    private var yearsJob: Job? = null

    init {
        loadCategories()
    }

    fun retry() {
        if (_state.value.categories.isEmpty()) loadCategories() else loadMore()
    }

    /**
     * Opens a category. An adult one needs the parental PIN first, if one is set: [ask] shows the
     * PIN dialog; otherwise (the rail selecting by focus) the category is skipped.
     */
    fun selectCategory(categoryId: String, ask: Boolean = true) {
        val adult = _state.value.categories.firstOrNull { it.id == categoryId }?.adult == true
        if (!adult) return open(categoryId)
        viewModelScope.launch {
            when {
                parentalLock.isOpen() -> open(categoryId)
                ask -> _state.update { it.copy(pinFor = categoryId, pinResult = null) }
            }
        }
    }

    fun submitPin(pin: String) {
        val categoryId = _state.value.pinFor ?: return
        viewModelScope.launch {
            val result = parentalLock.check(pin)
            _state.update { it.copy(pinResult = result) }
            if (result == PinResult.Correct) {
                _state.update { it.copy(pinFor = null, pinResult = null) }
                open(categoryId)
            }
        }
    }

    /** Leaves the category locked; one opened straight from Home gives way to everything. */
    fun cancelPin() {
        val wasOpening = _state.value.pinFor == _state.value.selectedCategoryId
        _state.update { it.copy(pinFor = null, pinResult = null) }
        if (wasOpening) open(ALL_CATEGORIES)
    }

    /** Narrows the grid to titles (and, on most portals, actors) matching [text]; blank shows everything. */
    fun search(text: String) {
        val filter = _state.value.filter.copy(search = text.trim())
        if (filter != _state.value.filter) reload(filter)
    }

    /** Narrows the grid to one of [VodBrowseUiState.years]; null shows every year. */
    fun selectYear(yearId: String?) {
        val filter = _state.value.filter.copy(yearId = yearId)
        if (filter != _state.value.filter) reload(filter)
    }

    private fun open(categoryId: String) {
        if (categoryId == _state.value.selectedCategoryId && itemsJob != null) return
        _state.update { it.copy(selectedCategoryId = categoryId, years = emptyList()) }
        // Year ids are per category, so a year picked in another one no longer applies.
        reload(_state.value.filter.copy(yearId = null))
        loadYears(categoryId)
    }

    /** Starts the selected category over with [filter]. */
    private fun reload(filter: VodFilter) {
        itemsJob?.cancel()
        itemsJob = null
        _state.update { it.copy(filter = filter, pages = PagedItems(it.selectedCategoryId), loadingItems = false) }
        loadMore()
    }

    /** The year filter is optional: a portal that lists no years, or fails to, just doesn't offer it. */
    private fun loadYears(categoryId: String) {
        val contentSource = source ?: return
        yearsJob?.cancel()
        yearsJob = viewModelScope.launch {
            try {
                // Portals list them oldest first; recent years are the ones people look for.
                val years = contentSource.vodYears(categoryId).sortedByDescending { it.title }
                _state.update { if (it.selectedCategoryId == categoryId) it.copy(years = years) else it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.w(TAG, e) { "On-demand years could not be loaded" }
            }
        }
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
                val start = _state.value.pages
                val filter = _state.value.filter
                // The first page shows at once. Later ones come a few at a time and reach the grid in
                // one update: every update lays the screen out again, which stutters scrolling on a TV.
                // A page can be all adult items, so keep going until something new shows or the list ends.
                val batch = if (start.loadedPages == 0) 1 else PAGES_PER_LOAD
                var pages = start
                var fetched = 0
                do {
                    val result = contentSource.vodItems(pages.categoryId, pages.nextPage, filter)
                    val items = result.items.filter { showAdult || !it.adult }
                    seen.remember(items)
                    pages = pages.append(pages.nextPage, items, result.pageCount, result.totalItems) { it.id }
                    fetched++
                } while (!pages.complete && (fetched < batch || pages.items.size == start.items.size))
                _state.update { it.copy(pages = pages, loadingItems = false) }
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

        /** Pages fetched per load after the first; portals send about 14 titles a page. */
        private const val PAGES_PER_LOAD = 3
    }
}
