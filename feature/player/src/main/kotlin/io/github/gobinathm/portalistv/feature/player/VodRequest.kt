package io.github.gobinathm.portalistv.feature.player

import androidx.lifecycle.SavedStateHandle
import io.github.gobinathm.portalistv.core.model.VodPlayback

/** A movie or episode to play, from the navigation arguments. Names match the app's route properties. */
internal data class VodRequest(
    val playback: VodPlayback,
    val title: VodTitle,
    val posterUrl: String?,
    val isSeries: Boolean,
    /** Play from the beginning even if it was watched part-way. */
    val startOver: Boolean,
) {
    companion object {
        const val ARG_ITEM_ID = "vodItemId"
        const val ARG_SEASON_ID = "vodSeasonId"
        const val ARG_EPISODE_ID = "vodEpisodeId"
        const val ARG_PLAYBACK_REF = "vodPlaybackRef"
        const val ARG_TITLE = "vodTitle"
        const val ARG_SUBTITLE = "vodSubtitle"
        const val ARG_POSTER_URL = "vodPosterUrl"
        const val ARG_IS_SERIES = "vodIsSeries"
        const val ARG_START_OVER = "vodStartOver"

        /** Null for live TV, which has no on-demand item. */
        fun from(args: SavedStateHandle): VodRequest? {
            val itemId = args.get<String>(ARG_ITEM_ID) ?: return null
            return VodRequest(
                playback = VodPlayback(itemId, args[ARG_SEASON_ID], args[ARG_EPISODE_ID], args[ARG_PLAYBACK_REF]),
                title = VodTitle(args[ARG_TITLE] ?: "", args[ARG_SUBTITLE]),
                posterUrl = args[ARG_POSTER_URL],
                isSeries = args[ARG_IS_SERIES] ?: false,
                startOver = args[ARG_START_OVER] ?: false,
            )
        }
    }
}
