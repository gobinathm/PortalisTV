package io.github.gobinathm.portalistv.feature.vod

import io.github.gobinathm.portalistv.core.model.Episode
import io.github.gobinathm.portalistv.core.model.Season
import io.github.gobinathm.portalistv.core.model.VodItem
import io.github.gobinathm.portalistv.core.model.VodPlayback

/** What the detail page asks the app to play. */
data class VodPlayRequest(
    val item: VodItem,
    val playback: VodPlayback,
    /** For an episode, e.g. "S1 E3 · Pilot". */
    val subtitle: String?,
    /** Play from the beginning even if it was watched part-way. */
    val startOver: Boolean,
) {
    companion object {
        fun movie(item: VodItem, startOver: Boolean = false) =
            VodPlayRequest(item, VodPlayback(item.id, playbackRef = item.playbackRef), subtitle = null, startOver)

        fun episode(item: VodItem, season: Season, episode: Episode, label: String) = VodPlayRequest(
            item,
            VodPlayback(item.id, season.id, episode.id, item.playbackRef),
            subtitle = label,
            startOver = false,
        )
    }
}
