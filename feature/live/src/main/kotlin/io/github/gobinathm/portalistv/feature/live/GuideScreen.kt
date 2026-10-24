package io.github.gobinathm.portalistv.feature.live

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.ImageLoader
import io.github.gobinathm.portalistv.core.designsystem.component.ChannelLogo
import io.github.gobinathm.portalistv.core.designsystem.component.backdrop
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing
import io.github.gobinathm.portalistv.core.model.LiveChannel
import io.github.gobinathm.portalistv.core.model.Programme
import java.text.DateFormat
import java.util.Date
import android.text.format.DateFormat as AndroidDateFormat

private val ChannelColumnWidth = 200.dp
private val RowHeight = 56.dp
private val LogoSize = DpSize(48.dp, 32.dp)
private val NowLineWidth = 2.dp

/**
 * A TV guide for one Live TV category: channels down the side, their programmes along a timeline.
 * Up and down move between channels, left and right between programmes; the timeline follows.
 */
@Composable
fun GuideScreen(
    onPlay: (channel: LiveChannel, categoryId: String, page: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LiveViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val imageLoader by viewModel.imageLoader.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Follows the TV's 12/24-hour setting.
    val timeFormat = remember(context) { AndroidDateFormat.getTimeFormat(context) }
    // The earliest the timeline goes: the half hour around now, from when the guide was opened.
    val earliest = remember { halfHourBefore(System.currentTimeMillis()) }
    var windowStart by remember { mutableLongStateOf(earliest) }
    // Glides to the new position rather than jumping. Relative to [earliest]: a Float can't hold epoch millis.
    val offsetMs by animateFloatAsState((windowStart - earliest).toFloat(), label = "guide")
    val shownStart = earliest + offsetMs.toLong()

    Column(modifier.fillMaxSize().backdrop(MaterialTheme.colorScheme).padding(vertical = Spacing.screenVertical)) {
        GuideHeader(state, shownStart, timeFormat)
        val listState = rememberLazyListState()
        val firstFocus = remember { FocusRequester() }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            itemsIndexed(state.channels, key = { _, channel -> channel.id }) { index, channel ->
                GuideRow(
                    channel = channel,
                    programmes = state.guide[channel.id].orEmpty(),
                    windowStart = shownStart,
                    nowMillis = state.nowMillis,
                    imageLoader = imageLoader,
                    firstFocus = firstFocus.takeIf { index == 0 },
                    onFocusBlock = { block -> windowStart = windowFor(block, windowStart, earliest) },
                    onClick = { onPlay(channel, state.selectedCategoryId, viewModel.pageOf(channel)) },
                )
            }
        }
        LaunchedEffect(listState) {
            snapshotFlow {
                val items = listState.layoutInfo.visibleItemsInfo
                (items.firstOrNull()?.index ?: 0) to (items.lastOrNull()?.index ?: 0)
            }.collect { (first, last) -> viewModel.onVisible(first, last) }
        }
        LaunchedEffect(state.channels.isNotEmpty()) {
            if (state.channels.isNotEmpty()) runCatching { firstFocus.requestFocus() }
        }
    }
}

/** The category name, then the time labels above the timeline, every half hour. */
@Composable
private fun GuideHeader(state: LiveUiState, windowStart: Long, timeFormat: DateFormat) {
    Column(Modifier.padding(horizontal = Spacing.screenHorizontal)) {
        val category = state.categories.firstOrNull { it.id == state.selectedCategoryId }
        Text(
            stringResource(R.string.guide_title, category?.label().orEmpty()),
            style = MaterialTheme.typography.headlineMedium,
        )
        Row(Modifier.fillMaxWidth().padding(top = Spacing.s)) {
            Box(Modifier.width(ChannelColumnWidth))
            BoxWithConstraints(Modifier.weight(1f).height(24.dp).clipToBounds()) {
                val dpPerMs = maxWidth / GUIDE_SPAN_MS.toFloat()
                val firstLabel = halfHourBefore(windowStart)
                for (step in 0..GUIDE_SPAN_MS / GUIDE_STEP_MS + 1) {
                    val time = firstLabel + step * GUIDE_STEP_MS
                    Text(
                        timeFormat.format(Date(time)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.offset(x = dpPerMs * (time - windowStart).toFloat()),
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideRow(
    channel: LiveChannel,
    programmes: List<Programme>,
    windowStart: Long,
    nowMillis: Long,
    imageLoader: ImageLoader?,
    firstFocus: FocusRequester?,
    onFocusBlock: (GuideBlock) -> Unit,
    onClick: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().height(RowHeight), verticalAlignment = Alignment.CenterVertically) {
        ChannelCell(channel, imageLoader)
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight().clipToBounds()) {
            val dpPerMs = maxWidth / GUIDE_SPAN_MS.toFloat()
            // A window either side of the visible one, so focus can move onto blocks just off screen.
            val blocks = guideBlocks(programmes, windowStart - GUIDE_SPAN_MS, windowStart + 2 * GUIDE_SPAN_MS)
            blocks.forEach { block ->
                // The first row's block at the left edge takes the first focus.
                val requester = firstFocus?.takeIf { block.startMillis <= windowStart && block.endMillis > windowStart }
                BlockCell(
                    block = block,
                    x = dpPerMs * (block.startMillis - windowStart).toFloat(),
                    width = dpPerMs * (block.endMillis - block.startMillis).toFloat(),
                    modifier = Modifier
                        .onFocusChanged { if (it.isFocused) onFocusBlock(block) }
                        .then(requester?.let { Modifier.focusRequester(it) } ?: Modifier),
                    onClick = onClick,
                )
            }
            if (nowMillis in windowStart until windowStart + GUIDE_SPAN_MS) {
                Box(
                    Modifier
                        .offset(x = dpPerMs * (nowMillis - windowStart).toFloat())
                        .width(NowLineWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Composable
private fun ChannelCell(channel: LiveChannel, imageLoader: ImageLoader?) {
    Row(
        Modifier.width(ChannelColumnWidth).padding(end = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        ChannelLogo(channel.name, channel.logoUrl, imageLoader, size = LogoSize)
        Text(
            listOfNotNull(channel.number?.toString(), channel.name).joinToString("  "),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BlockCell(block: GuideBlock, x: Dp, width: Dp, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .offset(x = x)
            .width(width)
            .fillMaxHeight()
            .padding(end = 2.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            focusedContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Text(
            block.programme?.title ?: stringResource(R.string.guide_no_info),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xs),
        )
    }
}
