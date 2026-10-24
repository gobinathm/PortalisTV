package io.github.gobinathm.portalistv.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import io.github.gobinathm.portalistv.core.data.PinResult
import io.github.gobinathm.portalistv.core.datastore.AppSettings
import io.github.gobinathm.portalistv.core.designsystem.component.ChoiceDialog
import io.github.gobinathm.portalistv.core.designsystem.component.ChoiceOption
import io.github.gobinathm.portalistv.core.designsystem.component.PinDialog
import io.github.gobinathm.portalistv.core.designsystem.component.SectionHeader
import io.github.gobinathm.portalistv.core.designsystem.component.SwitchRow
import io.github.gobinathm.portalistv.core.designsystem.component.ValueRow
import io.github.gobinathm.portalistv.core.designsystem.component.pausedMessage
import kotlinx.coroutines.launch
import io.github.gobinathm.portalistv.core.designsystem.R as DesignR

/** What happens once the current PIN has been entered. */
private enum class AfterPin { CHANGE, REMOVE, SHOW_ADULT, TOGGLE_LOCK }

/** The steps of the PIN dialogs. */
private sealed interface PinStep {
    data class Verify(val then: AfterPin) : PinStep

    data object NewPin : PinStep

    data class Confirm(val first: String) : PinStep
}

/** The PIN dialogs' state: which step is showing and what went wrong last. */
private class PinFlow(
    private val hasPin: Boolean,
    private val unlocked: () -> Boolean,
    private val run: (AfterPin) -> Unit,
) {
    var step by mutableStateOf<PinStep?>(null)
    var error by mutableStateOf<String?>(null)

    /** Runs [action] at once if no PIN is needed, else asks for the current PIN first. */
    fun guarded(action: AfterPin) {
        if (!hasPin || unlocked()) done(action) else step = PinStep.Verify(action)
    }

    fun done(action: AfterPin) {
        step = null
        error = null
        if (action == AfterPin.CHANGE) step = PinStep.NewPin else run(action)
    }

    fun close() {
        step = null
        error = null
    }
}

/**
 * Parental controls: the PIN, whether it also locks Settings and profiles, and adult categories.
 * Changing any of them asks for the current PIN first, unless it was entered since the app started.
 */
@Composable
internal fun ParentalSection(settings: AppSettings, viewModel: SettingsViewModel) {
    val hasPin = settings.parental.pin != null
    val flow = remember(hasPin) {
        PinFlow(hasPin, unlocked = { viewModel.unlocked.value }) { action -> viewModel.apply(action) }
    }
    var menuOpen by remember { mutableStateOf(false) }
    Column {
        SectionHeader(stringResource(R.string.settings_section_parental))
        ValueRow(
            label = stringResource(R.string.settings_pin),
            value = stringResource(if (hasPin) R.string.settings_pin_on else R.string.settings_pin_off),
            onClick = { if (hasPin) menuOpen = true else flow.step = PinStep.NewPin },
        )
        SwitchRow(
            label = stringResource(R.string.settings_lock_settings),
            checked = settings.parental.lockSettings,
            onCheckedChange = { if (hasPin) flow.guarded(AfterPin.TOGGLE_LOCK) },
            hint = stringResource(
                if (hasPin) R.string.settings_lock_settings_hint else R.string.settings_lock_needs_pin,
            ),
        )
        SwitchRow(
            label = stringResource(R.string.settings_show_adult),
            checked = settings.showAdultContent,
            onCheckedChange = { on ->
                if (on) flow.guarded(AfterPin.SHOW_ADULT) else viewModel.setShowAdultContent(false)
            },
            hint = stringResource(R.string.settings_show_adult_hint),
        )
    }
    if (menuOpen) {
        PinMenu(onPick = { flow.guarded(it) }, onClose = { menuOpen = false })
    }
    PinSteps(flow, viewModel)
}

private fun SettingsViewModel.apply(action: AfterPin) = when (action) {
    AfterPin.CHANGE -> Unit
    AfterPin.REMOVE -> removePin()
    AfterPin.SHOW_ADULT -> setShowAdultContent(true)
    AfterPin.TOGGLE_LOCK -> update { it.copy(parental = it.parental.copy(lockSettings = !it.parental.lockSettings)) }
}

@Composable
private fun PinMenu(onPick: (AfterPin) -> Unit, onClose: () -> Unit) {
    ChoiceDialog(
        title = stringResource(R.string.settings_pin),
        options = listOf(
            ChoiceOption(AfterPin.CHANGE.name, stringResource(R.string.settings_pin_change)),
            ChoiceOption(AfterPin.REMOVE.name, stringResource(R.string.settings_pin_remove)),
        ),
        selectedKey = null,
        onSelect = { key ->
            onClose()
            onPick(AfterPin.valueOf(key))
        },
        onDismiss = onClose,
    )
}

@Composable
private fun PinSteps(flow: PinFlow, viewModel: SettingsViewModel) {
    val scope = rememberCoroutineScope()
    val wrong = stringResource(DesignR.string.pin_wrong)
    val mismatch = stringResource(R.string.settings_pin_mismatch)
    val paused = pausedMessage()
    when (val step = flow.step) {
        null -> Unit
        is PinStep.Verify -> PinDialog(
            title = stringResource(DesignR.string.pin_enter),
            error = flow.error,
            onSubmit = { pin ->
                scope.launch {
                    when (val result = viewModel.checkPin(pin)) {
                        PinResult.Correct -> flow.done(step.then)
                        PinResult.Wrong -> flow.error = wrong
                        is PinResult.Paused -> flow.error = paused(result.seconds)
                    }
                }
            },
            onDismiss = flow::close,
        )
        PinStep.NewPin -> PinDialog(
            title = stringResource(R.string.settings_pin_new),
            message = stringResource(R.string.settings_pin_new_hint),
            error = flow.error,
            onSubmit = { pin ->
                flow.error = null
                flow.step = PinStep.Confirm(pin)
            },
            onDismiss = flow::close,
        )
        is PinStep.Confirm -> PinDialog(
            title = stringResource(R.string.settings_pin_confirm),
            error = flow.error,
            onSubmit = { pin ->
                if (pin == step.first) {
                    viewModel.setPin(pin)
                    flow.close()
                } else {
                    // Start over: a typo in either entry is as likely as in the second.
                    flow.step = PinStep.NewPin
                    flow.error = mismatch
                }
            },
            onDismiss = flow::close,
        )
    }
}
