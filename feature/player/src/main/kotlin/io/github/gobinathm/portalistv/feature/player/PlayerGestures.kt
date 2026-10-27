package io.github.gobinathm.portalistv.feature.player

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.github.gobinathm.portalistv.core.player.PlaybackState
import kotlin.math.abs

/** How far a finger must travel for a swipe to count. */
private val SwipeDistance = 64.dp

/** Which way a finger swiped across the picture. */
internal enum class Swipe { UP, DOWN, LEFT, RIGHT }

/**
 * Touch on the picture, for tablets: a tap shows or hides the controls, a swipe does what the
 * matching D-pad key does. [onTouch] hears every touch on the screen, controls included, before
 * anything acts on it, so the controls stay up while they are being used.
 *
 * The gestures keep the callbacks they were first given (restarting them on every recomposition
 * would drop a swipe in progress), so the callbacks must read state objects rather than capture values.
 */
internal fun Modifier.playerGestures(
    onTouch: () -> Unit,
    onTap: () -> Unit,
    onSwipe: (Swipe) -> Unit,
): Modifier = this
    .pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                if (awaitPointerEvent(PointerEventPass.Initial).type == PointerEventType.Press) onTouch()
            }
        }
    }
    .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) }
    .pointerInput(Unit) {
        var travel = Offset.Zero
        detectDragGestures(
            onDragStart = { travel = Offset.Zero },
            onDrag = { change, amount ->
                change.consume()
                travel += amount
            },
            onDragEnd = { swipeOf(travel, SwipeDistance.toPx())?.let(onSwipe) },
        )
    }

/** The swipe [travel] makes, along whichever axis it mostly moved; null when it is too short. */
internal fun swipeOf(travel: Offset, minDistance: Float): Swipe? = when {
    maxOf(abs(travel.x), abs(travel.y)) < minDistance -> null
    abs(travel.y) >= abs(travel.x) -> if (travel.y < 0) Swipe.UP else Swipe.DOWN
    else -> if (travel.x < 0) Swipe.LEFT else Swipe.RIGHT
}

/**
 * A swipe does what the D-pad does: up and down change channel on live TV (up is the next one,
 * as on the remote), left and right move the seek target where the stream can seek.
 */
internal fun PlayerViewModel.handleSwipe(swipe: Swipe, playback: PlaybackState) {
    when (swipe) {
        Swipe.UP -> zap(+1)
        Swipe.DOWN -> zap(-1)
        Swipe.LEFT, Swipe.RIGHT -> if (playback.seekable) scrub(forward = swipe == Swipe.RIGHT, repeatCount = 0)
    }
}
