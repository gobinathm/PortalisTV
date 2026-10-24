package io.github.gobinathm.portalistv.feature.profiles.editor

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import io.github.gobinathm.portalistv.core.catalog.DeviceCatalog
import io.github.gobinathm.portalistv.core.data.ProfileError
import io.github.gobinathm.portalistv.core.designsystem.component.ChoiceOption
import io.github.gobinathm.portalistv.core.model.Profile
import io.github.gobinathm.portalistv.feature.profiles.R
import io.github.gobinathm.portalistv.feature.profiles.component.ValueRow

internal const val CUSTOM_KEY = "custom"
internal const val DEFAULT_KEY = "default"

/** What the sections need: the draft, where to send changes, and how to open dialogs. */
internal class EditorController(
    val draft: Profile,
    val catalog: DeviceCatalog,
    /** Errors to show; empty until the user tries to save. */
    val errors: Set<ProfileError>,
    val viewModel: ProfileEditorViewModel,
    val open: (EditorDialog) -> Unit,
) {
    fun update(transform: (Profile) -> Profile) = viewModel.update(transform)
}

/** A row that edits a text value in a dialog. */
@Composable
internal fun TextRow(
    editor: EditorController,
    @StringRes label: Int,
    value: String?,
    set: (Profile, String) -> Profile,
    modifier: Modifier = Modifier,
    secret: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    @StringRes hint: Int? = null,
    @StringRes emptyText: Int = R.string.editor_not_set,
    error: String? = null,
) {
    val title = stringResource(label)
    val hintText = hint?.let { stringResource(it) }
    val shown = when {
        value.isNullOrEmpty() -> stringResource(emptyText)
        secret -> stringResource(R.string.editor_hidden)
        else -> value
    }
    ValueRow(
        label = title,
        value = shown,
        error = error,
        modifier = modifier,
        onClick = {
            editor.open(
                EditorDialog.TextInput(
                    title = title,
                    initial = value.orEmpty(),
                    onConfirm = { text -> editor.update { set(it, text) } },
                    secret = secret,
                    keyboardType = keyboardType,
                    hint = hintText,
                ),
            )
        },
    )
}

/** A row that picks one option in a dialog. */
@Composable
internal fun ChoiceRow(
    editor: EditorController,
    @StringRes label: Int,
    options: List<ChoiceOption>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(label)
    ValueRow(
        label = title,
        value = options.firstOrNull { it.key == selectedKey }?.label ?: selectedKey.orEmpty(),
        modifier = modifier,
        onClick = { editor.open(EditorDialog.Choice(title, options, selectedKey, onSelect)) },
    )
}

@Composable
internal fun errorText(editor: EditorController, vararg candidates: Pair<ProfileError, Int>): String? =
    candidates.firstOrNull { it.first in editor.errors }?.let { stringResource(it.second) }

internal fun String.orNull() = trim().ifEmpty { null }
