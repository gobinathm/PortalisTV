package io.github.gobinathm.portalistv.core.designsystem.component

/**
 * A language as its own speakers write it: [native] for tiles, [short] (a letter or two) where
 * there is room for little more than an initial.
 */
data class Language(val native: String, val short: String)

/**
 * Languages portals name categories after, by the English words they use for them. Only whole
 * words count, so "ARABIA" or "THAILAND" don't match.
 */
private val Languages: Map<String, Language> = buildMap {
    fun add(native: String, short: String, vararg words: String) = words.forEach { put(it, Language(native, short)) }
    // South Asia
    add("हिन्दी", "हि", "hindi")
    add("தமிழ்", "த", "tamil")
    add("తెలుగు", "తె", "telugu")
    add("മലയാളം", "മ", "malayalam")
    add("ಕನ್ನಡ", "ಕ", "kannada")
    add("বাংলা", "বা", "bengali", "bangla")
    add("मराठी", "म", "marathi")
    add("ગુજરાતી", "ગુ", "gujarati")
    add("ਪੰਜਾਬੀ", "ਪੰ", "punjabi")
    add("ଓଡ଼ିଆ", "ଓ", "odia", "oriya")
    add("සිංහල", "සි", "sinhala", "sinhalese")
    add("नेपाली", "ने", "nepali")
    add("اردو", "ار", "urdu")
    // Middle East and Asia
    add("العربية", "ع", "arabic")
    add("فارسی", "ف", "persian", "farsi")
    add("עברית", "ע", "hebrew")
    add("中文", "中", "chinese", "mandarin", "cantonese")
    add("日本語", "日", "japanese")
    add("한국어", "한", "korean")
    add("ไทย", "ไท", "thai")
    add("Tiếng Việt", "Vi", "vietnamese")
    // Europe
    add("Русский", "Ру", "russian")
    add("Українська", "Ук", "ukrainian")
    add("Ελληνικά", "Ελ", "greek")
    add("Türkçe", "Tr", "turkish")
    add("Français", "Fr", "french")
    add("Español", "Es", "spanish")
    add("Deutsch", "De", "german")
    add("Italiano", "It", "italian")
    add("Português", "Pt", "portuguese")
    add("Polski", "Pl", "polish")
    add("Nederlands", "Nl", "dutch")
    add("Română", "Ro", "romanian")
}

/** The first language named among [words] (lowercase), if any. */
internal fun languageIn(words: List<String>): Language? = words.firstNotNullOfOrNull { Languages[it] }
