package io.github.gobinathm.portalistv.feature.player

import android.view.KeyEvent.KEYCODE_CHANNEL_DOWN
import android.view.KeyEvent.KEYCODE_CHANNEL_UP
import android.view.KeyEvent.KEYCODE_DPAD_CENTER
import android.view.KeyEvent.KEYCODE_DPAD_DOWN
import android.view.KeyEvent.KEYCODE_DPAD_LEFT
import android.view.KeyEvent.KEYCODE_DPAD_RIGHT
import android.view.KeyEvent.KEYCODE_DPAD_UP
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gobinathm.portalistv.core.player.PlaybackState
import kotlinx.coroutines.delay

private const val OVERLAY_TIMEOUT_MS = 5_000L
private const val SEEK_STEP_MS = 10_000L

/** Full-screen live playback. Up/Down zap, OK shows the controls, Left/Right seek when possible. */
@Composable
fun PlayerScreen(modifier: Modifier = Modifier, viewModel: PlayerViewModel = hiltViewModel()) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val playback by viewModel.player.state.collectAsStateWithLifecycle()
    var overlayVisible by remember { mutableStateOf(true) }
    // Bumped on every key press so the overlay's hide timer restarts.
    var interaction by remember { mutableIntStateOf(0) }
    val rootFocus = remember { FocusRequester() }

    KeepScreenOn()
    BackgroundPlayback(onBackground = viewModel::onBackground, onForeground = viewModel::onForeground)
    BackHandler(enabled = overlayVisible) { overlayVisible = false }
    LaunchedEffect(overlayVisible, interaction) {
        if (overlayVisible) {
            delay(OVERLAY_TIMEOUT_MS)
            overlayVisible = false
        }
    }
    // A new channel shows its title briefly.
    LaunchedEffect(ui.channel?.id) { if (ui.channel != null) overlayVisible = true }
    LaunchedEffect(overlayVisible) { if (!overlayVisible) runCatching { rootFocus.requestFocus() } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                interaction++
                if (overlayVisible || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                handleKey(event.key.nativeKeyCode, playback, viewModel) { overlayVisible = true }
            }
            .focusRequester(rootFocus)
            .focusable(),
    ) {
        viewModel.player.VideoSurface(Modifier.fillMaxSize())
        PlaybackStatus(ui, playback, Modifier.align(Alignment.Center))
        if (overlayVisible) {
            ControlsOverlay(
                ui = ui,
                playback = playback,
                player = viewModel.player,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/** Keys while the controls are hidden. Returns true when the key was used. */
private fun handleKey(
    code: Int,
    playback: PlaybackState,
    viewModel: PlayerViewModel,
    showOverlay: () -> Unit,
): Boolean {
    val player = viewModel.player
    when (code) {
        KEYCODE_DPAD_UP, KEYCODE_CHANNEL_UP -> viewModel.zap(+1)
        KEYCODE_DPAD_DOWN, KEYCODE_CHANNEL_DOWN -> viewModel.zap(-1)
        KEYCODE_DPAD_CENTER, KEYCODE_ENTER -> showOverlay()
        KEYCODE_DPAD_LEFT, KEYCODE_DPAD_RIGHT -> if (playback.seekable) {
            val step = if (code == KEYCODE_DPAD_RIGHT) SEEK_STEP_MS else -SEEK_STEP_MS
            player.seekTo(playback.positionMs + step)
        } else {
            showOverlay()
        }
        KEYCODE_MEDIA_PLAY_PAUSE ->
            if (playback.status == PlaybackState.Status.PAUSED) player.resume() else player.pause()
        else -> return false
    }
    return true
}

@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

/** Stops on leaving the app and re-tunes on return, since live links expire. */
@Composable
private fun BackgroundPlayback(onBackground: () -> Unit, onForeground: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        var stopped = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    stopped = true
                    onBackground()
                }
                Lifecycle.Event.ON_START -> if (stopped) {
                    stopped = false
                    onForeground()
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}
