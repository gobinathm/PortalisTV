package io.github.gobinathm.portalistv.feature.player

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
import io.github.gobinathm.portalistv.core.model.LiveChannel
import io.github.gobinathm.portalistv.core.model.PlayerEngine
import io.github.gobinathm.portalistv.core.player.MediaSource
import io.github.gobinathm.portalistv.core.player.Player
import io.github.gobinathm.portalistv.core.player.PlayerFactory
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

data class PlayerUiState(
    val channel: LiveChannel? = null,
    /** Asking the portal for the stream link. */
    val resolving: Boolean = true,
    /** The portal could not provide a stream for this channel. */
    val linkFailed: Boolean = false,
    /** Continue in a picture-in-picture window when the user presses Home. */
    val pictureInPicture: Boolean = false,
    /** A stream to hand to the external player app; cleared once launched. */
    val externalStream: MediaSource? = null,
    /** The profile uses an external player, but no installed app can open the stream. */
    val noExternalPlayer: Boolean = false,
)

/**
 * Plays a live channel and zaps through its category. Owns the [Player] for the screen's lifetime,
 * created once the settings are read; profiles set to an external player get no internal player.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProfileRepository,
    private val sources: ContentSources,
    private val settings: SettingsRepository,
    private val playerFactory: PlayerFactory,
) : ViewModel() {
    private val profileId: Long = savedStateHandle[ARG_PROFILE_ID] ?: 0L
    private val categoryId: String = savedStateHandle[ARG_CATEGORY_ID] ?: ALL_CATEGORIES
    private val page: Int = savedStateHandle[ARG_PAGE] ?: 1
    private val channelId: String? = savedStateHandle[ARG_CHANNEL_ID]

    private val _player = MutableStateFlow<Player?>(null)
    val player: StateFlow<Player?> = _player.asStateFlow()

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var source: ContentSource? = null
    private var lineup: ChannelLineup? = null
    private var tuneJob: Job? = null

    init {
        viewModelScope.launch {
            val profile = repository.get(profileId) ?: return@launch
            val appSettings = settings.settings.first()
            val external = profile.playback.engine == PlayerEngine.EXTERNAL
            if (!external) _player.value = playerFactory.create(appSettings.player)
            _state.update { it.copy(pictureInPicture = appSettings.pictureInPicture && !external) }
            val contentSource = sources.forProfile(profile)
            val channels = ChannelLineup(contentSource, categoryId, appSettings.showAdultContent).also { lineup = it }
            source = contentSource
            val start = runCatching { channels.load(page) }.getOrDefault(emptyList())
            start.firstOrNull { it.id == channelId }?.let(::tune)
                ?: _state.update { it.copy(resolving = false, linkFailed = true) }
        }
    }

    /** Next (+1) or previous (-1) channel. Rapid presses cancel tuning of the channels skipped over. */
    fun zap(step: Int) {
        val current = _state.value.channel ?: return
        val channels = lineup ?: return
        viewModelScope.launch {
            runCatching { channels.neighbour(current, step) }.getOrNull()?.let(::tune)
        }
    }

    /** Stops playback while the app is in the background. */
    fun onBackground() {
        _player.value?.stop()
    }

    /** Re-tunes on return: live links expire, so a fresh one is requested. */
    fun onForeground() {
        _state.value.channel?.let(::tune)
    }

    private fun tune(channel: LiveChannel) {
        val contentSource = source ?: return
        tuneJob?.cancel()
        _state.update { PlayerUiState(channel = channel, pictureInPicture = it.pictureInPicture) }
        tuneJob = viewModelScope.launch {
            try {
                val url = contentSource.streamUrl(channel)
                val title = listOfNotNull(channel.number?.toString(), channel.name).joinToString("  ")
                val media = MediaSource(url, title, isLive = true, userAgent = contentSource.playbackUserAgent)
                val player = _player.value
                if (player != null) {
                    player.play(media)
                    _state.update { it.copy(resolving = false) }
                } else {
                    _state.update { it.copy(resolving = false, externalStream = media) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.w(TAG, e) { "No stream link for channel ${channel.id}" }
                _state.update { it.copy(resolving = false, linkFailed = true) }
            }
        }
    }

    /** The screen started the external player, or found none. */
    fun onExternalLaunched(found: Boolean) {
        _state.update { it.copy(externalStream = null, noExternalPlayer = !found) }
    }

    override fun onCleared() {
        _player.value?.release()
    }

    companion object {
        const val ARG_PROFILE_ID = "profileId"
        const val ARG_CATEGORY_ID = "categoryId"
        const val ARG_PAGE = "page"
        const val ARG_CHANNEL_ID = "channelId"
        private const val TAG = "Player"
    }
}
