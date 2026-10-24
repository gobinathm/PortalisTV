package io.github.gobinathm.portalistv.feature.settings

import java.math.BigDecimal
import java.util.Locale

/** Buffer sizes offered for live TV; more is smoother on weak networks but slower to change channel. */
internal val NetworkCachingChoicesMs = listOf(300, 500, 1000, 1500, 3000, 5000, 10_000)

/** Milliseconds are thousandths of a second. */
private const val MILLIS_DIGITS = 3

/** "1.5" for 1500 ms. */
internal fun formatSeconds(ms: Int): String =
    BigDecimal(ms).movePointLeft(MILLIS_DIGITS).stripTrailingZeros().toPlainString()

/** A language the player can prefer, by its ISO 639-1 code. */
internal data class LanguageChoice(val code: String, val name: String)

/** Every language the device knows, named in the device's language and sorted by name. */
internal fun languageChoices(display: Locale = Locale.getDefault()): List<LanguageChoice> =
    Locale.getAvailableLocales()
        .map { it.language }
        .filter { it.length == 2 }
        .distinct()
        .map { code -> LanguageChoice(code, Locale.forLanguageTag(code).getDisplayLanguage(display)) }
        .filter { it.name.isNotBlank() && it.name != it.code }
        .sortedBy { it.name.lowercase(display) }

internal fun languageName(code: String, display: Locale = Locale.getDefault()): String =
    Locale.forLanguageTag(code).getDisplayLanguage(display).ifBlank { code }
