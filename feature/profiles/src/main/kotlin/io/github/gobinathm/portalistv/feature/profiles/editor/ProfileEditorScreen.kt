package io.github.gobinathm.portalistv.feature.profiles.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import io.github.gobinathm.portalistv.core.designsystem.component.ChoiceDialog
import io.github.gobinathm.portalistv.core.designsystem.component.ChoiceOption
import io.github.gobinathm.portalistv.core.designsystem.component.backdrop
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing
import io.github.gobinathm.portalistv.feature.profiles.R
import io.github.gobinathm.portalistv.feature.profiles.component.ConfirmDialog
import io.github.gobinathm.portalistv.feature.profiles.component.TextInputDialog

/** The form is a single column of rows, sized for reading from a couch. */
private val FormWidth = 760.dp

internal sealed interface EditorDialog {
    data class TextInput(
        val title: String,
        val initial: String,
        val onConfirm: (String) -> Unit,
        val secret: Boolean = false,
        val keyboardType: KeyboardType = KeyboardType.Text,
        val hint: String? = null,
    ) : EditorDialog

    data class Choice(
        val title: String,
        val options: List<ChoiceOption>,
        val selectedKey: String?,
        val onSelect: (String) -> Unit,
    ) : EditorDialog

    data object ConfirmDelete : EditorDialog
}

@Composable
fun ProfileEditorScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<EditorDialog?>(null) }
    val firstField = remember { FocusRequester() }

    LaunchedEffect(state.finished) { if (state.finished) onDone() }
    val draft = state.draft ?: return

    val editor = EditorController(
        draft = draft,
        catalog = viewModel.catalog,
        errors = if (state.showErrors) state.errors else emptySet(),
        viewModel = viewModel,
        open = { dialog = it },
    )
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .backdrop(MaterialTheme.colorScheme),
        contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.screenVertical),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "header") {
            EditorHeader(state, onSave = viewModel::save, onDelete = { dialog = EditorDialog.ConfirmDelete })
        }
        editorSections(editor, firstField, Modifier.width(FormWidth))
    }
    LaunchedEffect(Unit) { firstField.requestFocus() }

    EditorDialogHost(
        dialog = dialog,
        onDelete = viewModel::delete,
        onDismiss = { dialog = null },
    )
}

@Composable
private fun EditorHeader(state: EditorUiState, onSave: () -> Unit, onDelete: () -> Unit) {
    Column(Modifier.width(FormWidth), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Text(
            stringResource(if (state.isNew) R.string.editor_title_new else R.string.editor_title_edit),
            style = MaterialTheme.typography.displayMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Button(onClick = onSave, enabled = !state.busy) { Text(stringResource(R.string.editor_save)) }
            if (!state.isNew) {
                OutlinedButton(onClick = onDelete, enabled = !state.busy) {
                    Text(stringResource(R.string.editor_delete))
                }
            }
        }
        val message = when {
            state.saveFailed -> R.string.editor_save_failed
            state.showErrors && state.errors.isNotEmpty() -> R.string.editor_fix_errors
            else -> null
        }
        message?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun EditorDialogHost(dialog: EditorDialog?, onDelete: () -> Unit, onDismiss: () -> Unit) {
    when (dialog) {
        null -> Unit
        is EditorDialog.TextInput -> TextInputDialog(
            title = dialog.title,
            initial = dialog.initial,
            onConfirm = {
                dialog.onConfirm(it)
                onDismiss()
            },
            onDismiss = onDismiss,
            secret = dialog.secret,
            keyboardType = dialog.keyboardType,
            hint = dialog.hint,
        )
        is EditorDialog.Choice -> ChoiceDialog(
            title = dialog.title,
            options = dialog.options,
            selectedKey = dialog.selectedKey,
            onSelect = {
                dialog.onSelect(it)
                onDismiss()
            },
            onDismiss = onDismiss,
        )
        EditorDialog.ConfirmDelete -> ConfirmDialog(
            message = stringResource(R.string.editor_delete_confirm),
            confirmLabel = stringResource(R.string.editor_delete),
            onConfirm = {
                onDismiss()
                onDelete()
            },
            onDismiss = onDismiss,
        )
    }
}
