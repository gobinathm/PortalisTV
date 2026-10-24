package io.github.gobinathm.portalistv.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.MaterialTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gobinathm.portalistv.core.data.ParentalLock
import io.github.gobinathm.portalistv.core.data.PinResult
import io.github.gobinathm.portalistv.core.datastore.SettingsRepository
import io.github.gobinathm.portalistv.core.designsystem.component.PinDialog
import io.github.gobinathm.portalistv.core.designsystem.component.backdrop
import io.github.gobinathm.portalistv.core.designsystem.component.pausedMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import io.github.gobinathm.portalistv.core.designsystem.R as DesignR

sealed interface GateState {
    data object Checking : GateState

    data object Open : GateState

    /** The PIN is needed; [error] says why the last one wasn't accepted. */
    data class Locked(val error: PinResult? = null) : GateState
}

/** Decides whether Settings and profile editing need the parental PIN, and checks it. */
@HiltViewModel
class ParentalGateViewModel @Inject constructor(
    private val lock: ParentalLock,
    settings: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<GateState>(GateState.Checking)
    val state: StateFlow<GateState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val parental = settings.settings.first().parental
            val locked = parental.lockSettings && parental.pin != null && !lock.unlocked.value
            _state.value = if (locked) GateState.Locked() else GateState.Open
        }
    }

    fun submit(pin: String) {
        viewModelScope.launch {
            val result = lock.check(pin)
            _state.value = if (result == PinResult.Correct) GateState.Open else GateState.Locked(result)
        }
    }
}

/**
 * Shows [content] only once the parental PIN has been entered, when Settings and profiles are
 * locked; otherwise at once. Dismissing the PIN dialog calls [onLeave].
 */
@Composable
fun ParentalGate(
    onLeave: () -> Unit,
    viewModel: ParentalGateViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val paused = pausedMessage()
    when (val current = state) {
        GateState.Open -> content()
        GateState.Checking -> Box(Modifier.fillMaxSize().backdrop(MaterialTheme.colorScheme))
        is GateState.Locked -> {
            Box(Modifier.fillMaxSize().backdrop(MaterialTheme.colorScheme))
            PinDialog(
                title = stringResource(DesignR.string.pin_enter),
                error = when (val error = current.error) {
                    PinResult.Wrong -> stringResource(DesignR.string.pin_wrong)
                    is PinResult.Paused -> paused(error.seconds)
                    else -> null
                },
                onSubmit = viewModel::submit,
                onDismiss = onLeave,
            )
        }
    }
}
