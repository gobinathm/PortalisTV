package io.github.gobinathm.portalistv.feature.vod

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@Composable
internal fun Hint(text: String) = Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
internal fun Muted(text: String) = Text(
    text,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
)

/** 65 000 ms reads "1:05", an hour or more "1:02:05". */
internal fun formatTime(ms: Long): String {
    val seconds = ms / MILLIS
    val hours = seconds / SECONDS_PER_HOUR
    val minutes = seconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val rest = seconds % SECONDS_PER_MINUTE
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, rest) else "%d:%02d".format(minutes, rest)
}

private const val MILLIS = 1000L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3600L
