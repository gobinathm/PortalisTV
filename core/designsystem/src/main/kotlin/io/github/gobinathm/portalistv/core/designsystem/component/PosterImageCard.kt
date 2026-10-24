package io.github.gobinathm.portalistv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
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
import coil3.compose.AsyncImage
import io.github.gobinathm.portalistv.core.designsystem.theme.Focus
import io.github.gobinathm.portalistv.core.designsystem.theme.Radius
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing

private val ProgressHeight = 4.dp
private val TitleScrim = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))

/**
 * A poster for a movie or series: the image with the title over a dark fade at the bottom, or
 * just the title on a plain card when there's no image. [progress] (0–1) draws how far it's been watched.
 */
@Composable
fun PosterImageCard(
    title: String,
    imageUrl: String?,
    imageLoader: ImageLoader?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: DpSize = PosterSize.Portrait,
    subtitle: String? = null,
    progress: Float? = null,
) {
    Card(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CardDefaults.shape(RoundedCornerShape(Radius.card)),
        scale = CardDefaults.scale(focusedScale = Focus.CARD_SCALE),
        border = CardDefaults.border(focusedBorder = Border.None),
        glow = CardDefaults.glow(
            focusedGlow = Glow(elevationColor = MaterialTheme.colorScheme.primary, elevation = 16.dp),
        ),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (imageUrl != null && imageLoader != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    imageLoader = imageLoader,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(TitleScrim))
                    .padding(Spacing.s),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                )
                subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                progress?.let { ProgressBar(it) }
            }
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float) {
    Box(Modifier.fillMaxWidth().height(ProgressHeight).background(Color.White.copy(alpha = 0.3f))) {
        Box(Modifier.fillMaxWidth(fraction).height(ProgressHeight).background(MaterialTheme.colorScheme.primary))
    }
}
