package io.github.gobinathm.portalistv.feature.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.ImageLoader
import io.github.gobinathm.portalistv.core.designsystem.component.ChannelLogo
import io.github.gobinathm.portalistv.core.designsystem.component.tapToClick
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing
import io.github.gobinathm.portalistv.core.model.LiveChannel
import io.github.gobinathm.portalistv.core.model.NowNext
import java.text.DateFormat
import java.util.Date
import io.github.gobinathm.portalistv.core.designsystem.R as DesignR

/** Fixed, so rows don't change height as guide data arrives and the list doesn't jump. */
private val GuideHeight = 44.dp
private val NumberWidth = 56.dp
private val ProgressHeight = 3.dp

/** One channel: number, logo, name, and now/next with how far the current programme has got. */
@Composable
internal fun ChannelRow(
    channel: LiveChannel,
    nowNext: NowNext,
    nowMillis: Long,
    favorite: Boolean,
    imageLoader: ImageLoader?,
    timeFormat: DateFormat,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        selected = false,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.tapToClick(onClick, onLongClick),
        leadingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                Text(
                    channel.number?.toString().orEmpty(),
                    modifier = Modifier.width(NumberWidth),
                    color = LocalContentColor.current.copy(alpha = 0.7f),
                    maxLines = 1,
                )
                ChannelLogo(channel.name, channel.logoUrl, imageLoader)
            }
        },
        headlineContent = {
            Text(channel.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = { Guide(nowNext, nowMillis, timeFormat) },
        trailingContent = if (favorite) {
            { Icon(painterResource(DesignR.drawable.ic_favorite), stringResource(R.string.live_favorite)) }
        } else {
            null
        },
    )
}

@Composable
private fun Guide(nowNext: NowNext, nowMillis: Long, timeFormat: DateFormat) {
    Column(Modifier.height(GuideHeight).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        val now = nowNext.now
        if (now != null) {
            Text(now.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Progress(now.progress(nowMillis))
        }
        nowNext.next?.let { next ->
            Text(
                stringResource(R.string.live_next, timeFormat.format(Date(next.startMillis)), next.title),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = LocalContentColor.current.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun Progress(fraction: Float) {
    Box(
        Modifier
            .fillMaxWidth(PROGRESS_WIDTH)
            .height(ProgressHeight)
            .clip(RoundedCornerShape(ProgressHeight))
            .background(LocalContentColor.current.copy(alpha = 0.2f)),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

private const val PROGRESS_WIDTH = 0.5f

/** A small star for favorites outside the list, e.g. in the header hint. */
@Composable
internal fun FavoriteIcon(modifier: Modifier = Modifier) {
    Icon(painterResource(DesignR.drawable.ic_favorite), contentDescription = null, modifier = modifier.size(20.dp))
}
