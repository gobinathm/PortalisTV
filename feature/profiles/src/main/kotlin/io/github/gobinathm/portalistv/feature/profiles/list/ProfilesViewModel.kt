package io.github.gobinathm.portalistv.feature.profiles.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gobinathm.portalistv.core.common.log.Log
import io.github.gobinathm.portalistv.core.data.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProfileSummary(val id: Long, val name: String, val model: String)

sealed interface ProfilesUiState {
    data object Loading : ProfilesUiState

    data class Loaded(val profiles: List<ProfileSummary>) : ProfilesUiState

    /** The database exists but cannot be decrypted. */
    data object Unavailable : ProfilesUiState
}

@HiltViewModel
class ProfilesViewModel @Inject constructor(repository: ProfileRepository) : ViewModel() {
    val state: StateFlow<ProfilesUiState> = repository.profiles
        .map<_, ProfilesUiState> { profiles ->
            ProfilesUiState.Loaded(profiles.map { ProfileSummary(it.id, it.name, it.device.stbModel) })
        }
        .catch { e ->
            Log.e(TAG, e) { "Profiles could not be loaded" }
            emit(ProfilesUiState.Unavailable)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ProfilesUiState.Loading)

    private companion object {
        const val TAG = "Profiles"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
