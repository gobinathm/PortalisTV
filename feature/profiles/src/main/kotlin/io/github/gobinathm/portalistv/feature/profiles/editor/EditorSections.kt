package io.github.gobinathm.portalistv.feature.profiles.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import io.github.gobinathm.portalistv.core.data.ProfileError
import io.github.gobinathm.portalistv.core.designsystem.component.ChoiceOption
import io.github.gobinathm.portalistv.core.model.ExternalPlayerKeys
import io.github.gobinathm.portalistv.core.model.MulticastProtocol
import io.github.gobinathm.portalistv.core.model.PlayerEngine
import io.github.gobinathm.portalistv.core.model.Secret
import io.github.gobinathm.portalistv.feature.profiles.R
import io.github.gobinathm.portalistv.feature.profiles.component.SectionHeader
import io.github.gobinathm.portalistv.feature.profiles.component.SwitchRow

internal fun LazyListScope.editorSections(editor: EditorController, firstField: FocusRequester, modifier: Modifier) {
    item(key = "general") { GeneralSection(editor, firstField, modifier) }
    item(key = "portal") { PortalSection(editor, modifier) }
    item(key = "proxy") { ProxySection(editor, modifier) }
    item(key = "device") { DeviceSection(editor, modifier) }
    item(key = "device-ids") { DeviceIdSection(editor, modifier) }
    if (editor.draft.firmware.presetId == null) {
        item(key = "firmware") { FirmwareSection(editor, modifier) }
    }
    item(key = "hardware") { HardwareSection(editor, modifier) }
    item(key = "compatibility") { CompatibilitySection(editor, modifier) }
    item(key = "playback") { PlaybackSection(editor, modifier) }
}

@Composable
private fun GeneralSection(editor: EditorController, firstField: FocusRequester, modifier: Modifier) {
    Column(modifier) {
        SectionHeader(stringResource(R.string.section_general))
        TextRow(
            editor,
            R.string.field_name,
            editor.draft.name,
            { p, v -> p.copy(name = v) },
            modifier = Modifier.focusRequester(firstField),
            error = errorText(editor, ProfileError.NAME_REQUIRED to R.string.error_name_required),
        )
    }
}

@Composable
private fun PortalSection(editor: EditorController, modifier: Modifier) {
    val portal = editor.draft.portal
    Column(modifier) {
        SectionHeader(stringResource(R.string.section_portal))
        TextRow(
            editor,
            R.string.field_portal_url,
            portal.url.value,
            { p, v -> p.copy(portal = p.portal.copy(url = Secret(v))) },
            keyboardType = KeyboardType.Uri,
            error = errorText(
                editor,
                ProfileError.URL_REQUIRED to R.string.error_url_required,
                ProfileError.URL_INVALID to R.string.error_url_invalid,
                ProfileError.URL_INSECURE to R.string.error_url_insecure,
            ),
        )
        SwitchRow(
            stringResource(R.string.field_allow_http),
            portal.allowInsecureHttp,
            { on -> editor.update { it.copy(portal = it.portal.copy(allowInsecureHttp = on)) } },
            hint = stringResource(R.string.field_allow_http_hint),
        )
        TextRow(editor, R.string.field_language, portal.language, { p, v ->
            p.copy(portal = p.portal.copy(language = v.trim().ifEmpty { "en" }))
        })
        TextRow(
            editor,
            R.string.field_timezone,
            portal.timezone,
            { p, v -> p.copy(portal = p.portal.copy(timezone = v.orNull())) },
            hint = R.string.field_timezone_hint,
            emptyText = R.string.editor_device_default,
        )
        TextRow(editor, R.string.field_login, portal.login?.value, { p, v ->
            p.copy(portal = p.portal.copy(login = v.orNull()?.let(::Secret)))
        })
        TextRow(editor, R.string.field_password, portal.password?.value, { p, v ->
            p.copy(portal = p.portal.copy(password = v.ifEmpty { null }?.let(::Secret)))
        }, secret = true)
    }
}

@Composable
private fun ProxySection(editor: EditorController, modifier: Modifier) {
    val proxy = editor.draft.proxy
    Column(modifier) {
        SectionHeader(stringResource(R.string.section_proxy))
        SwitchRow(
            stringResource(R.string.field_proxy_enabled),
            proxy.enabled,
            { on -> editor.update { it.copy(proxy = it.proxy.copy(enabled = on)) } },
        )
        if (!proxy.enabled) return@Column
        TextRow(
            editor,
            R.string.field_proxy_host,
            proxy.host,
            { p, v -> p.copy(proxy = p.proxy.copy(host = v)) },
            keyboardType = KeyboardType.Uri,
            error = errorText(editor, ProfileError.PROXY_HOST_REQUIRED to R.string.error_proxy_host_required),
        )
        TextRow(
            editor,
            R.string.field_proxy_port,
            proxy.port?.toString(),
            { p, v -> p.copy(proxy = p.proxy.copy(port = v.trim().toIntOrNull())) },
            keyboardType = KeyboardType.Number,
            error = errorText(editor, ProfileError.PROXY_PORT_INVALID to R.string.error_proxy_port_invalid),
        )
        TextRow(editor, R.string.field_proxy_username, proxy.username?.value, { p, v ->
            p.copy(proxy = p.proxy.copy(username = v.orNull()?.let(::Secret)))
        })
        TextRow(editor, R.string.field_proxy_password, proxy.password?.value, { p, v ->
            p.copy(proxy = p.proxy.copy(password = v.ifEmpty { null }?.let(::Secret)))
        }, secret = true)
    }
}

