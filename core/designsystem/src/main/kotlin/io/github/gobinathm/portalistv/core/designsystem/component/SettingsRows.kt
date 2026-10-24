package io.github.gobinathm.portalistv.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier.padding(top = Spacing.l, bottom = Spacing.s),
    )
}

/** A setting that shows its current value and opens an editor when clicked. */
@Composable
fun ValueRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    ListItem(
        selected = false,
        onClick = onClick,
        modifier = modifier,
        headlineContent = { Text(label) },
        supportingContent = {
            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error)
            } else {
                Text(value, maxLines = 1)
            }
        },
    )
}

/** A setting toggled with OK. */
@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    ListItem(
        selected = false,
        onClick = { onCheckedChange(!checked) },
        modifier = modifier,
        headlineContent = { Text(label) },
        supportingContent = hint?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
    )
}

/** A one-shot action, such as generating a new identifier. */
@Composable
fun ActionRow(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, hint: String? = null) {
    ListItem(
        selected = false,
        onClick = onClick,
        modifier = modifier,
        headlineContent = { Text(label, color = MaterialTheme.colorScheme.primary) },
        supportingContent = hint?.let { { Text(it) } },
    )
}
