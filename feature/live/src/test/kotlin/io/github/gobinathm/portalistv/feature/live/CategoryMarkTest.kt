package io.github.gobinathm.portalistv.feature.live

import com.google.common.truth.Truth.assertThat
import io.github.gobinathm.portalistv.core.model.ALL_CATEGORIES
import io.github.gobinathm.portalistv.core.model.LiveCategory
import org.junit.Test

class CategoryMarkTest {
    private fun mark(title: String, adult: Boolean = false, id: String = "1") = LiveCategory(id, title, adult).mark()

    @Test
    fun `matches genre words anywhere in the name`() {
        assertThat(mark("KIDS PREMIUM")).isEqualTo(CategoryMark.Icon(R.drawable.ic_category_kids))
        assertThat(mark("SPORTS PPV")).isEqualTo(CategoryMark.Icon(R.drawable.ic_category_sports))
        assertThat(mark("UK | News")).isEqualTo(CategoryMark.Icon(R.drawable.ic_category_news))
        assertThat(mark("Tamil Movies")).isEqualTo(CategoryMark.Icon(R.drawable.ic_category_movies))
    }

    @Test
    fun `adult categories get the lock whatever their name`() {
        assertThat(mark("Late night", adult = true)).isEqualTo(CategoryMark.Icon(R.drawable.ic_category_adult))
    }

    @Test
    fun `other categories get initials`() {
        assertThat(mark("TAMIL")).isEqualTo(CategoryMark.Initials("TA"))
        assertThat(mark("Sri Lanka")).isEqualTo(CategoryMark.Initials("SL"))
        assertThat(mark("---")).isEqualTo(CategoryMark.Initials("#"))
    }

    @Test
    fun `the all-channels entry has its own icon`() {
        assertThat(mark("All", id = ALL_CATEGORIES)).isEqualTo(CategoryMark.Icon(R.drawable.ic_category_all))
    }

    @Test
    fun `does not match inside other words`() {
        // "sport" must start a word: "Passport" is not sport.
        assertThat(mark("Passport TV")).isEqualTo(CategoryMark.Initials("PT"))
    }
}
