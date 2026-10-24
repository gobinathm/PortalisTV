package io.github.gobinathm.portalistv.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NowNextTest {
    private fun show(title: String, start: Long, end: Long) = Programme(title, null, start, end)

    private val guide = listOf(show("Later", 200, 300), show("Earlier", 0, 100), show("Current", 100, 200))

    @Test
    fun `picks the programme on now and the one after`() {
        val result = guide.nowNext(nowMillis = 150)
        assertThat(result.now?.title).isEqualTo("Current")
        assertThat(result.next?.title).isEqualTo("Later")
        assertThat(result.now?.progress(150)).isEqualTo(0.5f)
    }

    @Test
    fun `has only next during a gap`() {
        val result = listOf(show("Later", 200, 300)).nowNext(nowMillis = 150)
        assertThat(result.now).isNull()
        assertThat(result.next?.title).isEqualTo("Later")
    }

    @Test
    fun `has nothing once the guide has run out`() {
        assertThat(guide.nowNext(nowMillis = 400)).isEqualTo(NowNext(null, null))
    }
}
