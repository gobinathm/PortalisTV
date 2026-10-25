package io.github.gobinathm.portalistv.tools.logopack

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LogoMatcherTest {
    private val matcher = LogoMatcher(
        listOf(
            KnownChannel("SunTV.in", "Sun TV", emptyList(), "IN"),
            KnownChannel("DisneyChannel.us", "Disney Channel", listOf("Disney"), "US"),
            KnownChannel("DisneyChannel.uk", "Disney Channel", emptyList(), "UK"),
            KnownChannel("DisneyXD.us", "Disney XD", emptyList(), "US"),
            KnownChannel("Canal13.cl", "Canal 13", emptyList(), "CL"),
            KnownChannel("TeN.eg", "TeN", emptyList(), "EG"),
            KnownChannel("SuperHungama.in", "Super Hungama", listOf("Disney XD"), "IN"),
        ),
    )

    @Test
    fun `drops country tags, quality words and brackets`() {
        assertThat(matcher.match("|IN| SUN TV HD", null)?.id).isEqualTo("SunTV.in")
        assertThat(matcher.match("Sun TV (Backup) FHD", null)?.id).isEqualTo("SunTV.in")
        assertThat(matcher.match("DISNEY XD HD USA", null)?.id).isEqualTo("DisneyXD.us")
        assertThat(matcher.match("PREMIUM SUN TV 4K", "TAMIL")?.id).isEqualTo("SunTV.in")
    }

    @Test
    fun `matches without a trailing channel or TV`() {
        assertThat(matcher.match("SUN", null)?.id).isEqualTo("SunTV.in")
        assertThat(matcher.match("DISNEY HD", null)?.id).isEqualTo("DisneyChannel.us")
    }

    @Test
    fun `the name's or category's country picks between same-named channels`() {
        assertThat(matcher.match("UK: Disney Channel", null)?.id).isEqualTo("DisneyChannel.uk")
        assertThat(matcher.match("Disney Channel", "|UK| KIDS")?.id).isEqualTo("DisneyChannel.uk")
        assertThat(matcher.match("Disney Channel", "ENGLISH 4K")?.id).isEqualTo("DisneyChannel.us")
    }

    @Test
    fun `a same-named channel from another country doesn't match`() {
        assertThat(matcher.match("TEN HD", "ENGLISH AUSTRALIA")).isNull()
        assertThat(matcher.match("TEN HD", "ENTERTAINMENT")?.id).isEqualTo("TeN.eg")
    }

    @Test
    fun `a current name beats a former one`() {
        assertThat(matcher.match("DISNEY XD HD", "KIDS", preferred = listOf("IN", "US"))?.id).isEqualTo("DisneyXD.us")
        assertThat(matcher.match("Disney XD", "HINDI")?.id).isEqualTo("SuperHungama.in")
    }

    @Test
    fun `without a country, the portal's usual countries come first`() {
        assertThat(matcher.match("Disney Channel", "KIDS", preferred = listOf("UK", "US"))?.id)
            .isEqualTo("DisneyChannel.uk")
    }

    @Test
    fun `a name that matches nothing has no logo`() {
        assertThat(matcher.match("KIDS UPCOMING MOVIES", "KIDS")).isNull()
        assertThat(matcher.match("HD", null)).isNull()
    }

    @Test
    fun `reads countries from categories`() {
        assertThat(LogoMatcher.countriesOfCategory("HINDI")).containsExactly("IN")
        assertThat(LogoMatcher.countriesOfCategory("|US| NEWS")).containsExactly("US")
        assertThat(LogoMatcher.countriesOfCategory("ENGLISH EUROPE")).contains("UK")
        assertThat(LogoMatcher.countriesOfCategory("SPORTS")).isEmpty()
    }
}
