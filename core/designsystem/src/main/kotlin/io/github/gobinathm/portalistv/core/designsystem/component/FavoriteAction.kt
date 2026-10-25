package io.github.gobinathm.portalistv.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.gobinathm.portalistv.core.designsystem.R
import io.github.gobinathm.portalistv.core.model.LiveChannel

enum class FavoriteAction { ADD, SHOW_ON_HOME, HIDE_FROM_HOME, REMOVE }

/**
 * What can be done with [channel] from a hold of OK: add it to favorites, or, if it is one, show it
 * on Home or hide it there, or remove it.
 */
@Composable
fun FavoriteMenu(
    channel: LiveChannel,
    isFavorite: Boolean,
    onHome: Boolean,
    onAction: (FavoriteAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val actions = when {
        !isFavorite -> listOf(FavoriteAction.ADD)
        onHome -> listOf(FavoriteAction.HIDE_FROM_HOME, FavoriteAction.REMOVE)
        else -> listOf(FavoriteAction.SHOW_ON_HOME, FavoriteAction.REMOVE)
    }
    ActionDialog(
        title = channel.name,
        actions = actions.map { ChoiceOption(it.name, stringResource(it.labelRes())) },
        onSelect = { key -> onAction(FavoriteAction.valueOf(key)) },
        onDismiss = onDismiss,
    )
}

private fun FavoriteAction.labelRes() = when (this) {
    FavoriteAction.ADD -> R.string.favorite_add
    FavoriteAction.SHOW_ON_HOME -> R.string.favorite_show_on_home
    FavoriteAction.HIDE_FROM_HOME -> R.string.favorite_hide_from_home
    FavoriteAction.REMOVE -> R.string.favorite_remove
}