@Composable
private fun HardwareSection(editor: EditorController, modifier: Modifier) {
    val hardware = editor.draft.hardware
    Column(modifier) {
        SectionHeader(stringResource(R.string.section_hardware))
        TextRow(editor, R.string.field_hardware_vendor, hardware.vendor, { p, v ->
            p.copy(hardware = p.hardware.copy(vendor = v.orNull()))
        })
        TextRow(editor, R.string.field_hardware_version, hardware.version, { p, v ->
            p.copy(hardware = p.hardware.copy(version = v.orNull()))
        })
    }
}

@Composable
private fun CompatibilitySection(editor: EditorController, modifier: Modifier) {
    val compatibility = editor.draft.compatibility
    Column(modifier) {
        SectionHeader(stringResource(R.string.section_compatibility))
        SwitchRow(
            stringResource(R.string.field_ministra_mode),
            compatibility.ministraMode,
            { on -> editor.update { it.copy(compatibility = it.compatibility.copy(ministraMode = on)) } },
        )
        SwitchRow(
            stringResource(R.string.field_emulator_detection),
            compatibility.allowEmulatorDetection,
            { on -> editor.update { it.copy(compatibility = it.compatibility.copy(allowEmulatorDetection = on)) } },
        )
    }
}

@Composable
private fun PlaybackSection(editor: EditorController, modifier: Modifier) {
    val playback = editor.draft.playback
    Column(modifier) {
        SectionHeader(stringResource(R.string.section_playback))
        ChoiceRow(
            editor,
            R.string.field_player,
            listOf(
                ChoiceOption(PlayerEngine.VLC.name, stringResource(R.string.field_player_vlc)),
                ChoiceOption(PlayerEngine.EXTERNAL.name, stringResource(R.string.field_player_external)),
            ),
            playback.engine.name,
            { key -> editor.update { it.copy(playback = it.playback.copy(engine = PlayerEngine.valueOf(key))) } },
        )
        SwitchRow(
            stringResource(R.string.field_udpxy_enabled),
            playback.udpxyEnabled,
            { on -> editor.update { it.copy(playback = it.playback.copy(udpxyEnabled = on)) } },
        )
        if (playback.udpxyEnabled) {
            TextRow(
                editor,
                R.string.field_udpxy_url,
                playback.udpxyUrl,
                { p, v -> p.copy(playback = p.playback.copy(udpxyUrl = v.orNull())) },
                keyboardType = KeyboardType.Uri,
                error = errorText(editor, ProfileError.UDPXY_URL_INVALID to R.string.error_udpxy_url_invalid),
            )
            ChoiceRow(
                editor,
                R.string.field_udpxy_protocol,
                listOf(ChoiceOption(DEFAULT_KEY, stringResource(R.string.editor_portal_default))) +
                    MulticastProtocol.entries.map { ChoiceOption(it.name, it.name) },
                playback.udpxyProtocol?.name ?: DEFAULT_KEY,
                { key ->
                    val protocol = MulticastProtocol.entries.firstOrNull { it.name == key }
                    editor.update { it.copy(playback = it.playback.copy(udpxyProtocol = protocol)) }
                },
            )
        }
        if (playback.engine == PlayerEngine.EXTERNAL) ExternalKeyRows(editor)
    }
}

@Composable
private fun ExternalKeyRows(editor: EditorController) {
    val keys = editor.draft.playback.externalPlayerKeys
    fun setKeys(transform: (ExternalPlayerKeys) -> ExternalPlayerKeys) = editor.update {
        it.copy(playback = it.playback.copy(externalPlayerKeys = transform(it.playback.externalPlayerKeys)))
    }
    SwitchRow(stringResource(R.string.field_external_key_back), keys.back, { on -> setKeys { it.copy(back = on) } })
    SwitchRow(stringResource(R.string.field_external_key_exit), keys.exit, { on -> setKeys { it.copy(exit = on) } })
    SwitchRow(stringResource(R.string.field_external_key_ok), keys.ok, { on -> setKeys { it.copy(ok = on) } })
}
