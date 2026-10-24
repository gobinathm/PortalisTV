package io.github.gobinathm.portalistv.feature.profiles.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gobinathm.portalistv.core.common.log.Log
import io.github.gobinathm.portalistv.core.data.ProfileRepository
import io.github.gobinathm.portalistv.core.data.usesPlainHttp
import io.github.gobinathm.portalistv.core.datastore.SettingsRepository
import io.github.gobinathm.portalistv.core.model.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileSummary(
    val id: Long,
    val name: String,
    val model: String,
    val notSecure: Boolean = false,
    /** Chosen in Settings to open when the app starts. */
    val opensAtStart: Boolean = false,
)

sealed interface ProfilesUiState {
    data object Loading : ProfilesUiState

    data class Loaded(val profiles: List<ProfileSummary>) : ProfilesUiState

    /** The database exists but cannot be decrypted. */
    data object Unavailable : ProfilesUiState
}

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ProfileRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<ProfilesUiState> = combine(repository.profiles, settings.settings) { profiles, app ->
        ProfilesUiState.Loaded(
            profiles.map {
                ProfileSummary(
                    id = it.id,
                    name = it.name,
                    model = it.device.stbModel,
                    notSecure = it.usesPlainHttp,
                    opensAtStart = profiles.size > 1 && it.uuid == app.startProfileUuid,
                )
            },
        ) as ProfilesUiState
    }
        .catch { e ->
            Log.e(TAG, e) { "Profiles could not be loaded" }
            emit(ProfilesUiState.Unavailable)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ProfilesUiState.Loading)

    private val _openAtStart = MutableStateFlow<Long?>(null)

    /** The profile to open straight away, once per app start; null when the list should show. */
    val openAtStart: StateFlow<Long?> = _openAtStart.asStateFlow()

    init {
        if (savedStateHandle.get<Boolean>(KEY_START_HANDLED) != true) {
            viewModelScope.launch {
                val profiles = runCatching { repository.profiles.first() }.getOrDefault(emptyList())
                val startUuid = settings.settings.first().startProfileUuid
                _openAtStart.value = startProfile(profiles, startUuid)?.id
                savedStateHandle[KEY_START_HANDLED] = true
            }
        }
    }

    fun onStartProfileOpened() {
        _openAtStart.value = null
    }

    companion object {
        private const val TAG = "Profiles"
        private const val STOP_TIMEOUT_MS = 5_000L
        private const val KEY_START_HANDLED = "startHandled"

        /** A single profile always opens; with several, the one chosen in Settings, if any. */
        internal fun startProfile(profiles: List<Profile>, startUuid: String?): Profile? =
            profiles.singleOrNull() ?: profiles.firstOrNull { it.uuid == startUuid }
    }
}
