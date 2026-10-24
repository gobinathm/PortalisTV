package io.github.gobinathm.portalistv.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gobinathm.portalistv.core.common.log.Log
import io.github.gobinathm.portalistv.core.data.FavoritesRepository
import io.github.gobinathm.portalistv.core.data.ProfileRepository
import io.github.gobinathm.portalistv.core.data.WatchProgressRepository
import io.github.gobinathm.portalistv.core.data.source.ContentSources
import io.github.gobinathm.portalistv.core.datastore.SettingsRepository
import io.github.gobinathm.portalistv.core.model.ALL_CATEGORIES
import io.github.gobinathm.portalistv.core.model.ContentSource
import io.github.gobinathm.portalistv.core.model.FAVORITES_CATEGORY
import io.github.gobinathm.portalistv.core.model.LiveChannel
import io.github.gobinathm.portalistv.core.model.PagedResult
import io.github.gobinathm.portalistv.core.model.PlayerEngine
import io.github.gobinathm.portalistv.core.model.Programme
import io.github.gobinathm.portalistv.core.model.WatchProgress
import io.github.gobinathm.portalistv.core.player.MediaSource
import io.github.gobinathm.portalistv.core.player.Player
import io.github.gobinathm.portalistv.core.player.PlayerFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
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
    /** Digits typed on the remote so far, before they jump to that channel number. */
    val typedNumber: String = "",
    /** A typed channel number that doesn't exist in this lineup; shown briefly. */
    val numberNotFound: Int? = null,
    /** Now/next for the channel, when the portal has a guide for it. */
    val guide: List<Programme> = emptyList(),
    /** What's playing when it's a movie or an episode rather than a channel. */
    val vod: VodTitle? = null,
    /** Where left/right presses are taking playback; it seeks there once they pause. */
    val scrubTargetMs: Long? = null,
)

data class VodTitle(val title: String, val subtitle: String?)

/**
 * Plays a live channel and zaps through its category, or plays a movie or episode and keeps track
 * of how far it got. Owns the [Player] for the screen's lifetime, created once the settings are
 * read; profiles set to an external player get no internal player.
 */
