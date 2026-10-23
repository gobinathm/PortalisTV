package io.github.gobinathm.portalistv.feature.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.gobinathm.portalistv.core.designsystem.component.PosterCard
import io.github.gobinathm.portalistv.core.designsystem.theme.PortalisTheme
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing

private const val PLACEHOLDER_ITEMS = 12

private val rows = listOf(R.string.home_row_continue, R.string.home_row_live, R.string.home_row_movies)

/** Placeholder home until profiles and content arrive. Exercises D-pad focus across rows. */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val firstItem = remember { FocusRequester() }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(vertical = Spacing.screenVertical),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        item(key = "header") { Header() }
        items(rows, key = { it }) { titleRes ->
            PlaceholderRow(
                titleRes = titleRes,
                initialFocus = firstItem.takeIf { titleRes == rows.first() },
            )
        }
    }
    LaunchedEffect(Unit) { firstItem.requestFocus() }
}

@Composable
private fun Header() {
    Column(Modifier.padding(horizontal = Spacing.screenHorizontal)) {
        Text(stringResource(R.string.home_title), style = MaterialTheme.typography.displayMedium)
        Text(
            stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PlaceholderRow(@StringRes titleRes: Int, initialFocus: FocusRequester?) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Text(
            stringResource(titleRes),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            items((1..PLACEHOLDER_ITEMS).toList(), key = { it }) { index ->
                PosterCard(
                    title = stringResource(R.string.home_placeholder_item, index),
                    onClick = {},
                    modifier = if (index == 1 && initialFocus != null) {
                        Modifier.focusRequester(initialFocus)
                    } else {
                        Modifier
                    },
                )
            }
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun HomeScreenPreview() {
    PortalisTheme { HomeScreen() }
}
