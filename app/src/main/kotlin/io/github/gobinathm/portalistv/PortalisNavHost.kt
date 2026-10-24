package io.github.gobinathm.portalistv

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.gobinathm.portalistv.core.model.ALL_CATEGORIES
import io.github.gobinathm.portalistv.core.model.FAVORITES_CATEGORY
import io.github.gobinathm.portalistv.core.model.WatchProgress
import io.github.gobinathm.portalistv.feature.home.HomeActions
import io.github.gobinathm.portalistv.feature.home.HomeScreen
import io.github.gobinathm.portalistv.feature.home.HomeViewModel
import io.github.gobinathm.portalistv.feature.live.GuideScreen
import io.github.gobinathm.portalistv.feature.live.LiveScreen
import io.github.gobinathm.portalistv.feature.player.PlayerScreen
import io.github.gobinathm.portalistv.feature.profiles.editor.ProfileEditorScreen
import io.github.gobinathm.portalistv.feature.profiles.editor.ProfileEditorViewModel
import io.github.gobinathm.portalistv.feature.profiles.list.ProfilesScreen
import io.github.gobinathm.portalistv.feature.settings.SettingsScreen
import io.github.gobinathm.portalistv.feature.vod.VodBrowseScreen
import io.github.gobinathm.portalistv.feature.vod.VodDetailScreen
import io.github.gobinathm.portalistv.feature.vod.VodPlayRequest
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

/** On demand opened at [categoryId]. Names match VodBrowseViewModel's args. */
@Serializable
private data class VodRoute(val profileId: Long, val categoryId: String = ALL_CATEGORIES)

/** Names match VodDetailViewModel's args. */
@Serializable
private data class VodDetailRoute(val profileId: Long, val itemId: String)

/** Plays a movie or an episode. Names match VodRequest's args in the player. */
@Serializable
private data class VodPlayerRoute(
    val profileId: Long,
    val vodItemId: String,
    val vodSeasonId: String?,
    val vodEpisodeId: String?,
    val vodPlaybackRef: String?,
    val vodTitle: String,
    val vodSubtitle: String?,
    val vodPosterUrl: String?,
    val vodIsSeries: Boolean,
    val vodStartOver: Boolean = false,
)

private fun VodPlayRequest.toRoute(profileId: Long) = VodPlayerRoute(
    profileId = profileId,
    vodItemId = playback.itemId,
    vodSeasonId = playback.seasonId,
    vodEpisodeId = playback.episodeId,
    vodPlaybackRef = playback.playbackRef,
    vodTitle = item.title,
    vodSubtitle = subtitle,
    vodPosterUrl = item.posterUrl,
    vodIsSeries = item.isSeries,
    vodStartOver = startOver,
)

private fun WatchProgress.toRoute(profileId: Long) = VodPlayerRoute(
    profileId = profileId,
    vodItemId = playback.itemId,
    vodSeasonId = playback.seasonId,
    vodEpisodeId = playback.episodeId,
    vodPlaybackRef = playback.playbackRef,
    vodTitle = title,
    vodSubtitle = subtitle,
    vodPosterUrl = posterUrl,
    vodIsSeries = isSeries,
)

/** A category as a TV guide. Names match LiveViewModel's args, which the guide shares. */
@Serializable
private data class GuideRoute(val profileId: Long, val categoryId: String)

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
                    onResume = { progress -> navController.navigate(progress.toRoute(profileId)) },
                    onOpenVodCategory = { category -> navController.navigate(VodRoute(profileId, category.id)) },
                    onSwitchProfile = { navController.popBackStack() },
                    onSettings = { navController.navigate(SettingsRoute) },
                ),
            )
        }
        liveTv(navController)
        onDemand(navController)
    }
}

private fun NavGraphBuilder.liveTv(navController: NavController) {
    composable<LiveRoute> { entry ->
        val profileId = entry.toRoute<LiveRoute>().profileId
        LiveScreen(
            onPlay = { channel, categoryId, page ->
                navController.navigate(PlayerRoute(profileId, categoryId, page, channel.id))
            },
            onOpenGuide = { categoryId -> navController.navigate(GuideRoute(profileId, categoryId)) },
        )
    }
    composable<GuideRoute> { entry ->
        val profileId = entry.toRoute<GuideRoute>().profileId
        GuideScreen(
            onPlay = { channel, categoryId, page ->
                navController.navigate(PlayerRoute(profileId, categoryId, page, channel.id))
            },
        )
    }
    composable<PlayerRoute> {
        PlayerScreen(onExit = { navController.popBackStack() })
    }
}

private fun NavGraphBuilder.onDemand(navController: NavController) {
    composable<VodRoute> { entry ->
        val profileId = entry.toRoute<VodRoute>().profileId
        VodBrowseScreen(onOpen = { item -> navController.navigate(VodDetailRoute(profileId, item.id)) })
    }
    composable<VodDetailRoute> { entry ->
        val profileId = entry.toRoute<VodDetailRoute>().profileId
        VodDetailScreen(onPlay = { request -> navController.navigate(request.toRoute(profileId)) })
    }
    composable<VodPlayerRoute> {
        PlayerScreen(onExit = { navController.popBackStack() })
    }
}
