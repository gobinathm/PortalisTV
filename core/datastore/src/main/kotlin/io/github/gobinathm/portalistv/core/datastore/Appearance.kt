package io.github.gobinathm.portalistv.core.datastore

/** The app's base colors. The design system maps these to its own palettes. */
enum class AppTheme { DARK, OLED_BLACK, DIM }

/** The one saturated color in the UI. */
enum class AccentColor { AZURE, CORAL, MINT, AMBER, ORCHID }

/** The content rows on Home, which can be reordered and hidden. */
enum class HomeRow { CONTINUE_WATCHING, FAVORITES, LIVE_TV, ON_DEMAND }

data class HomeRowSetting(val row: HomeRow, val visible: Boolean = true)

/** Every row once, in [rows]' order, with rows missing from it (e.g. added in an update) shown at the end. */
fun normalizedHomeRows(rows: List<HomeRowSetting>): List<HomeRowSetting> {
    val known = rows.distinctBy { it.row }
    return known + HomeRow.entries.filter { row -> known.none { it.row == row } }.map { HomeRowSetting(it) }
}

/** Moves [row] one place up (-1) or down (+1); no change at either end. */
fun List<HomeRowSetting>.moved(row: HomeRow, step: Int): List<HomeRowSetting> {
    val index = indexOfFirst { it.row == row }
    val target = index + step
    if (index < 0 || target !in indices) return this
    return toMutableList().apply { add(target, removeAt(index)) }
}
