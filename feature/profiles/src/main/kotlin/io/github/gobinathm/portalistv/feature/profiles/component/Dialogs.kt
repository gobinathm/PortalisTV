package io.github.gobinathm.portalistv.feature.profiles.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import io.github.gobinathm.portalistv.core.designsystem.theme.Radius
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing
import io.github.gobinathm.portalistv.feature.profiles.R

private val DialogWidth = 640.dp
private val ChoiceListMaxHeight = 480.dp

data class ChoiceOption(val key: String, val label: String)

@Composable
private fun DialogFrame(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(DialogWidth),
            shape = RoundedCornerShape(Radius.card),
            colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                content()
            }
        }
    }
}

/** Edits one text value. The on-screen keyboard opens right away; Done or OK confirms. */
@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    secret: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    hint: String? = null,
) {
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    DialogFrame(onDismiss) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        hint?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (secret) KeyboardType.Password else keyboardType,
                imeAction = ImeAction.Done,
                autoCorrectEnabled = false,
            ),
            keyboardActions = KeyboardActions(onDone = { onConfirm(value.text) }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(Radius.card))
                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(Radius.card))
                .padding(Spacing.m),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Button(onClick = { onConfirm(value.text) }) { Text(stringResource(R.string.editor_ok)) }
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.editor_cancel)) }
        }
    }
    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }
}

/** Picks one of [options]; the selected one has focus when the dialog opens. */
@Composable
fun ChoiceDialog(
    title: String,
    options: List<ChoiceOption>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedIndex = options.indexOfFirst { it.key == selectedKey }.coerceAtLeast(0)
    val focus = remember { FocusRequester() }
    DialogFrame(onDismiss) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        LazyColumn(Modifier.heightIn(max = ChoiceListMaxHeight)) {
            itemsIndexed(options, key = { _, option -> option.key }) { index, option ->
                ListItem(
                    selected = option.key == selectedKey,
                    onClick = { onSelect(option.key) },
                    headlineContent = { Text(option.label) },
                    leadingContent = { RadioButton(selected = option.key == selectedKey, onClick = null) },
                    modifier = if (index == selectedIndex) Modifier.focusRequester(focus) else Modifier,
                )
            }
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}

@Composable
fun ConfirmDialog(message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val focus = remember { FocusRequester() }
    DialogFrame(onDismiss) {
        Text(message, style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            // Cancel is focused first so a stray OK press does nothing destructive.
            OutlinedButton(onClick = onDismiss, modifier = Modifier.focusRequester(focus)) {
                Text(stringResource(R.string.editor_cancel))
            }
            Button(onClick = onConfirm) { Text(confirmLabel) }
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}

/** Shows a result or a progress note. Without [onDismiss] it has no button and cannot be closed. */
@Composable
fun MessageDialog(message: String, onDismiss: (() -> Unit)?) {
    val focus = remember { FocusRequester() }
    DialogFrame(onDismiss = onDismiss ?: {}) {
        Text(message, style = MaterialTheme.typography.titleLarge)
        if (onDismiss != null) {
            Button(onClick = onDismiss, modifier = Modifier.focusRequester(focus)) {
                Text(stringResource(R.string.editor_ok))
            }
            LaunchedEffect(Unit) { focus.requestFocus() }
        }
    }
}
