package io.github.gobinathm.portalistv.core.player

/** Player tuning chosen in the app settings. Applied when a player is created. */
data class PlayerSettings(
    /** How much the player buffers before starting. More is smoother on weak networks, slower to zap. */
    val networkCachingMs: Int = DEFAULT_NETWORK_CACHING_MS,
    val hardwareDecoding: HardwareDecoding = HardwareDecoding.AUTO,
    val deinterlace: Deinterlace = Deinterlace.AUTO,
    val defaultAspect: AspectMode = AspectMode.BEST_FIT,
    /** Keeps audio pitch natural when playback speed changes to catch up with live streams. */
    val audioTimeStretch: Boolean = true,
    /** ISO 639 language codes, e.g. "eng" or "de"; null keeps the stream's default. */
    val preferredAudioLanguage: String? = null,
    val preferredSubtitleLanguage: String? = null,
) {
    companion object {
        const val DEFAULT_NETWORK_CACHING_MS = 1500
        val NETWORK_CACHING_RANGE_MS = 300..10_000
    }
}

enum class HardwareDecoding { AUTO, ON, OFF }

/** Interlaced SD channels look combed without it; AUTO deinterlaces only interlaced video. */
enum class Deinterlace { AUTO, ON, OFF }
