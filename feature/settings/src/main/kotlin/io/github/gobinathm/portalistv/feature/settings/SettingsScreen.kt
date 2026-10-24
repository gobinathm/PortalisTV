package io.github.gobinathm.portalistv.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.gobinathm.portalistv.core.datastore.AppSettings
import io.github.gobinathm.portalistv.core.designsystem.component.ChoiceDialog
import io.github.gobinathm.portalistv.core.designsystem.component.ChoiceOption
import io.github.gobinathm.portalistv.core.designsystem.component.SectionHeader
import io.github.gobinathm.portalistv.core.designsystem.component.SwitchRow
import io.github.gobinathm.portalistv.core.designsystem.component.ValueRow
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing
import io.github.gobinathm.portalistv.core.player.AspectMode
import io.github.gobinathm.portalistv.core.player.Deinterlace
import io.github.gobinathm.portalistv.core.player.HardwareDecoding
import io.github.gobinathm.portalistv.core.player.PlayerSettings
import io.github.gobinathm.portalistv.core.player.labelRes

private enum class Picker { BUFFER, DECODING, DEINTERLACE, ASPECT, AUDIO_LANGUAGE, SUBTITLE_LANGUAGE }

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    settings?.let { SettingsContent(it, viewModel, modifier) }
}

@Composable
private fun SettingsContent(settings: AppSettings, viewModel: SettingsViewModel, modifier: Modifier) {
    val player = settings.player
    var picker by remember { mutableStateOf<Picker?>(null) }
    val firstRow = remember { FocusRequester() }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.screenVertical),
    ) {
        item { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.displayMedium) }
        item {
            Text(
                stringResource(R.string.settings_applies_next),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.s),
            )
        }
        playbackRows(player, firstRow, viewModel, onPick = { picker = it })
        backgroundRows(settings, viewModel)
    }
    LaunchedEffect(Unit) { runCatching { firstRow.requestFocus() } }
    picker?.let { current ->
        SettingPicker(current, player, onUpdate = viewModel::updatePlayer, onDismiss = { picker = null })
    }
}

private fun LazyListScope.playbackRows(
    player: PlayerSettings,
    firstRow: FocusRequester,
    viewModel: SettingsViewModel,
    onPick: (Picker) -> Unit,
) {
    item { SectionHeader(stringResource(R.string.settings_section_playback)) }
    item {
        ValueRow(
            label = stringResource(R.string.settings_buffer),
            value = bufferLabel(player.networkCachingMs),
            onClick = { onPick(Picker.BUFFER) },
            modifier = Modifier.focusRequester(firstRow),
        )
    }
    item {
        ValueRow(
            stringResource(R.string.settings_hw_decoding),
            stringResource(player.hardwareDecoding.labelRes()),
            onClick = { onPick(Picker.DECODING) },
        )
    }
    item {
        ValueRow(
            stringResource(R.string.settings_deinterlace),
            stringResource(player.deinterlace.labelRes()),
            onClick = { onPick(Picker.DEINTERLACE) },
        )
    }
    item {
        ValueRow(
            stringResource(R.string.settings_aspect),
            stringResource(player.defaultAspect.labelRes()),
            onClick = { onPick(Picker.ASPECT) },
        )
    }
    item {
        SwitchRow(
            label = stringResource(R.string.settings_time_stretch),
            checked = player.audioTimeStretch,
            onCheckedChange = { on -> viewModel.updatePlayer { it.copy(audioTimeStretch = on) } },
            hint = stringResource(R.string.settings_time_stretch_hint),
        )
    }
    item {
        ValueRow(
            stringResource(R.string.settings_audio_language),
            languageLabel(player.preferredAudioLanguage),
            onClick = { onPick(Picker.AUDIO_LANGUAGE) },
        )
    }
    item {
        ValueRow(
            stringResource(R.string.settings_subtitle_language),
            languageLabel(player.preferredSubtitleLanguage),
            onClick = { onPick(Picker.SUBTITLE_LANGUAGE) },
        )
    }
}

private fun LazyListScope.backgroundRows(settings: AppSettings, viewModel: SettingsViewModel) {
    item { SectionHeader(stringResource(R.string.settings_section_background)) }
    item {
        SwitchRow(
            label = stringResource(R.string.settings_pip),
            checked = settings.pictureInPicture,
            onCheckedChange = viewModel::setPictureInPicture,
            hint = stringResource(R.string.settings_pip_hint),
        )
    }
}

@Composable
private fun SettingPicker(
    picker: Picker,
    player: PlayerSettings,
    onUpdate: ((PlayerSettings) -> PlayerSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val (title, options, selected) = when (picker) {
        Picker.BUFFER -> Triple(
            R.string.settings_buffer,
            NetworkCachingChoicesMs.map { ChoiceOption(it.toString(), bufferLabel(it)) },
            player.networkCachingMs.toString(),
        )
        Picker.DECODING -> Triple(
            R.string.settings_hw_decoding,
            HardwareDecoding.entries.map { ChoiceOption(it.name, stringResource(it.labelRes())) },
            player.hardwareDecoding.name,
        )
        Picker.DEINTERLACE -> Triple(
            R.string.settings_deinterlace,
            Deinterlace.entries.map { ChoiceOption(it.name, stringResource(it.labelRes())) },
            player.deinterlace.name,
        )
        Picker.ASPECT -> Triple(
            R.string.settings_aspect,
            AspectMode.entries.map { ChoiceOption(it.name, stringResource(it.labelRes())) },
            player.defaultAspect.name,
        )
        Picker.AUDIO_LANGUAGE -> Triple(
            R.string.settings_audio_language,
            languageOptions(),
            player.preferredAudioLanguage ?: STREAM_DEFAULT,
        )
        Picker.SUBTITLE_LANGUAGE -> Triple(
            R.string.settings_subtitle_language,
            languageOptions(),
            player.preferredSubtitleLanguage ?: STREAM_DEFAULT,
        )
    }
    ChoiceDialog(
        title = stringResource(title),
        options = options,
        selectedKey = selected,
        onSelect = { key ->
            onUpdate { it.apply(picker, key) }
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}

private fun PlayerSettings.apply(picker: Picker, key: String): PlayerSettings = when (picker) {
    Picker.BUFFER -> copy(networkCachingMs = key.toInt())
    Picker.DECODING -> copy(hardwareDecoding = HardwareDecoding.valueOf(key))
    Picker.DEINTERLACE -> copy(deinterlace = Deinterlace.valueOf(key))
    Picker.ASPECT -> copy(defaultAspect = AspectMode.valueOf(key))
    Picker.AUDIO_LANGUAGE -> copy(preferredAudioLanguage = key.ifEmpty { null })
    Picker.SUBTITLE_LANGUAGE -> copy(preferredSubtitleLanguage = key.ifEmpty { null })
}
