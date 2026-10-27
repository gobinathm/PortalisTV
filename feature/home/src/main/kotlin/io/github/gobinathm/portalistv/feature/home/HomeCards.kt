package io.github.gobinathm.portalistv.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import io.github.gobinathm.portalistv.core.designsystem.component.Language
import io.github.gobinathm.portalistv.core.designsystem.component.artwork
import io.github.gobinathm.portalistv.core.designsystem.component.mark
import io.github.gobinathm.portalistv.core.designsystem.component.tapToClick
import io.github.gobinathm.portalistv.core.designsystem.theme.Focus
import io.github.gobinathm.portalistv.core.designsystem.theme.Radius
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing
import io.github.gobinathm.portalistv.core.model.LiveCategory
import io.github.gobinathm.portalistv.core.model.LiveChannel

private val CardSize = DpSize(208.dp, 136.dp)
private val LogoSize = DpSize(120.dp, 64.dp)
private val CategoryIconSize = 48.dp

/** The number badge's backing on a title card. */
private const val BADGE_ALPHA = 0.35f

/** The category name under a language's own name, a step quieter. */
private const val LANGUAGE_LABEL_ALPHA = 0.8f

@Composable
internal fun ChannelCard(
    channel: LiveChannel,
    imageLoader: ImageLoader?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier,
) {
    if (channel.logoUrl == null || imageLoader == null) {
        ChannelTitleCard(channel, onClick, onLongClick, modifier)
        return
    }
    HomeCard(onClick, modifier, onLongClick) {
        CenteredContent {
            ChannelLogo(channel.name, channel.logoUrl, imageLoader, size = LogoSize)
            Text(
                listOfNotNull(channel.number?.toString(), channel.name).joinToString("  "),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A channel without a logo: its name, large, on its own colour, with the number in a badge. */
@Composable
private fun ChannelTitleCard(channel: LiveChannel, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier) {
    HomeCard(onClick, modifier, onLongClick) {
        Box(Modifier.fillMaxSize().artwork(channel.name).padding(Spacing.m)) {
            channel.number?.let { number ->
                Text(
                    number.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(Color.Black.copy(alpha = BADGE_ALPHA), RoundedCornerShape(Spacing.xs))
                        .padding(horizontal = Spacing.s, vertical = 2.dp),
                )
            }
            Text(
                channel.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
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

/** A category: its icon (or initials) above its name; a language gets a tile of its own. */
@Composable
internal fun IconCard(label: String, mark: CategoryMark, onClick: () -> Unit, modifier: Modifier) {
    if (mark is CategoryMark.Script) {
        LanguageCard(label, mark.language, onClick, modifier)
        return
    }
    HomeCard(onClick, modifier) {
        CenteredContent {
            CategoryMarkView(mark, size = CategoryIconSize)
            Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * A language category drawn as artwork: the language in its own script, large, on a colour picked
 * from it (so every Hindi category matches), with the category's name below.
 */
@Composable
private fun LanguageCard(label: String, language: Language, onClick: () -> Unit, modifier: Modifier) {
    HomeCard(onClick, modifier) {
        Column(
            Modifier.fillMaxSize().artwork(language.native).padding(Spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterVertically),
        ) {
            Text(
                language.native,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = LANGUAGE_LABEL_ALPHA),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CenteredContent(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(Spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.CenterVertically),
    ) { content() }
}

/** A focusable card that scales and glows in the accent color when focused. */
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
        modifier = modifier.size(CardSize).tapToClick(onClick, onLongClick),
        shape = CardDefaults.shape(RoundedCornerShape(Radius.card)),
        scale = CardDefaults.scale(focusedScale = Focus.CARD_SCALE),
        border = CardDefaults.border(focusedBorder = Border.None),
        glow = CardDefaults.glow(
            focusedGlow = Glow(elevationColor = MaterialTheme.colorScheme.primary, elevation = 16.dp),
        ),
    ) { content() }
}
