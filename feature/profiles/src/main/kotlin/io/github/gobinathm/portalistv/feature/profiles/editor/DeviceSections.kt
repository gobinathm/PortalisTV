package io.github.gobinathm.portalistv.feature.profiles.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import io.github.gobinathm.portalistv.core.data.ProfileError
import io.github.gobinathm.portalistv.core.designsystem.component.ChoiceOption
import io.github.gobinathm.portalistv.core.model.Secret
import io.github.gobinathm.portalistv.feature.profiles.R
import io.github.gobinathm.portalistv.feature.profiles.component.ActionRow
import io.github.gobinathm.portalistv.feature.profiles.component.SectionHeader
import io.github.gobinathm.portalistv.feature.profiles.component.SwitchRow

@Composable
internal fun DeviceSection(editor: EditorController, modifier: Modifier) {
    val device = editor.draft.device
    val catalog = editor.catalog
    val model = catalog.model(device.stbModel)
    val custom = stringResource(R.string.editor_custom)
    Column(modifier) {
        SectionHeader(stringResource(R.string.section_device))
        ChoiceRow(
            editor,
            R.string.field_stb_model,
            catalog.models.map { ChoiceOption(it.id, it.id) },
            device.stbModel,
            editor.viewModel::selectModel,
        )
        ChoiceRow(
            editor,
            R.string.field_firmware,
            model?.let(catalog::firmwareFor).orEmpty().map { ChoiceOption(it.id, it.imageDescription) } +
                ChoiceOption(CUSTOM_KEY, custom),
            editor.draft.firmware.presetId ?: CUSTOM_KEY,
            { key -> editor.viewModel.selectFirmware(key.takeUnless { it == CUSTOM_KEY }) },
        )
        ChoiceRow(
            editor,
            R.string.field_user_agent,
            listOf(
                ChoiceOption(DEFAULT_KEY, stringResource(R.string.editor_default)),
                ChoiceOption(CUSTOM_KEY, custom),
            ),
            if (device.userAgent == null) DEFAULT_KEY else CUSTOM_KEY,
            { key -> editor.viewModel.selectCustomUserAgent(key == CUSTOM_KEY) },
        )
        if (device.userAgent != null) {
            TextRow(editor, R.string.field_custom_user_agent, device.userAgent, { p, v ->
                p.copy(device = p.device.copy(userAgent = v.orNull()))
            })
        }
        ChoiceRow(
            editor,
            R.string.field_resolution,
            catalog.displayResolutions.map { ChoiceOption(it, it) },
            device.displayResolution,
            { key -> editor.update { it.copy(device = it.device.copy(displayResolution = key)) } },
        )
        MacRows(editor)
        TextRow(editor, R.string.field_serial, device.serial?.value, { p, v ->
            p.copy(device = p.device.copy(serial = v.orNull()?.let(::Secret)))
        })
    }
}

@Composable
internal fun DeviceIdSection(editor: EditorController, modifier: Modifier) {
    val ids = editor.draft.deviceIds
    Column(modifier) {
        SectionHeader(stringResource(R.string.section_device_ids))
        SwitchRow(
            stringResource(R.string.field_send_device_id),
            ids.sendDeviceId,
            { on -> editor.update { it.copy(deviceIds = it.deviceIds.copy(sendDeviceId = on)) } },
        )
        TextRow(editor, R.string.field_device_id, ids.deviceId?.value, { p, v ->
            p.copy(deviceIds = p.deviceIds.copy(deviceId = v.orNull()?.let(::Secret)))
        })
        TextRow(editor, R.string.field_device_id2, ids.deviceId2?.value, { p, v ->
            p.copy(deviceIds = p.deviceIds.copy(deviceId2 = v.orNull()?.let(::Secret)))
        })
        TextRow(editor, R.string.field_signature, ids.signature?.value, { p, v ->
            p.copy(deviceIds = p.deviceIds.copy(signature = v.orNull()?.let(::Secret)))
        })
        SwitchRow(
            stringResource(R.string.field_mac_based_device_id),
            ids.macBasedDeviceId,
            { on -> editor.update { it.copy(deviceIds = it.deviceIds.copy(macBasedDeviceId = on)) } },
        )
        ActionRow(
            stringResource(R.string.field_reset_device_ids),
            editor.viewModel::resetDeviceIds,
            hint = stringResource(R.string.field_reset_device_ids_hint),
        )
    }
}

@Composable
internal fun FirmwareSection(editor: EditorController, modifier: Modifier) {
    val firmware = editor.draft.firmware
    Column(modifier) {
        SectionHeader(stringResource(R.string.section_firmware))
        TextRow(editor, R.string.field_player_engine_version, firmware.playerEngineVersion, { p, v ->
            p.copy(firmware = p.firmware.copy(playerEngineVersion = v.orNull()))
        }, hint = R.string.field_player_engine_version_hint)
        TextRow(editor, R.string.field_js_api_version, firmware.jsApiVersion, { p, v ->
            p.copy(firmware = p.firmware.copy(jsApiVersion = v.orNull()))
        })
        TextRow(editor, R.string.field_stb_api_version, firmware.stbApiVersion, { p, v ->
            p.copy(firmware = p.firmware.copy(stbApiVersion = v.orNull()))
        })
        TextRow(editor, R.string.field_image_version, firmware.imageVersion, { p, v ->
            p.copy(firmware = p.firmware.copy(imageVersion = v.orNull()))
        })
        TextRow(editor, R.string.field_image_description, firmware.imageDescription, { p, v ->
            p.copy(firmware = p.firmware.copy(imageDescription = v.orNull()))
        })
        TextRow(editor, R.string.field_image_date, firmware.imageDate, { p, v ->
            p.copy(firmware = p.firmware.copy(imageDate = v.orNull()))
        })
    }
}

@Composable
private fun MacRows(editor: EditorController) {
    TextRow(
        editor,
        R.string.field_mac,
        editor.draft.device.mac.value,
        { p, v -> p.copy(device = p.device.copy(mac = Secret(v))) },
        keyboardType = KeyboardType.Ascii,
        error = errorText(editor, ProfileError.MAC_INVALID to R.string.error_mac_invalid),
    )
    ActionRow(
        stringResource(R.string.field_mac_generate),
        editor.viewModel::generateMac,
        hint = stringResource(R.string.field_mac_generate_hint),
    )
}
