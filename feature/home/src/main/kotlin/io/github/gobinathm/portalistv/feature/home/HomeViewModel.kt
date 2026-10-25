package io.github.gobinathm.portalistv.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gobinathm.portalistv.core.common.log.Log
import io.github.gobinathm.portalistv.core.data.FavoritesRepository
import io.github.gobinathm.portalistv.core.data.ProfileRepository
import io.github.gobinathm.portalistv.core.data.WatchProgressRepository
import io.github.gobinathm.portalistv.core.data.image.PortalImages
import io.github.gobinathm.portalistv.core.data.source.ContentSources
import io.github.gobinathm.portalistv.core.data.usesPlainHttp
import io.github.gobinathm.portalistv.core.datastore.HomeRow
import io.github.gobinathm.portalistv.core.datastore.SettingsRepository
import io.github.gobinathm.portalistv.core.designsystem.component.FavoriteAction
import io.github.gobinathm.portalistv.core.model.ALL_CATEGORIES
import io.github.gobinathm.portalistv.core.model.ContentSource
import io.github.gobinathm.portalistv.core.model.FAVORITES_CATEGORY
import io.github.gobinathm.portalistv.core.model.LiveCategory
import io.github.gobinathm.portalistv.core.model.LiveChannel
import io.github.gobinathm.portalistv.core.model.SourceException
import io.github.gobinathm.portalistv.core.model.VodCategory
import io.github.gobinathm.portalistv.core.model.WatchProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.net.ssl.SSLException

enum class ConnectionError {
    INVALID_ADDRESS, NOT_FOUND, DEVICE_REFUSED, AUTHORIZATION, BAD_RESPONSE,
    NO_NETWORK, TIMEOUT, TLS, UNKNOWN,
}

data class HomeUiState(
    val profileName: String = "",
    /** The portal is reached over plain HTTP. */
    val notSecure: Boolean = false,
    /** Connecting to the portal and reading its categories. */
    val connecting: Boolean = true,
    /** Live TV categories, starting with all channels (id [ALL_CATEGORIES]). */
    val categories: List<LiveCategory> = emptyList(),
    /** Every favorite channel, shown as a category after all channels. */
    val favorites: List<LiveChannel> = emptyList(),
    /** The favorites chosen for the Favorites row. */
    val homeFavorites: List<LiveChannel> = emptyList(),
    /** The first channels of the category last focused, shown under the categories. */
    val preview: CategoryPreview? = null,
    /** Movies and episodes started and not finished, most recent first. */
    val continueWatching: List<WatchProgress> = emptyList(),
    /** On-demand categories, starting with everything (id [ALL_CATEGORIES]); empty without on-demand. */
    val vodCategories: List<VodCategory> = emptyList(),
    /** The rows to show, in order, as chosen in Settings. */
    val rows: List<HomeRow> = HomeRow.entries,
    val error: ConnectionError? = null,
    val portalMessage: String? = null,
)

data class CategoryPreview(val category: LiveCategory, val channels: List<LiveChannel>)

