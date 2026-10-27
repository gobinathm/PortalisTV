package io.github.gobinathm.portalistv.feature.player

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

/**
 * Full-screen playback. Up/Down zap, OK shows the controls, Left/Right choose where to seek when possible.
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

@Suppress("LongMethod") // The screen's state with its key, touch and Back handling, which all change it.
@Composable
private fun InternalPlayback(ui: PlayerUiState, player: Player, viewModel: PlayerViewModel, modifier: Modifier) {
    val playback by player.state.collectAsStateWithLifecycle()
    val inPip = pictureInPicture(enabled = ui.pictureInPicture)
    var overlayVisible by remember { mutableStateOf(true) }
    var panel by remember { mutableStateOf<PlayerPanel?>(null) }
    // The last panel closed, so its button gets focus back.
    var lastPanel by remember { mutableStateOf<PlayerPanel?>(null) }
    // Bumped on every key press and touch so the overlay's hide timer restarts.
    var interaction by remember { mutableIntStateOf(0) }
    val rootFocus = remember { FocusRequester() }
    val screenActions = PlayerUi(showOverlay = { overlayVisible = true }, openPanel = { panel = it })
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
                    viewModel.handleKey(event.nativeKeyEvent, overlayVisible, panel, playback, player, screenActions)
            }
            .playerGestures(
                onTouch = { interaction++ },
                onTap = { if (panel != null) panel = null else overlayVisible = !overlayVisible },
                onSwipe = { viewModel.handleSwipe(it, playback) },
            )
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
    // While choosing where to seek, the bar says it all; a "buffering" note would only distract.
    if (ui.scrubTargetMs == null) PlaybackStatus(ui, playback, Modifier.align(Alignment.Center))
    ui.scrubTargetMs?.let { ScrubBar(it, playback, Modifier.align(Alignment.BottomCenter)) }
    NumberEntry(ui, Modifier.align(Alignment.TopEnd).padding(Spacing.screenHorizontal))
    FavoriteNotice(ui, Modifier.align(Alignment.TopEnd).padding(Spacing.screenHorizontal))
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
