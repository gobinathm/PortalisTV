package io.github.gobinathm.portalistv.feature.settings

import android.view.KeyEvent
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.gobinathm.portalistv.core.datastore.PlayerAction
import io.github.gobinathm.portalistv.core.datastore.RemoteKeys
import io.github.gobinathm.portalistv.core.designsystem.component.ActionRow
import io.github.gobinathm.portalistv.core.designsystem.component.ChoiceDialog
import io.github.gobinathm.portalistv.core.designsystem.component.ChoiceOption
import io.github.gobinathm.portalistv.core.designsystem.component.DialogFrame
import io.github.gobinathm.portalistv.core.designsystem.component.SectionHeader
import io.github.gobinathm.portalistv.core.designsystem.component.ValueRow
import java.util.Locale

/** Choosing "remove" in a button's menu. */
private const val REMOVE = "remove"

/** Remote buttons and what they do during playback; buttons can be added, changed and removed. */
internal fun LazyListScope.remoteKeyRows(bindings: Map<Int, PlayerAction>, viewModel: SettingsViewModel) {
    item { SectionHeader(stringResource(R.string.settings_section_keys)) }
    bindings.entries.sortedBy { it.key }.forEach { (keyCode, action) ->
        item(key = "key-$keyCode") {
            BindingRow(keyCode, action) { newAction ->
                viewModel.update { app ->
                    val updated = app.keyBindings - keyCode
                    app.copy(keyBindings = if (newAction == null) updated else updated + (keyCode to newAction))
                }
            }
        }
    }
    item(key = "key-add") {
        AddBindingRow { keyCode, action ->
            viewModel.update { it.copy(keyBindings = it.keyBindings + (keyCode to action)) }
        }
    }
    item(key = "key-reset") {
        ActionRow(
            label = stringResource(R.string.settings_keys_reset),
            onClick = { viewModel.update { it.copy(keyBindings = RemoteKeys.DEFAULT) } },
        )
    }
}

@Composable
private fun BindingRow(keyCode: Int, action: PlayerAction, onChange: (PlayerAction?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ValueRow(label = keyName(keyCode), value = stringResource(action.labelRes()), onClick = { open = true })
    if (open) {
        ChoiceDialog(
            title = keyName(keyCode),
            options = actionOptions() + ChoiceOption(REMOVE, stringResource(R.string.settings_keys_remove)),
            selectedKey = action.name,
            onSelect = { key ->
                onChange(PlayerAction.entries.firstOrNull { it.name == key })
                open = false
            },
            onDismiss = { open = false },
        )
    }
}

/** "Add a button": waits for a press on the remote, then asks what it should do. */
@Composable
private fun AddBindingRow(onAdd: (Int, PlayerAction) -> Unit) {
    var capturing by remember { mutableStateOf(false) }
    var captured by remember { mutableStateOf<Int?>(null) }
    ActionRow(
        label = stringResource(R.string.settings_keys_add),
        onClick = { capturing = true },
        hint = stringResource(R.string.settings_keys_add_hint),
    )
    if (capturing) {
        KeyCaptureDialog(
            onKey = { keyCode ->
                capturing = false
                captured = keyCode
            },
            onDismiss = { capturing = false },
        )
    }
    captured?.let { keyCode ->
        ChoiceDialog(
            title = keyName(keyCode),
            options = actionOptions(),
            selectedKey = null,
            onSelect = { key ->
                onAdd(keyCode, PlayerAction.valueOf(key))
                captured = null
            },
            onDismiss = { captured = null },
        )
    }
}

@Composable
private fun KeyCaptureDialog(onKey: (Int) -> Unit, onDismiss: () -> Unit) {
    val focus = remember { FocusRequester() }
    var reserved by remember { mutableStateOf(false) }
    DialogFrame(onDismiss) {
        Text(stringResource(R.string.settings_keys_press), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(if (reserved) R.string.settings_keys_reserved else R.string.settings_keys_press_hint),
            color = if (reserved) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .focusRequester(focus)
                .focusable()
                .onPreviewKeyEvent { event ->
                    val code = event.nativeKeyEvent.keyCode
                    when {
                        code == KeyEvent.KEYCODE_BACK -> false
                        event.type != KeyEventType.KeyDown -> true
                        RemoteKeys.isMappable(code) -> true.also { onKey(code) }
                        else -> true.also { reserved = true }
                    }
                },
        )
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

@Composable
private fun actionOptions(): List<ChoiceOption> =
    PlayerAction.entries.map { ChoiceOption(it.name, stringResource(it.labelRes())) }

/** A readable name for a button, e.g. "Red" or "Channel up". */
@Composable
private fun keyName(keyCode: Int): String = when (keyCode) {
    KeyEvent.KEYCODE_PROG_RED -> stringResource(R.string.settings_key_red)
    KeyEvent.KEYCODE_PROG_GREEN -> stringResource(R.string.settings_key_green)
    KeyEvent.KEYCODE_PROG_YELLOW -> stringResource(R.string.settings_key_yellow)
    KeyEvent.KEYCODE_PROG_BLUE -> stringResource(R.string.settings_key_blue)
    else -> KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_").lowercase(Locale.ROOT).replace('_', ' ')
        .replaceFirstChar { it.titlecase(Locale.ROOT) }
}

private fun PlayerAction.labelRes() = when (this) {
    PlayerAction.NEXT_CHANNEL -> R.string.settings_action_next
    PlayerAction.PREVIOUS_CHANNEL -> R.string.settings_action_previous
    PlayerAction.LAST_CHANNEL -> R.string.settings_action_last
    PlayerAction.SHOW_CONTROLS -> R.string.settings_action_controls
    PlayerAction.PLAY_PAUSE -> R.string.settings_action_play_pause
    PlayerAction.AUDIO_SUBTITLES -> R.string.settings_action_tracks
    PlayerAction.PICTURE_SIZE -> R.string.settings_action_picture
    PlayerAction.TOGGLE_FAVORITE -> R.string.settings_action_favorite
}
