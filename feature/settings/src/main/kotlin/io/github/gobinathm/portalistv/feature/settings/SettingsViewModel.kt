package io.github.gobinathm.portalistv.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gobinathm.portalistv.core.data.ProfileRepository
import io.github.gobinathm.portalistv.core.datastore.AppSettings
import io.github.gobinathm.portalistv.core.datastore.SettingsRepository
import io.github.gobinathm.portalistv.core.player.PlayerSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StartProfile(val uuid: String, val name: String)

/** App-wide settings. Changes are saved at once and apply to the next channel played. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    profileRepository: ProfileRepository,
) : ViewModel() {
    /** Null until the stored settings are read, so the screen never shows defaults that aren't saved. */
    val settings: StateFlow<AppSettings?> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    /** Profiles that can open at start, as uuid and name. */
    val profiles: StateFlow<List<StartProfile>> = profileRepository.profiles
        .map { list -> list.map { StartProfile(it.uuid, it.name) } }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun setStartProfile(uuid: String?) = update { it.copy(startProfileUuid = uuid) }

    fun updatePlayer(transform: (PlayerSettings) -> PlayerSettings) = update { it.copy(player = transform(it.player)) }

    fun setPictureInPicture(enabled: Boolean) = update { it.copy(pictureInPicture = enabled) }

    fun setShowAdultContent(enabled: Boolean) = update { it.copy(showAdultContent = enabled) }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repository.update(transform) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
