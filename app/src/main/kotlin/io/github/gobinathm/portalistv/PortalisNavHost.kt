package io.github.gobinathm.portalistv

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.gobinathm.portalistv.core.model.ALL_CATEGORIES
import io.github.gobinathm.portalistv.core.model.FAVORITES_CATEGORY
import io.github.gobinathm.portalistv.feature.home.HomeActions
import io.github.gobinathm.portalistv.feature.home.HomeScreen
import io.github.gobinathm.portalistv.feature.home.HomeViewModel
import io.github.gobinathm.portalistv.feature.live.LiveScreen
import io.github.gobinathm.portalistv.feature.player.PlayerScreen
import io.github.gobinathm.portalistv.feature.profiles.editor.ProfileEditorScreen
import io.github.gobinathm.portalistv.feature.profiles.editor.ProfileEditorViewModel
import io.github.gobinathm.portalistv.feature.profiles.list.ProfilesScreen
import io.github.gobinathm.portalistv.feature.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
private data object ProfilesRoute

@Serializable
private data object SettingsRoute

/**
 * Edits a profile, or creates one when [profileId] is 0. The property name is the argument key
 * that [ProfileEditorViewModel.PROFILE_ID_ARG] reads.
 */
@Serializable
private data class ProfileEditorRoute(val profileId: Long = 0)

/** The property name is the argument key that [HomeViewModel.PROFILE_ID_ARG] reads. */
@Serializable
private data class HomeRoute(val profileId: Long)

/** Live TV opened at [categoryId]. Names match LiveViewModel's args. */
@Serializable
private data class LiveRoute(val profileId: Long, val categoryId: String = ALL_CATEGORIES)

/** Plays [channelId], zapping within [categoryId] starting from its [page]. Names match PlayerViewModel's args. */
@Serializable
private data class PlayerRoute(val profileId: Long, val categoryId: String, val page: Int, val channelId: String)

/** The app's screens. Feature modules never navigate themselves; they report intents here. */
@Composable
fun PortalisNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = ProfilesRoute) {
        composable<ProfilesRoute> {
            ProfilesScreen(
                onOpen = { id -> navController.navigate(HomeRoute(id)) },
                onEdit = { id -> navController.navigate(ProfileEditorRoute(id)) },
                onAdd = { navController.navigate(ProfileEditorRoute()) },
                onSettings = { navController.navigate(SettingsRoute) },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen()
        }
        composable<ProfileEditorRoute> {
            ProfileEditorScreen(onDone = { navController.popBackStack() })
        }
        composable<HomeRoute> { entry ->
            val profileId = entry.toRoute<HomeRoute>().profileId
            HomeScreen(
                HomeActions(
                    onOpenCategory = { category -> navController.navigate(LiveRoute(profileId, category.id)) },
                    onPlayFavorite = { channel ->
                        navController.navigate(
                            PlayerRoute(profileId, FAVORITES_CATEGORY, page = 1, channelId = channel.id),
                        )
                    },
                    onPlayChannel = { channel, categoryId ->
                        navController.navigate(PlayerRoute(profileId, categoryId, page = 1, channelId = channel.id))
                    },
                    onSwitchProfile = { navController.popBackStack() },
                    onSettings = { navController.navigate(SettingsRoute) },
                ),
            )
        }
        composable<LiveRoute> { entry ->
            val profileId = entry.toRoute<LiveRoute>().profileId
            LiveScreen(
                onPlay = { channel, categoryId, page ->
                    navController.navigate(PlayerRoute(profileId, categoryId, page, channel.id))
                },
            )
        }
        composable<PlayerRoute> {
            PlayerScreen(onExit = { navController.popBackStack() })
        }
    }
}
