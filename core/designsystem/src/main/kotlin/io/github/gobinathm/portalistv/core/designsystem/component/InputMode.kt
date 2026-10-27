package io.github.gobinathm.portalistv.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager

/**
 * Whether the app is being used by touch rather than a remote or keyboard, so hints can say "tap"
 * instead of "press OK". It follows the last input: a tablet starts in touch mode, a TV never is,
 * and a keyboard plugged into a tablet switches it back.
 */
@Composable
fun usingTouch(): Boolean = LocalInputModeManager.current.inputMode == InputMode.Touch