@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions") // Injected collaborators; one entry point per player action.
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProfileRepository,
    private val sources: ContentSources,
    private val settings: SettingsRepository,
    private val favorites: FavoritesRepository,
    private val watchProgress: WatchProgressRepository,
    private val playerFactory: PlayerFactory,
) : ViewModel() {
    private val profileId: Long = savedStateHandle[ARG_PROFILE_ID] ?: 0L
    private val categoryId: String = savedStateHandle[ARG_CATEGORY_ID] ?: ALL_CATEGORIES
    private val page: Int = savedStateHandle[ARG_PAGE] ?: 1
    private val channelId: String? = savedStateHandle[ARG_CHANNEL_ID]

    private val vod: VodRequest? = VodRequest.from(savedStateHandle)

    private val _player = MutableStateFlow<Player?>(null)
    val player: StateFlow<Player?> = _player.asStateFlow()

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var source: ContentSource? = null
    private var lineup: ChannelLineup? = null
    private var tuneJob: Job? = null
    private var numberJob: Job? = null
    private var progressJob: Job? = null
    private var scrubJob: Job? = null

    init {
        viewModelScope.launch {
            val profile = repository.get(profileId) ?: return@launch
            val appSettings = settings.settings.first()
            val external = profile.playback.engine == PlayerEngine.EXTERNAL
            if (!external) _player.value = playerFactory.create(appSettings.player)
            _state.update { it.copy(pictureInPicture = appSettings.pictureInPicture && !external) }
            val contentSource = sources.forProfile(profile)
            source = contentSource
            if (vod != null) {
                playVod(vod, resume = !vod.startOver)
                return@launch
            }
            val channels = lineup(contentSource, appSettings.showAdultContent).also { lineup = it }
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

    /**
     * A digit from the remote. The number is taken after a short pause, or at once when it has as
     * many digits as channel numbers can have.
     */
    fun typeDigit(digit: Int) {
        // Movies have no channel numbers to jump to.
        if (vod != null) return
        val typed = (_state.value.typedNumber + digit).takeLast(MAX_NUMBER_DIGITS)
        _state.update { it.copy(typedNumber = typed, numberNotFound = null) }
        numberJob?.cancel()
        numberJob = viewModelScope.launch {
            if (typed.length < MAX_NUMBER_DIGITS) delay(NUMBER_ENTRY_MS)
            _state.update { it.copy(typedNumber = "") }
            val number = typed.toInt()
            val found = runCatching { lineup?.findNumber(number) }.getOrNull()
            if (found != null) {
                tune(found)
            } else {
                _state.update { it.copy(numberNotFound = number) }
                delay(NUMBER_ENTRY_MS)
                _state.update { it.copy(numberNotFound = null) }
            }
        }
    }

    /**
     * Moves the seek target forward or back without seeking yet, like other streaming apps: the
     * picture keeps playing while the target is chosen, and one seek follows a short pause.
     */
    fun scrub(forward: Boolean, repeatCount: Int) {
        val player = _player.value ?: return
        val playback = player.state.value
        if (!playback.seekable || playback.durationMs <= 0) return
        val from = _state.value.scrubTargetMs ?: playback.positionMs
        val target = scrubTarget(from, forward, repeatCount, playback.durationMs)
        _state.update { it.copy(scrubTargetMs = target) }
        scrubJob?.cancel()
        scrubJob = viewModelScope.launch {
            delay(SCRUB_COMMIT_MS)
            player.seekTo(target)
            _state.update { it.copy(scrubTargetMs = null) }
        }
    }

    /** Stops playback while the app is in the background, remembering where a movie got to. */
    fun onBackground() {
        saveProgress()
        _player.value?.stop()
    }

    /** Re-tunes on return: links expire, so a fresh one is requested. A movie resumes where it stopped. */
    fun onForeground() {
        val request = vod
        if (request != null) playVod(request, resume = true) else _state.value.channel?.let(::tune)
    }

    private fun playVod(request: VodRequest, resume: Boolean) {
        val contentSource = source ?: return
        tuneJob?.cancel()
        progressJob?.cancel()
        _state.update { PlayerUiState(vod = request.title, pictureInPicture = it.pictureInPicture) }
        tuneJob = viewModelScope.launch {
            try {
                val url = contentSource.vodStreamUrl(request.playback)
                val startMs = if (resume) watchProgress.get(profileId, request.playback)?.resumeMs ?: 0 else 0
                val media = MediaSource(
                    url,
                    listOfNotNull(request.title.title, request.title.subtitle).joinToString("  ·  "),
                    isLive = false,
                    userAgent = contentSource.playbackUserAgent,
                    startMs = startMs,
                )
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
                Log.w(TAG, e) { "No stream link for on-demand item ${request.playback.itemId}" }
                _state.update { it.copy(resolving = false, linkFailed = true) }
            }
        }
        progressJob = viewModelScope.launch {
            while (isActive) {
                delay(PROGRESS_SAVE_MS)
                saveProgress()
            }
        }
    }

    /** Stores the position of the movie or episode playing; nothing for live TV or before it starts. */
    private fun saveProgress() {
        val request = vod
        // Nothing to keep before the stream has started, or for live TV.
        val playback = _player.value?.state?.value?.takeIf { it.durationMs > 0 && it.positionMs > 0 }
        if (request == null || playback == null) return
        val progress = WatchProgress(
            playback = request.playback,
            title = request.title.title,
            subtitle = request.title.subtitle,
            posterUrl = request.posterUrl,
            isSeries = request.isSeries,
            positionMs = playback.positionMs,
            durationMs = playback.durationMs,
            updatedAtMillis = System.currentTimeMillis(),
        )
        // Not the view model's scope: the last save happens as the screen closes.
        progressScope.launch { watchProgress.save(profileId, progress) }
    }

    /** Favorites come from the device; other categories page through the portal. */
    private fun lineup(contentSource: ContentSource, showAdult: Boolean): ChannelLineup =
        if (categoryId == FAVORITES_CATEGORY) {
            ChannelLineup(
                fetch = { favorites.getChannels(profileId).let { PagedResult(it, it.size, pageCount = 1) } },
                showAdult = showAdult,
            )
        } else {
            ChannelLineup(contentSource, categoryId, showAdult)
        }

    private fun tune(channel: LiveChannel) {
        val contentSource = source ?: return
        tuneJob?.cancel()
        _state.update {
            PlayerUiState(channel = channel, pictureInPicture = it.pictureInPicture, typedNumber = it.typedNumber)
        }
        viewModelScope.launch {
            // Best effort: the guide is a nice-to-have next to the picture.
            val guide = runCatching { contentSource.nowNext(channel) }.getOrDefault(emptyList())
            _state.update { if (it.channel?.id == channel.id) it.copy(guide = guide) else it }
        }
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
        saveProgress()
        _player.value?.release()
    }

    companion object {
        const val ARG_PROFILE_ID = "profileId"
        const val ARG_CATEGORY_ID = "categoryId"
        const val ARG_PAGE = "page"
        const val ARG_CHANNEL_ID = "channelId"
        private const val MAX_NUMBER_DIGITS = 4
        private const val NUMBER_ENTRY_MS = 2_000L
        private const val PROGRESS_SAVE_MS = 15_000L

        /** Outlives the screen, so the position is saved even as the player closes. */
        private val progressScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private const val TAG = "Player"
    }
}
