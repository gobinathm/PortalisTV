package io.github.gobinathm.portalistv.core.designsystem.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequesterModifierNode
import androidx.compose.ui.focus.requestFocus
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.unit.IntSize

/**
 * Makes a TV Material component work by touch. Its clickable `Card`, `Surface`, `Button` and
 * `ListItem` answer only the remote's OK key, so on a tablet nothing happens when they are tapped.
 * Pass the same actions as the component's `onClick` and `onLongClick`; a long press without
 * [onLongClick] counts as a tap. A drag is not a tap, so lists still scroll by touch. A tap also
 * moves focus to the component, so its highlight follows the finger rather than staying where
 * the remote left it.
 */
fun Modifier.tapToClick(onClick: () -> Unit, onLongClick: (() -> Unit)? = null): Modifier =
    this then TapToClickElement(onClick, onLongClick)

private data class TapToClickElement(
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)?,
) : ModifierNodeElement<TapToClickNode>() {
    override fun create() = TapToClickNode(onClick, onLongClick)

    override fun update(node: TapToClickNode) {
        // The gesture reads these when it fires, so new lambdas take effect without restarting it.
        node.onClick = onClick
        node.onLongClick = onLongClick
    }
}

private class TapToClickNode(var onClick: () -> Unit, var onLongClick: (() -> Unit)?) :
    DelegatingNode(),
    PointerInputModifierNode,
    FocusRequesterModifierNode {
    private val input = delegate(
        SuspendingPointerInputModifierNode {
            detectTapGestures(
                onTap = {
                    requestFocus()
                    onClick()
                },
                onLongPress = {
                    requestFocus()
                    (onLongClick ?: onClick)()
                },
            )
        },
    )

    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) =
        input.onPointerEvent(pointerEvent, pass, bounds)

    override fun onCancelPointerInput() = input.onCancelPointerInput()
}
