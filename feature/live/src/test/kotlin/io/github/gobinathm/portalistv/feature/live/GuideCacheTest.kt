package io.github.gobinathm.portalistv.feature.live

import com.google.common.truth.Truth.assertThat
import io.github.gobinathm.portalistv.core.model.Programme
import org.junit.Test

class GuideCacheTest {
    private val cache = GuideCache()
    private val hour = 3_600_000L

    private fun show(start: Long, end: Long) = Programme("Show", null, start, end)

    @Test
    fun `fetches each channel once while a request is out`() {
        assertThat(cache.claim("1", 0)).isTrue()
        assertThat(cache.claim("1", 0)).isFalse()
        cache.release("1")
        assertThat(cache.claim("1", 0)).isTrue()
    }

    @Test
    fun `keeps a guide until fewer than two programmes are left`() {
        cache.claim("1", 0)
        cache.store("1", listOf(show(0, hour), show(hour, 2 * hour)), 0)
        assertThat(cache.claim("1", hour / 2)).isFalse()
        assertThat(cache.claim("1", hour + 1)).isTrue()
    }

    @Test
    fun `asks again for a channel without a guide only after a while`() {
        cache.claim("1", 0)
        cache.failed("1", 0)
        assertThat(cache.claim("1", GuideCache.EMPTY_RETRY_MS - 1)).isFalse()
        assertThat(cache.claim("1", GuideCache.EMPTY_RETRY_MS)).isTrue()
    }

    @Test
    fun `does not hammer a portal that sends a single programme`() {
        cache.claim("1", 0)
        cache.store("1", listOf(show(0, hour)), 0)
        assertThat(cache.claim("1", 1_000)).isFalse()
        assertThat(cache.claim("1", GuideCache.MIN_REFETCH_MS)).isTrue()
    }
}
