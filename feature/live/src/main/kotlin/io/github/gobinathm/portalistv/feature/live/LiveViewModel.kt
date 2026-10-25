package io.github.gobinathm.portalistv.feature.live

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gobinathm.portalistv.core.common.log.Log
import io.github.gobinathm.portalistv.core.data.FavoritesRepository
import io.github.gobinathm.portalistv.core.data.ParentalLock
import io.github.gobinathm.portalistv.core.data.PinResult
import io.github.gobinathm.portalistv.core.data.ProfileRepository
import io.github.gobinathm.portalistv.core.data.image.PortalImages
import io.github.gobinathm.portalistv.core.data.source.ContentSources
import io.github.gobinathm.portalistv.core.datastore.SettingsRepository
import io.github.gobinathm.portalistv.core.designsystem.component.FavoriteAction
import io.github.gobinathm.portalistv.core.model.ALL_CATEGORIES
import io.github.gobinathm.portalistv.core.model.ContentSource
import io.github.gobinathm.portalistv.core.model.FAVORITES_CATEGORY
import io.github.gobinathm.portalistv.core.model.LiveCategory
import io.github.gobinathm.portalistv.core.model.LiveChannel
import io.github.gobinathm.portalistv.core.model.Programme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.IOException
import javax.inject.Inject

data class LiveUiState(
    val profileName: String = "",
    /** Starts with the all-channels entry (id [ALL_CATEGORIES]), then favorites ([FAVORITES_CATEGORY]). */
    val categories: List<LiveCategory> = emptyList(),
    val selectedCategoryId: String = ALL_CATEGORIES,
    val pages: ChannelPages = ChannelPages(ALL_CATEGORIES),
    val loadingCategories: Boolean = true,
    val loadingChannels: Boolean = false,
    /** The last portal request failed; [LiveViewModel.retry] repeats it. */
    val failed: Boolean = false,
    val favoriteIds: Set<String> = emptySet(),
    /** Favorites shown in Home's Favorites row. */
    val homeIds: Set<String> = emptySet(),
    /** Guide entries per channel id, for the channels that have been on screen. */
    val guide: Map<String, List<Programme>> = emptyMap(),
    /** The clock the guide is shown against; ticks while the screen is open. */
    val nowMillis: Long = 0,
    /** An adult category waiting for the parental PIN, with the last PIN's outcome. */
    val pinFor: String? = null,
    val pinResult: PinResult? = null,
) {
    val channels: List<LiveChannel> get() = pages.channels
    val totalChannels: Int get() = pages.totalItems
}

/**
 * Live TV: the profile's categories and the selected category's channels, loaded page by page as
 * the list scrolls, with now/next for the channels on screen. Favorites are a category of their
 * own, kept on the device. Adult categories and channels are left out unless the settings allow them.
 */
