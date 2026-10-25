package io.github.gobinathm.portalistv.tools.logopack

import java.text.Normalizer
import java.util.Locale

/** A channel in the iptv-org database that has a usable logo. */
internal data class KnownChannel(val id: String, val name: String, val altNames: List<String>, val country: String)

/**
 * Finds the iptv-org channel a portal's channel name refers to. Portal names carry noise the
 * database doesn't ("|IN| SUN TV HD", "DISNEY XD HD USA"): country tags, quality words and
 * brackets are dropped. When the name or its category points at countries, only a channel from one
 * of them matches: a same-named channel elsewhere is usually a different channel.
 */
internal class LogoMatcher(channels: List<KnownChannel>) {
    /** A channel a key leads to, and whether through its current name rather than a former or other one. */
    private class Candidate(val channel: KnownChannel, val byName: Boolean)

    private val byKey: Map<String, List<Candidate>> = buildMap<String, MutableList<Candidate>> {
        for (channel in channels) {
            val names = keysOf(words(channel.name))
            (names + channel.altNames.flatMap { keysOf(words(it)) }).distinct().forEach { key ->
                getOrPut(key) { mutableListOf() } += Candidate(channel, byName = key in names)
            }
        }
    }

    /**
     * The channel [portalName] refers to. A channel's current name beats a former one (Disney XD
     * over a channel once called that). Without a country in the name or category, same-named
     * channels are then tried in the order of [preferred] countries (e.g. those the portal mostly carries).
     */
    fun match(portalName: String, categoryTitle: String?, preferred: List<String> = emptyList()): KnownChannel? {
        val name = parse(portalName)
        val countries = name.country?.let(::setOf) ?: categoryTitle?.let(::countriesOfCategory).orEmpty()
        return keysOf(name.words).firstNotNullOfOrNull { key ->
            val candidates = byKey[key].orEmpty().filter { countries.isEmpty() || it.channel.country in countries }
            val rank = compareBy<Candidate> { !it.byName }
                .thenBy { preferred.indexOf(it.channel.country).takeIf { i -> i >= 0 } ?: preferred.size }
            candidates.minWithOrNull(rank)?.channel
        }
    }

    /** Whether the name or category of a channel points at a country. */
    fun hasCountryHint(portalName: String, categoryTitle: String?): Boolean =
        parse(portalName).country != null || categoryTitle?.let(::countriesOfCategory).orEmpty().isNotEmpty()

    /** A portal name's meaningful words, and the country it names, if any. */
    data class ParsedName(val words: List<String>, val country: String?)

    companion object {
        private val brackets = Regex("[(\\[{][^)\\]}]*[)\\]}]")
        private val prefixTag = Regex("^\\W*([a-z]{2,3})\\s*[|:\\]\\-]+\\s*")
        private val nonWord = Regex("[^a-z0-9]+")
        private val marks = Regex("\\p{M}+")

        private val noise = setOf(
            "hd", "fhd", "uhd", "sd", "4k", "8k", "hevc", "h264", "h265", "hq", "lq", "backup", "bkp", "raw",
        )
        private val generic = setOf("tv", "channel", "network", "television")

        /** Words providers put in front of a channel's name, e.g. "PREMIUM STAR PLUS 4K". */
        private val providerLabels = setOf("premium", "vip")

        /** Tags in front of a name, e.g. "|IN|", "UK:"; values are iptv-org's codes (UK, not GB). */
        private val countryCodes = mapOf(
            "us" to "US", "usa" to "US", "uk" to "UK", "gb" to "UK", "in" to "IN", "ind" to "IN",
            "ca" to "CA", "can" to "CA", "pk" to "PK", "bd" to "BD", "lk" to "LK", "np" to "NP",
            "ae" to "AE", "uae" to "AE", "au" to "AU", "nz" to "NZ", "ie" to "IE", "za" to "ZA",
            "sg" to "SG", "my" to "MY", "ph" to "PH", "de" to "DE", "fr" to "FR", "es" to "ES",
            "it" to "IT", "nl" to "NL", "tr" to "TR",
        )

        /** Whole words at either end of a name that name a country. */
        private val countryWords = mapOf(
            "usa" to "US",
            "uk" to "UK",
            "india" to "IN",
            "canada" to "CA",
            "pakistan" to "PK",
            "uae" to "AE",
            "australia" to "AU",
            "bangladesh" to "BD",
        )

        /** Countries whose channels an "ENGLISH" category usually holds. */
        private val english = setOf("US", "UK", "CA", "AU", "IE", "NZ")

        /** Category words that point at a country, e.g. a "HINDI" or "TAMIL" category. */
        private val categoryWords = countryWords + mapOf(
            "hindi" to "IN", "tamil" to "IN", "telugu" to "IN", "malayalam" to "IN", "kannada" to "IN",
            "punjabi" to "IN", "marathi" to "IN", "bengali" to "IN", "bangla" to "IN", "gujarati" to "IN",
            "odia" to "IN", "oriya" to "IN", "indian" to "IN", "us" to "US", "american" to "US",
            "british" to "UK", "canadian" to "CA", "urdu" to "PK", "sinhala" to "LK", "nepali" to "NP",
        )

        fun parse(portalName: String): ParsedName {
            var text = fold(portalName)
            var country: String? = null
            prefixTag.find(text)?.let { tag ->
                countryCodes[tag.groupValues[1]]?.let {
                    country = it
                    text = text.substring(tag.range.last + 1)
                }
            }
            val words = words(text).dropWhile { it in providerLabels }.toMutableList()
            for (end in listOf(words.lastIndex, 0)) {
                val word = words.getOrNull(end) ?: continue
                if (words.size > 1 && word in countryWords) {
                    country = country ?: countryWords.getValue(word)
                    words.removeAt(end)
                }
            }
            return ParsedName(words, country)
        }

        /** The countries a category's name points at: a tag, a country or language word, or "English". */
        fun countriesOfCategory(title: String): Set<String> {
            val text = fold(title)
            val tagged = prefixTag.find(text)?.let { tag -> countryCodes[tag.groupValues[1]] }
            val words = text.split(nonWord)
            val named = words.mapNotNullTo(mutableSetOf()) { categoryWords[it] }
            return when {
                tagged != null -> setOf(tagged)
                named.isNotEmpty() -> named
                "english" in words -> english
                else -> emptySet()
            }
        }

        /** Lower case, without accents or bracketed parts. */
        private fun fold(text: String): String =
            Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
                .replace(marks, "")
                .replace(brackets, " ")

        private fun words(text: String): List<String> =
            fold(text).split(nonWord).filter { it.isNotEmpty() && it !in noise }

        /** The name squeezed together, then without a trailing "TV" or "Channel": "suntv", "sun". */
        private fun keysOf(words: List<String>): List<String> {
            if (words.isEmpty()) return emptyList()
            val full = words.joinToString("")
            val core = words.dropLastWhile { it in generic }.joinToString("")
            return listOf(full, core).filter { it.isNotEmpty() }.distinct()
        }
    }
}
