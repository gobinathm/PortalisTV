package io.github.gobinathm.portalistv.player.vlc

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import io.github.gobinathm.portalistv.core.player.AspectMode
import io.github.gobinathm.portalistv.core.player.Deinterlace
import io.github.gobinathm.portalistv.core.player.HardwareDecoding
import io.github.gobinathm.portalistv.core.player.MediaSource
import io.github.gobinathm.portalistv.core.player.PlaybackState
import io.github.gobinathm.portalistv.core.player.PlaybackState.Status
import io.github.gobinathm.portalistv.core.player.Player
import io.github.gobinathm.portalistv.core.player.PlayerError
import io.github.gobinathm.portalistv.core.player.PlayerSettings
import io.github.gobinathm.portalistv.core.player.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

/** [Player] on libVLC. Create and use it on the main thread; libVLC delivers events there too. */
@Suppress("TooManyFunctions") // Most are the Player interface.
internal class VlcPlayer(
    context: Context,
    private val settings: PlayerSettings,
    verboseLogging: Boolean,
) : Player {
    private val libVlc = LibVLC(context.applicationContext, vlcOptions(settings, verboseLogging))
    private val mediaPlayer = MediaPlayer(libVlc)

    private val _state = MutableStateFlow(PlaybackState(aspect = settings.defaultAspect))
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    init {
        mediaPlayer.setEventListener(::onEvent)
        mediaPlayer.setVideoScale(settings.defaultAspect.toScale())
    }

    @Composable
    override fun VideoSurface(modifier: Modifier) {
        AndroidView(
            factory = { context ->
                VLCVideoLayout(context).also {
                    mediaPlayer.attachViews(it, null, ENABLE_SUBTITLES, USE_TEXTURE_VIEW)
                }
            },
            modifier = modifier,
            onRelease = { mediaPlayer.detachViews() },
        )
    }

    override fun play(media: MediaSource) {
        val vlcMedia = Media(libVlc, media.url.toUri()).apply {
            setHWDecoderEnabled(
                settings.hardwareDecoding != HardwareDecoding.OFF,
                settings.hardwareDecoding == HardwareDecoding.ON,
            )
            addOption(":network-caching=${settings.networkCachingMs}")
            media.userAgent?.let { addOption(":http-user-agent=$it") }
        }
        mediaPlayer.media = vlcMedia
        // The player holds its own reference.
        vlcMedia.release()
        _state.value = PlaybackState(status = Status.OPENING, aspect = _state.value.aspect)
        mediaPlayer.play()
    }

    override fun pause() = mediaPlayer.pause()

    override fun resume() = mediaPlayer.play()

    override fun seekTo(positionMs: Long) {
        if (_state.value.seekable) mediaPlayer.setTime(positionMs.coerceAtLeast(0), FAST_SEEK)
    }

    override fun stop() = mediaPlayer.stop()

    override fun selectAudioTrack(id: Int) {
        mediaPlayer.setAudioTrack(id)
        refreshTracks()
    }

    override fun selectSubtitleTrack(id: Int?) {
        mediaPlayer.setSpuTrack(id ?: DISABLED_TRACK)
        refreshTracks()
    }

    override fun setAspect(mode: AspectMode) {
        mediaPlayer.setVideoScale(mode.toScale())
        _state.update { it.copy(aspect = mode) }
    }

    override fun release() {
        mediaPlayer.setEventListener(null)
        mediaPlayer.stop()
        mediaPlayer.release()
        libVlc.release()
    }

    private fun onEvent(event: MediaPlayer.Event) {
        when (event.type) {
            MediaPlayer.Event.Opening -> setStatus(Status.OPENING)
            MediaPlayer.Event.Buffering -> onBuffering(event.buffering)
            MediaPlayer.Event.Playing -> setStatus(Status.PLAYING)
            MediaPlayer.Event.Paused -> setStatus(Status.PAUSED)
            MediaPlayer.Event.Stopped -> if (_state.value.status != Status.ERROR) setStatus(Status.IDLE)
            MediaPlayer.Event.EndReached -> setStatus(Status.ENDED)
            MediaPlayer.Event.EncounteredError ->
                _state.update { it.copy(status = Status.ERROR, error = PlayerError.STREAM_FAILED) }
            MediaPlayer.Event.TimeChanged -> _state.update { it.copy(positionMs = event.timeChanged) }
            MediaPlayer.Event.LengthChanged -> _state.update { it.copy(durationMs = event.lengthChanged) }
            MediaPlayer.Event.SeekableChanged -> _state.update { it.copy(seekable = event.seekable) }
            MediaPlayer.Event.ESAdded, MediaPlayer.Event.ESDeleted, MediaPlayer.Event.ESSelected -> refreshTracks()
            else -> Unit
        }
    }

    private fun onBuffering(percent: Float) {
        val done = percent >= FULL_BUFFER
        _state.update {
            it.copy(
                bufferingPercent = percent.toInt(),
                // libVLC sends no Playing event after a mid-stream rebuffer completes.
                status = if (done) Status.PLAYING else if (it.status == Status.PAUSED) it.status else Status.BUFFERING,
            )
        }
    }

    private fun setStatus(status: Status) = _state.update { it.copy(status = status) }

    private fun refreshTracks() {
        // libVLC lists a "Disable" entry with id -1; the UI offers its own "off" choice.
        val audio = mediaPlayer.audioTracks.orEmpty().filter { it.id != DISABLED_TRACK }.map { Track(it.id, it.name) }
        val subtitles = mediaPlayer.spuTracks.orEmpty().filter { it.id != DISABLED_TRACK }.map { Track(it.id, it.name) }
        _state.update {
            it.copy(
                audioTracks = audio,
                selectedAudioTrack = mediaPlayer.audioTrack.takeIf { id -> id != DISABLED_TRACK },
                subtitleTracks = subtitles,
                selectedSubtitleTrack = mediaPlayer.spuTrack.takeIf { id -> id != DISABLED_TRACK },
            )
        }
    }

    private companion object {
        const val ENABLE_SUBTITLES = true
        const val USE_TEXTURE_VIEW = false
        const val FAST_SEEK = true
        const val DISABLED_TRACK = -1
        const val FULL_BUFFER = 100f

        fun vlcOptions(settings: PlayerSettings, verbose: Boolean): List<String> = buildList {
            // libVLC logs to logcat directly, bypassing the redacting logger, and its errors contain
            // stream URLs. Only debug builds get its messages.
            add(if (verbose) "-vv" else "--quiet")
            add("--http-reconnect")
            add(if (settings.audioTimeStretch) "--audio-time-stretch" else "--no-audio-time-stretch")
            when (settings.deinterlace) {
                Deinterlace.AUTO -> add("--deinterlace=-1")
                Deinterlace.ON -> add("--deinterlace=1")
                Deinterlace.OFF -> add("--deinterlace=0")
            }
            add("--deinterlace-mode=yadif")
            settings.preferredAudioLanguage?.let { add("--audio-language=$it") }
            settings.preferredSubtitleLanguage?.let { add("--sub-language=$it") }
        }

        fun AspectMode.toScale(): MediaPlayer.ScaleType = when (this) {
            AspectMode.BEST_FIT -> MediaPlayer.ScaleType.SURFACE_BEST_FIT
            AspectMode.FILL -> MediaPlayer.ScaleType.SURFACE_FILL
            AspectMode.RATIO_16_9 -> MediaPlayer.ScaleType.SURFACE_16_9
            AspectMode.RATIO_4_3 -> MediaPlayer.ScaleType.SURFACE_4_3
            AspectMode.ORIGINAL -> MediaPlayer.ScaleType.SURFACE_ORIGINAL
        }
    }
}
