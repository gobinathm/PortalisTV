package io.github.gobinathm.portalistv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.ImageLoader
import coil3.compose.AsyncImage

/** Logos are drawn on a light plate: most are made for white backgrounds. */
private const val PLATE_ALPHA = 0.92f

/**
 * A channel's logo on a rounded plate, or its initials when there is no logo (or it can't load).
 * [imageLoader] decides how the logo is fetched; logos are never fetched without one.
 */
@Composable
fun ChannelLogo(
    name: String,
    logoUrl: String?,
    imageLoader: ImageLoader?,
    modifier: Modifier = Modifier,
    size: DpSize = DpSize(72.dp, 48.dp),
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (logoUrl != null && imageLoader != null) {
            // The plate goes in only once the logo is there, so the initials show while loading or on failure.
            var loaded by remember(logoUrl) { mutableStateOf(false) }
            AsyncImage(
                model = logoUrl,
                contentDescription = null,
                imageLoader = imageLoader,
                contentScale = ContentScale.Fit,
                onSuccess = { loaded = true },
                onError = { loaded = false },
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (loaded) {
                            Modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = PLATE_ALPHA))
                        } else {
                            Modifier
                        },
                    )
                    .padding(4.dp),
            )
        }
    }
}
