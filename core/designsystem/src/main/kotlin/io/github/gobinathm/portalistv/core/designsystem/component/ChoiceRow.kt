package io.github.gobinathm.portalistv.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** A setting that shows its value and, when clicked, a list to choose a new one from. */
@Composable
fun ChoiceRow(
    label: String,
    options: List<ChoiceOption>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    ValueRow(
        label = label,
        value = options.firstOrNull { it.key == selectedKey }?.label.orEmpty(),
        onClick = { open = true },
        modifier = modifier,
    )
    if (open) {
        ChoiceDialog(
            title = label,
            options = options,
            selectedKey = selectedKey,
            onSelect = { key ->
                onSelect(key)
                open = false
            },
            onDismiss = { open = false },
        )
    }
}