/** The selected profile's home: what to continue watching, favorite channels, and the categories. */
@HiltViewModel
@Suppress("LongParameterList") // Injected collaborators; each is a separate concern.
class HomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProfileRepository,
    private val sources: ContentSources,
    private val settings: SettingsRepository,
    private val images: PortalImages,
    private val favorites: FavoritesRepository,
    private val watchProgress: WatchProgressRepository,
) : ViewModel() {
    private val profileId: Long = savedStateHandle[PROFILE_ID_ARG] ?: 0L

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _imageLoader = MutableStateFlow<ImageLoader?>(null)

    /** Loads channel logos through the profile's connection settings; null until the profile is read. */
    val imageLoader: StateFlow<ImageLoader?> = _imageLoader.asStateFlow()

    private var source: ContentSource? = null
    private var showAdult = false
    private var previewJob: Job? = null

    init {
        retry()
        viewModelScope.launch {
            val showAdult = settings.settings.first().showAdultContent
            favorites.channels(profileId).collect { list ->
                val shown = list.filter { channel -> showAdult || !channel.adult }
                _state.update { state ->
                    val preview = state.preview?.takeIf { it.category.id == FAVORITES_CATEGORY }?.copy(channels = shown)
                    state.copy(favorites = shown, preview = preview ?: state.preview)
                }
            }
        }
        viewModelScope.launch {
            val showAdult = settings.settings.first().showAdultContent
            favorites.homeChannels(profileId).collect { list ->
                _state.update { it.copy(homeFavorites = list.filter { channel -> showAdult || !channel.adult }) }
            }
        }
        viewModelScope.launch {
            settings.settings.collect { app ->
                _state.update { it.copy(rows = app.homeRows.filter { row -> row.visible }.map { row -> row.row }) }
            }
        }
        viewModelScope.launch {
            watchProgress.continueWatching(profileId).collect { list ->
                _state.update { it.copy(continueWatching = list) }
            }
        }
    }

    fun retry() {
        _state.update { it.copy(connecting = true, error = null, portalMessage = null) }
        viewModelScope.launch {
            try {
                val profile = repository.get(profileId) ?: return@launch
                _imageLoader.value = images.loader(profile)
                _state.update { it.copy(profileName = profile.name, notSecure = profile.usesPlainHttp) }
                val source = sources.forProfile(profile).also { source = it }
                source.connect()
                showAdult = settings.settings.first().showAdultContent
                val categories = source.liveCategories().filter { showAdult || !it.adult }
                val all = categories.firstOrNull { it.id == ALL_CATEGORIES } ?: LiveCategory(ALL_CATEGORIES, "", false)
                _state.update {
                    it.copy(
                        connecting = false,
                        categories = listOf(all) + categories.filter { c -> c.id != ALL_CATEGORIES },
                    )
                }
                previewCategory(all)
                loadVodCategories(source)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.w(TAG, e) { "Connection failed" }
                val portalMessage = (e as? SourceException)?.portalMessage
                _state.update {
                    it.copy(
                        connecting = false,
                        error = e.toConnectionError(),
                        portalMessage = portalMessage,
                    )
                }
            }
        }
    }

    /** On demand is optional: a portal without it, or a failure here, just leaves the row out. */
    private suspend fun loadVodCategories(source: ContentSource) {
        val categories = try {
            source.vodCategories().filter { showAdult || !it.adult }
        } catch (e: IOException) {
            Log.w(TAG, e) { "On-demand categories could not be loaded" }
            return
        }
        if (categories.isEmpty()) return
        val all = categories.firstOrNull { it.id == ALL_CATEGORIES } ?: VodCategory(ALL_CATEGORIES, "", false)
        _state.update { it.copy(vodCategories = listOf(all) + categories.filter { c -> c.id != ALL_CATEGORIES }) }
    }

    /** Shows the first page of [category]'s channels under the categories. */
    fun previewCategory(category: LiveCategory) {
        if (_state.value.preview?.category?.id == category.id) return
        previewJob?.cancel()
        if (category.id == FAVORITES_CATEGORY) {
            _state.update { it.copy(preview = CategoryPreview(category, it.favorites)) }
        } else {
            previewPortalCategory(category)
        }
    }

    private fun previewPortalCategory(category: LiveCategory) {
        val contentSource = source ?: return
        previewJob = viewModelScope.launch {
            try {
                val channels = contentSource.liveChannels(category.id, page = 1).items.filter { showAdult || !it.adult }
                _state.update { it.copy(preview = CategoryPreview(category, channels)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                // The categories still work; the preview row just stays as it was.
                Log.w(TAG, e) { "Channels of category ${category.id} could not be loaded" }
            }
        }
    }

    /** What was chosen in a channel's hold-OK menu. */
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

    /** Takes a movie or series off Continue Watching; its saved position goes too. */
    fun removeFromContinueWatching(progress: WatchProgress) {
        viewModelScope.launch { watchProgress.forget(profileId, progress.playback.itemId) }
    }

    companion object {
        /** Navigation argument with the id of the profile to open. */
        const val PROFILE_ID_ARG = "profileId"
        private const val TAG = "Home"
    }
}

internal fun IOException.toConnectionError(): ConnectionError = when (this) {
    is SourceException -> when (reason) {
        SourceException.Reason.INVALID_ADDRESS -> ConnectionError.INVALID_ADDRESS
        SourceException.Reason.NOT_FOUND -> ConnectionError.NOT_FOUND
        SourceException.Reason.DEVICE_REFUSED -> ConnectionError.DEVICE_REFUSED
        SourceException.Reason.AUTHORIZATION -> ConnectionError.AUTHORIZATION
        SourceException.Reason.BAD_RESPONSE -> ConnectionError.BAD_RESPONSE
    }
    is UnknownHostException -> ConnectionError.NO_NETWORK
    is SocketTimeoutException -> ConnectionError.TIMEOUT
    is SSLException -> ConnectionError.TLS
    else -> ConnectionError.UNKNOWN
}
