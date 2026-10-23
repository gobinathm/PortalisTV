package io.github.gobinathm.portalistv.core.data.source

import io.github.gobinathm.portalistv.core.catalog.DeviceCatalog
import io.github.gobinathm.portalistv.core.catalog.resolve
import io.github.gobinathm.portalistv.core.model.Profile
import io.github.gobinathm.portalistv.core.network.ProxyConfig
import io.github.gobinathm.portalistv.core.stalker.DeviceIdentity
import io.github.gobinathm.portalistv.core.stalker.DeviceIds
import io.github.gobinathm.portalistv.core.stalker.StalkerConfig
import java.security.MessageDigest
import java.util.TimeZone

/**
 * Turns a profile into what the portal client sends.
 *
 * Identifiers the user entered are sent as is. Missing ones are derived from the profile's random
 * seed (or from the MAC address when the profile asks for MAC-based ids), so they stay stable
 * for a profile and never come from the TV's own hardware identifiers.
 */
class PortalConfig(
    private val catalog: DeviceCatalog,
    private val deviceTimeZone: () -> String = { TimeZone.getDefault().id },
) {
    fun stalkerConfig(profile: Profile): StalkerConfig {
        val reported = catalog.resolve(profile)
        return StalkerConfig(
            portalUrl = profile.portal.url.value,
            allowInsecureHttp = profile.portal.allowInsecureHttp,
            mac = profile.device.mac.value,
            serial = profile.device.serial?.value ?: derivedSerial(profile),
            language = profile.portal.language,
            timezone = profile.portal.timezone ?: deviceTimeZone(),
            identity = DeviceIdentity(
                stbModel = reported.stbModel,
                userAgent = reported.userAgent,
                imageVersion = reported.imageVersion,
                imageDate = reported.imageDate,
                imageDescription = reported.imageDescription,
                jsApiVersion = reported.jsApiVersion,
                stbApiVersion = reported.stbApiVersion,
                playerEngineVersion = reported.playerEngineVersion,
                hardwareVersion = profile.hardware.version,
            ),
            deviceIds = deviceIds(profile),
            login = profile.portal.login?.value,
            password = profile.portal.password?.value,
        )
    }

    fun proxy(profile: Profile): ProxyConfig? = with(profile.proxy) {
        val port = port
        if (!enabled || host.isBlank() || port == null) {
            null
        } else {
            ProxyConfig(host, port, username?.value, password?.value)
        }
    }

    private fun deviceIds(profile: Profile): DeviceIds? {
        val ids = profile.deviceIds
        if (!ids.sendDeviceId) return null
        val source = if (ids.macBasedDeviceId) profile.device.mac.value.uppercase() else seed(profile)
        return DeviceIds(
            deviceId = ids.deviceId?.value ?: sha256Hex("device_id:$source"),
            deviceId2 = ids.deviceId2?.value ?: sha256Hex("device_id2:$source"),
            signature = ids.signature?.value ?: sha256Hex("signature:$source"),
        )
    }

    /** Set-top box serials are 13 characters: a fixed prefix and five digits. */
    private fun derivedSerial(profile: Profile): String {
        val digest = MessageDigest.getInstance("SHA-256").digest("serial:${seed(profile)}".encodeToByteArray())
        val number = digest.take(Int.SIZE_BYTES)
            .fold(0L) { acc, byte -> (acc shl Byte.SIZE_BITS) or (byte.toLong() and BYTE_MASK) }
        return SERIAL_PREFIX + (number % SERIAL_RANGE).toString().padStart(SERIAL_DIGITS, '0')
    }

    /** Profiles created before seeds existed fall back to their uuid, which is also random. */
    private fun seed(profile: Profile): String = profile.deviceIds.deviceIdSeed?.value ?: profile.uuid

    private fun sha256Hex(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.encodeToByteArray()).joinToString("") { "%02X".format(it) }

    private companion object {
        const val SERIAL_PREFIX = "062014N0"
        const val SERIAL_DIGITS = 5
        const val SERIAL_RANGE = 100_000L
        const val BYTE_MASK = 0xFFL
    }
}
