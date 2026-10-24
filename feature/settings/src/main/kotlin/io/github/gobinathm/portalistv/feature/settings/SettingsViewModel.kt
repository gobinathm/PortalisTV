package io.github.gobinathm.portalistv.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gobinathm.portalistv.core.datastore.AppSettings
import io.github.gobinathm.portalistv.core.datastore.SettingsRepository
import io.github.gobinathm.portalistv.core.player.PlayerSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** App-wide settings. Changes are saved at once and apply to the next channel played. */
@HiltViewModel
class SettingsViewModel @Inject constructor(private val repository: SettingsRepository) : ViewModel() {
    /** Null until the stored settings are read, so the screen never shows defaults that aren't saved. */
    val settings: StateFlow<AppSettings?> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

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
