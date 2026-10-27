package io.github.gobinathm.portalistv.feature.vod

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import coil3.ImageLoader
import io.github.gobinathm.portalistv.core.data.PinResult
import io.github.gobinathm.portalistv.core.designsystem.component.CategoryRail
import io.github.gobinathm.portalistv.core.designsystem.component.ChoiceDialog
import io.github.gobinathm.portalistv.core.designsystem.component.ChoiceOption
import io.github.gobinathm.portalistv.core.designsystem.component.PinDialog
import io.github.gobinathm.portalistv.core.designsystem.component.PosterImageCard
import io.github.gobinathm.portalistv.core.designsystem.component.TextInputDialog
import io.github.gobinathm.portalistv.core.designsystem.component.backdrop
import io.github.gobinathm.portalistv.core.designsystem.component.groupOf
import io.github.gobinathm.portalistv.core.designsystem.component.mark
import io.github.gobinathm.portalistv.core.designsystem.component.pausedMessage
import io.github.gobinathm.portalistv.core.designsystem.component.railEntries
import io.github.gobinathm.portalistv.core.designsystem.component.tapToClick
import io.github.gobinathm.portalistv.core.designsystem.theme.PortalisTheme
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing
import io.github.gobinathm.portalistv.core.model.ALL_CATEGORIES
import io.github.gobinathm.portalistv.core.model.CategoryNode
import io.github.gobinathm.portalistv.core.model.VodCategory
import io.github.gobinathm.portalistv.core.model.VodItem
import io.github.gobinathm.portalistv.core.model.groupCategories
import io.github.gobinathm.portalistv.core.designsystem.R as DesignR

private val RailWidth = 260.dp
private val GridPoster = DpSize(132.dp, 198.dp)

/** Movies and series: a category rail and a poster grid. [onOpen] shows an item's details. */
@Composable
fun VodBrowseScreen(
    onOpen: (VodItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VodBrowseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val imageLoader by viewModel.imageLoader.collectAsStateWithLifecycle()
    VodBrowseContent(
        state = state,
        imageLoader = imageLoader,
        onSelectCategory = { viewModel.selectCategory(it) },
        onFocusCategory = { viewModel.selectCategory(it, ask = false) },
        onScrolledTo = viewModel::onScrolledTo,
        onRetry = viewModel::retry,
        onOpen = onOpen,
        filterActions = FilterActions(onSearch = viewModel::search, onSelectYear = viewModel::selectYear),
        modifier = modifier,
    )
    if (state.pinFor != null) {
        val paused = pausedMessage()
        PinDialog(
            title = stringResource(DesignR.string.pin_enter),
            error = when (val result = state.pinResult) {
                PinResult.Wrong -> stringResource(DesignR.string.pin_wrong)
                is PinResult.Paused -> paused(result.seconds)
                else -> null
            },
            onSubmit = viewModel::submitPin,
            onDismiss = viewModel::cancelPin,
        )
    }
}

@Composable
private fun VodBrowseContent(
    state: VodBrowseUiState,
    imageLoader: ImageLoader?,
    onSelectCategory: (String) -> Unit,
    onFocusCategory: (String) -> Unit,
    onScrolledTo: (Int) -> Unit,
    onRetry: () -> Unit,
    onOpen: (VodItem) -> Unit,
    filterActions: FilterActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .backdrop(MaterialTheme.colorScheme)
            .padding(vertical = Spacing.screenVertical),
    ) {
        // Everything stays on top; the portal's categories are grouped by their first word.
        val nodes = remember(state.categories) {
            val (all, portal) = state.categories.partition { it.id == ALL_CATEGORIES }
            all.map { CategoryNode.Single(it) } + groupCategories(portal) { it.title }
        }
        var expanded by remember(nodes) { mutableStateOf(groupOf(nodes, state.selectedCategoryId) { it.id }) }
        val everything = stringResource(R.string.vod_all)
        val label = { category: VodCategory -> if (category.id == ALL_CATEGORIES) everything else category.title }
        CategoryRail(
            entries = railEntries(nodes, expanded, { it.id }, label, { it.mark() }),
            selectedId = state.selectedCategoryId,
            onSelect = onSelectCategory,
            modifier = Modifier.width(RailWidth).fillMaxHeight(),
            onToggleGroup = { key -> expanded = if (expanded == key) null else key },
            onFocusSelect = onFocusCategory,
        )
        Column(
            Modifier.weight(1f).fillMaxHeight().padding(end = Spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            GridHeader(state, filterActions)
            val message = when {
                state.failed -> R.string.vod_failed
                state.loadingCategories || (state.loadingItems && state.items.isEmpty()) -> R.string.vod_loading
                state.items.isEmpty() && state.pages.complete ->
                    if (state.filter.isEmpty) R.string.vod_empty else R.string.vod_no_matches
                else -> null
            }
            if (message != null && state.items.isEmpty()) {
                Column(
                    Modifier.padding(horizontal = Spacing.m),
                    verticalArrangement = Arrangement.spacedBy(Spacing.m),
                ) {
                    Text(stringResource(message), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.failed) {
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.tapToClick(onRetry)
                        ) { Text(stringResource(R.string.vod_retry)) }
                    }
                }
            } else {
                PosterGrid(state, imageLoader, onScrolledTo, onOpen)
            }
        }
    }
}

