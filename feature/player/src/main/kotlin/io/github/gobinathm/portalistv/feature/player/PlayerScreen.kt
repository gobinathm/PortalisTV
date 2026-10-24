package io.github.gobinathm.portalistv.feature.player

import android.view.KeyEvent.KEYCODE_0
import android.view.KeyEvent.KEYCODE_9
import android.view.KeyEvent.KEYCODE_CHANNEL_DOWN
import android.view.KeyEvent.KEYCODE_CHANNEL_UP
import android.view.KeyEvent.KEYCODE_DPAD_CENTER
import android.view.KeyEvent.KEYCODE_DPAD_DOWN
import android.view.KeyEvent.KEYCODE_DPAD_LEFT
import android.view.KeyEvent.KEYCODE_DPAD_RIGHT
import android.view.KeyEvent.KEYCODE_DPAD_UP
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
import android.view.KeyEvent.KEYCODE_NUMPAD_0
import android.view.KeyEvent.KEYCODE_NUMPAD_9
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing
import io.github.gobinathm.portalistv.core.player.PlaybackState
import io.github.gobinathm.portalistv.core.player.Player
import kotlinx.coroutines.delay

private const val OVERLAY_TIMEOUT_MS = 5_000L
private const val SEEK_STEP_MS = 10_000L

/**
 * Full-screen live playback. Up/Down zap, OK shows the controls, Left/Right seek when possible.
 * Profiles set to an external player hand the stream to that app and leave through [onExit].
 */
@Composable
fun PlayerScreen(onExit: () -> Unit, modifier: Modifier = Modifier, viewModel: PlayerViewModel = hiltViewModel()) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val player by viewModel.player.collectAsStateWithLifecycle()
    val context = LocalContext.current
    ui.externalStream?.let { media ->
        LaunchedEffect(media) {
            val found = launchExternalPlayer(context, media)
            viewModel.onExternalLaunched(found)
            if (found) onExit()
        }
    }
    val current = player
    if (current == null) {
        Box(modifier.fillMaxSize().background(Color.Black)) {
            PlaybackStatus(ui, PlaybackState(), Modifier.align(Alignment.Center))
        }
        return
    }
    InternalPlayback(ui, current, viewModel, modifier)
}

@Composable
private fun InternalPlayback(ui: PlayerUiState, player: Player, viewModel: PlayerViewModel, modifier: Modifier) {
    val playback by player.state.collectAsStateWithLifecycle()
    val inPip = pictureInPicture(enabled = ui.pictureInPicture)
    var overlayVisible by remember { mutableStateOf(true) }
    var panel by remember { mutableStateOf<PlayerPanel?>(null) }
    // The last panel closed, so its button gets focus back.
    var lastPanel by remember { mutableStateOf<PlayerPanel?>(null) }
    // Bumped on every key press so the overlay's hide timer restarts.
    var interaction by remember { mutableIntStateOf(0) }
    val rootFocus = remember { FocusRequester() }
    PlaybackLifecycle(viewModel)
    BackHandler(enabled = overlayVisible) { overlayVisible = false }
    // Composed later, so it wins while a panel is open.
    BackHandler(enabled = panel != null) {
        lastPanel = panel
        panel = null
    }
    LaunchedEffect(overlayVisible, interaction, panel) {
        // Panels stay until closed; the control bar hides after a pause.
        if (overlayVisible && panel == null) {
            delay(OVERLAY_TIMEOUT_MS)
            overlayVisible = false
            lastPanel = null
        }
    }
    // A new channel shows its title briefly.
    LaunchedEffect(ui.channel?.id) {
        if (ui.channel != null) {
            lastPanel = null
            overlayVisible = true
        }
    }
    LaunchedEffect(overlayVisible) { if (!overlayVisible) runCatching { rootFocus.requestFocus() } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                interaction++
                event.type == KeyEventType.KeyDown &&
                    viewModel.handleKey(event.key.nativeKeyCode, overlayVisible, playback, player) {
                        overlayVisible = true
                    }
            }
            .focusRequester(rootFocus)
            .focusable(),
    ) {
        player.VideoSurface(Modifier.fillMaxSize())
        // The small window shows only the picture.
        if (!inPip) StatusLayer(ui, playback)
        val openPanel = panel
        when {
            inPip -> Unit
            openPanel != null -> SidePanel(openPanel, playback, player)
            overlayVisible -> ControlsOverlay(
                ui = ui,
                playback = playback,
                player = player,
                returnFocusTo = lastPanel,
                onOpenPanel = { panel = it },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/** Keeps the screen on, and stops and resumes playback as the app leaves and returns. */
@Composable
private fun PlaybackLifecycle(viewModel: PlayerViewModel) {
    KeepScreenOn()
    BackgroundPlayback(onBackground = viewModel::onBackground, onForeground = viewModel::onForeground)
}

/** Messages over the picture: loading and errors in the middle, a typed channel number at the top. */
@Composable
private fun BoxScope.StatusLayer(ui: PlayerUiState, playback: PlaybackState) {
    PlaybackStatus(ui, playback, Modifier.align(Alignment.Center))
    NumberEntry(ui, Modifier.align(Alignment.TopEnd).padding(Spacing.screenHorizontal))
}

/** The digit a number key stands for, from the remote's or a keyboard's number row or keypad. */
private fun digitOf(code: Int): Int? = when (code) {
    in KEYCODE_0..KEYCODE_9 -> code - KEYCODE_0
    in KEYCODE_NUMPAD_0..KEYCODE_NUMPAD_9 -> code - KEYCODE_NUMPAD_0
    else -> null
}

/**
 * Keys on the playback screen. Number keys always pick a channel; the rest act only while the
 * controls are hidden. Returns true when the key was used.
 */
private fun PlayerViewModel.handleKey(
    code: Int,
    overlayVisible: Boolean,
    playback: PlaybackState,
    player: Player,
    showOverlay: () -> Unit,
): Boolean {
    val digit = digitOf(code)
    return when {
        digit != null -> {
            typeDigit(digit)
            true
        }
        overlayVisible -> false
        else -> handleControlKey(code, playback, player, this, showOverlay)
    }
}

/** Keys while the controls are hidden. Returns true when the key was used. */
private fun handleControlKey(
    code: Int,
    playback: PlaybackState,
    player: Player,
    viewModel: PlayerViewModel,
    showOverlay: () -> Unit,
): Boolean {
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
