package io.github.gobinathm.portalistv.core.data

import com.google.common.truth.Truth.assertThat
import io.github.gobinathm.portalistv.core.model.VodPlayback
import io.github.gobinathm.portalistv.core.model.WatchProgress
import org.junit.Test

class ContinueWatchingTest {
    private fun progress(item: String, episode: String? = null, position: Long = 10, updated: Long = 0) = WatchProgress(
        playback = VodPlayback(item, episodeId = episode),
        title = item,
        subtitle = null,
        posterUrl = null,
        isSeries = episode != null,
        positionMs = position,
        durationMs = 100,
        updatedAtMillis = updated,
    )

    @Test
    fun `keeps the latest episode of each series`() {
        val rows = listOf(
            progress("show", "e2", updated = 3),
            progress("movie", updated = 2),
            progress("show", "e1", updated = 1),
        )
        val result = LocalWatchProgressRepository.continueWatching(rows)
        assertThat(result.map { it.playback.episodeId ?: it.playback.itemId }).containsExactly("e2", "movie").inOrder()
    }

    @Test
    fun `drops what has been watched to the end`() {
        val rows = listOf(progress("done", position = 96), progress("half", position = 50))
        assertThat(LocalWatchProgressRepository.continueWatching(rows).map { it.title }).containsExactly("half")
    }

    @Test
    fun `resumes from the start once finished`() {
        assertThat(progress("done", position = 96).resumeMs).isEqualTo(0)
        assertThat(progress("half", position = 50).resumeMs).isEqualTo(50)
    }
}
