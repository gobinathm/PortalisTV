package io.github.gobinathm.portalistv.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gobinathm.portalistv.core.common.log.Log
import io.github.gobinathm.portalistv.core.data.ProfileRepository
import io.github.gobinathm.portalistv.core.data.source.ContentSources
import io.github.gobinathm.portalistv.core.data.usesPlainHttp
import io.github.gobinathm.portalistv.core.model.ALL_CATEGORIES
import io.github.gobinathm.portalistv.core.model.ContentSource
import io.github.gobinathm.portalistv.core.model.LiveCategory
import io.github.gobinathm.portalistv.core.model.LiveChannel
import io.github.gobinathm.portalistv.core.model.Profile
import io.github.gobinathm.portalistv.core.model.SourceException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.net.ssl.SSLException

/** Each step of the first connection, so a failure shows exactly where it stopped. */
enum class ConnectionStep { CONNECTING, LOADING_CATEGORIES, LOADING_CHANNELS, RESOLVING_STREAM, DONE }

enum class ConnectionError {
    INVALID_ADDRESS, NOT_FOUND, DEVICE_REFUSED, AUTHORIZATION, BAD_RESPONSE,
    NO_NETWORK, TIMEOUT, TLS, UNKNOWN,
}

data class HomeUiState(
    val profileName: String = "",
    /** The portal is reached over plain HTTP. */
    val notSecure: Boolean = false,
    val step: ConnectionStep = ConnectionStep.CONNECTING,
    val accountName: String? = null,
    val categories: List<LiveCategory> = emptyList(),
    val channels: List<LiveChannel> = emptyList(),
    val totalChannels: Int = 0,
    /** Host-free summary of a resolved stream link, e.g. "http://…/live/101.ts". */
    val sampleStream: String? = null,
    val error: ConnectionError? = null,
    val portalMessage: String? = null,
)

/**
 * Connects to the selected profile's portal and loads a first view of its content. Until the
 * live TV screens exist (M4), this doubles as the connection check.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProfileRepository,
    private val sources: ContentSources,
) : ViewModel() {
    private val profileId: Long = savedStateHandle[PROFILE_ID_ARG] ?: 0L

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        retry()
    }

    fun retry() {
        _state.value = HomeUiState(profileName = _state.value.profileName, notSecure = _state.value.notSecure)
        viewModelScope.launch {
            try {
                val profile = repository.get(profileId) ?: return@launch
                _state.update { it.copy(profileName = profile.name, notSecure = profile.usesPlainHttp) }
                load(profile)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.w(TAG, e) { "Connection check failed at ${_state.value.step}" }
                val portalMessage = (e as? SourceException)?.portalMessage
                _state.update { it.copy(error = e.toConnectionError(), portalMessage = portalMessage) }
            }
        }
    }

    private suspend fun load(profile: Profile) {
        val source: ContentSource = sources.forProfile(profile)
        val account = source.connect()
        _state.update { it.copy(accountName = account.displayName, step = ConnectionStep.LOADING_CATEGORIES) }

        val categories = source.liveCategories()
        _state.update { it.copy(categories = categories, step = ConnectionStep.LOADING_CHANNELS) }

        val page = source.liveChannels(ALL_CATEGORIES, page = 1)
        _state.update {
            it.copy(channels = page.items, totalChannels = page.totalItems, step = ConnectionStep.RESOLVING_STREAM)
        }

        val stream = page.items.firstOrNull()?.let { source.streamUrl(it) }
        _state.update { it.copy(sampleStream = stream?.let(::withoutHost), step = ConnectionStep.DONE) }
    }

    companion object {
        /** Navigation argument with the id of the profile to open. */
        const val PROFILE_ID_ARG = "profileId"
        private const val TAG = "Home"

        /** Shows the shape of a stream link without revealing the provider's host or tokens. */
        internal fun withoutHost(url: String): String =
            url.replace(Regex("""^([a-z]+://)[^/]+"""), "$1…").substringBefore('?')
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
