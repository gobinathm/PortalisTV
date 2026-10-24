package io.github.gobinathm.portalistv.feature.profiles.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import io.github.gobinathm.portalistv.core.designsystem.component.backdrop
import io.github.gobinathm.portalistv.core.designsystem.theme.Focus
import io.github.gobinathm.portalistv.core.designsystem.theme.PortalisTheme
import io.github.gobinathm.portalistv.core.designsystem.theme.Radius
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing
import io.github.gobinathm.portalistv.feature.profiles.R
import io.github.gobinathm.portalistv.feature.profiles.backup.BackupActions

private val TileSize = 168.dp
private val AvatarSize = 72.dp

@Composable
fun ProfilesScreen(
    onOpen: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onAdd: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfilesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val openAtStart by viewModel.openAtStart.collectAsStateWithLifecycle()
    LaunchedEffect(openAtStart) {
        openAtStart?.let { id ->
            viewModel.onStartProfileOpened()
            onOpen(id)
        }
    }
    ProfilesContent(state, onOpen, onEdit, onAdd, modifier) { canExport ->
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            BackupActions(canExport)
            OutlinedButton(onClick = onSettings) { Text(stringResource(R.string.profiles_settings)) }
        }
    }
}

@Composable
private fun ProfilesContent(
    state: ProfilesUiState,
    onOpen: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable (canExport: Boolean) -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .backdrop(MaterialTheme.colorScheme)
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.screenVertical),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            ProfilesUiState.Loading -> Unit
            ProfilesUiState.Unavailable -> Text(
                stringResource(R.string.profiles_unavailable),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            is ProfilesUiState.Loaded -> ProfilePicker(state.profiles, onOpen, onEdit, onAdd, actions)
        }
    }
}

@Composable
private fun ProfilePicker(
    profiles: List<ProfileSummary>,
    onOpen: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onAdd: () -> Unit,
    actions: @Composable (canExport: Boolean) -> Unit,
) {
    val firstTile = remember { FocusRequester() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
        Text(stringResource(R.string.profiles_title), style = MaterialTheme.typography.displayMedium)
        Text(
            stringResource(if (profiles.isEmpty()) R.string.profiles_empty else R.string.profiles_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(
            contentPadding = PaddingValues(Spacing.l),
            horizontalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            items(profiles, key = { it.id }) { profile ->
                ProfileTile(
                    title = profile.name,
                    subtitle = profileSubtitle(profile),
                    initial = profile.name.firstOrNull()?.uppercase() ?: "?",
                    onClick = { onOpen(profile.id) },
                    onLongClick = { onEdit(profile.id) },
                    modifier = if (profile == profiles.first()) Modifier.focusRequester(firstTile) else Modifier,
                )
            }
            item(key = "add") {
                ProfileTile(
                    title = stringResource(R.string.profiles_add),
                    subtitle = null,
                    initial = "+",
                    onClick = onAdd,
                    modifier = if (profiles.isEmpty()) Modifier.focusRequester(firstTile) else Modifier,
                )
            }
        }
        actions(profiles.isNotEmpty())
    }
    LaunchedEffect(Unit) { firstTile.requestFocus() }
}

/** The device model (or the start-up marker), led by a warning when the portal uses plain HTTP. */
@Composable
private fun profileSubtitle(profile: ProfileSummary): AnnotatedString = buildAnnotatedString {
    if (profile.notSecure) {
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.error)) {
            append(stringResource(R.string.profiles_not_secure))
        }
        append(" · ")
    }
    if (profile.opensAtStart) {
        // The tile is narrow; the start-up marker matters more than the model here.
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
            append(stringResource(R.string.profiles_opens_at_start))
        }
    } else {
        append(profile.model)
    }
}

@Composable
private fun ProfileTile(
    title: String,
    subtitle: AnnotatedString?,
    initial: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.size(TileSize),
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
        ) {
            Box(
                Modifier.size(AvatarSize).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(initial, style = MaterialTheme.typography.headlineMedium)
            }
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun ProfilesPreview() {
    PortalisTheme {
        ProfilesContent(
            ProfilesUiState.Loaded(listOf(ProfileSummary(1, "Living room", "MAG250", notSecure = true))),
            onOpen = {},
            onEdit = {},
            onAdd = {},
        )
    }
}
