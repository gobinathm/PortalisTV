package io.github.gobinathm.portalistv.feature.settings

import com.google.common.truth.Truth.assertThat
import io.github.gobinathm.portalistv.core.player.PlayerSettings
import org.junit.Test
import java.util.Locale

class SettingsChoicesTest {
    @Test
    fun `formats buffer sizes in seconds`() {
        assertThat(formatSeconds(300)).isEqualTo("0.3")
        assertThat(formatSeconds(1500)).isEqualTo("1.5")
        assertThat(formatSeconds(10_000)).isEqualTo("10")
    }

    @Test
    fun `buffer choices include the default and stay in range`() {
        assertThat(NetworkCachingChoicesMs).contains(PlayerSettings.DEFAULT_NETWORK_CACHING_MS)
        assertThat(NetworkCachingChoicesMs.all { it in PlayerSettings.NETWORK_CACHING_RANGE_MS }).isTrue()
    }

    @Test
    fun `languages are unique two-letter codes sorted by name`() {
        val choices = languageChoices(Locale.ENGLISH)
        assertThat(choices.map { it.code }).containsNoDuplicates()
        assertThat(choices.all { it.code.length == 2 }).isTrue()
        assertThat(choices.map { it.name.lowercase() }).isInOrder()
        assertThat(choices).contains(LanguageChoice("ta", "Tamil"))
    }
}
