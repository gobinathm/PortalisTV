package io.github.gobinathm.portalistv.feature.profiles.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import io.github.gobinathm.portalistv.core.designsystem.component.DialogFrame
import io.github.gobinathm.portalistv.core.designsystem.component.tapToClick
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing
import io.github.gobinathm.portalistv.feature.profiles.R

@Composable
fun ConfirmDialog(message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val focus = remember { FocusRequester() }
    DialogFrame(onDismiss) {
        Text(message, style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            // Cancel is focused first so a stray OK press does nothing destructive.
            OutlinedButton(onClick = onDismiss, modifier = Modifier.focusRequester(focus).tapToClick(onDismiss)) {
                Text(stringResource(R.string.editor_cancel))
            }
            Button(onClick = onConfirm, modifier = Modifier.tapToClick(onConfirm)) { Text(confirmLabel) }
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
            Button(onClick = onDismiss, modifier = Modifier.focusRequester(focus).tapToClick(onDismiss)) {
                Text(stringResource(R.string.editor_ok))
            }
            LaunchedEffect(Unit) { focus.requestFocus() }
        }
    }
}
