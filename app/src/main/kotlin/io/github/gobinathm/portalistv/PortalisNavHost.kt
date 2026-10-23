package io.github.gobinathm.portalistv

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.gobinathm.portalistv.feature.home.HomeScreen
import io.github.gobinathm.portalistv.feature.home.HomeViewModel
import io.github.gobinathm.portalistv.feature.profiles.editor.ProfileEditorScreen
import io.github.gobinathm.portalistv.feature.profiles.editor.ProfileEditorViewModel
import io.github.gobinathm.portalistv.feature.profiles.list.ProfilesScreen
import kotlinx.serialization.Serializable

@Serializable
private data object ProfilesRoute

/**
 * Edits a profile, or creates one when [profileId] is 0. The property name is the argument key
 * that [ProfileEditorViewModel.PROFILE_ID_ARG] reads.
 */
@Serializable
private data class ProfileEditorRoute(val profileId: Long = 0)

/** The property name is the argument key that [HomeViewModel.PROFILE_ID_ARG] reads. */
@Serializable
private data class HomeRoute(val profileId: Long)

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
            )
        }
        composable<ProfileEditorRoute> {
            ProfileEditorScreen(onDone = { navController.popBackStack() })
        }
        composable<HomeRoute> {
            HomeScreen()
        }
    }
}
