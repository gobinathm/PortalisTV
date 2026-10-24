package io.github.gobinathm.portalistv.core.datastore

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeRowsTest {
    @Test
    fun `lists every row once, adding missing ones at the end`() {
        val stored = listOf(HomeRowSetting(HomeRow.ON_DEMAND), HomeRowSetting(HomeRow.ON_DEMAND, visible = false))
        val rows = normalizedHomeRows(stored)
        assertThat(rows.map { it.row }).containsExactly(
            HomeRow.ON_DEMAND,
            HomeRow.CONTINUE_WATCHING,
            HomeRow.FAVORITES,
            HomeRow.LIVE_TV,
        ).inOrder()
        assertThat(rows.first().visible).isTrue()
    }

    @Test
    fun `moves a row up and down, not past the ends`() {
        val rows = normalizedHomeRows(emptyList())
        assertThat(rows.moved(HomeRow.FAVORITES, -1).map { it.row }.take(2))
            .containsExactly(HomeRow.FAVORITES, HomeRow.CONTINUE_WATCHING).inOrder()
        assertThat(rows.moved(HomeRow.CONTINUE_WATCHING, -1)).isEqualTo(rows)
        assertThat(rows.moved(HomeRow.ON_DEMAND, +1)).isEqualTo(rows)
    }
}
