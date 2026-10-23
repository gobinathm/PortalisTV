package io.github.gobinathm.portalistv.core.data.backup

import io.github.gobinathm.portalistv.core.model.Compatibility
import io.github.gobinathm.portalistv.core.model.DeviceIdentity
import io.github.gobinathm.portalistv.core.model.DeviceIds
import io.github.gobinathm.portalistv.core.model.ExternalPlayerKeys
import io.github.gobinathm.portalistv.core.model.Firmware
import io.github.gobinathm.portalistv.core.model.Hardware
import io.github.gobinathm.portalistv.core.model.MulticastProtocol
import io.github.gobinathm.portalistv.core.model.Playback
import io.github.gobinathm.portalistv.core.model.PlayerEngine
import io.github.gobinathm.portalistv.core.model.Portal
import io.github.gobinathm.portalistv.core.model.Profile
import io.github.gobinathm.portalistv.core.model.Proxy
import io.github.gobinathm.portalistv.core.model.Secret
import kotlinx.serialization.Serializable

/**
 * The plaintext inside an encrypted backup. This is a file format, separate from the domain
 * model and the database schema: changing a field here needs a new [version] and a reader for
 * the old one. Database row ids are not exported; profiles are matched by uuid on import.
 */
@Serializable
internal data class BackupDocument(
    val version: Int,
    val exportedAtMillis: Long,
    val profiles: List<BackupProfile>,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

@Serializable
internal data class BackupProfile(
    val uuid: String,
    val name: String,
    val sortOrder: Int = 0,
    val color: Int? = null,
    val createdAtMillis: Long,
    val portal: BackupPortal,
    val proxy: BackupProxy = BackupProxy(),
    val device: BackupDevice,
    val deviceIds: BackupDeviceIds = BackupDeviceIds(),
    val firmware: BackupFirmware = BackupFirmware(),
    val hardware: BackupHardware = BackupHardware(),
    val compatibility: BackupCompatibility = BackupCompatibility(),
    val playback: BackupPlayback = BackupPlayback(),
)

@Serializable
internal data class BackupPortal(
    val url: String,
    val language: String = "en",
    val timezone: String? = null,
    val allowInsecureHttp: Boolean = false,
    val login: String? = null,
    val password: String? = null,
)

@Serializable
internal data class BackupProxy(
    val enabled: Boolean = false,
    val host: String = "",
    val port: Int? = null,
    val username: String? = null,
    val password: String? = null,
)

@Serializable
internal data class BackupDevice(
    val stbModel: String,
    val userAgent: String? = null,
    val displayResolution: String = "1920x1080",
    val mac: String,
    val serial: String? = null,
)

@Serializable
internal data class BackupDeviceIds(
    val sendDeviceId: Boolean = false,
    val deviceId: String? = null,
    val deviceIdSeed: String? = null,
    val macBasedDeviceId: Boolean = false,
    val deviceId2: String? = null,
    val signature: String? = null,
)

@Serializable
internal data class BackupFirmware(
    val presetId: String? = null,
    val playerEngineVersion: String? = null,
    val jsApiVersion: String? = null,
    val stbApiVersion: String? = null,
    val imageVersion: String? = null,
    val imageDescription: String? = null,
    val imageDate: String? = null,
)

@Serializable
internal data class BackupHardware(val vendor: String? = null, val version: String? = null)

@Serializable
internal data class BackupCompatibility(
    val ministraMode: Boolean = false,
    val allowEmulatorDetection: Boolean = false,
)

@Serializable
internal data class BackupPlayback(
    val engine: PlayerEngine = PlayerEngine.VLC,
    val udpxyEnabled: Boolean = false,
    val udpxyUrl: String? = null,
    val udpxyProtocol: MulticastProtocol? = null,
    val externalKeyBack: Boolean = false,
    val externalKeyExit: Boolean = false,
    val externalKeyOk: Boolean = false,
)

internal fun Profile.toBackup() = BackupProfile(
    uuid = uuid,
    name = name,
    sortOrder = sortOrder,
    color = color,
    createdAtMillis = createdAtMillis,
    portal = with(portal) {
        BackupPortal(url.value, language, timezone, allowInsecureHttp, login?.value, password?.value)
    },
    proxy = with(proxy) { BackupProxy(enabled, host, port, username?.value, password?.value) },
    device = with(device) { BackupDevice(stbModel, userAgent, displayResolution, mac.value, serial?.value) },
    deviceIds = with(deviceIds) {
        BackupDeviceIds(
            sendDeviceId,
            deviceId?.value,
            deviceIdSeed?.value,
            macBasedDeviceId,
            deviceId2?.value,
            signature?.value,
        )
    },
    firmware = with(firmware) {
        BackupFirmware(
            presetId,
            playerEngineVersion,
            jsApiVersion,
            stbApiVersion,
            imageVersion,
            imageDescription,
            imageDate,
        )
    },
    hardware = BackupHardware(hardware.vendor, hardware.version),
    compatibility = BackupCompatibility(compatibility.ministraMode, compatibility.allowEmulatorDetection),
    playback = with(playback) {
        BackupPlayback(
            engine,
            udpxyEnabled,
            udpxyUrl,
            udpxyProtocol,
            externalPlayerKeys.back,
            externalPlayerKeys.exit,
            externalPlayerKeys.ok,
        )
    },
)

/** A profile not yet saved on this device (id 0). */
internal fun BackupProfile.toModel() = Profile(
    uuid = uuid,
    name = name,
    sortOrder = sortOrder,
    color = color,
    createdAtMillis = createdAtMillis,
    portal = with(portal) {
        Portal(Secret(url), language, timezone, allowInsecureHttp, login?.let(::Secret), password?.let(::Secret))
    },
    proxy = with(proxy) { Proxy(enabled, host, port, username?.let(::Secret), password?.let(::Secret)) },
    device = with(device) {
        DeviceIdentity(stbModel, userAgent, displayResolution, Secret(mac), serial?.let(::Secret))
    },
    deviceIds = with(deviceIds) {
        DeviceIds(
            sendDeviceId,
            deviceId?.let(::Secret),
            deviceIdSeed?.let(::Secret),
            macBasedDeviceId,
            deviceId2?.let(::Secret),
            signature?.let(::Secret),
        )
    },
    firmware = with(firmware) {
        Firmware(presetId, playerEngineVersion, jsApiVersion, stbApiVersion, imageVersion, imageDescription, imageDate)
    },
    hardware = Hardware(hardware.vendor, hardware.version),
    compatibility = Compatibility(compatibility.ministraMode, compatibility.allowEmulatorDetection),
    playback = with(playback) {
        Playback(
            engine,
            udpxyEnabled,
            udpxyUrl,
            udpxyProtocol,
            ExternalPlayerKeys(externalKeyBack, externalKeyExit, externalKeyOk),
        )
    },
)
