package io.github.gobinathm.portalistv.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import io.github.gobinathm.portalistv.core.designsystem.theme.Radius
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing

private val DialogWidth = 640.dp
private val ChoiceListMaxHeight = 480.dp

data class ChoiceOption(val key: String, val label: String)

/** A centered panel for TV dialogs. */
@Composable
fun DialogFrame(onDismiss: () -> Unit, content: @Composable () -> Unit) {
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
        // Starts at the selection so it is composed and can take focus in long lists.
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
        LazyColumn(Modifier.heightIn(max = ChoiceListMaxHeight), state = listState) {
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

/** A short menu of [actions] for [title]; the first has focus. Back closes it. */
@Composable
fun ActionDialog(title: String, actions: List<ChoiceOption>, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val focus = remember { FocusRequester() }
    DialogFrame(onDismiss) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Column {
            actions.forEachIndexed { index, action ->
                ListItem(
                    selected = false,
                    onClick = { onSelect(action.key) },
                    headlineContent = { Text(action.label) },
                    modifier = if (index == 0) Modifier.focusRequester(focus) else Modifier,
                )
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}
