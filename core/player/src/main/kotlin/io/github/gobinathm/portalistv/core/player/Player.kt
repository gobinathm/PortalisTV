package io.github.gobinathm.portalistv.core.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.StateFlow

/**
 * A video player engine. Screens use only this interface; libVLC is the first implementation.
 * Methods must be called on the main thread. A player is single-use: [release] it when the screen
 * goes away.
 */
interface Player {
    val state: StateFlow<PlaybackState>

    /** Renders the video. Place it once, full screen, behind any controls. */
    @Composable
    fun VideoSurface(modifier: Modifier)

    fun play(media: MediaSource)

    fun pause()

    fun resume()

    /** No-op unless [PlaybackState.seekable]. */
    fun seekTo(positionMs: Long)

    fun stop()

    fun selectAudioTrack(id: Int)

    /** [id] null turns subtitles off. */
    fun selectSubtitleTrack(id: Int?)

    fun setAspect(mode: AspectMode)

    fun release()
}

/** What to play. [userAgent] is sent with stream requests; some providers check it. */
data class MediaSource(
    val url: String,
    val title: String,
    val isLive: Boolean,
    val userAgent: String? = null,
)

data class PlaybackState(
    val status: Status = Status.IDLE,
    /** 0–100 while buffering. */
    val bufferingPercent: Int = 0,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val seekable: Boolean = false,
    val audioTracks: List<Track> = emptyList(),
    val selectedAudioTrack: Int? = null,
    val subtitleTracks: List<Track> = emptyList(),
    val selectedSubtitleTrack: Int? = null,
    val aspect: AspectMode = AspectMode.BEST_FIT,
    val error: PlayerError? = null,
) {
    enum class Status { IDLE, OPENING, BUFFERING, PLAYING, PAUSED, ENDED, ERROR }
}

data class Track(val id: Int, val name: String)

/** How the picture fits the screen. FILL zooms and crops the edges; STRETCH distorts instead. */
enum class AspectMode { BEST_FIT, FILL, STRETCH, RATIO_16_9, RATIO_4_3, ORIGINAL }

enum class PlayerError {
    /** The stream could not be opened or broke off. */
    STREAM_FAILED,
}

/** Creates players with the current settings. */
fun interface PlayerFactory {
    fun create(settings: PlayerSettings): Player
}
