package io.github.gobinathm.portalistv.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.ImageLoader
import io.github.gobinathm.portalistv.core.designsystem.component.CategoryMark
import io.github.gobinathm.portalistv.core.designsystem.component.CategoryMarkView
import io.github.gobinathm.portalistv.core.designsystem.component.ChannelLogo
import io.github.gobinathm.portalistv.core.designsystem.component.mark
import io.github.gobinathm.portalistv.core.designsystem.theme.Focus
import io.github.gobinathm.portalistv.core.designsystem.theme.Radius
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing
import io.github.gobinathm.portalistv.core.model.LiveCategory
import io.github.gobinathm.portalistv.core.model.LiveChannel

private val CardSize = DpSize(208.dp, 136.dp)
private val LogoSize = DpSize(120.dp, 64.dp)
private val CategoryIconSize = 48.dp

@Composable
internal fun ChannelCard(
    channel: LiveChannel,
    imageLoader: ImageLoader?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier,
) {
    HomeCard(onClick, modifier, onLongClick) {
        ChannelLogo(channel.name, channel.logoUrl, imageLoader, size = LogoSize)
        Text(
            listOfNotNull(channel.number?.toString(), channel.name).joinToString("  "),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun CategoryCard(category: LiveCategory, onClick: () -> Unit, modifier: Modifier) {
    IconCard(
        label = categoryLabel(category),
        mark = category.mark(),
        onClick = onClick,
        modifier = modifier,
    )
}

/** A category: its icon (or initials) above its name. */
@Composable
internal fun IconCard(label: String, mark: CategoryMark, onClick: () -> Unit, modifier: Modifier) {
    HomeCard(onClick, modifier) {
        CategoryMarkView(mark, size = CategoryIconSize)
        Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A focusable card with centered content that scales and glows in the accent color when focused. */
@Composable
private fun HomeCard(
    onClick: () -> Unit,
    modifier: Modifier,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.size(CardSize),
        shape = CardDefaults.shape(RoundedCornerShape(Radius.card)),
        scale = CardDefaults.scale(focusedScale = Focus.CARD_SCALE),
        border = CardDefaults.border(focusedBorder = Border.None),
        glow = CardDefaults.glow(
            focusedGlow = Glow(elevationColor = MaterialTheme.colorScheme.primary, elevation = 16.dp),
        ),
    ) {
        Column(
            Modifier.fillMaxSize().padding(Spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.CenterVertically),
        ) { content() }
    }
}
