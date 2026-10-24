package io.github.gobinathm.portalistv.feature.player

/** How long after the last left/right press the player actually seeks. */
internal const val SCRUB_COMMIT_MS = 900L

private const val TAP_STEP_MS = 10_000L
private const val HOLD_STEP_MS = 30_000L
private const val LONG_HOLD_STEP_MS = 60_000L
private const val HOLD_REPEATS = 5
private const val LONG_HOLD_REPEATS = 15

/** Stops short of the very end, so seeking forward never ends the movie by accident. */
private const val END_MARGIN_MS = 5_000L

/**
 * The next scrub position from [fromMs]: 10 s per press, growing to 30 s and then a minute while
 * the button is held ([repeatCount] is the key's auto-repeat count), kept within the recording.
 */
internal fun scrubTarget(fromMs: Long, forward: Boolean, repeatCount: Int, durationMs: Long): Long {
    val step = when {
        repeatCount >= LONG_HOLD_REPEATS -> LONG_HOLD_STEP_MS
        repeatCount >= HOLD_REPEATS -> HOLD_STEP_MS
        else -> TAP_STEP_MS
    }
    val target = if (forward) fromMs + step else fromMs - step
    return target.coerceIn(0, (durationMs - END_MARGIN_MS).coerceAtLeast(0))
}
