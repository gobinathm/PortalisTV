package io.github.gobinathm.portalistv.core.data

import io.github.gobinathm.portalistv.core.catalog.DeviceCatalog
import io.github.gobinathm.portalistv.core.model.DeviceIdentity
import io.github.gobinathm.portalistv.core.model.DeviceIds
import io.github.gobinathm.portalistv.core.model.Firmware
import io.github.gobinathm.portalistv.core.model.Portal
import io.github.gobinathm.portalistv.core.model.Profile
import io.github.gobinathm.portalistv.core.model.Secret
import java.security.SecureRandom
import java.util.UUID

/**
 * Creates new profiles and the random identifiers they need. Identifiers are random per
 * profile and never derived from the device's hardware ids.
 */
class ProfileDefaults(
    private val catalog: DeviceCatalog,
    private val random: SecureRandom = SecureRandom(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun newProfile(): Profile {
        val model = requireNotNull(catalog.model(catalog.defaultModel))
        return Profile(
            uuid = UUID.randomUUID().toString(),
            name = "",
            createdAtMillis = clock(),
            portal = Portal(url = Secret("")),
            device = DeviceIdentity(stbModel = model.id, mac = Secret(randomMac())),
            deviceIds = DeviceIds(deviceIdSeed = Secret(randomSeed())),
            firmware = Firmware(presetId = model.defaultFirmwareId),
        )
    }

    /** A MAC address with the prefix portals expect; the provider must register it. */
    fun randomMac(): String {
        val suffix = ByteArray(MAC_SUFFIX_BYTES).also(random::nextBytes)
        return (MAC_PREFIX + suffix.map { "%02X".format(it) }).joinToString(":")
    }

    /** A fresh seed for device ids, used when a profile's device ids are reset. */
    fun randomSeed(): String = ByteArray(SEED_BYTES).also(random::nextBytes).joinToString("") { "%02x".format(it) }

    private companion object {
        val MAC_PREFIX = listOf("00", "1A", "79")
        const val MAC_SUFFIX_BYTES = 3
        const val SEED_BYTES = 16
    }
}