/** What the header's search and year buttons do. */
private class FilterActions(val onSearch: (String) -> Unit, val onSelectYear: (String?) -> Unit)

@Composable
private fun GridHeader(state: VodBrowseUiState, actions: FilterActions) {
    val category = state.categories.firstOrNull { it.id == state.selectedCategoryId }
    Row(
        Modifier.padding(horizontal = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        Column(Modifier.weight(1f)) {
            Text(category?.label().orEmpty(), style = MaterialTheme.typography.headlineMedium)
            if (state.pages.totalItems > 0) {
                Text(
                    pluralStringResource(R.plurals.vod_item_count, state.pages.totalItems, state.pages.totalItems),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        FilterButtons(state, actions)
    }
}

/** Search (titles and, on most portals, actors), a year when the portal lists years, and Clear. */
@Composable
private fun FilterButtons(state: VodBrowseUiState, actions: FilterActions) {
    var searching by remember { mutableStateOf(false) }
    var pickingYear by remember { mutableStateOf(false) }
    val filter = state.filter
    val year = state.years.firstOrNull { it.id == filter.yearId }
    val searchFocus = remember { FocusRequester() }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        val search = { searching = true }
        OutlinedButton(onClick = search, modifier = Modifier.focusRequester(searchFocus).tapToClick(search)) {
            val label = filter.search.ifBlank { null }?.let { stringResource(R.string.vod_search_for, it) }
            Text(label ?: stringResource(R.string.vod_search))
        }
        if (state.years.isNotEmpty()) {
            val pickYear = { pickingYear = true }
            OutlinedButton(onClick = pickYear, modifier = Modifier.tapToClick(pickYear)) {
                Text(year?.title ?: stringResource(R.string.vod_any_year))
            }
        }
        if (!filter.isEmpty) {
            val clear = {
                // Clear goes away with the filter; keep focus in the header rather than losing it to the rail.
                searchFocus.requestFocus()
                actions.onSearch("")
                actions.onSelectYear(null)
            }
            OutlinedButton(onClick = clear, modifier = Modifier.tapToClick(clear)) {
                Text(stringResource(R.string.vod_clear_filter))
            }
        }
    }
    if (searching) {
        TextInputDialog(
            title = stringResource(R.string.vod_search),
            initial = filter.search,
            hint = stringResource(R.string.vod_search_hint),
            onConfirm = {
                searching = false
                actions.onSearch(it)
            },
            onDismiss = { searching = false },
        )
    }
    if (pickingYear) {
        ChoiceDialog(
            title = stringResource(R.string.vod_year),
            options = listOf(ChoiceOption(ANY_YEAR, stringResource(R.string.vod_any_year))) +
                state.years.map { ChoiceOption(it.id, it.title) },
            selectedKey = filter.yearId ?: ANY_YEAR,
            onSelect = {
                pickingYear = false
                actions.onSelectYear(it.takeUnless { key -> key == ANY_YEAR })
            },
            onDismiss = { pickingYear = false },
        )
    }
}

/** The year choice that lifts the filter; not a portal year id. */
private const val ANY_YEAR = "portalis:any-year"

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PosterGrid(
    state: VodBrowseUiState,
    imageLoader: ImageLoader?,
    onScrolledTo: (Int) -> Unit,
    onOpen: (VodItem) -> Unit,
) {
    // A new grid per category, so each starts at the top.
    val gridState = remember(state.selectedCategoryId) { LazyGridState() }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(GridPoster.width),
        state = gridState,
        modifier = Modifier.focusGroup().focusRestorer(),
        contentPadding = PaddingValues(Spacing.m),
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        items(state.items, key = { it.id }) { item ->
            PosterImageCard(
                title = item.title,
                imageUrl = item.posterUrl,
                imageLoader = imageLoader,
                onClick = { onOpen(item) },
                size = GridPoster,
                subtitle = item.year,
            )
        }
    }
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect(onScrolledTo)
    }
}

@Composable
private fun VodCategory.label(): String = if (id == ALL_CATEGORIES) stringResource(R.string.vod_all) else title

@Preview(device = "id:tv_1080p")
@Composable
private fun VodBrowsePreview() {
    val items = (1..10).map {
        VodItem("$it", "Movie $it", null, null, "2024", null, null, null, null, 90, false, false, null)
    }
    PortalisTheme {
        VodBrowseContent(
            state = VodBrowseUiState(
                categories = listOf(VodCategory(ALL_CATEGORIES, "", false), VodCategory("1", "KIDS MOVIES", false)),
                selectedCategoryId = "1",
                pages = PagedItems<VodItem>("1").append(1, items, 1, items.size) { it.id },
                loadingCategories = false,
            ),
            imageLoader = null,
            onSelectCategory = {},
            onFocusCategory = {},
            onScrolledTo = {},
            onRetry = {},
            onOpen = {},
            filterActions = FilterActions(onSearch = {}, onSelectYear = {}),
        )
    }
}
