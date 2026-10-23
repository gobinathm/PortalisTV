package io.github.gobinathm.portalistv.feature.profiles.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gobinathm.portalistv.core.catalog.DeviceCatalog
import io.github.gobinathm.portalistv.core.catalog.resolve
import io.github.gobinathm.portalistv.core.common.log.Log
import io.github.gobinathm.portalistv.core.data.ProfileDefaults
import io.github.gobinathm.portalistv.core.data.ProfileError
import io.github.gobinathm.portalistv.core.data.ProfileRepository
import io.github.gobinathm.portalistv.core.data.normalized
import io.github.gobinathm.portalistv.core.data.validate
import io.github.gobinathm.portalistv.core.model.DeviceIds
import io.github.gobinathm.portalistv.core.model.Firmware
import io.github.gobinathm.portalistv.core.model.Profile
import io.github.gobinathm.portalistv.core.model.Secret
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorUiState(
    /** Null while loading. */
    val draft: Profile? = null,
    val isNew: Boolean = true,
    val errors: Set<ProfileError> = emptySet(),
    /** Errors are shown only after the first save attempt. */
    val showErrors: Boolean = false,
    val busy: Boolean = false,
    val saveFailed: Boolean = false,
    /** The editor should close: saved, deleted, or the profile no longer exists. */
    val finished: Boolean = false,
)

@HiltViewModel
class ProfileEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProfileRepository,
    private val defaults: ProfileDefaults,
    val catalog: DeviceCatalog,
) : ViewModel() {
    private val profileId: Long = savedStateHandle[PROFILE_ID_ARG] ?: 0L

    private val _state = MutableStateFlow(EditorUiState(isNew = profileId == 0L))
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = if (profileId == 0L) defaults.newProfile() else repository.get(profileId)
            _state.update {
                if (profile == null) it.copy(finished = true) else it.copy(draft = profile, errors = check(profile))
            }
        }
    }

    fun update(transform: (Profile) -> Profile) {
        _state.update { state ->
            val draft = state.draft?.let(transform) ?: return@update state
            state.copy(draft = draft, errors = check(draft), saveFailed = false)
        }
    }

    /** Switching models resets a firmware preset to the new model's default; custom firmware is kept. */
    fun selectModel(modelId: String) {
        val model = catalog.model(modelId) ?: return
        update { profile ->
            val firmware = if (profile.firmware.presetId == null) {
                profile.firmware
            } else {
                Firmware(presetId = model.defaultFirmwareId)
            }
            profile.copy(device = profile.device.copy(stbModel = model.id), firmware = firmware)
        }
    }

    /** Selects a firmware preset, or custom firmware (null) prefilled with the values in effect. */
    fun selectFirmware(presetId: String?) = update { profile ->
        if (presetId != null) {
            profile.copy(firmware = Firmware(presetId = presetId))
        } else {
            val current = catalog.resolve(profile)
            profile.copy(
                firmware = Firmware(
                    presetId = null,
                    playerEngineVersion = current.playerEngineVersion,
                    jsApiVersion = current.jsApiVersion,
                    stbApiVersion = current.stbApiVersion,
                    imageVersion = current.imageVersion,
                    imageDescription = current.imageDescription,
                    imageDate = current.imageDate,
                ),
            )
        }
    }

    /** Uses the preset user agent (null) or a custom one prefilled with the current value. */
    fun selectCustomUserAgent(custom: Boolean) = update { profile ->
        val userAgent = if (custom) catalog.resolve(profile).userAgent else null
        profile.copy(device = profile.device.copy(userAgent = userAgent))
    }

    fun generateMac() = update { it.copy(device = it.device.copy(mac = Secret(defaults.randomMac()))) }

    fun resetDeviceIds() = update { profile ->
        profile.copy(
            deviceIds = DeviceIds(
                sendDeviceId = profile.deviceIds.sendDeviceId,
                macBasedDeviceId = profile.deviceIds.macBasedDeviceId,
                deviceIdSeed = Secret(defaults.randomSeed()),
            ),
        )
    }

    fun save() {
        val state = _state.value
        val draft = state.draft?.normalized()
        val errors = draft?.validate().orEmpty()
        when {
            draft == null || state.busy -> Unit
            errors.isNotEmpty() -> _state.update { it.copy(draft = draft, errors = errors, showErrors = true) }
            else -> persist(draft)
        }
    }

    private fun persist(draft: Profile) {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val saved = runCatching { repository.save(draft) }
                .onFailure { Log.e(TAG, it) { "Profile could not be saved" } }
                .isSuccess
            _state.update { it.copy(busy = false, finished = saved, saveFailed = !saved) }
        }
    }

    fun delete() {
        if (profileId == 0L || _state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            repository.delete(profileId)
            _state.update { it.copy(busy = false, finished = true) }
        }
    }

    private fun check(profile: Profile) = profile.normalized().validate()

    companion object {
        /** Navigation argument holding the id of the profile to edit; absent or 0 creates one. */
        const val PROFILE_ID_ARG = "profileId"
        private const val TAG = "ProfileEditor"
    }
}
