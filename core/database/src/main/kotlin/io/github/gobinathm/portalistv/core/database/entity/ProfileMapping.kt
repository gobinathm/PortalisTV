package io.github.gobinathm.portalistv.core.database.entity

import io.github.gobinathm.portalistv.core.model.Compatibility
import io.github.gobinathm.portalistv.core.model.DeviceIdentity
import io.github.gobinathm.portalistv.core.model.DeviceIds
import io.github.gobinathm.portalistv.core.model.ExternalPlayerKeys
import io.github.gobinathm.portalistv.core.model.Firmware
import io.github.gobinathm.portalistv.core.model.Hardware
import io.github.gobinathm.portalistv.core.model.Playback
import io.github.gobinathm.portalistv.core.model.Portal
import io.github.gobinathm.portalistv.core.model.Profile
import io.github.gobinathm.portalistv.core.model.Proxy
import io.github.gobinathm.portalistv.core.model.Secret

fun ProfileEntity.toModel() = Profile(
    id = id,
    uuid = uuid,
    name = name,
    sortOrder = sortOrder,
    color = color,
    createdAtMillis = createdAtMillis,
    portal = with(portal) {
        Portal(
            url = Secret(url),
            language = language,
            timezone = timezone,
            allowInsecureHttp = allowInsecureHttp,
            login = login?.let(::Secret),
            password = password?.let(::Secret),
        )
    },
    proxy = with(proxy) {
        Proxy(enabled, host, port, username?.let(::Secret), password?.let(::Secret))
    },
    device = with(device) {
        DeviceIdentity(stbModel, userAgent, displayResolution, Secret(mac), serial?.let(::Secret))
    },
    deviceIds = with(deviceIds) {
        DeviceIds(
            sendDeviceId = sendDeviceId,
            deviceId = deviceId?.let(::Secret),
            deviceIdSeed = seed?.let(::Secret),
            macBasedDeviceId = macBasedDeviceId,
            deviceId2 = deviceId2?.let(::Secret),
            signature = signature?.let(::Secret),
        )
    },
    firmware = with(firmware) {
        Firmware(presetId, playerEngineVersion, jsApiVersion, stbApiVersion, imageVersion, imageDescription, imageDate)
    },
    hardware = Hardware(hardware.vendor, hardware.version),
    compatibility = Compatibility(compatibility.ministraMode, compatibility.allowEmulatorDetection),
    playback = with(playback) {
        Playback(
            engine = engine,
            udpxyEnabled = udpxyEnabled,
            udpxyUrl = udpxyUrl,
            udpxyProtocol = udpxyProtocol,
            externalPlayerKeys = ExternalPlayerKeys(externalKeyBack, externalKeyExit, externalKeyOk),
        )
    },
)

fun Profile.toEntity() = ProfileEntity(
    id = id,
    uuid = uuid,
    name = name,
    sortOrder = sortOrder,
    color = color,
    createdAtMillis = createdAtMillis,
    portal = with(portal) {
        PortalColumns(url.value, language, timezone, allowInsecureHttp, login?.value, password?.value)
    },
    proxy = with(proxy) {
        ProxyColumns(enabled, host, port, username?.value, password?.value)
    },
    device = with(device) {
        DeviceColumns(stbModel, userAgent, displayResolution, mac.value, serial?.value)
    },
    deviceIds = with(deviceIds) {
        DeviceIdColumns(
            sendDeviceId = sendDeviceId,
            deviceId = deviceId?.value,
            seed = deviceIdSeed?.value,
            macBasedDeviceId = macBasedDeviceId,
            deviceId2 = deviceId2?.value,
            signature = signature?.value,
        )
    },
    firmware = with(firmware) {
        FirmwareColumns(
            presetId,
            playerEngineVersion,
            jsApiVersion,
            stbApiVersion,
            imageVersion,
            imageDescription,
            imageDate,
        )
    },
    hardware = HardwareColumns(hardware.vendor, hardware.version),
    compatibility = CompatibilityColumns(compatibility.ministraMode, compatibility.allowEmulatorDetection),
    playback = with(playback) {
        PlaybackColumns(
            engine = engine,
            udpxyEnabled = udpxyEnabled,
            udpxyUrl = udpxyUrl,
            udpxyProtocol = udpxyProtocol,
            externalKeyBack = externalPlayerKeys.back,
            externalKeyExit = externalPlayerKeys.exit,
            externalKeyOk = externalPlayerKeys.ok,
        )
    },
)
