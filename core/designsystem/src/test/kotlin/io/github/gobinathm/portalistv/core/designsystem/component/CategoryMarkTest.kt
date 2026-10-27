package io.github.gobinathm.portalistv.core.designsystem.component

import com.google.common.truth.Truth.assertThat
import io.github.gobinathm.portalistv.core.designsystem.R
import io.github.gobinathm.portalistv.core.model.ALL_CATEGORIES
import io.github.gobinathm.portalistv.core.model.FAVORITES_CATEGORY
import io.github.gobinathm.portalistv.core.model.LiveCategory
import org.junit.Test

class CategoryMarkTest {
    private fun mark(title: String, adult: Boolean = false, id: String = "1") = LiveCategory(id, title, adult).mark()

    @Test
    fun `matches genre words anywhere in the name`() {
        assertThat(mark("KIDS PREMIUM")).isEqualTo(CategoryMark.Icon(R.drawable.ic_category_kids))
        assertThat(mark("SPORTS PPV")).isEqualTo(CategoryMark.Icon(R.drawable.ic_category_sports))
        assertThat(mark("UK | News")).isEqualTo(CategoryMark.Icon(R.drawable.ic_category_news))
        assertThat(mark("Late Movies")).isEqualTo(CategoryMark.Icon(R.drawable.ic_category_movies))
    }

    @Test
    fun `languages are written in their own script, ahead of genres`() {
        assertThat(mark("HINDI")).isEqualTo(CategoryMark.Script(Language("हिन्दी", "हि")))
        assertThat(mark("|IN| TAMIL MOVIES")).isEqualTo(CategoryMark.Script(Language("தமிழ்", "த")))
        assertThat(mark("Bangla News")).isEqualTo(CategoryMark.Script(Language("বাংলা", "বা")))
        // Adult categories keep the lock even when they name a language.
        assertThat(mark("HINDI 18+", adult = true)).isEqualTo(CategoryMark.Icon(R.drawable.ic_category_adult))
    }

    @Test
    fun `country names are not languages`() {
        assertThat(mark("THAILAND")).isEqualTo(CategoryMark.Initials("TH"))
        assertThat(mark("Saudi Arabia")).isEqualTo(CategoryMark.Initials("SA"))
    }

    @Test
    fun `adult categories get the lock whatever their name`() {
        assertThat(mark("Late night", adult = true)).isEqualTo(CategoryMark.Icon(R.drawable.ic_category_adult))
    }

    @Test
    fun `other categories get initials`() {
        assertThat(mark("MALAYSIA")).isEqualTo(CategoryMark.Initials("MA"))
        assertThat(mark("Sri Lanka")).isEqualTo(CategoryMark.Initials("SL"))
        assertThat(mark("---")).isEqualTo(CategoryMark.Initials("#"))
    }

    @Test
    fun `the all-channels and favorites entries have their own icons`() {
        assertThat(mark("All", id = ALL_CATEGORIES)).isEqualTo(CategoryMark.Icon(R.drawable.ic_category_all))
        assertThat(mark("", id = FAVORITES_CATEGORY)).isEqualTo(CategoryMark.Icon(R.drawable.ic_favorite))
    }

    @Test
    fun `does not match inside other words`() {
        // "sport" must start a word: "Passport" is not sport.
        assertThat(mark("Passport TV")).isEqualTo(CategoryMark.Initials("PT"))
    }
}