@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions") // Collaborators and screen actions, each separate.
class LiveViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProfileRepository,
    private val sources: ContentSources,
    private val settings: SettingsRepository,
    private val favorites: FavoritesRepository,
    private val images: PortalImages,
    private val parentalLock: ParentalLock,
) : ViewModel() {
    private val profileId: Long = savedStateHandle[ARG_PROFILE_ID] ?: 0L
    private val initialCategoryId: String = savedStateHandle[ARG_CATEGORY_ID] ?: ALL_CATEGORIES

    private val _state =
        MutableStateFlow(LiveUiState(selectedCategoryId = initialCategoryId, nowMillis = System.currentTimeMillis()))
    val state: StateFlow<LiveUiState> = _state.asStateFlow()

    private val _imageLoader = MutableStateFlow<ImageLoader?>(null)

    /** Loads channel logos through the profile's connection settings; null until the profile is read. */
    val imageLoader: StateFlow<ImageLoader?> = _imageLoader.asStateFlow()

    private var source: ContentSource? = null
    private var showAdult = false
    private var channelsJob: Job? = null
    private var favoriteChannels: List<LiveChannel> = emptyList()

    private val guide = GuideCache()
    private val guideRequests = Semaphore(GUIDE_PARALLEL_REQUESTS)
    private var visible = IntRange.EMPTY

    init {
        loadCategories()
        viewModelScope.launch {
            favorites.channels(profileId).collect { list ->
                favoriteChannels = list
                _state.update { it.copy(favoriteIds = list.mapTo(HashSet()) { channel -> channel.id }) }
                if (_state.value.selectedCategoryId == FAVORITES_CATEGORY) showFavorites()
            }
        }
        viewModelScope.launch {
            favorites.homeIds(profileId).collect { ids -> _state.update { it.copy(homeIds = ids) } }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(CLOCK_TICK_MS)
                _state.update { it.copy(nowMillis = System.currentTimeMillis()) }
                loadGuide()
            }
        }
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

    private fun open(categoryId: String) {
        if (categoryId == _state.value.selectedCategoryId && channelsJob != null) return
        channelsJob?.cancel()
        channelsJob = null
        visible = IntRange.EMPTY
        _state.update {
            it.copy(selectedCategoryId = categoryId, pages = ChannelPages(categoryId), loadingChannels = false)
        }
        if (categoryId == FAVORITES_CATEGORY) showFavorites() else loadMore()
    }

    /** Called with the channel indexes on screen: loads the next page when near the end, and their guide. */
    fun onVisible(first: Int, last: Int) {
        visible = first..last
        if (_state.value.pages.needsMore(last)) loadMore()
        loadGuide()
    }

    /** What was chosen for [channel]: adding it to favorites, or a change from a favorite's menu. */
    fun onFavoriteAction(channel: LiveChannel, action: FavoriteAction) {
        viewModelScope.launch {
            when (action) {
                FavoriteAction.ADD -> favorites.add(profileId, channel)
                FavoriteAction.SHOW_ON_HOME -> favorites.setOnHome(profileId, channel.id, onHome = true)
                FavoriteAction.HIDE_FROM_HOME -> favorites.setOnHome(profileId, channel.id, onHome = false)
                FavoriteAction.REMOVE -> favorites.remove(profileId, channel.id)
            }
        }
    }

    /** The portal page [channel] came from, so the player zaps on from there. */
    fun pageOf(channel: LiveChannel): Int = _state.value.pages.pageOf(channel.id)

    private fun loadCategories() {
        _state.update { it.copy(loadingCategories = true, failed = false) }
        viewModelScope.launch {
            try {
                val profile = repository.get(profileId) ?: return@launch
                _imageLoader.value = images.loader(profile)
                showAdult = settings.settings.first().showAdultContent
                val contentSource = sources.forProfile(profile).also { source = it }
                val categories = contentSource.liveCategories().filter { showAdult || !it.adult }
                val all = categories.firstOrNull { it.id == ALL_CATEGORIES } ?: LiveCategory(ALL_CATEGORIES, "", false)
                val favoritesEntry = LiveCategory(FAVORITES_CATEGORY, "", adult = false)
                val list = listOf(all, favoritesEntry) + categories.filter { it.id != ALL_CATEGORIES }
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

    private fun showFavorites() {
        val channels = favoriteChannels.filter { showAdult || !it.adult }
        val pages = ChannelPages(FAVORITES_CATEGORY).append(1, channels, pageCount = 1, totalItems = channels.size)
        _state.update { it.copy(pages = pages, loadingChannels = false, failed = false) }
        loadGuide()
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
                loadGuide()
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.w(TAG, e) { "Channels could not be loaded" }
                _state.update { it.copy(loadingChannels = false, failed = true) }
            }
        }
    }

    /** Fetches now/next for the channels on screen (and a few beyond) that need it. */
    private fun loadGuide() {
        val contentSource = source ?: return
        val channels = _state.value.channels
        if (channels.isEmpty()) return
        val range = if (visible.isEmpty()) 0..GUIDE_LOOKAHEAD else visible.first..visible.last + GUIDE_LOOKAHEAD
        val now = System.currentTimeMillis()
        channels.slice(range.first.coerceAtLeast(0)..range.last.coerceAtMost(channels.lastIndex))
            .filter { guide.claim(it.id, now) }
            .forEach { channel -> viewModelScope.launch { fetchGuide(contentSource, channel) } }
    }

    private suspend fun fetchGuide(contentSource: ContentSource, channel: LiveChannel) {
        try {
            // The full guide rather than now/next: the guide view shows hours ahead from the same data.
            val programmes = guideRequests.withPermit { contentSource.guide(channel) }
            guide.store(channel.id, programmes, System.currentTimeMillis())
        } catch (e: CancellationException) {
            guide.release(channel.id)
            throw e
        } catch (e: IOException) {
            Log.w(TAG, e) { "No guide for channel ${channel.id}" }
            guide.failed(channel.id, System.currentTimeMillis())
        }
        _state.update { it.copy(guide = guide.snapshot()) }
    }

    companion object {
        const val ARG_PROFILE_ID = "profileId"
        const val ARG_CATEGORY_ID = "categoryId"
        private const val TAG = "Live"
        private const val GUIDE_PARALLEL_REQUESTS = 3

        /** Channels below the visible ones whose guide is fetched too, so it's there when scrolled to. */
        private const val GUIDE_LOOKAHEAD = 6
        private const val CLOCK_TICK_MS = 30_000L
    }
}
