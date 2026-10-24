package io.github.gobinathm.portalistv.core.designsystem.component

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import io.github.gobinathm.portalistv.core.designsystem.R
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing

private val DotSize = 16.dp
private val KeySize = 56.dp
private const val ERASE = -1
private val PadRows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9), listOf(ERASE, 0))

/**
 * Asks for a [length]-digit PIN. TV remotes often have no number keys, so it has its own keypad;
 * number keys work too. [onSubmit] gets the PIN once all digits are in, and the entry clears for a
 * retry. [error] is shown under the dots, e.g. after a wrong PIN.
 */
@Composable
fun PinDialog(
    title: String,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
    message: String? = null,
    error: String? = null,
    length: Int = 4,
) {
    var digits by remember { mutableStateOf("") }
    val firstKey = remember { FocusRequester() }
    fun press(key: Int) {
        digits = if (key == ERASE) digits.dropLast(1) else (digits + key).take(length)
        if (digits.length == length) {
            onSubmit(digits)
            digits = ""
        }
    }
    DialogFrame(onDismiss) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        message?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            repeat(length) { index ->
                val filled = index < digits.length
                Box(
                    Modifier
                        .size(DotSize)
                        .background(
                            if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape,
                        ),
                )
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        PadRows.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                modifier = Modifier.onPreviewKeyEvent { event ->
                    val digit = digitOf(event.nativeKeyEvent.keyCode)
                    if (digit != null && event.type == KeyEventType.KeyDown) press(digit)
                    digit != null
                },
            ) {
                row.forEachIndexed { columnIndex, key ->
                    val first = rowIndex == 0 && columnIndex == 0
                    PadKey(key, onClick = { press(key) }, if (first) Modifier.focusRequester(firstKey) else Modifier)
                }
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { firstKey.requestFocus() } }
}

@Composable
private fun PadKey(key: Int, onClick: () -> Unit, modifier: Modifier) {
    val label = if (key == ERASE) "⌫" else key.toString()
    if (key == ERASE) {
        OutlinedButton(onClick = onClick, modifier = modifier.size(KeySize)) {
            Box(contentAlignment = Alignment.Center) { Text(label) }
        }
    } else {
        Button(onClick = onClick, modifier = modifier.size(KeySize)) {
            Box(contentAlignment = Alignment.Center) { Text(label, style = MaterialTheme.typography.titleLarge) }
        }
    }
}

private fun digitOf(code: Int): Int? = when (code) {
    in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> code - KeyEvent.KEYCODE_0
    in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> code - KeyEvent.KEYCODE_NUMPAD_0
    else -> null
}

/** "Too many tries. Try again in n seconds." for a given n, for [PinDialog]'s error line. */
@Composable
fun pausedMessage(): (Int) -> String {
    val resources = LocalResources.current
    return { seconds -> resources.getQuantityString(R.plurals.pin_paused, seconds, seconds) }
}
