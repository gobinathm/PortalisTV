package io.github.gobinathm.portalistv.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import io.github.gobinathm.portalistv.core.designsystem.component.ChoiceOption
import io.github.gobinathm.portalistv.core.player.Deinterlace
import io.github.gobinathm.portalistv.core.player.HardwareDecoding
import io.github.gobinathm.portalistv.core.player.PlayerSettings

/** Key for "no preference" in the language pickers. */
internal const val STREAM_DEFAULT = ""

@Composable
internal fun languageOptions(): List<ChoiceOption> {
    val streamDefault = stringResource(R.string.settings_language_default)
    return remember(streamDefault) {
        listOf(ChoiceOption(STREAM_DEFAULT, streamDefault)) + languageChoices().map { ChoiceOption(it.code, it.name) }
    }
}

@Composable
internal fun languageLabel(code: String?): String =
    code?.let { languageName(it) } ?: stringResource(R.string.settings_language_default)

@Composable
internal fun bufferLabel(ms: Int): String {
    val isDefault = ms == PlayerSettings.DEFAULT_NETWORK_CACHING_MS
    val label = if (isDefault) R.string.settings_seconds_default else R.string.settings_seconds
    return stringResource(label, formatSeconds(ms))
}

internal fun HardwareDecoding.labelRes() = when (this) {
    HardwareDecoding.AUTO -> R.string.settings_hw_auto
    HardwareDecoding.ON -> R.string.settings_hw_on
    HardwareDecoding.OFF -> R.string.settings_hw_off
}

internal fun Deinterlace.labelRes() = when (this) {
    Deinterlace.AUTO -> R.string.settings_deinterlace_auto
    Deinterlace.ON -> R.string.settings_deinterlace_on
    Deinterlace.OFF -> R.string.settings_deinterlace_off
}
