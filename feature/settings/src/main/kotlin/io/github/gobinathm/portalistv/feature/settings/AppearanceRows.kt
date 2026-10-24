package io.github.gobinathm.portalistv.feature.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import io.github.gobinathm.portalistv.core.datastore.AccentColor
import io.github.gobinathm.portalistv.core.datastore.AppSettings
import io.github.gobinathm.portalistv.core.datastore.AppTheme
import io.github.gobinathm.portalistv.core.datastore.HomeRow
import io.github.gobinathm.portalistv.core.datastore.HomeRowSetting
import io.github.gobinathm.portalistv.core.datastore.moved
import io.github.gobinathm.portalistv.core.designsystem.component.ChoiceDialog
import io.github.gobinathm.portalistv.core.designsystem.component.ChoiceOption
import io.github.gobinathm.portalistv.core.designsystem.component.ChoiceRow
import io.github.gobinathm.portalistv.core.designsystem.component.SectionHeader
import io.github.gobinathm.portalistv.core.designsystem.component.ValueRow

internal fun LazyListScope.appearanceRows(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    firstRow: FocusRequester,
) {
    item { SectionHeader(stringResource(R.string.settings_section_appearance)) }
    item {
        ChoiceRow(
            label = stringResource(R.string.settings_theme),
            options = AppTheme.entries.map { ChoiceOption(it.name, stringResource(it.labelRes())) },
            selectedKey = settings.theme.name,
            onSelect = { key -> viewModel.update { it.copy(theme = AppTheme.valueOf(key)) } },
            modifier = Modifier.focusRequester(firstRow),
        )
    }
    item {
        ChoiceRow(
            label = stringResource(R.string.settings_accent),
            options = AccentColor.entries.map { ChoiceOption(it.name, stringResource(it.labelRes())) },
            selectedKey = settings.accent.name,
            onSelect = { key -> viewModel.update { it.copy(accent = AccentColor.valueOf(key)) } },
        )
    }
}

/** One entry per Home row; each opens a small menu to show, hide or move it. */
internal fun LazyListScope.homeRowRows(settings: AppSettings, viewModel: SettingsViewModel) {
    item { SectionHeader(stringResource(R.string.settings_section_home)) }
    settings.homeRows.forEach { setting ->
        item(key = "home-${setting.row}") {
            HomeRowItem(setting) { action ->
                viewModel.update { app -> app.copy(homeRows = action.applyTo(app.homeRows, setting.row)) }
            }
        }
    }
}

private enum class RowAction { SHOW, HIDE, UP, DOWN }

private fun RowAction.applyTo(rows: List<HomeRowSetting>, row: HomeRow): List<HomeRowSetting> = when (this) {
    RowAction.SHOW -> rows.map { if (it.row == row) it.copy(visible = true) else it }
    RowAction.HIDE -> rows.map { if (it.row == row) it.copy(visible = false) else it }
    RowAction.UP -> rows.moved(row, -1)
    RowAction.DOWN -> rows.moved(row, +1)
}

@Composable
private fun HomeRowItem(setting: HomeRowSetting, onAction: (RowAction) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = stringResource(setting.row.labelRes())
    ValueRow(
        label = label,
        value = stringResource(if (setting.visible) R.string.settings_row_shown else R.string.settings_row_hidden),
        onClick = { open = true },
    )
    if (open) {
        ChoiceDialog(
            title = label,
            options = listOf(
                ChoiceOption(RowAction.SHOW.name, stringResource(R.string.settings_row_show)),
                ChoiceOption(RowAction.HIDE.name, stringResource(R.string.settings_row_hide)),
                ChoiceOption(RowAction.UP.name, stringResource(R.string.settings_row_up)),
                ChoiceOption(RowAction.DOWN.name, stringResource(R.string.settings_row_down)),
            ),
            selectedKey = if (setting.visible) RowAction.SHOW.name else RowAction.HIDE.name,
            onSelect = { key ->
                onAction(RowAction.valueOf(key))
                open = false
            },
            onDismiss = { open = false },
        )
    }
}

private fun AppTheme.labelRes() = when (this) {
    AppTheme.DARK -> R.string.settings_theme_dark
    AppTheme.OLED_BLACK -> R.string.settings_theme_black
    AppTheme.DIM -> R.string.settings_theme_dim
}

private fun AccentColor.labelRes() = when (this) {
    AccentColor.AZURE -> R.string.settings_accent_azure
    AccentColor.CORAL -> R.string.settings_accent_coral
    AccentColor.MINT -> R.string.settings_accent_mint
    AccentColor.AMBER -> R.string.settings_accent_amber
    AccentColor.ORCHID -> R.string.settings_accent_orchid
}

private fun HomeRow.labelRes() = when (this) {
    HomeRow.CONTINUE_WATCHING -> R.string.settings_row_continue
    HomeRow.FAVORITES -> R.string.settings_row_favorites
    HomeRow.LIVE_TV -> R.string.settings_row_live
    HomeRow.ON_DEMAND -> R.string.settings_row_vod
}
